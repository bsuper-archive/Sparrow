package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by aschmitt on 1/31/16.
 * This thread acts as an interface to the bluetooth's data input and output streams.
 * It will constanlty listen and can also be written to.
 * This is where we can potentially put all of the proto-buff code.
 */
class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final Handler mHandler;
    private UUID userUUID = null;

    public ConnectedThread(BluetoothSocket socket, Handler dataReceivedHandler) {
        mHandler = dataReceivedHandler;
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                mHandler.obtainMessage(0, this.new ConnectionData(mmSocket, bytes, buffer))
                        .sendToTarget();
            } catch (IOException e) {
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }

    public UUID getUUID() {
        return userUUID;
    }

    public void setUUID(UUID uuid) {
        userUUID = uuid;
    }

    /**
     * This class represents data that is being passed from one connection to this device.
     * This is nested because we need access to the parent instance to assign this ConnectedThread object a UUID
     */
    class ConnectionData {

        private byte[] data;
        private BluetoothSocket socket;
        private int numBytes;

        ConnectionData(BluetoothSocket s, int bytesRead, byte[] d) {
            socket = s;
            data = d;
            numBytes = bytesRead;
        }

        public byte[] getData() {
            return data;
        }

        public BluetoothSocket getSocket() {
            return socket;
        }

        public byte[] getBytes() {
            return data;
        }

        public int getNumBytes() {
            return numBytes;
        }

        public String getMacAddress() {
            return socket.getRemoteDevice().getAddress();
        }

        public ConnectedThread getParentThread() {
            return ConnectedThread.this;
        }
    }
}
