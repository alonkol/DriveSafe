package com.drivesafe.drivesafe;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import java.util.Random;

public class SoundManager {

    private static MediaPlayer mp;
    private static final int[] sounds = new int[]{R.raw.wake_up, R.raw.siren};

    static void Alert(Context appContext){
        try {
            int soundIndex = new Random().nextInt(sounds.length);

            if (mp != null){
                if (mp.isPlaying()) {
                    return;
                }
                mp.release();
            }

            mp = MediaPlayer.create(appContext, sounds[soundIndex]);
            mp.start();

        } catch (Exception e) {
            // TODO: ignore?
            e.printStackTrace();
        }
    }
}
