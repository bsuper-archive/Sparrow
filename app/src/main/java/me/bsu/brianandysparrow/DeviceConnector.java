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
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.os.IBinder;
import android.os.Binder;

import java.util.ArrayList;
import java.util.UUID;
import java.util.HashSet;
import java.util.HashMap;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by aschmitt on 2/3/16.
 *
 * This Service creates threads that poll for new devices.
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
    private UUID BLUETOOTH_UUID = UUID.fromString("9a74be0b-49c2-4a93-9dee-df037f822b4");
    private String APP_NAME = "GROUP-10-TWITTER-BT";

    private Long TIMEOUT_MILLIS = new Long(15000);
    private Long CONNECT_TIME = new Long(20000);
    private Long DISCOVER_TIME = new Long(10000);

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
    public void findDevices(Handler connectedHandler, Handler dataHandler, BluetoothAdapter adapter) {

        cHandler = connectedHandler;
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
        (new AcceptThread(mBluetoothAdapter, APP_NAME, BLUETOOTH_UUID, connectedHandler)).start();

        // START POLLING DEVICES
        pollDevicesThread.start();
    }

    public void removeConnectedDevice(int index) {
            connectedDevices.remove(index);
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
     */
    private Thread pollDevicesThread = new Thread() {

        int numConnections = 0;
        public void run() {
            while(true) {

                Log.d(TAG, "Discovering devices");
                mBluetoothAdapter.startDiscovery();

                try {
                    Thread.sleep(DISCOVER_TIME);
                } catch(InterruptedException ie) {
                    Log.d(TAG, "Device is discoverable: " + mBluetoothAdapter.isDiscovering());
                    Log.d(TAG, "Thread interrupted while in discovery");
                    continue;
                }
                Log.d(TAG, "Device is discoverable: " + mBluetoothAdapter.isDiscovering());

                numConnections = countConnections(connectedDevices);
                if (numConnections >= ANDROID_DEVICE_LIMIT) {
                    continue;
                }
                Log.d(TAG, "Number of devices connected: " + numConnections);

                // Cancel discovery because it will slow down when we try to connect
                mBluetoothAdapter.cancelDiscovery();
//                Log.d(TAG, "Connecting to devices" + Util.deviceListToString(availableDevices));
                for(BluetoothDevice btd : availableDevices) {

                    if (!connectedDevices.containsKey(btd)) {
                        ConnectThread t = new ConnectThread(btd, mBluetoothAdapter, BLUETOOTH_UUID, cHandler);

                        t.start();
                        connectedDevices.put(btd, new DeviceTriplet(btd, t, System.currentTimeMillis()));
                    } else if (!connectedDevices.get(btd).isConnected() && shouldTimeout(connectedDevices.get(btd))) {
                        Log.d(TAG, "Timing out device: " + btd.getAddress());
                        connectedDevices.get(btd).thread.cancel();
                        connectedDevices.remove(btd);
                    }
                }

                try {
                    Thread.sleep(CONNECT_TIME);
                } catch(InterruptedException ie) {
                    Log.d(TAG, "Thread interrupted while in connecting to devices");
                    continue;
                }

//                Log.d(TAG, "Connected Devices: " + Util.deviceMapToString(connectedDevices));
            }
        }
    };

    private int countConnections(HashMap<BluetoothDevice, DeviceTriplet> devices) {
        int result = 0;
        for (DeviceTriplet dvt : devices.values()) {
            if (dvt.isConnected()) {
                result += 1;
            }
        }

        return result;
    }

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
            if (msg.what == 0) {
                BluetoothDevice btd = ((BluetoothSocket) msg.obj).getRemoteDevice();
                connectedDevices.get(btd).connect();
                cHandler.sendMessage(msg);
            }
        }
    };

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
