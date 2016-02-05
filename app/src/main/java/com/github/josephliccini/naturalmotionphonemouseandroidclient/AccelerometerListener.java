package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by Joseph on 2/4/2016.
 */
public class AccelerometerListener extends UserActivityObservable implements SensorEventListener2 {
    private float prevX = 0.0f;
    private float prevY = 0.0f;
    private float prevZ = 0.0f;


    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float diffX = Math.abs(prevX - x);
        float diffY = Math.abs(prevY - y);
        float diffZ = Math.abs(prevZ - z);

        if (diffX > 0.1f || diffY > 0.1f || diffZ > 0.1f) {
            Log.d("AccelVals", diffX + "," + diffY + "diffZ");
            notifyObservers();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }
}
