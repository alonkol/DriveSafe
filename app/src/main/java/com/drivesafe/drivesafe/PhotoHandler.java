    package com.drivesafe.drivesafe;

    import java.io.ByteArrayOutputStream;
    import java.io.IOException;
    import java.lang.reflect.Type;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.UUID;

    import android.content.Context;
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.graphics.Matrix;
    import android.hardware.Camera;
    import android.hardware.Camera.PictureCallback;
    import android.os.AsyncTask;
    import android.util.Log;

    import com.google.gson.Gson;
    import com.google.gson.GsonBuilder;
    import com.google.gson.reflect.TypeToken;
    import com.microsoft.projectoxford.face.common.RequestMethod;
    import com.microsoft.projectoxford.face.contract.*;
    import com.microsoft.projectoxford.face.rest.ClientException;
    import com.microsoft.projectoxford.face.rest.WebServiceRequest;

    import com.drivesafe.drivesafe.Auxiliary.*;

    public class PhotoHandler implements PictureCallback  {

        private final String sub_key = "f828f841dd4642249e6c9fcd69784bed";
        private final String api_endpoint = "https://westeurope.api.cognitive.microsoft.com/face/v1.0";
        // copied from FaceServiceRestClient
        private final WebServiceRequest mRestCall = new WebServiceRequest(sub_key);
        private Gson mGson = (new GsonBuilder()).setDateFormat("M/d/yyyy h:m:s a").create();

        private final Context context;
        private final MainActivity mainActivity;

        public PhotoHandler(Context context, MainActivity mainActivity) {
            this.context = context;
            this.mainActivity = mainActivity;
        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            bitmap = RotateBitmap(bitmap, 270);
            MainActivity.imageView.setImageBitmap(bitmap);
            detectAndFrame(bitmap);
        }

        public static Bitmap RotateBitmap(Bitmap source, float angle)
        {
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        }

        private void detectAndFrame(final Bitmap imageBitmap)
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            DetectionTask().execute(outputStream);
        }

        public AsyncTask<ByteArrayOutputStream, String, Face[]> DetectionTask(){
            return new AsyncTask<ByteArrayOutputStream, String, Face[]>() {
                public String occlusionTag = "OcclusionDetector";

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
                        Log.i(this.occlusionTag, String.format("Found Occclusion: %f", averageOcclusion));
                        //notify listener that face was detected
                        if (mainActivity.initFaceDetectionListener != null)
                            mainActivity.initFaceDetectionListener.onFaceDetection();

                        //check if we can start now
                        //TODO: change 2 for an enum
                        if (mainActivity.bandIsReady && mainActivity.faceIsReady) {
                            if (mainActivity.initDetectionCompletion != null) {
                                mainActivity.initDetectionCompletion.onCompletion();
                            }
                        }

                        OcclusionHistory.add(averageOcclusion);
                        if (OcclusionHistory.getAlertnessLevel() == AlertnessLevel.Low){
                            mainActivity.pictureTakingTimer.setHighRate();
                            SoundManager.Alert(context);
                        }

                        if (OcclusionHistory.getAlertnessLevel() == AlertnessLevel.Medium){
                            mainActivity.pictureTakingTimer.setHighRate();
                        }

                    } catch (Exception e) {
                        Log.i(this.occlusionTag,"Failed");
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

        static class Face {
            public UUID faceId;
            public FaceRectangle faceRectangle;
            public FaceLandmarks faceLandmarks;
            public FaceAttribute faceAttributes;

            public Face() {
            }
        }

        // TODO: if necessary, add property for Occlusion etc.
        static class FaceAttribute {
            public double age;
            public String gender;
            public double smile;
            public FacialHair facialHair;
            public HeadPose headPose;

            public FaceAttribute() {
            }
        }
    }
