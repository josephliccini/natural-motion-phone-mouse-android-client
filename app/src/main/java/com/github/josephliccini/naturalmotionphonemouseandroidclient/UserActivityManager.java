package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.content.Context;
import android.graphics.Camera;
import android.os.Handler;
import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by Joseph on 2/4/2016.
 */
public class UserActivityManager implements Runnable {
    private final double USER_ACITIVITY_FREEZE_THRESHOLD = 4.0;

    private Date lastActive = new Date();
    private CameraBridgeViewBase cameraView;
    private Handler handler;

    public UserActivityManager(Handler handler, CameraBridgeViewBase cameraView) {
        this.handler = handler;
        this.cameraView = cameraView;
    }

    public synchronized void onUserActivity() {
        this.lastActive = new Date();
        if (!this.cameraView.isEnabled()) {
            this.cameraView.enableView();
        }
    }

    private boolean userNotActiveAfter(double seconds) {
        Date now = new Date();
        long difference = now.getTime() - lastActive.getTime();
        long secondsElapsed = TimeUnit.MILLISECONDS.toSeconds(difference);
        Log.d("SecondsElapsed", "" + secondsElapsed);
        return secondsElapsed > seconds;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) { }
            if (userNotActiveAfter(USER_ACITIVITY_FREEZE_THRESHOLD) &&
                    this.cameraView.isEnabled()) {
                // Use Handler to Freeze UI
            }
        }
    }
}
