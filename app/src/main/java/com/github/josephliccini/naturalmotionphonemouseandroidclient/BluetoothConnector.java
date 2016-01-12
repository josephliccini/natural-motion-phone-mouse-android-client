package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;

/**
 * Created by Joseph on 1/8/2016.
 */
public class BluetoothConnector implements Runnable {
    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;

    private boolean mIsConnected = false;

    public BluetoothConnector(BluetoothDevice device) {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothSocket tmpSocket = null;
        try {
            tmpSocket = device.createRfcommSocketToServiceRecord(BluetoothIO.BT_UUID);
        } catch (IOException ex) {
            return;
        }

        this.mSocket = tmpSocket;
    }

    @Override
    public void run() {
        try {
            connect();
        } catch (IOException ex) {
            return;
        }
        mIsConnected = true;
    }

    private void connect() throws IOException {
        this.mBluetoothAdapter.cancelDiscovery();

        try {
            this.mSocket.connect();
        } catch (IOException ex) {
            this.mSocket.close();
        }
    }

    public BluetoothSocket getSocket() throws Exception {
        if (this.mIsConnected) {
            return this.mSocket;
        }
        throw new Exception("Not Initialized Socket");
    }

}
