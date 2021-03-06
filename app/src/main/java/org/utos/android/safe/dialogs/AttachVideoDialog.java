package org.utos.android.safe.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import org.utos.android.safe.R;
import org.utos.android.safe.dialogs.camTEST.dep.AttachDepVideoDialog;
import org.utos.android.safe.dialogs.camTEST.notdep.AttachNonDepVideoDialog;

import static org.utos.android.safe.NonUrgentActivity.SELECT_VIDEO_SELECTION_REQUEST_CODE;

/**
 * Created by zachariah.davis on 1/24/17.
 */
public class AttachVideoDialog extends DialogFragment {

    /* Checks if external storage is available for read and write */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    private static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(getResources().getStringArray(R.array.get_vid), new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        // Take Video
                        if (isExternalStorageReadable() && isExternalStorageWritable()) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                // your code using Camera API here - is between 1-20
                                new AttachDepVideoDialog().show(getActivity().getSupportFragmentManager(), "dialog");
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                // your code using Camera2 API here - is api 21 or higher
                                new AttachNonDepVideoDialog().show(getActivity().getSupportFragmentManager(), "dialog");
                            }

                            //
                            //                            new AttachNonDepVideoDialog().show(getActivity().getSupportFragmentManager(), "dialog");
                            //                            new AttachDepVideoDialog().show(getActivity().getSupportFragmentManager(), "dialog");


                            //                            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                            //                            // Create the File where the photo should go
                            //                            File vidFile = null;
                            //                            try {
                            //                                vidFile = ((NonUrgentActivity) getActivity()).getOutputMediaFile(MEDIA_TYPE_VIDEO);
                            //                            } catch (IOException ex) {
                            //                                // Error occurred while creating the File
                            //                                Log.i("cameraIntent", "IOException");
                            //                            }
                            //
                            //                            // It says that value 0 means low quality. You could change your value to 1.
                            //                            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
                            //
                            //                            // limit to 5mp
                            //                            // 10000000=10MB
                            //                            intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 10000000);
                            //
                            //                            // limit length
                            ////                            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 20);
                            //
                            //                            // Continue only if the File was successfully created
                            //                            if (vidFile != null) {
                            //                                Uri photoURI = FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".provider", vidFile);
                            //                                //
                            //                                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            //                                // FileProvider permissions
                            //                                // https://github.com/commonsguy/cw-omnibus/blob/master/Camera/FileProvider/app/src/main/java/com/commonsware/android/camcon/MainActivity.java
                            //                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            //                                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            //                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            //                                    ClipData clip = ClipData.newUri(getActivity().getContentResolver(), "A video", photoURI);
                            //
                            //                                    intent.setClipData(clip);
                            //                                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            //                                } else {
                            //                                    List<ResolveInfo> resInfoList = getActivity().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                            //
                            //                                    for (ResolveInfo resolveInfo : resInfoList) {
                            //                                        String packageName = resolveInfo.activityInfo.packageName;
                            //                                        getActivity().grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            //                                    }
                            //                                }
                            //                                //
                            //                                // Verify that the intent will resolve to an activity
                            //                                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                            //                                    getActivity().startActivityForResult(intent, CAPTURE_VIDEO_SELECTION_REQUEST_CODE);
                            //                                }
                            //
                            //                            }
                        } else {
                            Toast.makeText(getActivity(), "Device storage is not currently available. Check to see if the device is connected to a computer or if the storage has been removed.", Toast.LENGTH_LONG).show();
                        }
                        break;
                    case 1:
                        // Select Video
                        if (isExternalStorageReadable() && isExternalStorageWritable()) {
                            Intent intentSelectVideo = new Intent();
                            intentSelectVideo.setType("video/*");
                            intentSelectVideo.setAction(Intent.ACTION_GET_CONTENT);

                            // Verify that the intent will resolve to an activity
                            if (intentSelectVideo.resolveActivity(getActivity().getPackageManager()) != null) {
                                getActivity().startActivityForResult(Intent.createChooser(intentSelectVideo, "Select Video"), SELECT_VIDEO_SELECTION_REQUEST_CODE);
                            }
                        } else {
                            Toast.makeText(getActivity(), "Device storage is not currently available. Check to see if the device is connected to a computer or if the storage has been removed.", Toast.LENGTH_LONG).show();
                        }
                        break;
                    default:
                        // User cancelled, do nothing
                        break;
                }
            }
        });
        // Create the AlertDialog object and return it
        return builder.create();
    }

}