package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import com.google.gson.Gson;

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
    private final Handler mHandler;

    public BluetoothMessageTransmitter(BluetoothSocket socket, Handler handler) {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException ex) { }

        this.mInputStream = tmpIn;
        this.mOutputStream = tmpOut;

        this.mSocket = socket;
        this.mHandler = handler;
    }

    public void sendMessage(String message) {
        String delimitedMessage = message + "\n";
        byte[] bytes = delimitedMessage.getBytes();
        writeOut(bytes);
    }

    public void listenForMessages() {
        byte[] buffer = new byte[1024];
        int begin = 0;
        int bytes = 0;
        while (true) {
            try {
                bytes += mInputStream.read(buffer, bytes, buffer.length - bytes);
                for (int i = begin; i < bytes; ++i) {
                    if (buffer[i] == "\n".getBytes()[0]) {
                        byte[] bufferTwo = new byte[i];

                        Gson gson = new Gson();

                        System.arraycopy(buffer, 0, bufferTwo, 0, bufferTwo.length);
                        String message = new String(bufferTwo);

                        ClientMessage clientMsg = gson.fromJson(message, ClientMessage.class);

                        if (clientMsg.getMessage().equals("ConnectionAck")) {
                            mHandler.obtainMessage(3, clientMsg.getMessage()).sendToTarget();
                        }

                        begin = i + 1;

                        if (i == bytes - 1) {
                            bytes = 0;
                            begin = 0;
                        }
                    }
                }
            } catch (IOException e) {
                break;
            }
        }
    }

    public void writeOut(byte[] bytes) {
        try {
            this.mOutputStream.write(bytes);
        } catch (IOException ex) { }
    }

    public void cancel() {
        try {
            this.mSocket.close();
        } catch (IOException ex) {

        }
    }
}
