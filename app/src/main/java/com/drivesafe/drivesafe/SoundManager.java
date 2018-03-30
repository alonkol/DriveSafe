package com.drivesafe.drivesafe;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import java.util.Random;

public class SoundManager {

    private static final String TAG = "Sound Manager";
    private static MediaPlayer mp;
    private static boolean inCoolDown = false;
    private static final int[] sounds = new int[]{R.raw.siren, R.raw.shouts};

    static void Alert(Context appContext){
        try {
            int soundIndex = new Random().nextInt(sounds.length);

            if (mp != null){
                if (inCoolDown || mp.isPlaying()) {
                    return;
                }
                mp.release();
            }

            mp = MediaPlayer.create(appContext, sounds[soundIndex]);
            inCoolDown = true;
            Log.d(TAG, "In cool down");
            mp.start();
            turnOffCoolDown();



        } catch (Exception e) {
            // TODO: ignore?
            e.printStackTrace();
        }
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
