package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static {
        System.loadLibrary("opencv_java3");
    }

    private BluetoothAdapter mBluetoothAdapter;

    private CameraBridgeViewBase mOpenCvCameraView;

    private Mat mPrevFrame;

    private MatOfPoint2f mFeaturesToTrack;
    private MatOfPoint2f mPrevKeypointsFound;

    private double mouseSensitivity = 1.5;

    private static final int REQUEST_CONNECT_DEVICE = 2;
    private FeatureDetector mFeatureDetector = FeatureDetector.create(FeatureDetector.PYRAMID_SIMPLEBLOB);
    private final ShortestYDistanceComparator comp = new ShortestYDistanceComparator();
    private MessageDispatcher messageDispatcher;
    private TextView mouseSensitivityView;

    private Date lastActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

         // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initButtonTouchListeners();
        initCamera();
        initMouseSensitivityView();

        getDevice();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    private void initMouseSensitivityView() {
        this.mouseSensitivityView = (TextView) findViewById(R.id.sensitivity_indicator);
        setMouseSensitivityText();
    }

    private void initCamera() {
        this.mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.open_cv_camera);

        this.mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        this.mOpenCvCameraView.setCvCameraViewListener(this);
    }

    public synchronized boolean onKeyDown(int keyCode, KeyEvent event){

        boolean recognized = false;
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            this.mouseSensitivity += 0.25;
            recognized = true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            this.mouseSensitivity -= 0.25;
            recognized = true;
        }

        if (recognized) {
            MouseSensitivityMessage message = new MouseSensitivityMessage(this.mouseSensitivity);
            this.messageDispatcher.sendMouseSensitivityMessage(message);
            setMouseSensitivityText();
            acknowledgeUserActivity();
        }
        return recognized || super.onKeyDown(keyCode, event);
    }

    private void setMouseSensitivityText() {
        String unformatted = getResources().getString(R.string.sensitivity_indicator_text);
        this.mouseSensitivityView.setText(String.format(unformatted, this.mouseSensitivity));
    }

    private synchronized void acknowledgeUserActivity() {
        this.lastActive = new Date();
    }

    private void initButtonTouchListeners() {
        View leftButton = this.findViewById(R.id.left_click_button);
        final Vibrator vib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        leftButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean recognized = false;
                MouseButtonAction action = null;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        recognized = true;
                        action = MouseButtonAction.LEFT_PRESS;
                        break;
                    case MotionEvent.ACTION_UP:
                        recognized = true;
                        action = MouseButtonAction.LEFT_RELEASE;
                        break;
                }
                if (recognized) {
                    vib.vibrate(25);
                    messageDispatcher.sendMouseButtonAction(action);
                    acknowledgeUserActivity();
                }
                return recognized;
            }
        });

        View rightButton = this.findViewById(R.id.right_click_button);

        rightButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        vib.vibrate(25);
                        messageDispatcher.sendMouseButtonAction(MouseButtonAction.RIGHT_PRESS);
                        return true;
                    case MotionEvent.ACTION_UP:
                        vib.vibrate(25);
                        messageDispatcher.sendMouseButtonAction(MouseButtonAction.RIGHT_RELEASE);
                        return true;
                }
                return false;
            }
        });

        final View mouseWheelButton = this.findViewById(R.id.mouse_wheel_button);

        mouseWheelButton.setOnTouchListener(new View.OnTouchListener() {
            private double initialY;
            private double prevY;
            private final double THRESHOLD = 12.0;
            private double offset;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean recognized = false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialY = event.getY();
                        prevY = 0.0;
                        offset = event.getY();
                        recognized = true;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        double eventY = event.getY() - offset;

                        if (Math.abs(eventY - initialY) > THRESHOLD) {
                            MouseWheelDelta moueWheelDelta = null;

                            if (eventY < prevY) {
                                moueWheelDelta = MouseWheelDelta.MOUSE_WHEEL_UP;
                            } else if (eventY > prevY) {
                                moueWheelDelta = MouseWheelDelta.MOUSE_WHEEL_DOWN;
                            }

                            if (moueWheelDelta != null) {
                                initialY = eventY;
                                messageDispatcher.sendMouseWheelMessage(moueWheelDelta);
                                vib.vibrate(5);
                            }
                            prevY = eventY;
                        }
                        recognized = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        recognized = true;
                }

                if (recognized) {
                    acknowledgeUserActivity();
                }
                return recognized;
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
                BluetoothIO io = new BluetoothIO(this, null);
                this.messageDispatcher = new MessageDispatcher(io, device);
        }
    }

    private void initializeFeaturesToTrack() {
        Mat img = null;

        try {
            img = Utils.loadResource(MainActivity.this, R.drawable.circle);
        } catch (IOException ex) {
            Log.e("LoadingPointOfInterest", ex.getMessage());
            return;
        }

        this.mFeaturesToTrack = new MatOfPoint2f();

        detectFeatures(img, this.mFeaturesToTrack);
    }

    private void detectFeatures(Mat img, MatOfPoint2f keypoints) {
        MatOfKeyPoint tmp = new MatOfKeyPoint();

        this.mFeatureDetector.detect(img, tmp);

        PointUtils.convertToPoint(tmp).copyTo(keypoints);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    initializeFeaturesToTrack();
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
        this.lastActive = new Date();
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat greyMat = inputFrame.gray();

        MatOfPoint2f keypointsFound = new MatOfPoint2f();

        detectFeatures(greyMat, keypointsFound);

        if (mPrevFrame == null) {
            mPrevFrame = new Mat();
            mPrevKeypointsFound = new MatOfPoint2f();

            greyMat.copyTo(mPrevFrame);
            keypointsFound.copyTo(mPrevKeypointsFound);

            return greyMat; }

        List<Point> prevKeypointList = mPrevKeypointsFound.toList();
        List<Point> keypointList = keypointsFound.toList();

        if ((prevKeypointList.size() == 1) && (keypointList.size() == 1)) {
            this.messageDispatcher.sendDisplacementMessage(calculateDisplacement(prevKeypointList.get(0), keypointList.get(0)));
            acknowledgeUserActivity();

       } else if ((prevKeypointList.size()) > 1 &&
                  (keypointList.size() > 1) /*&&*/
                  /* (keypointList.size() == prevKeypointList.size()) */) {

            Collections.sort(prevKeypointList, this.comp);
            Collections.sort(keypointList, this.comp);

            this.messageDispatcher.sendDisplacementMessage(calculateDisplacement(prevKeypointList.get(0), keypointList.get(0)));
            acknowledgeUserActivity();
        }

        Features2d.drawKeypoints(greyMat, PointUtils.convertToKeyPoint(keypointsFound), greyMat, new Scalar(255, 0, 0), 3);

        greyMat.copyTo(mPrevFrame);
        keypointsFound.copyTo(mPrevKeypointsFound);

        if (userNotActiveAfter(5)) {
            try {
                Thread.sleep(1500);
            } catch (Exception ex) {}
        }

        return greyMat;
    }

    private boolean userNotActiveAfter(int seconds) {
        Date now = new Date();
        long difference = now.getTime() - lastActive.getTime();
        long secondsElapsed = TimeUnit.MILLISECONDS.toSeconds(difference);
        Log.d("SecondsElapsed", "" + secondsElapsed);
        return secondsElapsed > seconds;
    }

    private DeltaPair calculateDisplacement(Point a, Point b) {
        // Camera films in landscape mode, so swap x and y
        // Camera also films left at the bottom, so swap ax and bx
        return new DeltaPair(b.y - a.y, a.x - b.x);
    }

}
