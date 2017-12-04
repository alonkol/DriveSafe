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
import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.common.RequestMethod;
import com.microsoft.projectoxford.face.contract.*;
import com.microsoft.projectoxford.face.rest.ClientException;
import com.microsoft.projectoxford.face.rest.WebServiceRequest;


import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final int PICK_IMAGE = 1;
    private ProgressDialog detectionProgressDialog;
    private final String sub_key = "f828f841dd4642249e6c9fcd69784bed";
    private final String api_endpoint = "https://westeurope.api.cognitive.microsoft.com/face/v1.0";

    // copied from FaceServiceRestClient
    private final WebServiceRequest mRestCall = new WebServiceRequest(sub_key);
    private Gson mGson = (new GsonBuilder()).setDateFormat("M/d/yyyy h:m:s a").create();


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

        AsyncTask<ByteArrayOutputStream, String, Face[]> detectTask =
                new AsyncTask<ByteArrayOutputStream, String, Face[]>() {
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
        detectTask.execute(outputStream);


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

