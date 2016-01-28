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
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

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
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static {
        System.loadLibrary("opencv_java3");
    }

    private BluetoothAdapter mBluetoothAdapter;

    private CameraBridgeViewBase mOpenCvCameraView;

    private Mat mPrevFrame;

    private MatOfPoint2f mFeaturesToTrack;
    private MatOfPoint2f mPrevKeypointsFound;

    private static final int REQUEST_CONNECT_DEVICE = 2;
    FeatureDetector mFeatureDetector = FeatureDetector.create(FeatureDetector.PYRAMID_SIMPLEBLOB);
    private final ShortestYDistanceComparator comp = new ShortestYDistanceComparator();
    private MessageDispatcher messageDispatcher;

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
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        vib.vibrate(25);
                        messageDispatcher.sendMouseButtonAction(MouseButtonAction.LEFT_PRESS);
                        return true;
                    case MotionEvent.ACTION_UP:
                        vib.vibrate(25);
                        messageDispatcher.sendMouseButtonAction(MouseButtonAction.LEFT_RELEASE);
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
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialY = event.getY();
                        prevY = 0.0;
                        offset = event.getY();
                        return true;

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
                                vib.vibrate(1);
                            }
                            prevY = eventY;
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
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

        convertToPoint(tmp).copyTo(keypoints);
    }

    private MatOfPoint2f convertToPoint(MatOfKeyPoint mat) {
        KeyPoint[] arr = mat.toArray();
        Point[] pointArr = new Point[arr.length];

        for(int i = 0; i < arr.length; ++i) {
            pointArr[i] = arr[i].pt;
        }

        return new MatOfPoint2f(pointArr);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i("MAIN_ACTIVITY", "OpenCV loaded successfully");
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

    }

    @Override
    public void onCameraViewStopped() {

    }

    private MatOfKeyPoint convertToKeyPoint(MatOfPoint2f mat) {
        Point[] arr = mat.toArray();
        KeyPoint[] pointArr = new KeyPoint[arr.length];

        for(int i = 0; i < arr.length; ++i) {
            Point p = arr[i];
            pointArr[i] = new KeyPoint((float)p.x, (float)p.y, 1.0f);
        }

        return new MatOfKeyPoint(pointArr);
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

        if ((prevKeypointList.size() == 1) && (keypointList.size() == 1)) {
            this.messageDispatcher.sendDisplacementMessage(calculateDisplacement(prevKeypointList.get(0), keypointList.get(0)));

       } else if ((prevKeypointList.size()) > 1 &&
                  (keypointList.size() > 1) /*&&*/
                  /* (keypointList.size() == prevKeypointList.size()) */) {

            Collections.sort(prevKeypointList, this.comp);
            Collections.sort(keypointList, this.comp);

            this.messageDispatcher.sendDisplacementMessage(calculateDisplacement(prevKeypointList.get(0), keypointList.get(0)));
        }

        Features2d.drawKeypoints(greyMat, convertToKeyPoint(keypointsFound), greyMat, new Scalar(255, 0, 0), 3);

        greyMat.copyTo(mPrevFrame);
        keypointsFound.copyTo(mPrevKeypointsFound);

        return greyMat;
    }

    private DeltaPair calculateDisplacement(Point a, Point b) {
        // Camera films in landscape mode, so swap x and y
        // Camera also films left at the bottom, so swap ax and bx
        return new DeltaPair(b.y - a.y, a.x - b.x);
    }

}
