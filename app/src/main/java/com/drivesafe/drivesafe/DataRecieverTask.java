package com.drivesafe.drivesafe;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class DataRecieverTask extends AsyncTask<Void, Void, Void> {

    private static String TAG = "DataRecieverTask";
    private final static String urlRequest = "https://api.applicationinsights.io/v1/apps/ae986fd1-75ce-4773-b966-c364d6802517/metrics/customMetrics%2FAlert?timespan=P1D&interval=PT1H&aggregation=count&top=1";
    OkHttpClient client = null;
    MainActivity mainActivity;

    private final int HIGH_RISK_THRESHOLD = 1/12;



    public DataRecieverTask(MainActivity mainActivity) {
        this.client = new OkHttpClient();
        this.mainActivity = mainActivity;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        sendApiRequest(urlRequest);
        return null;
    }

    private void determineRiskScore(JSONObject json) {
        JSONArray arr = null;
        int totalDailyAlerts = 0;
        int lastHourAlerts = 0;
        Calendar c = GregorianCalendar.getInstance();
        int Hr24= c.get(Calendar.HOUR_OF_DAY);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        int closestHour = 24;
        try {
            arr = json.getJSONObject("value").getJSONArray("segments");
            for (int i = 0; i < arr.length(); i++)
            {
                //update daily alerts
                int alerts = Integer.parseInt(arr.getJSONObject(i).getJSONObject("customMetrics/Alert").getString("count"));
                totalDailyAlerts += alerts;
                //check if this is the latest hour
                String dateToParse = arr.getJSONObject(i).getString("end");
                Date date = format.parse(dateToParse.replaceAll("Z$", "+0000"));
                System.out.println(date);
                c.setTime(date);   // assigns calendar to given date
                int segmentHour = c.get(Calendar.HOUR_OF_DAY);

                if (segmentHour - Hr24 <= 0) // segment hour is from today
                {
                    if ( (segmentHour - Hr24) < closestHour) {
                        closestHour = segmentHour;
                        lastHourAlerts = alerts;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (totalDailyAlerts == 0) {
            mainActivity.isHighRisk = false;
            return;
        }
        mainActivity.isHighRisk = (totalDailyAlerts / lastHourAlerts > HIGH_RISK_THRESHOLD);
    }

    private void sendApiRequest(String apiUrl ) {

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("x-api-key", "f6mfimdhku0fqg8xlslmafkuqd82xo0hjepvgzsh")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String mMessage = e.toString();
                Log.e(TAG, mMessage); // no need inside run()
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String yossi = response.body().string();
                Log.i(TAG, yossi);
                try {
                    String resStr = yossi;
                    JSONObject jsonObject = new JSONObject(resStr);
                    determineRiskScore(jsonObject);
                }
                  catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
