package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
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
    private final int position;

    public ConnectThread(BluetoothDevice device, int index, BluetoothAdapter adapter, UUID uuid, Handler connectionHandler) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        mBluetoothAdapter = adapter;
        mHandler = connectionHandler;
        BluetoothSocket tmp = null;
        mmDevice = device;
        position = index;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) { }
        mmSocket = tmp;
    }

    public void run() {
        Log.d(TAG, "Running connect thread");

        // Cancel discovery because it will slow down the connection
        mBluetoothAdapter.cancelDiscovery();

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
        Log.d(TAG, "connected to server");
        mHandler.obtainMessage(0, position, 0, mmSocket)
                .sendToTarget();
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
