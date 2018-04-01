package com.drivesafe.drivesafe;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.microsoft.projectoxford.face.common.RequestMethod;
import com.microsoft.projectoxford.face.contract.FaceLandmarks;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.contract.FeatureCoordinate;
import com.microsoft.projectoxford.face.rest.ClientException;
import com.microsoft.projectoxford.face.rest.WebServiceRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetectionTask extends AsyncTask<ByteArrayOutputStream, String, PhotoHandler.Face[]> {
    private String TAG = "PhotoHandler";
    private final String api_endpoint = "https://westeurope.api.cognitive.microsoft.com/face/v1.0";
    private Gson mGson = (new GsonBuilder()).setDateFormat("M/d/yyyy h:m:s a").create();
    private final String sub_key = "f828f841dd4642249e6c9fcd69784bed";
    private final WebServiceRequest mRestCall = new WebServiceRequest(sub_key);

    @SuppressLint("StaticFieldLeak")
    private final MainActivity mainActivity;

    DetectionTask(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    protected PhotoHandler.Face[] doInBackground(ByteArrayOutputStream... params) {
        PhotoHandler.Face[] result = new PhotoHandler.Face[]{};
        try {
            result = myDetect(
                    params[0], // image stream
                    true,         // returnFaceId
                    true,        // returnFaceLandmarks
                    "age,occlusion"           // returnFaceAttributes: a string like "age, gender"
            );

            double averageOcclusion = getAverageOcclusion(result[0].faceLandmarks, result[0].faceRectangle);
            Log.i(TAG, String.format("Found Occclusion: %f", averageOcclusion));
            //notify listener that face was detected
            if (MainActivity.initFaceDetectionListener != null)
                MainActivity.initFaceDetectionListener.onFaceDetection();

            if (MainActivity.STATE == Auxiliary.AppState.Active) {
                Log.i(TAG, "Adding occlusion to history");
                OcclusionHistory.add(averageOcclusion, mainActivity.alertManager);
            }

        } catch (Exception e) {
            Log.i(TAG,"Failed at detecting face in image");
            e.printStackTrace();
            MainActivity.initFaceDetectionListener.onFaceNotDetected();
        }

        return result;
    }

    private double getAverageOcclusion(FaceLandmarks landmarks, FaceRectangle rectangle){
        double occlusionLeft = getDistance(landmarks.eyeLeftTop, landmarks.eyeLeftBottom);
        double occlusionRight = getDistance(landmarks.eyeRightTop, landmarks.eyeRightBottom);
        double avg_occ = (occlusionLeft + occlusionRight) / 2;
        return (avg_occ / rectangle.height) * 100;
    }

    private double getDistance(FeatureCoordinate feat1, FeatureCoordinate feat2){
        double deltaX = feat1.x - feat2.x;
        double deltaY = feat1.y - feat2.y;
        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
    }

    private PhotoHandler.Face[] myDetect(ByteArrayOutputStream byteArrayOutputStream, boolean returnFaceId, boolean returnFaceLandmarks, String returnFaceAttributes) throws ClientException, IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("returnFaceId", returnFaceId);
        params.put("returnFaceLandmarks", returnFaceLandmarks);
        params.put("returnFaceAttributes", returnFaceAttributes);

        String path = String.format("%s/%s", this.api_endpoint, "detect");
        String uri = WebServiceRequest.getUrl(path, params);

        byte[] data = byteArrayOutputStream.toByteArray();
        params.clear();
        params.put("data", data);
        String json = (String)this.mRestCall.request(uri, RequestMethod.POST, params, "application/octet-stream");
        Type listType = (new TypeToken<List<PhotoHandler.Face>>() {
        }).getType();
        List<PhotoHandler.Face> faces = this.mGson.fromJson(json, listType);
        return faces.toArray(new PhotoHandler.Face[faces.size()]);
    }
}
