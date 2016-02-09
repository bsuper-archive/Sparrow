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
    private ArrayList<BluetoothDevice> availableDevices;
    private Boolean DEBUG = true;
    private Handler dHandler;
    private Handler cHandler;
    private LocalBinder mIBinder;
    private HashSet<Integer> connectedDevices = new HashSet<>();
    private int ANDROID_DEVICE_LIMIT = 7;
    UUID BLUETOOTH_UUID = UUID.fromString("9a74be0b-49c2-4a93-9dee-df037f822b4");
    String APP_NAME = "GROUP-10-TWITTER-BT";

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
        availableDevices = new ArrayList<>();
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

        public void run() {
            int length, i;
            while(true) {
                if (connectedDevices.size() >= ANDROID_DEVICE_LIMIT) {
                    continue;
                }

                length = availableDevices.size();
                for(i = 0; i < length; i++) {
                    if (!connectedDevices.contains(i)) {
                        ConnectThread t = new ConnectThread(availableDevices.get(i), i, mBluetoothAdapter, BLUETOOTH_UUID, cHandler);
                        t.start();
                        connectedDevices.add(i);
                    }
                }
            }
        }
    };

    /**
     * Add the device index to the list of connected devices
     * then pass the data to main activity
     */
    private Handler connectHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                connectedDevices.add(msg.arg1);
                cHandler.handleMessage(msg);
            }
        }
    };

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
