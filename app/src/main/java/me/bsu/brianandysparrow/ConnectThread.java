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
    private boolean cancled = false;

    public ConnectThread(BluetoothDevice device, BluetoothAdapter adapter, UUID uuid, Handler connectionHandler) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        mBluetoothAdapter = adapter;
        mHandler = connectionHandler;
        BluetoothSocket tmp = null;
        mmDevice = device;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            tmp = mmDevice.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException io) {
            Log.d(TAG, "Could not open socket for: " + io.getMessage());
        }
        mmSocket = tmp;
    }

    public void run() {

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
        } catch (IOException connectException) {
            // Tell our parent to kill this thread if we weren't already explicitly canceled
            if (!cancled) {
                Log.d(TAG, "Error in connect thread for device: " + mmDevice.getAddress());
                Log.d(TAG, connectException.getMessage());
                mHandler.obtainMessage(1, mmDevice).sendToTarget();
            }
            return;
        }

        // Send the socket back to the main thread
        Log.d(TAG, "connected to server: " + mmSocket.getRemoteDevice().getAddress());
        mHandler.obtainMessage(0, mmSocket).sendToTarget();
        return;
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        cancled = true;
        try {
            mmSocket.close();
        } catch (IOException e) { };
        interrupt();
    }

}
