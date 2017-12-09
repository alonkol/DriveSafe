package com.drivesafe.drivesafe;

import java.util.LinkedList;
import java.util.Queue;

class IntervalHistory {

    private static Queue<Double> history = new LinkedList<>();

    private static double variance;

    private static final double highVarianceThreshold = 0.3;
    private static final double mediumVarianceThreshold = 0.1;

    private static final long minHistory = 10;
    private static final long maxHistory = 300;

    enum AlertnessLevel{
        Low, Medium, High
    }

    static void add(double interval){
        history.add(interval);

        if (history.size() >= maxHistory){
            history.remove();
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
