package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
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
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, UserActivityObserver, Handler.Callback {

    static {
        System.loadLibrary("opencv_java3");
    }

    private boolean firstLaunch = true;

    private BluetoothAdapter mBluetoothAdapter;

    private CameraBridgeViewBase mOpenCvCameraView;
    private MouseButtonDragListener dragListener;

    private Mat mPrevFrame;

    private MatOfPoint2f mFeaturesToTrack;
    private MatOfPoint2f mPrevKeypointsFound;

    private double mouseSensitivity = 0.0;

    private static final int OPEN_SETTINGS = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;

    private FeatureDetector mFeatureDetector = FeatureDetector.create(FeatureDetector.PYRAMID_SIMPLEBLOB);
    private final ShortestYDistanceComparator comp = new ShortestYDistanceComparator();
    private MessageDispatcher messageDispatcher;
    private TextView mouseSensitivityView;

    private UserActivityManager userActivityManager;

    private Thread userActivityMonitorThread;

    private Handler mHandler;
    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPrefListener;

    AccelerometerListener acelListener;
    SensorManager sensorManager;
    Sensor linearAccelerometer;

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

        this.mHandler = new Handler(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        initCamera();

        getDevice();
    }

    private void initSensorListeners() {
        acelListener = new AccelerometerListener();
        sensorManager.registerListener(acelListener, linearAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        acelListener.registerObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        sensorManager.unregisterListener(acelListener);
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
            this.mouseSensitivity = Math.max(this.mouseSensitivity - 0.25, 0.0);
            recognized = true;
        }

        if (recognized) {
            MouseSensitivityMessage message = new MouseSensitivityMessage(this.mouseSensitivity);
            this.messageDispatcher.sendMouseSensitivityMessage(message);
            setMouseSensitivityText();
            onUserActivity();
        }

        return recognized || super.onKeyDown(keyCode, event);
    }

    private void setMouseSensitivityText() {
        String unformatted = getResources().getString(R.string.sensitivity_indicator_text);
        if (this.mouseSensitivityView != null) {
            this.mouseSensitivityView.setText(String.format(unformatted, this.mouseSensitivity));
        }
    }

    private void initButtonTouchListeners() {
        final Vibrator vib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        // Left Button
        final View leftButton = this.findViewById(R.id.left_click_button);

        final MouseButtonPressListener leftButtonListener = new MouseButtonPressListener(
                messageDispatcher, vib, MouseButtonAction.LEFT_PRESS,
                MouseButtonAction.LEFT_RELEASE);

        leftButtonListener.registerObserver(this);

        // Right Button
        final View rightButton = this.findViewById(R.id.right_click_button);

        final MouseButtonPressListener rightListener = new MouseButtonPressListener(
                messageDispatcher, vib, MouseButtonAction.RIGHT_PRESS,
                MouseButtonAction.RIGHT_RELEASE);

        rightListener.registerObserver(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean reversedLayout = prefs.getBoolean("left_right_swap", false);

        if (reversedLayout) {
            rightButton.setOnTouchListener(leftButtonListener);
            leftButton.setOnTouchListener(rightListener);
        } else {
            leftButton.setOnTouchListener(leftButtonListener);
            rightButton.setOnTouchListener(rightListener);
        }

        // MouseWheel Button
        final View mouseWheelButton = this.findViewById(R.id.mouse_wheel_button);


        double mouseWheelThreshold = Double.parseDouble(prefs.getString("default_wheel_sensitivity", "10"));

        dragListener = new MouseButtonDragListener(
                messageDispatcher, vib, mouseWheelThreshold);

        dragListener.registerObserver(this);
        mouseWheelButton.setOnTouchListener(dragListener);

        final View disconnectButton = this.findViewById(R.id.disconnect_button);

        disconnectButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    messageDispatcher.close();
                } catch(Exception ex) {}

                sensorManager.unregisterListener(acelListener);
                userActivityMonitorThread.interrupt();
                mOpenCvCameraView.disableView();
                getDevice();
                return true;
            }
        });

        final View settingsButton = this.findViewById(R.id.options_button);

        settingsButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                MainActivity.this.startActivityForResult(settingsIntent, OPEN_SETTINGS);
                return true;
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

                initMouseSensitivityView();
                initButtonTouchListeners();
                initSensorListeners();
                initUserActivityManager();
        }
    }

    private void initUserActivityManager() {
        this.userActivityManager = new UserActivityManager(mHandler);
        userActivityMonitorThread =new Thread(userActivityManager);
        userActivityMonitorThread.start();
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
        if (acelListener != null) {
            sensorManager.registerListener(acelListener, linearAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (dragListener != null) {
            double thresholdValue = Double.parseDouble(prefs.getString("default_wheel_sensitivity", "10"));
            this.dragListener.setThreshold(thresholdValue);
        }

        if (firstLaunch) {
            this.mouseSensitivity = Double.parseDouble(prefs.getString("default_sensitivity", "2.0"));
            firstLaunch = false;
        } else {
            initButtonTouchListeners();
        }

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        TextView cameraStatusBar = (TextView) findViewById(R.id.camera_status_bar);
        cameraStatusBar.setText(R.string.camera_on_text);
        cameraStatusBar.setTextColor(ContextCompat.getColor(this, R.color.colorCameraOn));
    }

    @Override
    public void onCameraViewStopped() {
        TextView cameraStatusBar = (TextView) findViewById(R.id.camera_status_bar);
        cameraStatusBar.setText(R.string.camera_off_text);
        cameraStatusBar.setTextColor(ContextCompat.getColor(this, R.color.colorCameraOff));
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

            return greyMat;
        }

        List<Point> prevKeypointList = mPrevKeypointsFound.toList();
        List<Point> keypointList = keypointsFound.toList();

        int prevSize = prevKeypointList.size();
        int curSize = keypointList.size();

        if (prevSize >= 1 && curSize >= 1) {
            Point a, b;

            if (prevSize > 1) {
                Collections.sort(prevKeypointList, this.comp);
            } else if (curSize > 1) {
                Collections.sort(keypointList, this.comp);
            }

            a = prevKeypointList.get(0);
            b = keypointList.get(0);

            DeltaPair deltaPair = calculateDisplacement(a, b);
            this.messageDispatcher.sendDisplacementMessage(deltaPair);

            onUserActivity();
        }

        Features2d.drawKeypoints(greyMat, PointUtils.convertToKeyPoint(keypointsFound), greyMat, new Scalar(255, 0, 0), 3);

        greyMat.copyTo(mPrevFrame);
        keypointsFound.copyTo(mPrevKeypointsFound);

        return greyMat;
    }

    private DeltaPair calculateDisplacement(Point a, Point b) {
        // Camera films in landscape mode, so swap x and y
        // Camera also films left at the bottom, so swap ax and bx
        return new DeltaPair(b.y - a.y, a.x - b.x);
    }

    @Override
    public synchronized void onUserActivity() {
        this.userActivityManager.onUserActivity();
    }

    @Override
    public boolean handleMessage(Message msg) {
        final int TURN_OFF_CAMERA = 1;
        final int TURN_ON_CAMERA = 2;

        switch(msg.what) {
            case TURN_OFF_CAMERA:
                mOpenCvCameraView.disableView();
                return true;
            case TURN_ON_CAMERA:
                mOpenCvCameraView.enableView();
                return true;
        }
        return false;
    }
}
