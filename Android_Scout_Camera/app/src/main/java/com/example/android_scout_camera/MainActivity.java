package com.example.android_scout_camera;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;


/**
 * App's Main Activity showing a simple usage of the picture taking service.
 *
 * @author hzitoun (zitoun.hamed@gmail.com)
 */
public class MainActivity extends AppCompatActivity implements PictureCapturingListener,
        ActivityCompat.OnRequestPermissionsResultCallback {


    public static final int MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1;

    // private ImageView uploadBackPhoto;
    private TextView LogMsg;
    private APictureCapturingService pictureService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        LogMsg = (TextView) findViewById(R.id.LogMsg);
        // uploadBackPhoto = (ImageView) findViewById(R.id.backIV);
        final Button btn = (Button) findViewById(R.id.ButtonBlock);
        pictureService = PictureCapturingServiceImpl.getInstance(this);
        btn.setOnClickListener(v -> {
                    showToast("Starting capture!");
                    pictureService.startCapturing(this);
                }
        );
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {
        if (picturesTaken != null && !picturesTaken.isEmpty()) {
            showToast("Done capturing all photos!");
            return;
        }
        showToast("No camera detected!");
    }

    // @Override
    // public void onCaptureDone(String pictureUrl, byte[] pictureData) {
    //     if (pictureData != null && pictureUrl != null) {
    //         runOnUiThread(() -> {
    //             final Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
    //             final int nh = (int) (bitmap.getHeight() * (512.0 / bitmap.getWidth()));
    //             final Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
    //             uploadBackPhoto.setImageBitmap(scaled);
    //         });
    //         showToast("Picture saved to " + pictureUrl);
    //     }
    // }

    @Override
    public void onCaptureDone(String pictureUrl, byte[] pictureData) {
        if (pictureData != null && pictureUrl != null) {
            runOnUiThread(() -> {
                LogMsg.setText(LogMsg.getText() + String.valueOf(LogMsg.getLineCount()) + ":" + pictureUrl  + "\n");
            });
            showToast("Picture saved to " + pictureUrl);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_CODE: {
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkPermissions();
                }
            }
        }
    }

    /**
     * checking  permissions at Runtime.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        final String[] requiredPermissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
        };
        final List<String> neededPermissions = new ArrayList<>();
        for (final String p : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    p) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(p);
            }
        }
        if (!neededPermissions.isEmpty()) {
            requestPermissions(neededPermissions.toArray(new String[]{}),
                    MY_PERMISSIONS_REQUEST_ACCESS_CODE);
        }
    }
}