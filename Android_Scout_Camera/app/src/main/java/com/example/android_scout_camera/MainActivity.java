package com.example.android_scout_camera;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;

import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
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

    private Button TakePhotoButton;
    private Button SaveLogButton;
    private TextView PhotoCountedNumber;
    private TextView NetworkLogText;


    private APictureCapturingService pictureService;

    private ServerSocket httpServerSocket;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();

        TakePhotoButton = findViewById(R.id.TakePhotoButton);
        SaveLogButton = findViewById(R.id.SaveLogButton);

        PhotoCountedNumber = findViewById(R.id.PhotoCountedNumber);
        NetworkLogText = findViewById(R.id.NetworkLogText);

        PhotoCountedNumber.setText(String.valueOf(0));
        NetworkLogText.setText(getIpAddress() + ":" + HttpServerThread.HttpServerPORT + "\n");

        HttpServerThread httpServerThread = new HttpServerThread();
        httpServerThread.start();

        pictureService = PictureCapturingServiceImpl.getInstance(this);
        TakePhotoButton.setOnClickListener(v -> StartCapturing());
        SaveLogButton.setOnClickListener(v -> SaveLog());
    }

    private void StartCapturing(){
        runOnUiThread(() -> {
            showToast("Starting capture!");
            pictureService.startCapturing(this);
        });
    }

    private void SaveLog() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String date = dateFormat.format(new Date());

        String fileName = date + "_Log.txt";
        String fileContent = "Photo counted: " + PhotoCountedNumber.getText() + "\n" + NetworkLogText.getText();

        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(fileContent.getBytes());
            fos.close();
            showToast("Log saved!");
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                try {
                    int PhotoPrevNumber = Integer.parseInt(PhotoCountedNumber.getText().toString());
                    PhotoCountedNumber.setText(String.valueOf(PhotoPrevNumber + 1));
                } catch (NumberFormatException e) {
                    showToast("Error saving picture!");
                }
            });
            // showToast("Picture saved to " + pictureUrl);
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




    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (httpServerSocket != null) {
            try {
                httpServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: " + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    private class HttpServerThread extends Thread {

        static final int HttpServerPORT = 8888;

        @Override
        public void run() {
            Socket socket = null;

            try {
                httpServerSocket = new ServerSocket(HttpServerPORT);

                while(true) {
                    socket = httpServerSocket.accept();

                    HttpResponseThread httpResponseThread =
                            new HttpResponseThread(
                                    socket,
                                    "welcomeMsg.getText().toString()");
                    httpResponseThread.start();
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }


    }

    private class HttpResponseThread extends Thread {

        Socket socket;
        String h1;

        HttpResponseThread(Socket socket, String msg){
            this.socket = socket;
            h1 = msg;
        }

        @Override
        public void run() {
            BufferedReader is;
            PrintWriter os;
            String request;


            try {
                is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                request = is.readLine();

                os = new PrintWriter(socket.getOutputStream(), true);

                // String response =
                //         "<html><head></head>" +
                //                 "<body>" +
                //                 "<h1>" + h1 + "</h1>" +
                //                 "</body></html>";
                String response = "{\"status\":1}";

                os.print("HTTP/1.0 200" + "\r\n");
                os.print("Content type: application/json" + "\r\n");
                os.print("Content length: " + response.length() + "\r\n");
                os.print("\r\n");
                os.print(response + "\r\n");
                os.flush();
                // socket.close();

                //

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                String date = dateFormat.format(new Date());

                String msgLog = date + " | " + request + " - " + socket.getInetAddress().toString() + "\n";
                socket.close();

                MainActivity.this.runOnUiThread(() -> NetworkLogText.setText(NetworkLogText.getText() + msgLog));

                if (!request.contains("favicon")) {
                    MainActivity.this.runOnUiThread(MainActivity.this::StartCapturing);
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


        }
    }
}