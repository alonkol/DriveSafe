package com.drivesafe.drivesafe;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import java.util.Random;

public class SoundManager {

    private static final String TAG = "Sound Manager";
    private static MediaPlayer mp;
    private static final int[] sounds = new int[]{R.raw.siren, R.raw.shouts};

    static void Alert(Context appContext){
        try {
            int soundIndex = new Random().nextInt(sounds.length);

            if (mp != null){
                Log.d(TAG, "Alert sound requested but mp is playing");
                if (mp.isPlaying()) {
                    return;
                }
                mp.release();
            }

            mp = MediaPlayer.create(appContext, sounds[soundIndex]);
            Log.d(TAG, "Sounding alert");
            mp.start();

        } catch (Exception e) {
            // TODO: ignore?
            e.printStackTrace();
        }
    }

}
