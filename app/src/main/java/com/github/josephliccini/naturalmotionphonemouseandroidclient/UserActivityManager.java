package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.content.Context;
import android.graphics.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by Joseph on 2/4/2016.
 */
public class UserActivityManager implements Runnable {
    public final int TURN_OFF_CAMERA = 1;
    public final int TURN_ON_CAMERA = 2;
    private final double USER_ACITIVITY_FREEZE_THRESHOLD = 1.5;

    private Date lastActive = new Date();
    private Handler handler;
    private boolean viewDisabled = false;

    public UserActivityManager(Handler handler) {
        this.handler = handler;
    }

    public synchronized void onUserActivity() {
        this.lastActive = new Date();
        Log.d("ShouldEnable", "" + viewDisabled);
        if (viewDisabled) {
            Log.d("Enabling", "" + viewDisabled);
            Message msg = Message.obtain(handler, TURN_ON_CAMERA);
            msg.sendToTarget();
            viewDisabled = false;
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
        try {
            while (true) {
                Thread.sleep(100);
                if (!viewDisabled && userNotActiveAfter(USER_ACITIVITY_FREEZE_THRESHOLD)) {
                    Log.d("Disabling", "" + viewDisabled);
                    Message msg = Message.obtain(handler, TURN_OFF_CAMERA);
                    msg.sendToTarget();
                    synchronized (this) {
                        viewDisabled = true;
                    }
                }
            }
        } catch (InterruptedException ex) {
            Log.d("UAMInterrupt", "UAM");
            return;
        }
    }
}
