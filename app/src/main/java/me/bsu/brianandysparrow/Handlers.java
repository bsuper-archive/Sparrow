package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by aschmitt on 2/12/16.
 */
public class Handlers {

    private static String TAG = "me.bsu.Handlers";

    /**
     * Called by either the accept or connect thread when a connection is established
     * If success (what == 0) then msg.obj holds a connected bluetooth socket
     */
    static class ConnectedHandler extends Handler {

        private final WeakReference<MainActivity> activity;

        ConnectedHandler(MainActivity parent) {
            activity = new WeakReference<MainActivity>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            // We got a good socket back
            MainActivity parent = activity.get();
            if (parent != null) {
                if (msg.what == 0) {
                    parent.initiateHandshake((BluetoothSocket) msg.obj);
                }
            } else {
                Log.d(TAG, "Could not get main activity in ConnectedHandler");
            }
        }
    }

    /**
     * Called by the device service when it receives data on the open bluetooth socket
     */
    static class DataHandler extends Handler {

        private final WeakReference<MainActivity> activity;

        DataHandler(MainActivity parent) {
            activity = new WeakReference<MainActivity>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            // We got a good socket back
            MainActivity parent = activity.get();
            if (parent != null) {
                if (msg.what == 0) {
                    parent.receiveData((ConnectedThread.ConnectionData) msg.obj);
                }

                // Unexpected error in connected thread, so close it and remove the connection
                else if (msg.what == 1) {
                    parent.removeConnection((ConnectedThread) msg.obj);
                }

            } else {
                Log.d(TAG, "Could not get main activity in DataHandler");
            }
        }
    }

    /**
     * Change the connected flag to true for the device and
     * then pass the opened socket to main activity
     */
    static class ServiceConnectHandler extends Handler {

        private final WeakReference<DeviceConnector> service;

        ServiceConnectHandler(DeviceConnector deviceService) {
            service = new WeakReference<DeviceConnector>(deviceService);
        }
        @Override
        public void handleMessage(Message msg) {

            synchronized (this) {
                DeviceConnector parent = service.get();

                if (parent != null) {
                    // We opened a connection, pass the socket to main acitivity
                    if (msg.what == 0) {
                        BluetoothSocket socket = (BluetoothSocket) msg.obj;
                        BluetoothDevice btd = socket.getRemoteDevice();

                        if (!parent.deviceIsConnected(btd)) {

                            parent.connectDevice(btd);
                            Log.d(TAG, "Sending message to main activity");
                            parent.cHandler.obtainMessage(msg.what, msg.obj).sendToTarget();
                        } else {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.d(TAG, "Failed to close unneeded socket to: " + btd.getAddress());
                            }
                        }
                    }

                    // A client connect thread failed (see ConnectThread)
                    // Only close the connection if we didn't also connect as a server
                    else if (msg.what == 1) {
                        BluetoothDevice device = (BluetoothDevice) msg.obj;
                        if (!parent.deviceIsConnected(device)) {
                            parent.closeConnection(device);
                        }
                    }

                    // Our server socket failed (see AcceptThread)
                    // Close the thread and restart it
                    else if (msg.what == 2) {
                        ((AcceptThread) msg.obj).cancel();
                        parent.restartAcceptThread();
                    }

                } else {
                    Log.d(TAG, "Couldn't get DeviceConnector in ServiceConnectorHandler");
                }
            }
        }
    }
}
