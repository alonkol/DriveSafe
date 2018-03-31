package com.drivesafe.drivesafe;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.projectoxford.face.common.RequestMethod;
import com.microsoft.projectoxford.face.rest.ClientException;
import com.microsoft.projectoxford.face.rest.WebServiceRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class AlertManager {
    private static final String TAG = "Alert Manager";
    private static Auxiliary.AlertnessLevel bandAlertnessLevel;
    private static Auxiliary.AlertnessLevel pictureAlertnessLevel;
    private MainActivity main_activity;
    private static Context context;
    private static boolean inCoolDown = false;
    public static final MediaType JSON = MediaType.parse("application/json");
    private final String sub_key = "23cbada0-dd3e-4af3-87e1-6895d48acdd2";
    private static ByteArrayOutputStream currentImageOutputStream;
    private final WebServiceRequest mRestCall = new WebServiceRequest(sub_key);
    private Gson mGson = (new GsonBuilder()).setDateFormat("M/d/yyyy h:m:s a").create();
    private static final String api_endpoint = "https://prod-24.westeurope.logic.azure.com:443/workflows/41d3890ebfd54071814359c127c30e6e/triggers/manual/paths/invoke?api-version=2016-10-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=BJ8QijHIX_mMxQiORHOcpODmLngNJ8S90P7qqjKQICQ";

    public AlertManager(MainActivity mainActivity) {
        main_activity = mainActivity;
        context = mainActivity.getApplicationContext();
        bandAlertnessLevel = Auxiliary.AlertnessLevel.High;
        pictureAlertnessLevel = Auxiliary.AlertnessLevel.High;
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


    public void setPictureRate(){
        if (pictureAlertnessLevel == Auxiliary.AlertnessLevel.Medium || pictureAlertnessLevel == Auxiliary.AlertnessLevel.Low){
            main_activity.pictureTakingTimer.setHighRate();
        }
        if (bandAlertnessLevel == Auxiliary.AlertnessLevel.Medium || bandAlertnessLevel == Auxiliary.AlertnessLevel.Low){
            main_activity.pictureTakingTimer.setHighRate();
        }
    }

    public void setBandAlertness(Auxiliary.AlertnessLevel level){
        Log.d(TAG, String.format("Setting band alert to %s", level));
        bandAlertnessLevel = level;
        setPictureRate();
        checkIfToAlert();
    }

    public void setPictureAlertness(Auxiliary.AlertnessLevel level){
        Log.d(TAG, String.format("Setting picture alert to %s", level));
        pictureAlertnessLevel = level;
        setPictureRate();
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

    public String uploadToOD(ByteArrayOutputStream byteArrayOutputStream) throws ClientException, IOException {
        Map<String, Object> params = new HashMap();
        URL url = new URL(api_endpoint);
        JSONObject requestObject = new JSONObject();

        try {
            requestObject.put("name", String.format("img%d.bmp", DateFormat.getDateTimeInstance().format(new Date())));
            requestObject.put("image", Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create( JSON, requestObject.toString());
        Request request = new Request.Builder()
                .url(api_endpoint)
                .post(body)
                .build();

        okhttp3.Response response = main_activity.client.newCall(request).execute();
        Log.i(this.TAG,response.body().string());
        // String res = this.mGson.fromJson(json, List<String>);
        return "YES!!!";
    }
}
