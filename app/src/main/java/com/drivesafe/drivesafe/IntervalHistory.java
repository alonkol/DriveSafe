package com.drivesafe.drivesafe;

import java.util.LinkedList;
import java.util.List;

class IntervalHistory {

    private static List<Double> history = new LinkedList<>();

    private static double variance;

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

    private static void setAlertnessLevel(AlertManager alertManager){
        if (history.size() < minHistory){
            alertManager.setBandAlertness(1.0);
            return;
        }

        double score = 1.0 - (3 * variance);
        alertManager.setBandAlertness(score);
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
