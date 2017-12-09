package com.drivesafe.drivesafe;

import java.util.LinkedList;
import java.util.List;

import com.drivesafe.drivesafe.Auxiliary.*;

class IntervalHistory {

    private static List<Double> history = new LinkedList<>();

    private static double variance;

    private static final double highVarianceThreshold = 0.3;
    private static final double mediumVarianceThreshold = 0.1;

    private static final int minHistory = 10;
    private static final int maxHistory = 300;

    static void add(double interval){
        history.add(interval);

        if (history.size() == maxHistory){
            history.remove(history.size() - 1);
        }

        calcVariance();
    }

    static AlertnessLevel getAlertnessLevel(){
        if (history.size() < minHistory){
            return AlertnessLevel.High;
        }

        if (variance > highVarianceThreshold){
            return AlertnessLevel.Low;
        }

        if (variance > mediumVarianceThreshold){
            return AlertnessLevel.Medium;
        }

        return AlertnessLevel.High;
    }

    private static void calcVariance(){
        double sum = 0.0;
        double average = calcAverage();

        for (double interval: history){
            sum += Math.pow((interval - average),2);
        }

        variance = sum/(double)history.size();
    }

    private static double calcAverage(){
        double sum = 0.0;
        for (double interval: history) {
            sum += interval;
        }
        return sum/(double)history.size();
    }

}
