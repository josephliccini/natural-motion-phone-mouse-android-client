package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.google.gson.Gson;

/**
 * Created by Joseph on 1/27/2016.
 */
public class MessageDispatcher {
    private int shortMessageCount = 0;
    private BluetoothIO io;
    private Gson gson = new Gson();

    public MessageDispatcher(BluetoothIO io, BluetoothDevice device) {
        this.io = io;
        io.connect(device);
    }

    private void sendMessage(String json) {
        this.io.sendMessage(json);
    }

    public void sendDisplacementMessage(DeltaPair coord) {
        sendMessage(gson.toJson(coord));
    }

    public void sendMouseButtonAction(MouseButtonAction mouseButtonAction) {
        sendMessage(gson.toJson(mouseButtonAction));
    }

    public void sendMouseWheelMessage(MouseWheelDelta mouseWheelDelta) {
        sendMessage(gson.toJson(mouseWheelDelta));
    }

    public void sendMouseSensitivityMessage(MouseSensitivityMessage message) {
        sendMessage(gson.toJson(message));
    }
}
