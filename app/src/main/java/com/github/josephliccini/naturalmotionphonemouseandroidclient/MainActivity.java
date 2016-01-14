package com.github.josephliccini.naturalmotionphonemouseandroidclient;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;

    private static final int REQUEST_CONNECT_DEVICE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        getDevice();
    }

    private void getDevice() {
        // Launch the DeviceListActivity to see devices and do scan
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode != Activity.RESULT_OK) {
                    return;
                }

                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                sendMessagesOverBT(device);
        }
    }

    private void sendMessagesOverBT(BluetoothDevice device) {
        final BluetoothIO io = new BluetoothIO(this, null);

        io.connect(device);

        new Thread( new Runnable() {

            @Override
            public void run() {
                double x = 0;
                double y = 0;

                DeltaCoordinate coord = new DeltaCoordinate(x, y);
                Gson gson = new Gson();

                while (true) {
                    String json = gson.toJson(coord);

                    io.sendMessage(json);

                    try {
                        TimeUnit.MILLISECONDS.sleep(200);
                    } catch (InterruptedException ex) { }

                    coord.setDisplacementX(x);
                }
            }

        }).start();
    }
}
