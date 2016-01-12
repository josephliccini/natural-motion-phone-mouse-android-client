package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Joseph on 1/8/2016.
 */
public class BluetoothMessageTransmitter {
    private final InputStream mInputStream;
    private final OutputStream mOutputStream;
    private final BluetoothSocket mSocket;

    public BluetoothMessageTransmitter(BluetoothSocket socket) {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException ex) { }

        this.mInputStream = tmpIn;
        this.mOutputStream = tmpOut;

        this.mSocket = socket;
    }

    public void sendMessage(String message) {
        String delimitedMessage = message + "<>";
        byte[] bytes = delimitedMessage.getBytes();
        writeOut(bytes);
    }

    public void writeOut(byte[] bytes) {
        try {
            this.mOutputStream.write(bytes);
        } catch (IOException ex) { }
    }
}
