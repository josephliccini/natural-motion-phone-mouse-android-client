package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.google.gson.Gson;

/**
 * Created by Joseph on 1/27/2016.
 */
public class MessageDispatcher {
    private BluetoothIO io;
    private Gson gson = new Gson();
    private boolean connectionsEnabled = false;

    public MessageDispatcher(BluetoothIO io, BluetoothDevice device) {
        this.io = io;
        io.connect(device);
    }

    private void sendMessage(String json) {
        if (!connectionsEnabled) {
            return;
        }

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

    public synchronized boolean isConnected() {
        return this.io.isConnected() && connectionsEnabled;
    }

    public void enableConnections() {
        this.connectionsEnabled = true;
    }

    public void close() {
        this.connectionsEnabled = false;
        this.io.disconnect();
    }
}
