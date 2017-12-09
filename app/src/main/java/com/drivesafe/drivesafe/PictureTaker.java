package com.drivesafe.drivesafe;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import java.io.IOException;
import java.util.TimerTask;

public class PictureTaker extends TimerTask {
    public static Camera camera;

    public void setCamera(){
        if (this.camera == null){
            this.camera = MainActivity.camera;
        }
    }

    @Override
    public void run() {
        take_picture();
    }

    private void take_picture() {
        try{
            setCamera();
            SurfaceTexture st = new SurfaceTexture(0);
            camera.setPreviewTexture(st);
            camera.startPreview();
            camera.takePicture( null, null, MainActivity.pictureCallback);
        }catch (IOException e) {

        }
    }
}
