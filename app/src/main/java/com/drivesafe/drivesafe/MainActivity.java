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

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.projectoxford.face.contract.*;


import java.util.UUID;

import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;


public class MainActivity extends AppCompatActivity {

    public static ProgressDialog detectionProgressDialog;
    public static ImageView imageView;
    public static Camera camera;
    public static Camera.PictureCallback pictureCallback;
    public Camera.CameraInfo cameraInfo;



    public Activity mainActivityReference = this;

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
        detectionProgressDialog = new ProgressDialog(this);
        new RRIntervalSubscriptionTask(this).execute();

    }
    private void take_picture() {
        try{
        SurfaceTexture st = new SurfaceTexture(MODE_PRIVATE);
            camera.setPreviewTexture(st);
            camera.startPreview();
            camera.takePicture( null , null, this.pictureCallback) ;
            }catch (IOException e){

                }
                }
                private void initFrontCamera(){
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


}

class Face {
    public UUID faceId;
    public FaceRectangle faceRectangle;
    public FaceLandmarks faceLandmarks;
    public FaceAttribute faceAttributes;

    public Face() {
    }
}

// TODO: if necessary, add property for Occlusion etc.
class FaceAttribute {
    public double age;
    public String gender;
    public double smile;
    public FacialHair facialHair;
    public HeadPose headPose;

    public FaceAttribute() {
    }
}

