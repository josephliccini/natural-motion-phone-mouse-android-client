package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.content.res.Resources;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by Joseph on 1/31/2016.
 */
public class MouseButtonDragListener extends UserActivityObservable implements View.OnTouchListener {
    private double initialY;
    private double prevY;
    private double threshold;
    private double offset;
    private Date buttonDown;
    private final Vibrator vib;
    private final MessageDispatcher messageDispatcher;
    private final double SCREEN_HEIGHT = Resources.getSystem().getDisplayMetrics().heightPixels;

    public MouseButtonDragListener(MessageDispatcher messageDispatcher, Vibrator vib, double threshold) {
        this.messageDispatcher = messageDispatcher;
        this.vib = vib;
        this.threshold = threshold;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean recognized = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.buttonDown = new Date();
                initialY = event.getY();
                prevY = 0.0;
                offset = event.getY();
                recognized = true;
                break;
            case MotionEvent.ACTION_MOVE:
                double eventY = event.getY() - offset;

                double percentOfScreenMoved = Math.abs((eventY - initialY) / SCREEN_HEIGHT) * 100.0;
                Log.d("PercentMoved", "" + percentOfScreenMoved);

                if (percentOfScreenMoved > threshold) {
                    MouseWheelDelta mouseWheelDelta = null;

                    if (eventY < prevY) {
                        mouseWheelDelta = MouseWheelDelta.MOUSE_WHEEL_UP;
                    } else if (eventY > prevY) {
                        mouseWheelDelta = MouseWheelDelta.MOUSE_WHEEL_DOWN;
                    }

                    if (mouseWheelDelta != null) {
                        initialY = eventY;
                        messageDispatcher.sendMouseWheelMessage(mouseWheelDelta);
                        vib.vibrate(5);
                    }
                    prevY = eventY;
                }
                recognized = true;
                break;
            case MotionEvent.ACTION_UP:
                Date buttonUp = new Date();
                long millisecondsElapsed = buttonUp.getTime() - buttonDown.getTime();

                if (millisecondsElapsed < 150) {
                    messageDispatcher.sendMouseButtonAction(MouseButtonAction.MIDDLE_PRESS);
                    vib.vibrate(5);
                }
                recognized = true;
                break;
        }

        if (recognized) {
            notifyObservers();
        }

        return recognized;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
}
