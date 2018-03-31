package com.drivesafe.drivesafe;

import android.util.Log;
import com.microsoft.applicationinsights.library.ApplicationInsights;
import com.microsoft.applicationinsights.library.TelemetryClient;

public class DataSender {

    private static TelemetryClient telemetry;
    private static String TAG = "DataSender";

    public DataSender(MainActivity activity) {
        ApplicationInsights.setup(activity.getApplicationContext(), activity.getApplication());
        ApplicationInsights.start();
        //TODO: remove line below at the end of development
        //ApplicationInsights.setDeveloperMode(true);
        this.telemetry = TelemetryClient.getInstance();;
    }

    public static void ReportOnAlert() {
        DataSender.SendData("Alert", 1.0);
    }

    public static void SendData(String name, double value) {
        telemetry.trackMetric(name, value);
        Log.i(TAG, String.format("Data from type %s and value %f was sent", name, value));
    }
}
