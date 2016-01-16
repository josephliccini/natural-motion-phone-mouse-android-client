package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.google.gson.Gson;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {

    static {
        System.loadLibrary("opencv_java3");
    }

    private BluetoothAdapter mBluetoothAdapter;

    private final BluetoothIO io = new BluetoothIO(this, null);
    private final Gson gson = new Gson();

    private CameraBridgeViewBase mOpenCvCameraView;

    private static final int REQUEST_CONNECT_DEVICE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        setButtonTouchListeners();

        this.mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.open_cv_camera);

        this.mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        this.mOpenCvCameraView.setCvCameraViewListener(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        getDevice();
    }

    private void setButtonTouchListeners() {
        View leftButton = this.findViewById(R.id.left_click_button);
        final Vibrator vib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        leftButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        vib.vibrate(25);
                        io.sendMessage(gson.toJson(MouseButtonAction.LEFT_PRESS));
                        return true;
                    case MotionEvent.ACTION_UP:
                        vib.vibrate(25);
                        io.sendMessage(gson.toJson(MouseButtonAction.LEFT_RELEASE));
                        return true;
                }
                return false;
            }
        });

        View rightButton = this.findViewById(R.id.right_click_button);

        rightButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        vib.vibrate(25);
                        io.sendMessage(gson.toJson(MouseButtonAction.RIGHT_PRESS));
                        return true;
                    case MotionEvent.ACTION_UP:
                        vib.vibrate(25);
                        io.sendMessage(gson.toJson(MouseButtonAction.RIGHT_RELEASE));
                        return true;
                }
                return false;
            }
        });

    }

    private void getDevice() {
        // Launch the DeviceListActivity to see devices and do scan
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode != Activity.RESULT_OK) {
                    return;
                }

                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                sendMessagesOverBT(device);
        }
    }

    private void sendMessagesOverBT(BluetoothDevice device) {
        io.connect(device);

        new Thread( new Runnable() {

            @Override
            public void run() {
                double x = 1;
                double y = -1;

                DeltaPair coord = new DeltaPair(x, y);

                while (true) {
                    String json = gson.toJson(coord);

                    io.sendMessage(json);

                    try {
                        TimeUnit.MILLISECONDS.sleep(200);
                    } catch (InterruptedException ex) { }

                    coord.setDisplacementX(x);
                }
            }

        }).start();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i("MAIN_ACTIVITY", "OpenCV loaded successfully");
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        return inputFrame;
    }
}
