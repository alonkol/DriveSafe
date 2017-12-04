    package com.drivesafe.drivesafe;

    import java.io.ByteArrayInputStream;
    import java.io.ByteArrayOutputStream;
    import java.io.File;
    import java.io.FileOutputStream;
    import java.io.InputStream;
    import java.text.SimpleDateFormat;
    import java.util.Date;

    import android.content.Context;
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.graphics.Canvas;
    import android.graphics.Color;
    import android.graphics.Paint;
    import android.hardware.Camera;
    import android.hardware.Camera.PictureCallback;
    import android.os.AsyncTask;

    import com.microsoft.projectoxford.face.contract.Face;
    import com.microsoft.projectoxford.face.contract.FaceRectangle;
    import com.microsoft.projectoxford.face.*;
    import com.microsoft.projectoxford.face.contract.*;

    public class PhotoHandler implements PictureCallback  {

        private final String sub_key = "f828f841dd4642249e6c9fcd69784bed";
        private final String api_endpoint = "https://westeurope.api.cognitive.microsoft.com/face/v1.0/detect?returnFaceId=true&returnFaceLandmarks=false";
        public FaceServiceClient faceServiceClient =
                new FaceServiceRestClient(api_endpoint, sub_key);
        private final Context context;

        public PhotoHandler(Context context) {
            this.context = context;
        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            MainActivity.imageView.setImageBitmap(bitmap);
            // detectAndFrame(bitmap);
        }

        // Detect faces by uploading face images
        // Frame faces after detection
        private void detectAndFrame(final Bitmap imageBitmap)
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(outputStream.toByteArray());
            AsyncTask<InputStream, String, Face[]> detectTask =
                    new AsyncTask<InputStream, String, Face[]>() {
                        @Override
                        protected Face[] doInBackground(InputStream... params) {
                            try {
                                publishProgress("Detecting...");
                                Face[] result = faceServiceClient.detect(
                                        params[0],
                                        true,         // returnFaceId
                                        false,        // returnFaceLandmarks
                                        null           // returnFaceAttributes: a string like "age, gender"
                                );
                                if (result == null)
                                {
                                    publishProgress("Detection Finished. Nothing detected");
                                    return null;
                                }
                                publishProgress(
                                        String.format("Detection Finished. %d face(s) detected",
                                                result.length));
                                return result;
                            } catch (Exception e) {
                                publishProgress("Detection failed");
                                return null;
                            }
                        }
                        @Override
                        protected void onPreExecute() {

                            MainActivity.detectionProgressDialog.show();
                        }
                        @Override
                        protected void onProgressUpdate(String... progress) {

                            MainActivity.detectionProgressDialog.setMessage(progress[0]);
                        }
                        @Override
                        protected void onPostExecute(Face[] result) {

                            MainActivity.detectionProgressDialog.dismiss();
                            if (result == null) return;
                            MainActivity.imageView.setImageBitmap(drawFaceRectanglesOnBitmap(imageBitmap, result));
                            imageBitmap.recycle();
                        }
                    };
            detectTask.execute(inputStream);
        }

        private static Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, Face[] faces) {
            Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.RED);
            int stokeWidth = 2;
            paint.setStrokeWidth(stokeWidth);
            if (faces != null) {
                for (Face face : faces) {
                    FaceRectangle faceRectangle = face.faceRectangle;
                    canvas.drawRect(
                            faceRectangle.left,
                            faceRectangle.top,
                            faceRectangle.left + faceRectangle.width,
                            faceRectangle.top + faceRectangle.height,
                            paint);
                }
            }
            return bitmap;
        }
    }
