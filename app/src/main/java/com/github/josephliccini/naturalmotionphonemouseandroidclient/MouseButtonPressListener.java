package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Joseph on 1/31/2016.
 */
public class MouseButtonPressListener extends MouseButtonObservable implements View.OnTouchListener {
    private MessageDispatcher messageDispatcher;
    private Vibrator vib;
    private MouseButtonAction messagePress;
    private MouseButtonAction messageRelease;

    public MouseButtonPressListener(MessageDispatcher messageDispatcher, Vibrator vib,
                                    MouseButtonAction messagePress, MouseButtonAction messageRelease) {
        this.messageDispatcher = messageDispatcher;
        this.vib = vib;
        this.messagePress = messagePress;
        this.messageRelease = messageRelease;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean recognized = false;

        MouseButtonAction message = null;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                message = messagePress;
                recognized = true;
                break;
            case MotionEvent.ACTION_UP:
                message = messageRelease;
                recognized = true;
                break;
        }
        if (recognized) {
            vib.vibrate(25);
            messageDispatcher.sendMouseButtonAction(message);
            notifyObservers();
        }

        return recognized;
    }

}
