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
import okhttp3.Request;
import okhttp3.RequestBody;

public class AlertManager {
    private static final String TAG = "Alert Manager";
    private static Auxiliary.AlertnessLevel bandAlertnessLevel;
    private static Auxiliary.AlertnessLevel pictureAlertnessLevel;
    private static double bandAlertnessScore;
    private static double pictureAlertnessScore;
    private static boolean bandDisabled = false;
    private MainActivity mainActivity;
    private static Context context;
    private static boolean inCoolDown = false;

    public static final MediaType JSON = MediaType.parse("application/json");
    private static ByteArrayOutputStream currentImageOutputStream;
    private static final String api_endpoint = "https://prod-24.westeurope.logic.azure.com:443/workflows/41d3890ebfd54071814359c127c30e6e/triggers/manual/paths/invoke?api-version=2016-10-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=BJ8QijHIX_mMxQiORHOcpODmLngNJ8S90P7qqjKQICQ";
    private double pictureRatio = 0.7;

    public AlertManager(MainActivity main_activity) {
        mainActivity = main_activity;
        context = mainActivity.getApplicationContext();
        bandAlertnessScore = 0;
        bandAlertnessLevel = Auxiliary.AlertnessLevel.High;
        pictureAlertnessScore = 0;
        pictureAlertnessLevel = Auxiliary.AlertnessLevel.High;
    }

    public void setBandAlertnessLevel(double bandScore) {
        if (bandScore < IntervalHistory.highRiskScoreThreshold){
            bandAlertnessLevel = Auxiliary.AlertnessLevel.Low;
        }else if(bandScore < IntervalHistory.mediumRiskScoreThreshold){
            bandAlertnessLevel = Auxiliary.AlertnessLevel.Medium;
        }else{
            bandAlertnessLevel = Auxiliary.AlertnessLevel.High;
        }
    }

    public void setPictureAlertnessLevel(double pictureScore) {
        if (pictureScore < OcclusionHistory.highRiskScoreThreshold){
            pictureAlertnessLevel = Auxiliary.AlertnessLevel.Low;
        }else if(pictureScore < OcclusionHistory.mediumRiskScoreThreshold){
            pictureAlertnessLevel = Auxiliary.AlertnessLevel.Medium;
        }else{
            pictureAlertnessLevel = Auxiliary.AlertnessLevel.High;
        }
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

    public void setBandDisabled(){
        Log.d(TAG, "Band scoring disabled");
        bandDisabled = true;
    }


    public void setPictureRateIfNeeded(){
        if (pictureAlertnessLevel == Auxiliary.AlertnessLevel.Medium || pictureAlertnessLevel == Auxiliary.AlertnessLevel.Low){
            mainActivity.pictureTakingTimer.setHighRate();
        }
        if (bandAlertnessLevel == Auxiliary.AlertnessLevel.Medium || bandAlertnessLevel == Auxiliary.AlertnessLevel.Low){
            mainActivity.pictureTakingTimer.setHighRate();
        }
    }

    public void setBandAlertness(double score) {
        Log.d(TAG, String.format("Setting band alert score to %f", score));
        bandAlertnessScore = score;
        setBandAlertnessLevel(score);
        setPictureRateIfNeeded();
        updateTotalAlertnessScore();
        checkIfToAlert();
    }

    public void setPictureAlertness(double score){
        Log.d(TAG, String.format("Setting picture alert score to %f", score));
        pictureAlertnessScore = score;
        setPictureAlertnessLevel(score);
        setPictureRateIfNeeded();
        updateTotalAlertnessScore();
        checkIfToAlert();
    }

    public void checkIfToAlert(){
        if (bandAlertnessLevel == Auxiliary.AlertnessLevel.Low ||
                pictureAlertnessLevel == Auxiliary.AlertnessLevel.Low)
            Alert(context);
        if (bandAlertnessLevel == Auxiliary.AlertnessLevel.Medium &&
                pictureAlertnessLevel == Auxiliary.AlertnessLevel.Medium)
            Alert(context);
    }

    public void updateTotalAlertnessScore(){
        double totalAlertnessScore;
        if (bandDisabled){
            totalAlertnessScore =  pictureAlertnessScore;
        }else {
            totalAlertnessScore = pictureAlertnessScore * pictureRatio +
                    bandAlertnessScore * (1.0 - pictureRatio);
        }
        totalAlertnessScore =  Math.max(Math.min(totalAlertnessScore, 10), 0);
        Log.d(TAG, String.format("Alertness score: %f", totalAlertnessScore));
        mainActivity.alertnessScoreUpdateListener.onScoreUpdate(totalAlertnessScore);
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

        okhttp3.Response response = mainActivity.client.newCall(request).execute();
        Log.i(this.TAG,String.format("Response status: %s", response.isSuccessful()));
        if (!response.isSuccessful()){
            throw new Exception("Response from app logic failed");
        }

        return "YES!!!";
    }
}
