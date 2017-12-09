package com.drivesafe.drivesafe;

import java.util.LinkedList;
import java.util.Queue;

class OcclusionHistory {

    private static Queue<Double> history = new LinkedList<>();

    private static double avg;
    private static double currentOcclusion;

    private static final double highDeltaThreshold = 0.8;
    private static final double mediumDeltaThreshold = 0.3;
    private static final long minHistory = 3;
    private static final long maxHistory = 200;

    enum AlertnessLevel{
        Low, Medium, High
    }

    static void add(double occlusion){
        history.add(occlusion);
        currentOcclusion = occlusion;

        if (history.size() >= maxHistory){
            history.remove();
        }

        calcAverage();
    }

    static AlertnessLevel getAlertnessLevel(){
        if (history.size() < minHistory){
            return AlertnessLevel.High;
        }

        double delta = avg - currentOcclusion;

        if (delta > highDeltaThreshold){
            return AlertnessLevel.Low;
        }

        if (delta > mediumDeltaThreshold){
            return AlertnessLevel.Medium;
        }

        return AlertnessLevel.High;
    }

    private static void calcAverage(){
        double sum = 0.0;
        for (double occlusion: history) {
            sum += occlusion;
        }

        avg = sum/(double)history.size();
    }

}
