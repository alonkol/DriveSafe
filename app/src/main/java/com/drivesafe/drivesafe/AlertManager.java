package com.drivesafe.drivesafe;


import android.content.Context;
import android.util.Log;

public class AlertManager {
    private static final String TAG = "Alert Manager";
    private static Auxiliary.AlertnessLevel bandAlertnessLevel;
    private static Auxiliary.AlertnessLevel pictureAlertnessLevel;
    private MainActivity main_activity;
    private static boolean inCoolDown = false;
    private static Context context;

    public AlertManager(MainActivity mainActivity) {
        main_activity = mainActivity;
        context = mainActivity.getApplicationContext();
        bandAlertnessLevel = Auxiliary.AlertnessLevel.High;
        pictureAlertnessLevel = Auxiliary.AlertnessLevel.High;
    }

    public void Alert(Context appContext){
        if (inCoolDown){
            Log.d(TAG, "Alert request received but in cool down");
            return;
        }
        inCoolDown = true;
        Log.d(TAG, "Alert request received, sounding alert");
        SoundManager.Alert(appContext);
        Log.d(TAG, "Starting alert manager's cool down");
        turnOffCoolDown();
    }

    public void setPictureRate(){
        if (pictureAlertnessLevel == Auxiliary.AlertnessLevel.Medium || pictureAlertnessLevel == Auxiliary.AlertnessLevel.Low){
            main_activity.pictureTakingTimer.setHighRate();
        }
        if (bandAlertnessLevel == Auxiliary.AlertnessLevel.Medium || bandAlertnessLevel == Auxiliary.AlertnessLevel.Low){
            main_activity.pictureTakingTimer.setHighRate();
        }
    }

    public void setBandAlertness(Auxiliary.AlertnessLevel level){
        Log.d(TAG, String.format("Setting band alert to %s", level));
        bandAlertnessLevel = level;
        setPictureRate();
        checkIfToAlert();
    }

    public void setPictureAlertness(Auxiliary.AlertnessLevel level){
        Log.d(TAG, String.format("Setting picture alert to %s", level));
        pictureAlertnessLevel = level;
        setPictureRate();
        checkIfToAlert();
    }

    public void checkIfToAlert(){
        if (bandAlertnessLevel == Auxiliary.AlertnessLevel.Low ||
                pictureAlertnessLevel == Auxiliary.AlertnessLevel.Low)
            Alert(context);
        if (bandAlertnessLevel == Auxiliary.AlertnessLevel.Medium &&
                pictureAlertnessLevel == Auxiliary.AlertnessLevel.Medium)
            Alert(context);
    }

    private static void turnOffCoolDown(){
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        inCoolDown = false;
                        Log.d(TAG, "Finished cool down");
                    }
                },
                15000
        );
    }
}
