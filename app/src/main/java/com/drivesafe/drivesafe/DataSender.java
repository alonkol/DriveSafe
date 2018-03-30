package com.drivesafe.drivesafe;

import android.util.Log;
import com.microsoft.applicationinsights.library.TelemetryClient;

public class DataSender {

    private static TelemetryClient telemetry = TelemetryClient.getInstance();
    private static String dataSenderTag = "DataSender";


    public static void ReportOnAlert() {
        telemetry.trackMetric("Alert", 1.0);
        Log.i(dataSenderTag, String.format("Alert was Sent"));
    }

    public static void SendData(String name, double value) {
        telemetry.trackMetric(name, value);
        Log.i(dataSenderTag, String.format("Data from type %s and value %f was sent", name, value));
    }
}
