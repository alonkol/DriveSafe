package com.drivesafe.drivesafe;

import java.util.TimerTask;

public class PictureTaker extends TimerTask {
    @Override
    public void run() {
        take_picture();
    }

    private void take_picture() {
        MainActivity.camera.startPreview();
        MainActivity.camera.takePicture(null, null, null,
                MainActivity.pictureCallback);
    }
}
