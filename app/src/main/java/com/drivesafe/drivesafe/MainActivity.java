    package com.drivesafe.drivesafe;


    import android.content.pm.PackageManager;
    import android.support.v7.app.AppCompatActivity;
    import android.os.Bundle;
    import android.app.*;
    import android.view.*;
    import android.widget.*;
    import android.hardware.Camera;
    import java.util.Timer;


    public class MainActivity extends AppCompatActivity {

        public static ProgressDialog detectionProgressDialog;
        public static ImageView imageView;
        public static Camera camera;
        public static Camera.PictureCallback pictureCallback;

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
    //        Thread that takes picture every 3 seconds and starts 1.5 seconds after app init
    //        PictureTaker myTask = new PictureTaker();
    //        Timer myTimer = new Timer();
    //        myTimer.schedule(myTask, 1500, 3000);
        }
        private void take_picture() {
            camera.startPreview();
            camera.takePicture(null, null, null, this.pictureCallback);
        }

        private void initFrontCamera() {
            if (getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                camera = Camera.open(this.findFrontFacingCamera());
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
                    break;
                }
            }
            return cameraId;
        }

    }
