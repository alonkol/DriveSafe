package com.drivesafe.drivesafe;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import com.drivesafe.drivesafe.Auxiliary.*;

class OcclusionHistory {

    private static List<Double> history = new LinkedList<>();

    private static double avg;

    private static final int numberOfLatestIndexesToFocus = 3;
    public static final double highRiskScoreThreshold = 3;
    public static final double mediumRiskScoreThreshold = 6;
    private static final int minHistory = 5;
    private static final int maxHistory = 200;
    public static final double latestRatio = 0.2;

    static void add(double occlusion, AlertManager alertManager){
        history.add(occlusion);

        if (history.size() == maxHistory){
            history.remove(0);
        }
        DataSender.SendData("Occlusion", occlusion);

        calcAverage();
        setPictureAlertness(alertManager);
    }

    static void setPictureAlertness(AlertManager alertManager){
        if (history.size() < minHistory){
            alertManager.setPictureAlertness((10.0 + mediumRiskScoreThreshold) / 2);
            return;
        }

        double averageDelta = getAverageDeltaOfLatestData(numberOfLatestIndexesToFocus);
        Log.i("Average occlusion delta", Double.toString(averageDelta));
        double latestDelta = getAverageDeltaOfLatestData(1);
        Log.i("Latest occlusion delta", Double.toString(latestDelta));
        double totalScore = ((1.0 - latestRatio) * averageDelta + latestRatio * latestDelta) * 10;
        totalScore = Math.max(Math.min(10 - totalScore, 10) ,0);
        alertManager.setPictureAlertness(totalScore);
    }

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
