package com.example.android.emojify;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

/**
 * Created by ctyeung on 1/13/18.
 */

public class Emojifier {

    public enum EmojiType {
        CLOSED_EYE_FROWN,
        CLOSED_EYE_SMILE,
        LEFT_WINK_FROWN,
        LEFT_WINK_SMILE,
        RIGHT_WINK_FROWN,
        RIGHT_WINK_SMILE,
        OPEN_EYE_FROWN,
        OPEN_EYE_SMILE};

    public static EmojiType[] detectFaces(Context context,
                                          Bitmap bitmap)
    {
        com.google.android.gms.vision.face.FaceDetector detector = new com.google.android.gms.vision.face.FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        // This is a temporary workaround for a bug in the face detector with respect to operating
        // on very small images.  This will be fixed in a future release.  But in the near term, use
        // of the SafeFaceDetector class will patch the issue.
        Detector<Face> safeDetector = new SafeFaceDetector(detector);

        // Create a frame from the bitmap and run face detection on the frame.
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> faces = safeDetector.detect(frame);
        boolean hasLowStorage = false;

        if (!safeDetector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            //Log.w(TAG, "Face detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            hasLowStorage = context.registerReceiver(null, lowstorageFilter) != null;
        }

        EmojiType[] emojiType = null;

        if(false==hasLowStorage)
        {
            emojiType = ClassifyEmojiType(context, faces);

            // FaceView overlay = (FaceView) findViewById(R.id.faceView);
            // overlay.setContent(bitmap, faces);

            // Although detector may be used multiple times for different images, it should be released
            // when it is no longer needed in order to free native resources.
            safeDetector.release();
        }

        return emojiType;
    }

    private static EmojiType[] ClassifyEmojiType(Context context, SparseArray<Face> faces)
    {
        EmojiType[] emojiSelect = new EmojiType[faces.size()];
        float threshold = 0.5f;

        int numOfFaces = faces.size();

        if(0==numOfFaces)
            Toast.makeText(context, "No faces detected", Toast.LENGTH_SHORT).show();


        // number of faces
        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.valueAt(i);
            for (Landmark landmark : face.getLandmarks())
            {
                float leftEyeOpen = face.getIsLeftEyeOpenProbability();
                float rightEyeOpen = face.getIsRightEyeOpenProbability();
                float smile = face.getIsSmilingProbability();

                int type = landmark.getType();
                int cx = (int) landmark.getPosition().x;
                int cy = (int) landmark.getPosition().y;

                if(smile > threshold)
                {
                    if(leftEyeOpen > threshold && rightEyeOpen > threshold)
                    {// open
                        emojiSelect[i] = EmojiType.OPEN_EYE_SMILE;
                    }
                    else if (leftEyeOpen < threshold && rightEyeOpen < threshold)
                    {// closed
                        emojiSelect[i] = EmojiType.CLOSED_EYE_SMILE;
                    }
                    else if(rightEyeOpen > threshold)
                    {// left wink
                        emojiSelect[i] = EmojiType.LEFT_WINK_SMILE;
                    }
                    else
                        emojiSelect[i] = EmojiType.RIGHT_WINK_SMILE;

                }
                else // frown
                {
                    if(leftEyeOpen > threshold && rightEyeOpen > threshold)
                    {// open
                        emojiSelect[i] = EmojiType.OPEN_EYE_FROWN;
                    }
                    else if (leftEyeOpen < threshold && rightEyeOpen < threshold)
                    {// closed
                        emojiSelect[i] = EmojiType.CLOSED_EYE_FROWN;
                    }
                    else if(rightEyeOpen > threshold)
                    {// left wink
                        emojiSelect[i] = EmojiType.LEFT_WINK_FROWN;
                    }
                    else
                        emojiSelect[i] = EmojiType.RIGHT_WINK_FROWN;
                }
            }
        }
        return emojiSelect;
    }

}
