package me.bsu.brianandysparrow;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.os.IBinder;
import android.os.Binder;
import java.util.UUID;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by aschmitt on 2/3/16.
 *
 * This Service creates a thread that polls for new devices, opening a new thread to connect to each device.
 * We delegate this task to a service because we want to do this even
 * when the user is not directly interacting with the application.
 */
public class DeviceConnector extends Service {

    private static String TAG = "me.bsu.DeviceConnector";

    private BluetoothAdapter mBluetoothAdapter;
    private HashSet<BluetoothDevice> availableDevices;
    private Boolean DEBUG = true;
    private Handler dHandler;
    private Handler cHandler;
    private LocalBinder mIBinder;
    private HashMap<BluetoothDevice, DeviceTriplet> connectedDevices = new HashMap<>();
    private int ANDROID_DEVICE_LIMIT = 7;
    private int ATTEMPT_LIMIT = 4;
    private UUID BLUETOOTH_UUID = UUID.fromString("9a74be0b-49c2-4a93-9dee-df037f822b4");
    private String APP_NAME = "GROUP-10-TWITTER-BT";

    private Long TIMEOUT_MILLIS = new Long(40000); // 40 SECONDS
    private Long CONNECT_TIME = new Long(20000); // 20 SECONDS
    private Long DISCOVER_TIME = new Long(15000); // 15 SECONDS

    @Override
    public void onCreate() {
        mIBinder = new LocalBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }

    /**
     * Spawn a new thread that attempts to connect to new devices
     */
    public void findDevices(Handler handler, Handler dataHandler, BluetoothAdapter adapter) {

        cHandler = handler;
        dHandler = dataHandler;
        mBluetoothAdapter = adapter;

        if (DEBUG) {
            Log.d(TAG, "finding devices");
        }

        // GET PAIRED DEVICES
        availableDevices = new HashSet<>();
        for (BluetoothDevice btd : mBluetoothAdapter.getBondedDevices()) {
            availableDevices.add(btd);
        }

        // FIND DISCOVERABLE DEVICES
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        // START A SINGLE SERVER THREAD FROM OUR DEVICE
        (new AcceptThread(mBluetoothAdapter, APP_NAME, BLUETOOTH_UUID, connectHandler, this)).start();

        // START POLLING DEVICES
        pollDevicesThread.start();
    }

    /**
     * Broadcast receiver that is called when we discover a new device
     * Adds the new device to the list of available devices
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                availableDevices.add(device);
            }
        }
    };

    /**
     * Loop over all devices forever and try to connect to them
     * Can only connect to 7 devices at a time
     *
     * The general flow is:
     * - Wait for discovery to start.
     * - discover for some time
     * - cancel discovery and connect to devices
     * - wait for time to connect
     * - cancel threads that failed to connect
     * - start again
     */
    private Thread pollDevicesThread = new Thread() {

        int numConnections = 0;
        public void run() {
            while(true) {

                Log.d(TAG, "Discovering devices");
                mBluetoothAdapter.startDiscovery();

                // wait for device to start discovery
                while (!mBluetoothAdapter.isDiscovering()) {}

                try {
                    Thread.sleep(DISCOVER_TIME);
                } catch(InterruptedException ie) {
                    Log.d(TAG, "Thread interrupted while in discovery");
                    break;
                }

                numConnections = countConnections(connectedDevices);
                if (numConnections >= ANDROID_DEVICE_LIMIT) {
                    continue;
                }
                Log.d(TAG, "Number of devices connected: " + numConnections);

                // Cancel discovery because it will slow down when we try to connect
                mBluetoothAdapter.cancelDiscovery();
                for(BluetoothDevice btd : availableDevices) {

                    if (!connectedDevices.containsKey(btd)) {
                        ConnectThread t = new ConnectThread(btd, mBluetoothAdapter, BLUETOOTH_UUID, connectHandler);
                        t.start();
                        addConnectedDevice(btd, t);
                    }
                }

                try {
                    Thread.sleep(CONNECT_TIME);
                } catch(InterruptedException ie) {
                    Log.d(TAG, "Thread interrupted while connecting to devices");
                    break;
                }

                // Before going back to discovery prune devices that are not currently connected
                // and cancel their connections
                for (BluetoothDevice btd : availableDevices) {
                    if (connectedDevices.containsKey(btd) && !connectedDevices.get(btd).isConnected()) {
                        Log.d(TAG, "Giving up on connection to: " + btd.getAddress());
                        closeConnection(btd);
                    }
                }

                Log.d(TAG, "Connected devices after pruning\n" + Utils.deviceMapToString(connectedDevices));
            }

            // If the loop ever breaks start everything over
            Log.d(TAG, "ERROR ENCOUNTERED IN POLL DEVICES THREAD, FINDING DEVICES");
            findDevices(cHandler, dHandler, mBluetoothAdapter);
        }
    };

    /**
     * Add a connected thread to me.
     * Thread is null for connections where I am the server
     */
    public void addConnectedDevice(BluetoothDevice btd, ConnectThread ct) {
        DeviceTriplet triplet = new DeviceTriplet(btd, ct, System.currentTimeMillis());
        connectedDevices.put(btd, triplet);
    }

    /**
     * Closes a connection by cancling the connecting thread and
     * removing the entry from the list of devices
     *
     * @param btd
     */
    public void closeConnection(BluetoothDevice btd) {
        ConnectThread t = connectedDevices.get(btd).thread;
        if (t != null) {
            t.cancel();
        }
        connectedDevices.remove(btd);
        return;
    }

    /**
     * In the event that our server connection experiences an error we can restart it
     */
    private void restartAcceptThread() {
        (new AcceptThread(mBluetoothAdapter, APP_NAME, BLUETOOTH_UUID, connectHandler, this)).start();
    }


    /**
     * Returns the number of devices that we are currently connected to
     *
     * @param devices
     * @return
     */
    private int countConnections(HashMap<BluetoothDevice, DeviceTriplet> devices) {
        int result = 0;
        for (DeviceTriplet dvt : devices.values()) {
            if (dvt.isConnected()) {
                result += 1;
            }
        }

        return result;
    }

    /**
     * Returns true iff a device connection should be timed out
     *
     * @param dvt
     * @return
     */
    private Boolean shouldTimeout(DeviceTriplet dvt) {
        return System.currentTimeMillis() - dvt.createTime > TIMEOUT_MILLIS;
    }

    /**
     * Change the connected flag to true for the device and
     * then pass the opened socket to main activity
     */
    private Handler connectHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            // We opened a connection, pass the socket to main acitivity
            if (msg.what == 0) {
                BluetoothDevice btd = ((BluetoothSocket) msg.obj).getRemoteDevice();
                connectedDevices.get(btd).connect();
                cHandler.obtainMessage(msg.what, msg.obj).sendToTarget();
            }

            // Our server socket failed (see AcceptThread)
            if (msg.what == 2) {
                restartAcceptThread();
            }
        }
    };

    /**
     * A way to efficiently store info about a connecting thread
     */
    class DeviceTriplet {

        private BluetoothDevice btd;
        private ConnectThread thread;
        private Long createTime;
        private Boolean connected;

        DeviceTriplet(BluetoothDevice device, ConnectThread t, Long time) {
            btd = device;
            thread = t;
            createTime = time;
            connected = false;
        }

        public void connect() {
            connected = true;
        }

        public Boolean isConnected() {
            return connected;
        }

        @Override
        public String toString() {
            return String.format("DeviceTriplet(mac:%s,createTime:%d,connected:%b)", btd.getAddress(), createTime, connected);
        }
    }

    /**
     * Binder so that the main activity can get an instance of this service
     * and pass handlers to this service.
     *
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "onBind binder: " + mIBinder);
        return mIBinder;
    }

    public class LocalBinder extends Binder
    {
        public DeviceConnector getInstance()
        {
            return DeviceConnector.this;
        }
    }
}
