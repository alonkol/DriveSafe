package com.drivesafe.drivesafe;

import android.os.AsyncTask;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class DataReceiver extends Thread {

    private static String TAG = "DataReceiverTask";
    private final static String get24HoursAlertsNumber = "https://api.applicationinsights.io/v1/apps/ae986fd1-75ce-4773-b966-c364d6802517/metrics/customMetrics%2FAlert?timespan=P1D&interval=P1D&aggregation=count&top=1";
    private final static String get1HourAlertsNumber = "https://api.applicationinsights.io/v1/apps/ae986fd1-75ce-4773-b966-c364d6802517/metrics/customMetrics%2FAlert?timespan=PT1H&interval=PT1H&aggregation=count&top=1";
    public static final MediaType JSON = MediaType.parse("application/json");
    OkHttpClient client = null;
    MainActivity mainActivity;

    private final double HIGH_RISK_THRESHOLD = 1.0/12;

    public DataReceiver(MainActivity mainActivity) {
        this.client = new OkHttpClient();
        this.mainActivity = mainActivity;
    }

    public void run() {
        double score =  getRiskScore();
        if (score > HIGH_RISK_THRESHOLD){
            Log.d(this.TAG, String.format("Risk score = %f", score));
            mainActivity.isHighRisk = true;
        }
    }

    public double getRiskScore() {
        double last24HoursAlerts = (double) getLast24HoursAlerts();
        double lastHourAlerts = (double) getLastHourAlerts();
        if (last24HoursAlerts == 0)
        {
            return 0;
        }
        return (double)lastHourAlerts/ last24HoursAlerts;
    }

    private double getLastHourAlerts() {
        JSONObject json = sendApiRequest(get1HourAlertsNumber);
        return getNumOfAlertsFromJson(json);
    }

    private double getLast24HoursAlerts() {
        JSONObject json = sendApiRequest(get24HoursAlertsNumber);
        return getNumOfAlertsFromJson(json);
    }

    private int getNumOfAlertsFromJson(JSONObject json) {
        JSONArray arr = null;
        int alerts = 0;
        if (json == null)
        {
            return 0;
        }
        try {
            arr = json.getJSONObject("value").getJSONArray("segments");
            for (int i = 0; i < arr.length(); i++)
            {
                alerts += Integer.parseInt(arr.getJSONObject(i).getJSONObject("customMetrics/Alert").getString("count"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    private JSONObject sendApiRequest(String apiUrl ) {

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("x-api-key", "f6mfimdhku0fqg8xlslmafkuqd82xo0hjepvgzsh")
                .build();
        okhttp3.Response response = null;
        try {
            response = client.newCall(request).execute();
            String responseData = response.body().string();
            JSONObject jsonObject = new JSONObject(responseData);
            Log.i(TAG, responseData);
            return jsonObject;
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }
        catch (NetworkOnMainThreadException e2) {
            e2.printStackTrace();
            return null;
        }
         catch (JSONException e3) {
             e3.printStackTrace();
             return null;
         }
    }

}
