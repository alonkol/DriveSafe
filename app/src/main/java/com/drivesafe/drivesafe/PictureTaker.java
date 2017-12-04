package com.drivesafe.drivesafe;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import java.io.IOException;
import java.util.TimerTask;

public class PictureTaker extends TimerTask {
    Camera camera = MainActivity.camera;

    @Override
    public void run() {
        take_picture();
    }

    private void take_picture() {
        try{
            SurfaceTexture st = new SurfaceTexture(0);
            camera.setPreviewTexture(st);
            camera.startPreview();
            camera.takePicture( null, null, MainActivity.pictureCallback);
        }catch (IOException e) {

        }
    }
}
