package com.drivesafe.drivesafe;

import android.os.NetworkOnMainThreadException;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;

class DataReceiver extends Thread {

    private static String TAG = "DataReceiverTask";
    private final static String get24HoursAlertsNumber = "https://api.applicationinsights.io/v1/apps/ae986fd1-75ce-4773-b966-c364d6802517/metrics/customMetrics%2FAlert?timespan=P1D&interval=P1D&aggregation=count&top=1";
    private final static String get1HourAlertsNumber = "https://api.applicationinsights.io/v1/apps/ae986fd1-75ce-4773-b966-c364d6802517/metrics/customMetrics%2FAlert?timespan=PT1H&interval=PT1H&aggregation=count&top=1";
    private OkHttpClient client = null;
    private MainActivity mainActivity;

    private final double HIGH_RISK_THRESHOLD = 1.0/12;

    public DataReceiver(MainActivity mainActivity) {
        this.client = new OkHttpClient();
        this.mainActivity = mainActivity;
    }

    public void run() {
        double score =  getRiskScore();
        if (score > HIGH_RISK_THRESHOLD){
            Log.d(this.TAG, String.format("Risk score = %f", score));
            mainActivity.displaySpecialNotification = true;
        }
    }

    public double getRiskScore() {
        double last24HoursAlerts = getLast24HoursAlerts();
        double lastHourAlerts = getLastHourAlerts();
        if (last24HoursAlerts == 0)
        {
            return 0;
        }
        return lastHourAlerts / last24HoursAlerts;
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
        int alerts = 0;
        if (json == null)
        {
            return 0;
        }
        try {
            JSONArray arr = json.getJSONObject("value").getJSONArray("segments");
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
        try {
            okhttp3.Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            JSONObject jsonObject = new JSONObject(responseData);
            Log.i(TAG, responseData);
            return jsonObject;
        } catch (IOException | NetworkOnMainThreadException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

}
