package com.drivesafe.drivesafe;

import android.Manifest;
import android.content.pm.PackageManager;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.*;
import android.util.Log;
import android.widget.*;
import android.hardware.Camera;
import java.util.Timer;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main Activity";
    public static ImageView imageView;
    public static Camera camera;
    public static Camera.PictureCallback pictureCallback;
    public Camera.CameraInfo cameraInfo;
    public Activity mainActivityReference = this;
    private Timer timerPictureTaker;
    private PictureTaker pictureTakerTask;
    private final int PERMISSION_REQUEST_FOR_APP = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.imageView = (ImageView)findViewById(R.id.imageView1);
        requestPermissionsIfNeeded();

    }

    private void requestPermissionsIfNeeded(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED){
            // Missing Permissions, Ask for them
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH},
                    PERMISSION_REQUEST_FOR_APP);
        } else {
            initAppLogic();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FOR_APP: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permissions were granted, start the app logic
                    initAppLogic();
                } else {
                    Log.d(this.TAG, "Permissions were not granted!");
                    finish();
                }
            }
        }
    }

    private void initAppLogic(){
        // Init variables
        this.timerPictureTaker = new Timer();
        this.pictureTakerTask = new PictureTaker();
        this.pictureCallback = new PhotoHandler(getApplicationContext());

        this.initFrontCamera();
        // Start thread that takes picture every 5 seconds and starts 1.5 seconds after app init
        startPictureTaker(5);
        // Start thread that takes RR interval
        new RRIntervalSubscriptionTask(this).execute();
    }

    private void startPictureTaker(int rate){
        // rate in seconds
        this.timerPictureTaker.schedule(this.pictureTakerTask, 1500, rate * 1000);
    }

    private void initFrontCamera(){
        if (getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                camera = Camera.open(this.findFrontFacingCamera());
        } else {
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
}

