    package com.drivesafe.drivesafe;

    import java.io.ByteArrayOutputStream;
    import java.util.UUID;

    import android.content.Context;
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.graphics.Matrix;
    import android.hardware.Camera;
    import android.hardware.Camera.PictureCallback;
    import com.microsoft.projectoxford.face.contract.*;

    public class PhotoHandler implements PictureCallback  {
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
            if (mainActivity.STATE == Auxiliary.AppState.Init) {
                mainActivity.mainImage.setImageBitmap(bitmap);
            }
            detectAndFrame(bitmap);
        }

        private static Bitmap RotateBitmap(Bitmap source, float angle)
        {
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        }

        private void detectAndFrame(final Bitmap imageBitmap)
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            mainActivity.alertManager.setCurrentImage(outputStream);
            ByteArrayOutputStream outputStreamCompressed = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 10, outputStreamCompressed);
            new DetectionTask(mainActivity).execute(outputStreamCompressed);
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
