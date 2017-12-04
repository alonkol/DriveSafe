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
import android.provider.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.common.RequestMethod;
import com.microsoft.projectoxford.face.contract.*;
import com.microsoft.projectoxford.face.rest.ClientException;
import com.microsoft.projectoxford.face.rest.WebServiceRequest;


import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;


public class MainActivity extends AppCompatActivity {

    public static ProgressDialog detectionProgressDialog;
    public static ImageView imageView;
    public static Camera camera;
    public static Camera.PictureCallback pictureCallback;
    public Camera.CameraInfo cameraInfo;


    // copied from FaceServiceRestClient
    private final WebServiceRequest mRestCall = new WebServiceRequest(sub_key);
    private Gson mGson = (new GsonBuilder()).setDateFormat("M/d/yyyy h:m:s a").create();

    // band
    private BandClient client = null;

    public Activity mainActivityReference = this;

    private BandRRIntervalEventListener mRRIntervalEventListener = new BandRRIntervalEventListener() {
        @Override
        public void onBandRRIntervalChanged(final BandRRIntervalEvent event) {
            if (event != null) {
                double interval = event.getInterval();
            }
        }
    };

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
        new RRIntervalSubscriptionTask().execute();

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

    private void detectAndFrame(final Bitmap imageBitmap)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

        DetectionTask().execute(outputStream);
    }

    public AsyncTask<ByteArrayOutputStream, String, Face[]> DetectionTask(){
        return new AsyncTask<ByteArrayOutputStream, String, Face[]>() {
            @Override
            protected Face[] doInBackground(ByteArrayOutputStream... params) {
                Face[] result = new Face[]{};
                try {
                    result = myDetect(
                            params[0], // image stream
                            true,         // returnFaceId
                            true,        // returnFaceLandmarks
                            "age,occlusion"           // returnFaceAttributes: a string like "age, gender"
                    );
                    if (result == null)
                    {
                        // TODO
                    }

                    double averageOcclusion = getAverageOcclusion(result[0].faceLandmarks);

                    // TODO: On consistent low occlusion, sound alert

                } catch (Exception e) {
                    // TODO
                }

                return result;
            }
        };
    }

    public double getAverageOcclusion(FaceLandmarks landmarks){
        double occlusionLeft = getDistance(landmarks.eyeRightTop, landmarks.eyeRightBottom);
        double occlusionRight = getDistance(landmarks.eyeRightTop, landmarks.eyeRightBottom);
        return (occlusionLeft + occlusionRight) / 2;
    }

    public double getDistance(FeatureCoordinate feat1, FeatureCoordinate feat2){
        double deltaX = feat1.x - feat2.x;
        double deltaY = feat1.y - feat2.y;
        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
    }

    public Face[] myDetect(ByteArrayOutputStream byteArrayOutputStream, boolean returnFaceId, boolean returnFaceLandmarks, String returnFaceAttributes) throws ClientException, IOException {
        Map<String, Object> params = new HashMap();
        params.put("returnFaceId", Boolean.valueOf(returnFaceId));
        params.put("returnFaceLandmarks", Boolean.valueOf(returnFaceLandmarks));
        params.put("returnFaceAttributes", returnFaceAttributes);

        String path = String.format("%s/%s", new Object[]{this.api_endpoint, "detect"});
        String uri = WebServiceRequest.getUrl(path, params);

        byte[] data = byteArrayOutputStream.toByteArray();
        params.clear();
        params.put("data", data);
        String json = (String)this.mRestCall.request(uri, RequestMethod.POST, params, "application/octet-stream");
        Type listType = (new TypeToken<List<Face>>() {
        }).getType();
        List<Face> faces = (List)this.mGson.fromJson(json, listType);
        return faces.toArray(new Face[faces.size()]);
    }

    private class RRIntervalSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {

                        if (client.getSensorManager().getCurrentHeartRateConsent() != UserConsent.GRANTED) {
                            client.getSensorManager().requestHeartRateConsent(mainActivityReference, new HeartRateConsentListener(){

                                @Override
                                public void userAccepted(boolean b) {

                                }
                            });
                        }

                        client.getSensorManager().registerRRIntervalEventListener(mRRIntervalEventListener);
                    } else {
                        // appendToUI("The RR Interval sensor is not supported with your Band version. Microsoft Band 2 is required.\n");
                    }
                } else {
                    //appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                // TODO: do something with exception?

            } catch (Exception e) {
                // TODO: do something with exception?
            }
            return null;
        }
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        return ConnectionState.CONNECTED == client.connect().await();
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

