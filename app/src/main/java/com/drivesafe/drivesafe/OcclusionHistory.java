package com.drivesafe.drivesafe;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

class OcclusionHistory {

    private static List<Double> history = new LinkedList<>();

    private static double avg;

    private static final int numberOfLatestIndexesToFocus = 3;
    private static final int minHistory = 5;
    private static final int maxHistory = 200;

    static void add(double occlusion, AlertManager alertManager){
        history.add(occlusion);

        if (history.size() == maxHistory){
            history.remove(0);
        }
        DataSender.SendData("Occlusion", occlusion);

        calcAverage();
        setPictureAlertness(alertManager);
    }

    private static void setPictureAlertness(AlertManager alertManager){
        if (history.size() < minHistory){
            alertManager.setPictureAlertness(1.0);
            return;
        }

        double averageDelta = getAverageDeltaOfLatestData(numberOfLatestIndexesToFocus);

        double score = 1.0 - (2 * averageDelta);
        Log.i("Occlusion score", Double.toString(score));

        alertManager.setPictureAlertness(score);
    }

    // returns ratio, number between 0 to 1.0
    private static double getAverageDeltaOfLatestData(int scope){
        double deltaSum = 0.0;

        for (int i = 0; i < scope; i++){
            deltaSum += (avg - history.get(history.size() - 1 - i));
        }

        double averageDelta = deltaSum / scope;

        return averageDelta / avg;
    }

    private static void calcAverage(){
        double sum = 0.0;
        for (double occlusion: history) {
            sum += occlusion;
        }

        avg = sum/(double)history.size();
    }

}
