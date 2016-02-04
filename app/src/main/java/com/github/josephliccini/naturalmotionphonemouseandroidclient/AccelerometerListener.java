package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.util.Log;

/**
 * Created by Joseph on 2/4/2016.
 */
public class AccelerometerListener extends UserActivityObservable implements SensorEventListener2 {

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            Log.d("AccelerometerListener", "" + event.timestamp);
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
