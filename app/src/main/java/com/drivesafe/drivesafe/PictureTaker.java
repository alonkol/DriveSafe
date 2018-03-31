package com.drivesafe.drivesafe;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.TimerTask;

public class PictureTaker extends TimerTask {
    public static Camera camera;

    public void setCameraIfNeeded(){
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
            setCameraIfNeeded();
            SurfaceTexture st = new SurfaceTexture(0);
            camera.stopPreview();
            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> supportedSizes = parameters.getSupportedPictureSizes();
            parameters.setPictureSize(supportedSizes.get(0).width, supportedSizes.get(0).height);
            camera.setParameters(parameters);
            camera.setPreviewTexture(st);
            camera.startPreview();
            camera.takePicture( null, null, MainActivity.pictureCallback);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
