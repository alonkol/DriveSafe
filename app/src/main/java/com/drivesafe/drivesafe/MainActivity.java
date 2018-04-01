package com.drivesafe.drivesafe;

import android.Manifest;
import android.content.Intent;
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

import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main Activity";
    public ImageView mainImage;
    public TextView band_rec;
    public TextView face_rec;
    public Button start_btn;
    public static Camera camera;
    public static Camera.PictureCallback pictureCallback;
    public Camera.CameraInfo cameraInfo;
    public Activity mainActivityReference = this;
    public AlertManager alertManager;
    public DataSender dataSender;
    public static PictureTakingTimer pictureTakingTimer;
    public static onFaceDetectionListener initFaceDetectionListener = null;
    public onBandDetectionListener initBandDetectionListener = null;
    public onDetectionCompletionEventListener initDetectionCompletion = null;
    public static boolean faceIsReady = false;
    public static boolean bandIsReady = false;
    public boolean displaySpecialNotification = false;
    private boolean demoMode = true;
    public static Auxiliary.AppState STATE = AppState.Init;
    private final int PERMISSION_REQUEST_FOR_APP = 100;
    public static View driving_screen;
    private ImageView driving_image;
    private TextView driving_text;
    private TextView driving_score;
    public onAlertnessScoreUpdateListener alertnessScoreUpdateListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.mainImage = (ImageView)findViewById(R.id.main_image);
        this.alertManager = new  AlertManager(this);
        this.dataSender = new DataSender(this);
        if (demoMode)
        {
            displaySpecialNotification = true;
        }
        this.setOnFaceDetectionEventListener(new onFaceDetectionListener() {
            @Override
            public void onFaceDetection() {
                if (!faceIsReady && STATE == AppState.Init) {
                    faceIsReady=true;
                    Log.d(TAG, "Face detected!");
                    try {
                        runOnUiThread(new Runnable() {@Override public void run() {
                            face_rec.setText(R.string.face_detected);
                            face_rec.setTextColor(getResources().getColor(R.color.button_green));
                            start_btn.setBackgroundColor(getResources().getColor(R.color.button_green));
                            start_btn.setTextColor(Color.WHITE);
                        }});
                    } catch (Exception e) {
                        Log.e("FaceDetectedException", e.toString());
                    }
                }
            }

            @Override
            public void onFaceNotDetected() {
                if (faceIsReady && STATE == AppState.Init) {
                    faceIsReady=false;
                    Log.d(TAG, "Face not detected");
                    try {
                        runOnUiThread(new Runnable() {@Override public void run()
                        {
                            face_rec.setText(R.string.face_detecting);
                            face_rec.setTextColor(getResources().getColor(R.color.button_disabled));
                            start_btn.setBackgroundColor(getResources().getColor(R.color.button_disabled));
                            start_btn.setTextColor(Color.GRAY);
                        }});
                        pictureTakingTimer.setHighRate();
                    } catch (Exception e) {
                        Log.e("NotDetectedException", e.toString());
                    }

                }
            }
        });

        this.setOnBandDetectionEventListener(new onBandDetectionListener() {
            @Override
            public void onBandDetection() {
                if (!bandIsReady && STATE == AppState.Init) {
                    runOnUiThread(new Runnable() {@Override public void run()
                    {
                        band_rec.setText(R.string.band_detected);
                        band_rec.setTextColor(getResources().getColor(R.color.button_green));
                    }});
                    bandIsReady=true;
                }
            }
        });

        this.setOnAlertnessScoreUpdateListener(new onAlertnessScoreUpdateListener() {
            @Override
            public void onScoreUpdate(double alertnessScore) {
                if (STATE == AppState.Active)
                {
                    final int new_text;
                    final int new_image;
                    final int new_color;
                    if (alertnessScore < AlertManager.highRiskScore){
                        new_text = R.string.driving_alert_title;
                        new_image = R.drawable.face_fear;
                        new_color = getResources().getColor(R.color.driving_alert);
                    }
                    else if (alertnessScore < AlertManager.mediumRiskScore){
                        new_text = R.string.driving_warning_title;
                        new_image = R.drawable.face_worried;
                        new_color = getResources().getColor(R.color.driving_warning);
                    }
                    else{
                        new_text = R.string.driving_normal_title;
                        new_image = R.drawable.face_smile;
                        new_color = getResources().getColor(R.color.driving_normal);
                    }

                    final double alertness = alertnessScore;
                    runOnUiThread(new Runnable() {@Override public void run()
                    {
                        driving_score.setText(String.format("Alertness Score: %.1f", alertness));
                        driving_text.setText(new_text);
                        driving_image.setImageResource(new_image);
                        driving_screen.setBackgroundColor(new_color);
                    }});

                }
            }
        });

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
        new DataReceiver(this).start();
        MainActivity.pictureCallback = new PhotoHandler(getApplicationContext(), this);
        this.band_rec = findViewById(R.id.band_rec);
        this.face_rec = findViewById(R.id.face_rec);
        this.start_btn = findViewById(R.id.start_btn);
        MainActivity.driving_screen = findViewById(R.id.driving_screen);
        this.driving_image = findViewById(R.id.driving_image);
        this.driving_text = findViewById(R.id.driving_text);
        this.driving_score = findViewById(R.id.driving_score);

        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (faceIsReady) {
                    start_btn.setVisibility(View.INVISIBLE);
                    if (displaySpecialNotification) {
                        try {
                            startActivity(new Intent(MainActivity.this, SpecialNoticeActivity.class));
                        }catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        STATE = AppState.Active;
                        if (!bandIsReady) {
                            Log.d(TAG, "No Band, Starting App logic");
                            pictureTakingTimer.setHighRate();
                        }
                        driving_screen.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        this.initFrontCamera();
        // Start thread that takes picture every 5 seconds and starts 1.5 seconds after app init
        startPictureTaker();
        // Start thread that takes RR interval
        new RRIntervalTaker(this).start();
    }

    private void startPictureTaker(){
        Log.i(this.TAG, "Starting Picture Taker");
        MainActivity.pictureTakingTimer = new PictureTakingTimer(new PictureTaker());
        MainActivity.pictureTakingTimer.start();
        MainActivity.pictureTakingTimer.setHighRate();
    }

    private void initFrontCamera(){
        if (getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                camera = Camera.open(this.findFrontFacingCamera());
                Camera.Parameters parameters = camera.getParameters();
                List<Camera.Size> supportedSizes = parameters.getSupportedPictureSizes();
                int listSize = supportedSizes.size();
                parameters.setPictureSize(supportedSizes.get(listSize/2).width,
                        supportedSizes.get(listSize/2).height);
                camera.setParameters(parameters);
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





}

