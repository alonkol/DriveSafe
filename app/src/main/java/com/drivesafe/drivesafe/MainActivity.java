package com.drivesafe.drivesafe;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.io.*;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.graphics.*;
import android.widget.*;
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

    private final int PICK_IMAGE = 1;
    private ProgressDialog detectionProgressDialog;
    private final String sub_key = "f828f841dd4642249e6c9fcd69784bed";
    private final String api_endpoint = "https://westeurope.api.cognitive.microsoft.com/face/v1.0";

    // copied from FaceServiceRestClient
    private final WebServiceRequest mRestCall = new WebServiceRequest(sub_key);
    private Gson mGson = (new GsonBuilder()).setDateFormat("M/d/yyyy h:m:s a").create();

    // band
    private BandClient client = null;

    public Activity that = this;

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
        Button button1 = (Button)findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                gallIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
            }

        });
        detectionProgressDialog = new ProgressDialog(this);
        new RRIntervalSubscriptionTask().execute();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ImageView imageView = (ImageView) findViewById(R.id.imageView1);
                imageView.setImageBitmap(bitmap);
                detectAndFrame(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Detect faces by uploading face images
    // Frame faces after detection

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
                    FaceLandmarks landmarks = result[0].faceLandmarks;
                    // TODO: change to sqrt(dx^2 + dy^2). Currently vertical only
                    // TODO: why is eyeLeftBottom higher than eyeLeftTop???
                    double occlusionLeft = landmarks.eyeLeftTop.y - landmarks.eyeLeftBottom.y;
                    double occlusionRight = landmarks.eyeRightTop.y - landmarks.eyeRightBottom.y;
                    double averageOcclusion = (occlusionLeft + occlusionRight) / 2;

                } catch (Exception e) {
                    // TODO
                }

                return result;
            }
        };
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

                        if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                            client.getSensorManager().registerRRIntervalEventListener(mRRIntervalEventListener);
                        } else {
                            client.getSensorManager().requestHeartRateConsent(that, new HeartRateConsentListener(){

                                @Override
                                public void userAccepted(boolean b) {

                                }
                            });

                            // appendToUI("You have not given this application consent to access heart rate data yet."
                            //        + " Please press the Heart Rate Consent button.\n");
                        }
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
                // appendToUI(exceptionMessage);

            } catch (Exception e) {
                Exception e2 = e;
                // appendToUI(e.getMessage());
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

