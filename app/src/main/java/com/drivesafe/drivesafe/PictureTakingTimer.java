package com.drivesafe.drivesafe;

import android.util.Log;

import java.util.TimerTask;

public class PictureTakingTimer extends Thread{
    private static final int DEFAULT_RATE = 3;
    private static final int HIGH_RATE = 1;
    private static final int MAX_IMAGES_ON_HIGH_RATE = 15;
    private static final String TAG = "picture taking timer";
    private int rate = DEFAULT_RATE;
    private int count;
    private boolean onHighRate = false;
    private TimerTask task;

    public PictureTakingTimer(TimerTask t){
        this.task = t;
        this.setDefaultRate();
    }

    public void run(){
        while (true){
            task.run();
            if (onHighRate){
                count++;
                if (count == MAX_IMAGES_ON_HIGH_RATE){
                    Log.i(this.TAG, "Reached max images on high rate, returning to default");
                    setDefaultRate();
                }
            }
            try {
                sleep(rate * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }

    }

    private void setDefaultRate(){
        Log.i(this.TAG, String.format("Setting Default Rate %d", DEFAULT_RATE));
        this.rate = DEFAULT_RATE;
        this.onHighRate = false;
    }

    public void setHighRate() {
        // This Method will set High Rate for HIGH_RATE_COUNT TIMES
        if (!this.onHighRate){
            Log.i(this.TAG, String.format("Setting High Rate %d", HIGH_RATE));
            this.count = 0;
            this.rate = HIGH_RATE;
            this.onHighRate = true;
        }
    }
}
