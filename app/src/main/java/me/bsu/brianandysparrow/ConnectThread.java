package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.util.UUID;
import java.io.IOException;

/**
 * Created by aschmitt on 1/31/16.
 * This thread acts as the client and attempts to connect to an open server socket
 * on another device. The two devices must use the same UUID.
 */
class ConnectThread extends Thread {

    private static final String TAG = "me.bsu.Connect";

    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler;

    public ConnectThread(BluetoothDevice device, BluetoothAdapter adapter, UUID uuid, Handler connectionHandler) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        mBluetoothAdapter = adapter;
        mHandler = connectionHandler;
        BluetoothSocket tmp = null;
        mmDevice = device;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) { }
        mmSocket = tmp;
    }

    public void run() {
        Log.d(TAG, "Starting connect thread to device: " + mmDevice.getAddress());

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                mmSocket.close();
            } catch (IOException closeException) { }

            // Notify main thread that we couldn't connect to the device
            mHandler.obtainMessage(1, null)
                    .sendToTarget();
            return;
        }

        // Send the socket back to the main thread
        Log.d(TAG, "connected to server: " + mmSocket.getRemoteDevice().getAddress());
        mHandler.obtainMessage(0, mmSocket).sendToTarget();
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }

}
