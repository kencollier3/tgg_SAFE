package org.utos.android.safe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.utos.android.safe.aws.Constants;
import org.utos.android.safe.dialogs.AttachAudioDialog;
import org.utos.android.safe.dialogs.AttachImageDialog;
import org.utos.android.safe.dialogs.AttachVideoDialog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.provider.ContactsContract.CommonDataKinds.StructuredName.SUFFIX;

public class NonUrgentActivity extends BaseActivity {

    public final String TAG = "NonUrgentActivity";

    // Activity request codes
    public static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int SELECT_IMAGE_ACTIVITY_REQUEST_CODE = 200;
    public static final int SELECT_VIDEO_SELECTION_REQUEST_CODE = 300;
    public static final int CAPTURE_VIDEO_SELECTION_REQUEST_CODE = 400;

    // directory name to store captured images and videos
    public String REPORT_DIRECTORY_NAME;

    // media types
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    public AppCompatButton attachImageButton, attachVoiceButton, attachVideoButton;
    private Spinner spinner;
    private EditText editTextDesc;

    public String mCurrentImagePath, mCurrentVideoPath, whatToDo;

    public ArrayList<String> imageArray, videoArray, audioArray;

    //    private ProgressDialog mProgressDialogDownload;


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_non_urgent);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.colorYellow));

        // create file structure
        REPORT_DIRECTORY_NAME = getExternalFilesDir(null).getAbsolutePath() + File.separator + "Report";
        // External location
        File mediaStorageDir = new File(REPORT_DIRECTORY_NAME);
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(REPORT_DIRECTORY_NAME, "Oops! Failed create " + REPORT_DIRECTORY_NAME + " directory");
            }
        }
        // create file structure

        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // set title works when language change
        setTitle(getString(R.string.btn_non_urgent));

        imageArray = new ArrayList<>();
        videoArray = new ArrayList<>();
        audioArray = new ArrayList<>();

        // setup up nav
        assert getSupportActionBar() != null;
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // UI
        editTextDesc = (EditText) findViewById(R.id.editTextDesc);
        spinner = (Spinner) findViewById(R.id.spinnerCat);
        attachImageButton = (AppCompatButton) findViewById(R.id.attachImage);
        attachVoiceButton = (AppCompatButton) findViewById(R.id.attachVoice);
        attachVideoButton = (AppCompatButton) findViewById(R.id.attachVideo);
        AppCompatButton submitButton = (AppCompatButton) findViewById(R.id.submitButton);

        // set images
        submitButton.setCompoundDrawablesWithIntrinsicBounds(null, null, VectorDrawableCompat.create(getResources(), R.drawable.ic_send, getTheme()), null);
        attachImageButton.setCompoundDrawablesWithIntrinsicBounds(null, VectorDrawableCompat.create(getResources(), R.drawable.ic_add_photo, getTheme()), null, null);
        attachVoiceButton.setCompoundDrawablesWithIntrinsicBounds(null, VectorDrawableCompat.create(getResources(), R.drawable.ic_mic, getTheme()), null, null);
        attachVideoButton.setCompoundDrawablesWithIntrinsicBounds(null, VectorDrawableCompat.create(getResources(), R.drawable.ic_video, getTheme()), null, null);

    }

    //    @Override protected void onStop() {
    //        super.onStop();
    //
    //        Log.d(TAG, "onStop");
    //        deleteEverything();
    //    }

    @Override public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy");
        deleteEverything();
    }

    //    @Override public void onPause() {
    //        super.onPause();
    //
    //        Log.d(TAG, "onPause");
    //    }

    /**
     * returning image / video
     */
    public File getOutputMediaFile(int type) throws IOException {
        // External location
        File mediaStorageDir = new File(REPORT_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(REPORT_DIRECTORY_NAME, "Oops! Failed create " + REPORT_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            Log.d(TAG, "MEDIA_TYPE_IMAGE");
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "image_" + (imageArray.size() + 1) + ".jpg");
            // Save a path
            imageArray.add(mediaFile.getAbsolutePath());
            mCurrentImagePath = mediaFile.getAbsolutePath();
            Log.d(TAG, "" + mediaFile.getAbsolutePath());
        } else if (type == MEDIA_TYPE_VIDEO) {
            Log.d(TAG, "MEDIA_TYPE_VIDEO");
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "video_" + (videoArray.size() + 1) + ".mp4");
            // Save a path
            videoArray.add(mediaFile.getAbsolutePath());
            mCurrentVideoPath = mediaFile.getAbsolutePath();
            Log.d(TAG, "" + mediaFile.getAbsolutePath());
        } else {
            return null;
        }

        return mediaFile;
    }

    /**
     * Will launch camera app or select image
     */
    public void attachImage(View view) {
        if (isGooglePlayServicesAvailable(this)) {
            //
            whatToDo = "attachImage";
            //
            if (ActivityCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, CALL_PHONE)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this).
                            setTitle("Camera and Write to Storage Permission").
                            setMessage("This app needs permissions to access camera and write to storage.");
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialogInterface, int which) {
                            ActivityCompat.requestPermissions(NonUrgentActivity.this, new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAM_AND_WRITE_EXTERNAL_STORAGE_PERMISSION);
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAM_AND_WRITE_EXTERNAL_STORAGE_PERMISSION);
                }
            } else {
                new AttachImageDialog().show(getSupportFragmentManager(), "dialog");
            }
        }

    }

    /**
     * Will launch camera app request video capture
     */
    public void attachVideo(View view) {
        if (isGooglePlayServicesAvailable(this)) {
            //
            whatToDo = "attachVideo";
            //
            if (ActivityCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, CALL_PHONE)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this).
                            setTitle("Camera and Write to Storage Permission").
                            setMessage("This app needs permissions to access camera and write to storage.");
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialogInterface, int which) {
                            ActivityCompat.requestPermissions(NonUrgentActivity.this, new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAM_AND_WRITE_EXTERNAL_STORAGE_PERMISSION);
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAM_AND_WRITE_EXTERNAL_STORAGE_PERMISSION);
                }
            } else {
                new AttachVideoDialog().show(getSupportFragmentManager(), "dialog");
            }
        }

    }

    /**
     * Will launch audio recorder
     */
    public void attachVoice(View view) {
        if (isGooglePlayServicesAvailable(this)) {
            //
            if (ActivityCompat.checkSelfPermission(this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this).
                            setTitle("Record Audio and Write to Storage Permission").
                            setMessage("This app needs permissions to record audio and write to storage.");
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialogInterface, int which) {
                            ActivityCompat.requestPermissions(NonUrgentActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, RECORD_AUDIO_WRITE_EXTERNAL_STORAGE_PERMISSION);
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, RECORD_AUDIO_WRITE_EXTERNAL_STORAGE_PERMISSION);
                }
            } else {
                new AttachAudioDialog().recordAudio(this);
            }
        }
    }

    private void deleteCurrentImage() {
        File file = new File(mCurrentImagePath);
        boolean deleted = file.delete();
        // remove from imageArray list
        imageArray.remove(imageArray.size() - 1);
    }

    public void deleteCurrentVideo() {
        if (mCurrentVideoPath != "" || mCurrentVideoPath != null) {
            File file = new File(mCurrentVideoPath);
            boolean deleted = file.delete();
            // remove from videoArray list
            videoArray.remove(videoArray.size() - 1);
        }
    }

    private void deleteEverything() {
        // delete images
        for (String uri : imageArray) {
            Log.d(TAG, uri);
            File file = new File(uri);
            boolean deleted = file.delete();
        }
        // delete videos
        for (String uri : videoArray) {
            Log.d(TAG, uri);
            File file = new File(uri);
            boolean deleted = file.delete();
        }
        // delete audio
        for (String uri : audioArray) {
            Log.d(TAG, uri);
            File file = new File(uri);
            boolean deleted = file.delete();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //  CAMERA_CAPTURE_IMAGE_REQUEST_CODE
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.d("RESULT_OK", "RESULT_OK");
                if (new File(mCurrentImagePath).exists()) {
                    Log.d("RESULT_OK", "exists");
                    // TODO: 2/8/17 set btn to number of images
                    attachImageButton.setText(String.valueOf(imageArray.size()));
                } else {
                    Log.d("RESULT_OK", "NO exists");
                }
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled Image capture
                Toast.makeText(getApplicationContext(), "User cancelled image capture", Toast.LENGTH_SHORT).show();
                // delete current file
                deleteCurrentImage();
            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(), "Sorry! Failed to capture image", Toast.LENGTH_SHORT).show();
                // delete current file
                deleteCurrentImage();
            }
        }

        // SELECT_IMAGE_ACTIVITY_REQUEST_CODE
        if (requestCode == SELECT_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(data.getData());
                        BufferedInputStream reader = new BufferedInputStream(inputStream);

                        // Create an output stream to a file that you want to save to
                        File storageDir = new File(REPORT_DIRECTORY_NAME);

                        // Create the storage directory if it does not exist
                        if (!storageDir.exists()) {
                            if (!storageDir.mkdirs()) {
                                Toast.makeText(getApplicationContext(), "Unable to create directory! Please verify there is space available on the device.", Toast.LENGTH_LONG).show();
                            }
                        }

                        //
                        File finalDestImage = new File(storageDir.getPath() + File.separator + "image_" + (imageArray.size() + 1) + ".jpg");
                        imageArray.add(finalDestImage.getAbsolutePath());
                        Log.d(TAG, "" + finalDestImage.getAbsolutePath());
                        BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(finalDestImage));

                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = reader.read(buf)) > 0) {
                            outStream.write(buf, 0, len);

                        }
                        inputStream.close();
                        outStream.close();

                        // Save a path
                        mCurrentImagePath = finalDestImage.getAbsolutePath();
                        //
                        Log.d("mCurrentImagePath", "B4");
                        // TODO: 2/8/17 image button
                        attachImageButton.setText(String.valueOf(imageArray.size()));
                    } catch (IOException re) {
                        re.printStackTrace();
                        Toast.makeText(getApplicationContext(), "File not found, try another image.", Toast.LENGTH_SHORT).show();
                    }
                } catch (NullPointerException e) {
                    Toast.makeText(getApplicationContext(), "File not found, try another image.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled Image capture
                Toast.makeText(getApplicationContext(), "User cancelled image selection", Toast.LENGTH_SHORT).show();
            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(), "Sorry! Failed to select image", Toast.LENGTH_SHORT).show();
            }

        }

        //        // CAPTURE_VIDEO_SELECTION_REQUEST_CODE
        //        if (requestCode == CAPTURE_VIDEO_SELECTION_REQUEST_CODE) {
        //            if (resultCode == RESULT_OK) {
        //                // set text
        //                attachVideoButton.setText(String.valueOf(videoArray.size()));
        //            } else if (resultCode == RESULT_CANCELED) {
        //                // user cancelled recording
        //                Toast.makeText(getApplicationContext(), "User cancelled video recording", Toast.LENGTH_SHORT).show();
        //                // delete file
        //                deleteCurrentVideo();
        //            } else {
        //                // failed to record video
        //                Toast.makeText(getApplicationContext(), "Sorry! Failed to record video", Toast.LENGTH_SHORT).show();
        //                // delete file
        //                deleteCurrentVideo();
        //            }
        //        }

        //  SELECT_VIDEO_SELECTION_REQUEST_CODE
        if (requestCode == SELECT_VIDEO_SELECTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(data.getData());
                        BufferedInputStream reader = new BufferedInputStream(inputStream);

                        // Create an output stream to a file that you want to save to
                        File storageDir = new File(REPORT_DIRECTORY_NAME);

                        // Create the storage directory if it does not exist
                        if (!storageDir.exists()) {
                            if (!storageDir.mkdirs()) {
                                Toast.makeText(getApplicationContext(), "Unable to create directory! Please verify there is space available on the device.", Toast.LENGTH_LONG).show();
                            }
                        }

                        //
                        File finalDestImage = new File(storageDir.getPath() + File.separator + "video_" + (videoArray.size() + 1) + ".mp4");
                        videoArray.add(finalDestImage.getAbsolutePath());
                        Log.d(TAG, "" + finalDestImage.getAbsolutePath());

                        BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(finalDestImage));

                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = reader.read(buf)) > 0) {
                            outStream.write(buf, 0, len);

                        }
                        inputStream.close();
                        outStream.close();

                        // Save a path
                        mCurrentVideoPath = finalDestImage.getAbsolutePath();
                        //
                        // set text
                        attachVideoButton.setText(String.valueOf(videoArray.size()));
                        //                        if (new File(mCurrentVideoPath).exists()) {
                        //                            //                            attachVideoButton.setColorFilter(Color.parseColor("#009900"));
                        //                            //                            attachVideoButton.setImageResource(R.drawable.ic_check);
                        //                        }
                    } catch (IOException re) {
                        re.printStackTrace();
                        Toast.makeText(getApplicationContext(), "File not found, try another video.", Toast.LENGTH_SHORT).show();
                    }

                } catch (NullPointerException e) {
                    Toast.makeText(getApplicationContext(), "File not found, try another video.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled recording
                Toast.makeText(getApplicationContext(), "User cancelled video selection", Toast.LENGTH_SHORT).show();
            } else {
                // failed to record video
                Toast.makeText(getApplicationContext(), "Sorry! Failed to select video", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public static void createFolder(String bucketName, String folderName, AmazonS3 client) {
        // create meta-data for your folder and set content-length to 0
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);

        // create empty content
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

        // create a PutObjectRequest passing the folder name suffixed by /
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, folderName + SUFFIX, emptyContent, metadata);

        // send request to S3 to create folder
        client.putObject(putObjectRequest);
    }

    public void submitReport(View view) {
        ArrayList<String> allFiles = new ArrayList<>();
        for (String uri : imageArray) {
            allFiles.add(uri);
        }
        for (String uri : audioArray) {
            allFiles.add(uri);
        }
        for (String uri : videoArray) {
            allFiles.add(uri);
        }


        ////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: 2/15/17 !!!!!!just testing!!!!! 
        ////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////
        // https://github.com/awslabs/aws-sdk-android-samples/tree/master/S3TransferUtilitySample
        // http://docs.aws.amazon.com/mobile-hub/latest/developerguide/google-auth.html
        // http://docs.aws.amazon.com/mobile-hub/latest/developerguide/create-oauth-android-clientid.html
        ////////////////////////////////
        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(getApplicationContext(), "us-west-2:da4317d3-fe48-4dcc-b357-7026fd226af0", // Identity Pool ID
                Regions.US_WEST_2 // Region
        );
        // Initialize the Cognito Sync client
        CognitoSyncManager syncClient = new CognitoSyncManager(getApplicationContext(), Regions.US_EAST_1, // Region
                credentialsProvider);
        //
        AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
        //
        TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());


        // Image File
        AlertDialog.Builder alert = new AlertDialog.Builder(this).setTitle("Upload");
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(parms);
        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(layout);
        // add LL and SV
        alert.setView(scrollView);
        //
        alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        alert.show();

        // loop image array to upload 
        for (String uri : allFiles) {
            if (uri != null) {
                //
                final File imageFile = new File(uri);
                Log.d(TAG, humanReadableByteCount(imageFile.length(), false));
                reduceImgSize(imageFile);
                Log.d(TAG, humanReadableByteCount(imageFile.length(), false));
                ///
                Log.d(TAG, imageFile.getName());
                //
                final TextView tv1 = new TextView(this);
                tv1.setText(imageFile.getName());
                layout.addView(tv1);
                //
                final TextView tv2 = new TextView(this);
                tv2.setText(imageFile.getName());
                layout.addView(tv2);
                //
                final ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.VISIBLE);
                layout.addView(progressBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                Log.d(TAG + " image", imageFile.getAbsolutePath());
                Log.d(TAG + " size", humanReadableByteCount(imageFile.length(), false));
                TransferObserver observer = transferUtility.upload(Constants.BUCKET_NAME, "dsfs123/" + imageFile.getName(), imageFile);
                observer.setTransferListener(new TransferListener() {
                    @Override public void onStateChanged(int id, TransferState state) {
                        if (state.equals(TransferState.COMPLETED)) {
                            Log.d(TAG, "onStateChanged COMPLETED");
                            tv1.setText(imageFile.getName() + " Complete");
                        } else if (state.equals(TransferState.FAILED)) {
                            Log.d(TAG, "onStateChanged FAILED");
                            tv1.setText(imageFile.getName() + " FAILED");
                        } else if (state.equals(TransferState.CANCELED)) {
                            Log.d(TAG, "onStateChanged CANCELED");
                            tv1.setText(imageFile.getName() + " CANCELED");
                        } else if (state.equals(TransferState.IN_PROGRESS)) {
                            Log.d(TAG, "onStateChanged IN_PROGRESS");
                            tv1.setText(imageFile.getName() + " IN_PROGRESS");
                        }
                    }

                    @Override public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        //                        int percentage = (int) (bytesCurrent / bytesTotal * 100);
                        //                        Log.d(TAG, "percentage " + percentage);
                        Log.d(TAG, "bytesCurrent " + bytesCurrent);
                        Log.d(TAG, "bytesTotal " + bytesTotal);
                        tv2.setText(bytesCurrent + "/" + bytesTotal);
                        //                        Log.d(TAG, "id " + id);
                        //                        mProgressDialogDownload.show();
                        //
                        progressBar.setIndeterminate(false);
                        progressBar.setMax((int) bytesTotal);
                        progressBar.setProgress((int) bytesCurrent);
                    }

                    @Override public void onError(int id, Exception ex) {
                        Log.d(TAG + " image", ex.toString());
                        //                        mProgressDialogDownload.dismiss();
                    }
                });

            }

        }
        ////////////////////////////////


        //gather data into object
        //submit data to POST api call to submit the report

        //        if (!editTextDesc.getText().toString().equals("")) {
        //            // Description Text
        //            String desString = editTextDesc.getText().toString().trim();
        //            Log.d(TAG + " desSt", desString);
        //
        //            // Category Selection
        //            String catSelectionString = spinner.getSelectedItem().toString();
        //            Log.d(TAG + " catSe", catSelectionString);
        //
        //            // Image File
        //            for (String uri : imageArray) {
        //                if (uri != null) {
        //                    File imageFile = new File(uri);
        //                    if (imageFile.exists()) {
        //                        Log.d(TAG + " image", imageFile.getAbsolutePath());
        //                        Log.d(TAG + " size", humanReadableByteCount(imageFile.length(), false));
        //                    }
        //                }
        //            }
        //
        //            // Video File
        //            for (String uri : videoArray) {
        //                if (uri != null) {
        //                    File videoFile = new File(uri);
        //                    if (videoFile.exists()) {
        //                        Log.d(TAG + " video", videoFile.getAbsolutePath());
        //                        Log.d(TAG + " size", humanReadableByteCount(videoFile.length(), false));
        //                    }
        //                }
        //            }
        //
        //            // Audio File
        //            for (String uri : audioArray) {
        //                if (uri != null) {
        //                    File audioFile = new File(uri);
        //                    if (audioFile.exists()) {
        //                        Log.d(TAG + " audio", audioFile.getAbsolutePath());
        //                        Log.d(TAG + " size", humanReadableByteCount(audioFile.length(), false));
        //                    }
        //                }
        //            }
        //
        //            //
        //            Snackbar snackbar = Snackbar.make(view, "Incident Report submitted", Snackbar.LENGTH_LONG);
        //            snackbar.show();
        //            // close activity when done
        //            //            finish();
        //
        //            // TODO: 1/30/17 delete all files when done submitting
        //        } else {
        //            //
        //            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        //            editTextDesc.startAnimation(shake);
        //            editTextDesc.setError(getString(R.string.setup_required));
        //            //
        //            Snackbar snackbar = Snackbar.make(view, "Description is needed before submission", Snackbar.LENGTH_LONG);
        //            snackbar.show();
        //        }

    }

    public String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(getCurrentLocale(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    ////////////////////////////////////////////////////////
    // Permission Listener
    ////////////////////////////////////////////////////////
    @Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAM_AND_WRITE_EXTERNAL_STORAGE_PERMISSION:
                if (ActivityCompat.checkSelfPermission(this, CAMERA) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    if (whatToDo != null) {
                        switch (whatToDo) {
                            case "attachImage":
                                new AttachImageDialog().show(getSupportFragmentManager(), "dialog");
                                break;
                            case "attachVideo":
                                new AttachVideoDialog().show(getSupportFragmentManager(), "dialog");
                                break;
                        }
                    }
                } else {
                    // Permission Denied
                }
                break;
            case RECORD_AUDIO_WRITE_EXTERNAL_STORAGE_PERMISSION:
                if (ActivityCompat.checkSelfPermission(this, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    new AttachAudioDialog().recordAudio(this);
                } else {
                    // Permission Denied
                }
                break;
        }
    }
    ////////////////////////////////////////////////////////
    // Permission Listener
    ////////////////////////////////////////////////////////

    // check google play services is available and updated
    private boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show();
            }
            Log.d("PlayServicesAvailable", "false");
            return false;
        }
        Log.d("PlayServicesAvailable", "true");
        return true;
    }

    private void reduceImgSize(File file) {
        try {
            // BitmapFactory options to downsize the image
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            o.inSampleSize = 6;
            // factor of downsizing the image

            FileInputStream inputStream = new FileInputStream(file);
            //Bitmap selectedBitmap = null;
            BitmapFactory.decodeStream(inputStream, null, o);
            inputStream.close();

            // The new size we want to scale to
            final int REQUIRED_SIZE = 75;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            inputStream = new FileInputStream(file);

            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2);
            inputStream.close();

            // here i override the original image file
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file);

            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}