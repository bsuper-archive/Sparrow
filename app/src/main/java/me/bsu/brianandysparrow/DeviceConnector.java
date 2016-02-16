package me.bsu.brianandysparrow;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
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

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.UUID;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    Handler cHandler;
    private LocalBinder mIBinder;
    private ConcurrentHashMap<BluetoothDevice, DeviceTriplet> connectedDevices = new ConcurrentHashMap<>();
    private int ANDROID_DEVICE_LIMIT = 5;
    private UUID BLUETOOTH_UUID = UUID.fromString("9a74be0b-49c2-4a93-9dee-df037f822b4");
    private String APP_NAME = "GROUP-10-TWITTER-BT";
    private Handlers.ServiceConnectHandler connectHandler;
    private HashMap<BluetoothDevice, ConnectedThread> shouldClose = new HashMap<>();

    private Long TIMEOUT_TIME = new Long(50000); // 60 seconds
    private Long CONNECT_TIME = new Long(35000); // 35 SECONDS
    private Long DISCOVER_TIME = new Long(10000); // 10 SECONDS
    private int CONNECT_ATTEMPTS = 2;

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
        unregisterReceiver(mReceiver);

        // Close all connections, kill all the threads
        for (BluetoothDevice btd : connectedDevices.keySet()) {
            if (shouldClose.containsKey(btd)) {
                closeSuccessfulConnection(btd);
            } else if (connectedDevices.containsKey(btd)) {
                closeConnection(btd);
            }
        }

        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }

    /**
     * Spawn a new thread that attempts to connect to new devices
     */
    public void findDevices(Handler handler, Handler dataHandler, BluetoothAdapter adapter) {

        cHandler = handler;
        dHandler = dataHandler;
        mBluetoothAdapter = adapter;
        connectHandler = new Handlers.ServiceConnectHandler(this);

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

                //wait for discovery to end
                while (mBluetoothAdapter.isDiscovering()) {};
                Log.d(TAG, "Starting connections. Device is discovering: " + mBluetoothAdapter.isDiscovering());

                for (int i = 0; i < CONNECT_ATTEMPTS; i++) {
                    Iterator<BluetoothDevice> devices = availableDevices.iterator();
                    BluetoothDevice btd;
                    while (devices.hasNext()) {

                        // Only try to connect to ANDROID_DEVICE_LIMIT devices at a time
                        int numAttempts = 0;
                        while (numAttempts < ANDROID_DEVICE_LIMIT && devices.hasNext()) {
                            btd = devices.next();
                            if (!connectedDevices.containsKey(btd)) {
                                ConnectThread t = new ConnectThread(btd, mBluetoothAdapter, BLUETOOTH_UUID, connectHandler);
                                t.start();
                                addDevice(btd, t);
                            }
                            numAttempts += 1;
                        }

                        // Wait for devices to connect
                        try {
                            Thread.sleep(CONNECT_TIME);
                        } catch (InterruptedException ie) {
                            Log.d(TAG, "Thread interrupted while connecting to devices");
                            break;
                        }


                        for (BluetoothDevice device : availableDevices) {

                            // Cancel connections that we just exchanged data with
                            if (shouldClose.containsKey(device)) {
                                Log.d(TAG, "Closing successful connection to: " + device.getAddress());
                                closeSuccessfulConnection(device);
                            }

                            // Cancel connections that never connected
                            else if (connectedDevices.containsKey(device) && !connectedDevices.get(device).isConnected()) {
                                Log.d(TAG, "Giving up on connection to: " + device.getAddress());
                                closeConnection(device);
                            }

                            // Cancel connections that have been open for too long
                            else if (connectedDevices.containsKey(device) && shouldTimeout(connectedDevices.get(device))) {
                                Log.d(TAG, "Timing out bad connection to: " + device.getAddress());
                                closeConnection(device);
                            }
                        }
                    }
                }
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
    public void addDevice(BluetoothDevice btd, ConnectThread ct) {
        DeviceTriplet triplet = new DeviceTriplet(btd, ct, System.currentTimeMillis());
        connectedDevices.put(btd, triplet);
    }

    public boolean deviceIsConnected(BluetoothDevice btd) {
        return connectedDevices.containsKey(btd) && connectedDevices.get(btd).isConnected();
    }

    public void connectDevice(BluetoothDevice btd) {
        Log.d(TAG, "In connect device");
        if (connectedDevices.containsKey(btd)) {
            connectedDevices.get(btd).connect();
        }

        // If we connect as a server we may  need to create a new entry
        else {
            DeviceTriplet newDvt = new DeviceTriplet(btd, null, System.currentTimeMillis());
            newDvt.connect();
            connectedDevices.put(btd, newDvt);
        }
    }

    public void addToClose(ConnectedThread conn) {
        shouldClose.put(conn.getSocket().getRemoteDevice(), conn);
    }

    private boolean shouldTimeout(DeviceTriplet dvt) {
        return (System.currentTimeMillis() - dvt.createTime) >= TIMEOUT_TIME;
    }

    /**
     * Closes a connection by cancling the connecting thread and
     * removing the entry from the list of devices
     *
     * @param btd
     */
    public void closeConnection(BluetoothDevice btd) {
        if (connectedDevices.containsKey(btd)) {
            ConnectThread t = connectedDevices.get(btd).thread;
            if (t != null) {
                t.cancel();
            }
            connectedDevices.remove(btd);
        }
    }

    public void closeSuccessfulConnection(BluetoothDevice btd) {
        closeConnection(btd);
        if (shouldClose.containsKey(btd)) {
            ConnectedThread conn = shouldClose.get(btd);
            conn.cancel();
            shouldClose.remove(btd);
        }
    }

    /**
     * In the event that our server connection experiences an error we can restart it
     */
    void restartAcceptThread() {
        (new AcceptThread(mBluetoothAdapter, APP_NAME, BLUETOOTH_UUID, connectHandler, this)).start();
    }


    /**
     * Returns the number of devices that we are currently connected to
     *
     * @param devices
     * @return
     */
    private int countConnections(ConcurrentHashMap<BluetoothDevice, DeviceTriplet> devices) {
        int result = 0;
        for (DeviceTriplet dvt : devices.values()) {
            if (dvt.isConnected()) {
                result += 1;
            }
        }

        return result;
    }

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
