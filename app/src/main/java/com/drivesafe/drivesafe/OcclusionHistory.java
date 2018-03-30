package com.drivesafe.drivesafe;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import com.drivesafe.drivesafe.Auxiliary.*;

class OcclusionHistory {

    private static List<Double> history = new LinkedList<>();

    private static double avg;

    private static final int numberOfLatestIndexesToFocus = 3;
    private static final double highDeltaPercentThreshold = 0.3;
    private static final double mediumDeltaPercentThreshold = 0.15;
    private static final int minHistory = 5;
    private static final int maxHistory = 200;

    static void add(double occlusion){
        history.add(occlusion);

        if (history.size() == maxHistory){
            history.remove(0);
        }

        calcAverage();
    }

    static AlertnessLevel getAlertnessLevel(){
        if (history.size() < minHistory){
            return AlertnessLevel.High;
        }

        double averageDelta = getAverageDeltaOfLatestData(numberOfLatestIndexesToFocus);
        Log.i("Average occlusion delta", Double.toString(averageDelta));

        if (averageDelta > highDeltaPercentThreshold){
            return AlertnessLevel.Low;
        }

        if (averageDelta > mediumDeltaPercentThreshold){
            return AlertnessLevel.Medium;
        }

        double latestDelta = getAverageDeltaOfLatestData(1);
        if (latestDelta > highDeltaPercentThreshold){
            return AlertnessLevel.Medium;
        }

        return AlertnessLevel.High;
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
