package com.drivesafe.drivesafe;

import java.util.LinkedList;
import java.util.List;

import com.drivesafe.drivesafe.Auxiliary.*;

class IntervalHistory {

    private static List<Double> history = new LinkedList<>();

    private static double variance;

    public static final double highRiskScoreThreshold = 7;
    public static final double mediumRiskScoreThreshold = 9;

    private static final int minHistory = 10;
    private static final int maxHistory = 300;

    static void add(double interval, AlertManager alertManager){
        history.add(interval);

        if (history.size() == maxHistory){
            history.remove(0);
        }

        DataSender.SendData("RRInterval", interval);
        calcVariance();
        setAlertnessLevel(alertManager);
    }

    static void setAlertnessLevel(AlertManager alertManager){
        if (history.size() < minHistory){
            alertManager.setBandAlertness((10.0 + mediumRiskScoreThreshold) / 2);
            return;
        }

        double totalScore =  Math.max(Math.min((1.0 - variance) * 10, 10) ,0);
        alertManager.setBandAlertness(totalScore);
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
