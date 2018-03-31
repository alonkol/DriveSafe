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

    static void add(double interval, AlertManager alertManager){
        history.add(interval);

        if (history.size() == maxHistory){
            history.remove(history.size() - 1);
        }

        DataSender.SendData("RRInterval", interval);
        calcVariance();
        setAlertnessLevel(alertManager);
    }

    static void setAlertnessLevel(AlertManager alertManager){
        if (history.size() < minHistory){
            alertManager.setBandAlertness(AlertnessLevel.High, 0);
            return;
        }

        if (variance > highVarianceThreshold){
            alertManager.setBandAlertness(AlertnessLevel.Low, variance - highVarianceThreshold);
            return;
        }

        if (variance > mediumVarianceThreshold){
            alertManager.setBandAlertness(AlertnessLevel.Medium, variance - mediumVarianceThreshold);
            return;
        }

        alertManager.setBandAlertness(AlertnessLevel.High, variance);
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
