package com.drivesafe.drivesafe;

import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.*;
import android.view.*;
import android.widget.*;
import android.hardware.Camera;

import java.io.IOException;
import java.util.Timer;


public class MainActivity extends AppCompatActivity {

    public static ProgressDialog detectionProgressDialog;
    public static ImageView imageView;
    public static Camera camera;
    public static Camera.PictureCallback pictureCallback;
    public Camera.CameraInfo cameraInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.imageView = (ImageView)findViewById(R.id.imageView1);
        Button button1 = (Button)findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                take_picture();
            }

        });
        this.detectionProgressDialog = new ProgressDialog(this);
        this.pictureCallback = new PhotoHandler(getApplicationContext());
        this.initFrontCamera();

        //Thread that takes picture every 3 seconds and starts 1.5 seconds after app init
        PictureTaker myTask = new PictureTaker();
        Timer myTimer = new Timer();
        myTimer.schedule(myTask, 1500, 3000);
    }
    private void take_picture() {
        try{
            SurfaceTexture st = new SurfaceTexture(MODE_PRIVATE);
            camera.setPreviewTexture(st);
            camera.startPreview();
            camera.takePicture( null, null, this.pictureCallback);
        }catch (IOException e) {

        }
    }
    private void initFrontCamera() {
        if (getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            camera = Camera.open(this.findFrontFacingCamera());
            //camera.setDisplayOrientation(getCorrectCameraOrientation(cameraInfo, camera));
            //camera.getParameters().setRotation(getCorrectCameraOrientation(cameraInfo, camera));
        }else {
            finish(); // no Camera on device
        }
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraInfo = info;
                break;
            }
        }
        return cameraId;
    }

    public int getCorrectCameraOrientation(Camera.CameraInfo info, Camera camera) {

        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch(rotation){
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;

        }

        int result;

        result = (info.orientation + degrees) % 360;
        result = (360 - result) % 360;

        return result;
    }

}
