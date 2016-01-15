package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.UUID;

/**
 * Created by Joseph on 1/8/2016.
 */
public class BluetoothIO {
    public final static UUID BT_UUID = UUID.fromString("11f4ad42-73f7-4bb4-a5c9-998cbf22b6fb");

    private final Handler mHandler;

    private BluetoothConnector mBluetoothConnector;
    private BluetoothMessageTransmitter mBluetoothTransmitter;

    public BluetoothIO(Context context, Handler handler) {
        this.mHandler = handler;
    }

    public void connect(BluetoothDevice device) {
        this.mBluetoothConnector = new BluetoothConnector(device);

        Thread connectionThread = new Thread(this.mBluetoothConnector);
        connectionThread.start();
    }

    public synchronized void sendMessage(String message) {
        BluetoothSocket tmpSocket = null;
        try {
            tmpSocket = this.mBluetoothConnector.getSocket();
        } catch (Exception ex) {
            Log.d("BLuetoothIO", "Socket is not connected");
            return;
        }

        if (this.mBluetoothTransmitter == null) {
            this.mBluetoothTransmitter = new BluetoothMessageTransmitter(tmpSocket);
        }

        this.mBluetoothTransmitter.sendMessage(message);
    }

}
