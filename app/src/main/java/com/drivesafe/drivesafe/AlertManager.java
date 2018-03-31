package com.drivesafe.drivesafe;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;


import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class AlertManager {
    private static final String TAG = "Alert Manager";
    private static double bandAlertnessScore;
    private static double pictureAlertnessScore;
    private static boolean bandDisabled = false;
    private MainActivity mainActivity;
    private static Context context;
    private static boolean inCoolDown = false;

    private static double mediumRiskThreshold = 0.7;
    public static double mediumRiskScore = 7.0;
    public static double highRiskScore = 4.0;

    public static final MediaType JSON = MediaType.parse("application/json");
    private static ByteArrayOutputStream currentImageOutputStream;
    private static final String api_endpoint = "https://prod-24.westeurope.logic.azure.com:443/workflows/41d3890ebfd54071814359c127c30e6e/triggers/manual/paths/invoke?api-version=2016-10-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=BJ8QijHIX_mMxQiORHOcpODmLngNJ8S90P7qqjKQICQ";
    OkHttpClient client = null;

    public AlertManager(MainActivity main_activity) {
        mainActivity = main_activity;
        context = mainActivity.getApplicationContext();
        bandAlertnessScore = 1.0;
        pictureAlertnessScore = 1.0;
        client = new OkHttpClient();
    }

    public void setCurrentImage(ByteArrayOutputStream lastImageOutputStream) {
        AlertManager.currentImageOutputStream = lastImageOutputStream;
    }

    public void Alert(Context appContext){
        if (inCoolDown){
            Log.d(TAG, "Alert request received but in cool down");
            return;
        }
        inCoolDown = true;
        Log.d(TAG, "Alert request received, sounding alert");
        SoundManager.Alert(appContext);
        uploadImageToDrive(currentImageOutputStream);
        Log.d(TAG, "Starting alert manager's cool down");
        turnOffCoolDown();
        DataSender.ReportOnAlert();
    }

    private void uploadImageToDrive(ByteArrayOutputStream currentImageOutputStream) {
        UploadImageTask().execute(currentImageOutputStream);
    }

    public static void setBandDisabled(){
        Log.d(TAG, "Band scoring disabled");
        bandDisabled = true;
    }


    public void setPictureRate(){
        if (bandAlertnessScore < mediumRiskThreshold || pictureAlertnessScore < mediumRiskThreshold){
            mainActivity.pictureTakingTimer.setHighRate();
        }
    }

    public void setBandAlertness(double score){
        bandAlertnessScore = score;
        setPictureRate();
        checkIfToAlert();
        mainActivity.alertnessScoreUpdateListener.onScoreUpdate(getTotalAlertnessScore());
    }

    public void setPictureAlertness(double score){
        pictureAlertnessScore = score;
        setPictureRate();
        checkIfToAlert();
        mainActivity.alertnessScoreUpdateListener.onScoreUpdate(getTotalAlertnessScore());
    }

    public void checkIfToAlert(){
        double alertnessLevel = getTotalAlertnessScore();
        // Most likely will happen when One indicator is in Low state or both in Medium
        if (alertnessLevel < highRiskScore){
            Alert(context);
        }
    }

    public double getTotalAlertnessScore(){
        if (bandDisabled){
            Log.d(TAG, String.format("Alertness score: %f", pictureAlertnessScore));
            return Math.max(Math.min(10 * pictureAlertnessScore, 10), 0);
        }

        double pictureWeight = 0.7; // picture score is more significant
        double weightedScore = pictureWeight*pictureAlertnessScore + (1 - pictureWeight)*bandAlertnessScore;
        double finalScore = Math.max(Math.min(10 * weightedScore, 10), 0);

        Log.d(TAG, String.format("Alertness score: %f", finalScore));
        return finalScore;
    }

    private static void turnOffCoolDown(){
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        inCoolDown = false;
                        Log.d(TAG, "Finished cool down");
                    }
                },
                15000
        );
    }

    public AsyncTask<ByteArrayOutputStream, String, String> UploadImageTask() {
        return new AsyncTask<ByteArrayOutputStream, String, String>() {
            public String TAG = "PictureUploader";

            @Override
            protected String doInBackground(ByteArrayOutputStream... params) {
                try{
                    Log.i(this.TAG,"Trying to upload image");
                    uploadToOD(params[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(this.TAG,"Failed Uploading Image");
                }
                Log.i(this.TAG,"Image uploaded successfully");
                return "YES";
            }
        };
    }

    public String uploadToOD(ByteArrayOutputStream byteArrayOutputStream) throws Exception {
        JSONObject requestObject = new JSONObject();

        requestObject.put("name", String.format("img%s.jpg", DateFormat.getDateTimeInstance().format(new Date())));
        requestObject.put("image", Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT));
        RequestBody body = RequestBody.create( JSON, requestObject.toString());
        Request request = new Request.Builder()
                .url(api_endpoint)
                .post(body)
                .build();

        okhttp3.Response response = client.newCall(request).execute();
        Log.i(this.TAG,String.format("Response status: %s", response.isSuccessful()));
        if (!response.isSuccessful()){
            throw new Exception("Response from app logic failed");
        }

        return "YES!!!";
    }
}
