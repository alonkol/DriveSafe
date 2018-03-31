package com.drivesafe.drivesafe;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Request;


public class DataReciever {

    private static String TAG = "DataReciever";
    private final static String get24HoursAlertsNumber = "https://api.applicationinsights.io/v1/apps/ae986fd1-75ce-4773-b966-c364d6802517/metrics/customMetrics%2FAlert?timespan=P1D&interval=P1D&aggregation=count&top=1";
    private final static String get1HourAlertsNumber = "https://api.applicationinsights.io/v1/apps/ae986fd1-75ce-4773-b966-c364d6802517/metrics/customMetrics%2FAlert?timespan=PT1H&interval=PT1H&aggregation=count&top=1";
    public static final MediaType JSON = MediaType.parse("application/json");
    private MainActivity main_activity;

    public DataReciever(MainActivity mainActivity) {
        main_activity = mainActivity;
    }

    public double getRiskScore() {
        int last24HoursAlerts = getLast24HoursAlerts();
        int lastHourAlerts = getLastHourAlerts();
        if (last24HoursAlerts == 0)
        {
            return 0;
        }
        return lastHourAlerts/last24HoursAlerts;
    }

    private int getLastHourAlerts() {
        JSONObject json = sendApiRequest(get1HourAlertsNumber);
        return getNumOfAlertsFromJson(json);
    }

    private int getLast24HoursAlerts() {
        JSONObject json = sendApiRequest(get24HoursAlertsNumber);
        return getNumOfAlertsFromJson(json);
    }

    private int getNumOfAlertsFromJson(JSONObject json) {
        JSONArray arr = null;
        int alerts = 0;
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
            response = main_activity.client.newCall(request).execute();
            Log.i(this.TAG, response.body().string());
            String responseData = response.body().string();
            JSONObject jsonObject = new JSONObject(responseData);
            Log.e(TAG, "onResponse: " + responseData);
            return jsonObject;
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }
         catch (JSONException e2) {
             e2.printStackTrace();
             return null;
         }
    }
}
