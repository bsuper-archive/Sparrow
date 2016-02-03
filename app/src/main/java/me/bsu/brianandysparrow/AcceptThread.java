package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothServerSocket;
import android.util.Log;
import android.os.Handler;
import android.os.Message;

import java.util.UUID;
import java.io.IOException;

/**
 * Created by aschmitt on 1/31/16.
 * This thread acts as a bluetooth server and attempts to open a bluetooth server socket.
 * Applications using the same UUID can then connec to this socket.
 */
class AcceptThread extends Thread {

    private static final String TAG = "me.bsu.Accept";

    private final BluetoothServerSocket mmServerSocket;
    private final Handler mHandler;

    public AcceptThread(BluetoothAdapter mBluetoothAdapter, String NAME, UUID uuid, Handler connectedHandler) {
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        mHandler = connectedHandler;

        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, uuid);
        } catch (IOException e) { }
        mmServerSocket = tmp;
    }

    public void run() {
        Log.d(TAG, "Running accept thread");

        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                break;
            }
            // If a connection was accepted
            if (socket != null) {

                Log.d(TAG, "connected to client");
                // Send the socket back to the main thread
                mHandler.obtainMessage(0, socket)
                        .sendToTarget();

                // Close the server socket because we don't need it anymore
                try {
                    mmServerSocket.close();
                } catch( IOException closeException) {}
                break;
            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) { }
    }
}
