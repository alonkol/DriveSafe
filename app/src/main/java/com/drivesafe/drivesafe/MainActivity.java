package com.drivesafe.drivesafe;

import android.Manifest;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.*;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.hardware.Camera;
import com.drivesafe.drivesafe.Auxiliary.*;

import okhttp3.OkHttpClient;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main Activity";
    public static ImageView mainImage;
    public static TextView band_rec;
    public static TextView face_rec;
    public static Button start_btn;
    public static Camera camera;
    public static Camera.PictureCallback pictureCallback;
    public Camera.CameraInfo cameraInfo;
    public Activity mainActivityReference = this;
    public AlertManager alertManager;
    public DataSender dataSender;
    public DataReciever dataReciever;

    public PictureTakingTimer pictureTakingTimer;
    public onFaceDetectionListener initFaceDetectionListener = null;
    public onBandDetectionListener initBandDetectionListener = null;
    public onDetectionCompletionEventListener initDetectionCompletion = null;
    public boolean faceIsReady = false;
    public boolean bandIsReady = false;
    public boolean isHighRisk = false;
    public Auxiliary.AppState STATE = AppState.Init;
    private final int PERMISSION_REQUEST_FOR_APP = 100;
    private final int HIGH_RISK_THRESHOLD = 1/12;
    OkHttpClient client = null;

    public static View driving_screen;
    private static ImageView driving_image;
    private static TextView driving_text;
    private static TextView driving_score;
    public onAlertnessScoreUpdateListener alertnessScoreUpdateListener = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.mainImage = (ImageView)findViewById(R.id.main_image);
        this.alertManager = new  AlertManager(this);
        this.dataSender = new DataSender(this);
        // this.dataReciever = new DataReciever(this);
        this.client = new OkHttpClient();
        this.setOnFaceDetectionEventListener(new onFaceDetectionListener() {
            @Override
            public void onFaceDetection() {
                if (!faceIsReady) {
                    face_rec.setText(R.string.face_detected);
                    face_rec.setTextColor(Color.GREEN);
                    faceIsReady=true;
                }
            }

            @Override
            public void onFaceNotDetected() {
                if (STATE == AppState.Init && faceIsReady) {
                    face_rec.setText(R.string.face_not_detected);
                    face_rec.setTextColor(Color.RED);
                    faceIsReady=false;
                }
            }
        });

        this.setOnBandDetectionEventListener(new onBandDetectionListener() {
            @Override
            public void onBandDetection() {
                if (!bandIsReady) {
                    band_rec.setText("Band is recognized! ");
                    band_rec.setTextColor(Color.GREEN);
                    bandIsReady=true;
                }
            }
        });

        this.setOnDetectionCompletionEventListener(new onDetectionCompletionEventListener() {
            @Override
            public void onCompletion() {
                if (STATE == AppState.Init)
                {
                    band_rec.setVisibility(View.GONE);
                    face_rec.setVisibility(View.GONE);
                }
            }
        });

        this.setOnAlertnessScoreUpdateListener(new onAlertnessScoreUpdateListener() {
            @Override
            public void onScoreUpdate(double alertnessScore) {
                if (STATE == AppState.Active)
                {
                    driving_score.setText(String.format("Alertness Score: %.1f", alertnessScore));
                    if (alertnessScore < AlertManager.highRiskScore){
                        driving_text.setText(R.string.driving_alert_title);
                        driving_image.setImageResource(R.drawable.face_fear);
                        driving_screen.setBackgroundColor(getResources().getColor(R.color.driving_alert));
                    }
                    else if (alertnessScore < AlertManager.mediumRiskScore){
                        driving_text.setText(R.string.driving_warning_title);
                        driving_image.setImageResource(R.drawable.face_worried);
                        driving_screen.setBackgroundColor(getResources().getColor(R.color.driving_warning));
                    }
                    else{
                        driving_text.setText(R.string.driving_normal_title);
                        driving_image.setImageResource(R.drawable.face_smile);
                        driving_screen.setBackgroundColor(getResources().getColor(R.color.driving_normal));

                    }
                }
            }
        });

        requestPermissionsIfNeeded();
//        if (dataReciever.getRiskScore() > HIGH_RISK_THRESHOLD){
//            isHighRisk = true;
//        }

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
        this.pictureCallback = new PhotoHandler(getApplicationContext(), this);
        this.band_rec = (TextView) findViewById(R.id.band_rec);
        this.face_rec = (TextView) findViewById(R.id.face_rec);
        this.start_btn = (Button)  findViewById(R.id.start_btn);
        this.driving_screen = (View) findViewById(R.id.driving_screen);
        this.driving_image = (ImageView) findViewById(R.id.driving_image);
        this.driving_text = (TextView) findViewById(R.id.driving_text);
        this.driving_score = (TextView) findViewById(R.id.driving_score);

        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (faceIsReady) {
                    STATE = AppState.Active;
                    start_btn.setVisibility(View.INVISIBLE);
                    if (!bandIsReady) {
                        Log.d(TAG, "No Band, Starting App logic");
                        alertManager.setBandDisabled();
                    }
                    driving_screen.setVisibility(View.VISIBLE);
                }
            }
        });

        this.initFrontCamera();
        // Start thread that takes picture every 5 seconds and starts 1.5 seconds after app init
        startPictureTaker();
        // Start thread that takes RR interval
        new RRIntervalSubscriptionTask(this).execute();
    }

    private void startPictureTaker(){
        Log.i(this.TAG, "Starting Picture Taker");
        this.pictureTakingTimer = new PictureTakingTimer(new PictureTaker());
        this.pictureTakingTimer.start();
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

    //Listener to detect when Face is detected
    public interface onFaceDetectionListener {
        public void onFaceDetection();
        public void onFaceNotDetected();
    }

    public void setOnFaceDetectionEventListener(onFaceDetectionListener eventListener) {
        this.initFaceDetectionListener = eventListener;
    }

    //Listener to detect when Band is recognized
    public interface onBandDetectionListener {
        public void onBandDetection();
    }

    public void setOnBandDetectionEventListener(onBandDetectionListener eventListener) {
        this.initBandDetectionListener = eventListener;
    }

    //Listener to change driving view components
    public interface onAlertnessScoreUpdateListener {
        public void onScoreUpdate(double score);
    }

    public void setOnAlertnessScoreUpdateListener(onAlertnessScoreUpdateListener eventListener) {
        this.alertnessScoreUpdateListener = eventListener;
    }

    //Listener to detect both face and band are good and we are ready to start
    public interface onDetectionCompletionEventListener {
        public void onCompletion();
    }

    public void setOnDetectionCompletionEventListener(onDetectionCompletionEventListener eventListener) {
        this.initDetectionCompletion = eventListener;
    }



}

