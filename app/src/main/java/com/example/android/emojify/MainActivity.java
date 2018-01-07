/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.emojify;


import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.example.android.emojify.EmojiFragment;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 1;

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";

    private ImageView mImageView;

    private Button mEmojifyButton;
    private FloatingActionButton mShareFab;
    private FloatingActionButton mSaveFab;
    private FloatingActionButton mClearFab;

    private TextView mTitleTextView;

    private String mTempPhotoPath;

    private Bitmap mResultsBitmap;

    private enum EmojiType {
        CLOSED_EYE_FROWN,
        CLOSED_EYE_SMILE,
        LEFT_WINK_FROWN,
        LEFT_WINK_SMILE,
        RIGHT_WINK_FROWN,
        RIGHT_WINK_SMILE,
        OPEN_EYE_FROWN,
        OPEN_EYE_SMILE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind the views
        mImageView = (ImageView) findViewById(R.id.image_view);
        mEmojifyButton = (Button) findViewById(R.id.emojify_button);
        mShareFab = (FloatingActionButton) findViewById(R.id.share_button);
        mSaveFab = (FloatingActionButton) findViewById(R.id.save_button);
        mClearFab = (FloatingActionButton) findViewById(R.id.clear_button);
        mTitleTextView = (TextView) findViewById(R.id.title_text_view);
    }

    /**
     * OnClick method for "Emojify Me!" Button. Launches the camera app.
     *
     * @param view The emojify me button.
     */
    public void emojifyMe(View view) {
        // Check for the external storage permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // If you do not have permission, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            // Launch the camera if the permission exists
            launchCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, launch the camera
                    launchCamera();
                } else {
                    // If you do not get permission, show a Toast
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    /**
     * Creates a temporary image file and captures a picture to store in it.
     */
    private void launchCamera() {

        // Create the capture image intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the temporary File where the photo should go
            File photoFile = null;
            try {
                photoFile = BitmapUtils.createTempImageFile(this);
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                // Get the path of the temporary file
                mTempPhotoPath = photoFile.getAbsolutePath();

                // Get the content URI for the image file
                Uri photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile);

                // Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Launch the camera activity
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the image capture activity was called and was successful
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Process the image and set it to the TextView
            processAndSetImage();
        } else {

            // Otherwise, delete the temporary image file
            BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        }
    }

    /**
     * Method for processing the captured image and setting it to the TextView.
     */
    private void processAndSetImage() {

        // Toggle Visibility of the views
        mEmojifyButton.setVisibility(View.GONE);
        mTitleTextView.setVisibility(View.GONE);
        mSaveFab.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.VISIBLE);
        mClearFab.setVisibility(View.VISIBLE);

        // Resample the saved image to fit the ImageView
        mResultsBitmap = BitmapUtils.resamplePic(this, mTempPhotoPath);

        // Set the new bitmap to the ImageView
        mImageView.setImageBitmap(mResultsBitmap);

        // face detection
        EmojiType[] emojiType = detectFace(mResultsBitmap);
        insertFragment(emojiType);

    }

    private void insertFragment(EmojiType[] emojiType)
    {
        /*
         * for now, just insert emoji
         *
         * next step will want to draw emoji over face and align size, rotation
         */

        EmojiFragment emojiFragment = new EmojiFragment();
        if(null!=emojiType && emojiType.length>0)
        {
            int i = emojiType[0].ordinal();
            emojiFragment.setIndex(i);
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.head_container, emojiFragment)
                .commit();
    }

    private EmojiType[] detectFace(Bitmap bitmap)
    {
        com.google.android.gms.vision.face.FaceDetector detector = new com.google.android.gms.vision.face.FaceDetector.Builder(getApplicationContext())
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
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                   Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                // Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        EmojiType[] emojiType = ClassifyEmojiType(faces);

        // FaceView overlay = (FaceView) findViewById(R.id.faceView);
        // overlay.setContent(bitmap, faces);

        // Although detector may be used multiple times for different images, it should be released
        // when it is no longer needed in order to free native resources.
        safeDetector.release();

        return emojiType;
    }

    private EmojiType[] ClassifyEmojiType(SparseArray<Face> faces)
    {
        EmojiType[] emojiSelect = new EmojiType[faces.size()];
        float threshold = 0.5f;
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


    /**
     * OnClick method for the save button.
     *
     * @param view The save button.
     */
    public void saveMe(View view) {
        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        BitmapUtils.saveImage(this, mResultsBitmap);
    }

    /**
     * OnClick method for the share button, saves and shares the new bitmap.
     *
     * @param view The share button.
     */
    public void shareMe(View view) {
        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        BitmapUtils.saveImage(this, mResultsBitmap);

        // Share the image
        BitmapUtils.shareImage(this, mTempPhotoPath);
    }

    /**
     * OnClick for the clear button, resets the app to original state.
     *
     * @param view The clear button.
     */
    public void clearImage(View view) {
        // Clear the image and toggle the view visibility
        mImageView.setImageResource(0);
        mEmojifyButton.setVisibility(View.VISIBLE);
        mTitleTextView.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.GONE);
        mSaveFab.setVisibility(View.GONE);
        mClearFab.setVisibility(View.GONE);

        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);
    }
}
