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

import com.google.gson.Gson;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.video.Video;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static {
        System.loadLibrary("opencv_java3");
    }

    private BluetoothAdapter mBluetoothAdapter;

    private final BluetoothIO io = new BluetoothIO(this, null);
    private final Gson gson = new Gson();

    private CameraBridgeViewBase mOpenCvCameraView;

    private Mat mPrevFrame;

    private MatOfPoint2f mFeaturesToTrack;
    private MatOfPoint2f mPrevKeypointsFound;

    private static final int REQUEST_CONNECT_DEVICE = 2;
    FeatureDetector mFeatureDetector = FeatureDetector.create(FeatureDetector.PYRAMID_SIMPLEBLOB);

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

        // getDevice();
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
        MatOfByte keypointStatus = new MatOfByte();
        MatOfFloat err = new MatOfFloat();
        Size winSize = new Size(25, 25);
        int maxLevel = 3;

        detectFeatures(greyMat, keypointsFound);

        if (mPrevFrame == null) {
            mPrevFrame = new Mat();
            mPrevKeypointsFound = new MatOfPoint2f();

            greyMat.copyTo(mPrevFrame);
            keypointsFound.copyTo(mPrevKeypointsFound);

            return greyMat;
        }

        /*
        try {
            Video.calcOpticalFlowPyrLK(mPrevFrame, greyMat, mPrevKeypointsFound, keypointsFound, keypointStatus, err, winSize, maxLevel);
            Log.d("NOERROR", "There was no error");
        } catch (Exception ex) {
            Log.d("ERROR", ex.getMessage());
        }
        */

        Log.d("CheckSize", "Size: " + keypointsFound.toList().size());

        Features2d.drawKeypoints(greyMat, convertToKeyPoint(keypointsFound), greyMat, new Scalar(0, 0, 255), 3);

        greyMat.copyTo(mPrevFrame);

        return greyMat;
    }
}
