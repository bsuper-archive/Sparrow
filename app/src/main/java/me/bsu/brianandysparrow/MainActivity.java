package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

// UTIL
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// For the list adapter
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "me.bsu.MainActivity";

    // UI
    TextView macAddressTextView;
    EditText messageEditText;
    Button sendMessageButton;
    ListView deviceListView;

    // Bluetooth
    BluetoothAdapter mBluetoothAdapter;
    String adapterName;
    String MY_MAC_ADDRESS;
    private boolean bluetoothReady = false;
    List<BluetoothDevice> availableDevices;
    List<String> displayableDevices;
    Boolean isServer;

    // This uniquely identifies our app on bluetooth connection
    UUID MY_UUID = UUID.fromString("9a74be0b-49c2-4a93-9dee-df037f822b4");
    String APP_NAME = "GROUP-10-TWITTER-BT";

    // DEBUG MESSAGES
    Boolean DEBUG = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        macAddressTextView = (TextView) findViewById(R.id.my_mac_address_text_view);
        messageEditText = (EditText) findViewById(R.id.message_edittext);
        sendMessageButton = (Button) findViewById(R.id.message_send_button);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = messageEditText.getText().toString();
                sendMessage(msg);
            }
        });
        deviceListView = (ListView) findViewById(R.id.devices_list_view);
        deviceListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                connectToDevice(availableDevices.get(position));
            }
        });

        setupBluetooth();

    }

    private void sendMessage(String msg) {
        Toast.makeText(this, "Message Sent", Toast.LENGTH_SHORT).show();
    }

    /************************************
     * SETUP BLUETOOTH AND FIND DEVICES *
     ************************************/

    private final static int REQUEST_ENABLE_BT = 1;

    private void setupBluetooth() {
        if (DEBUG) {
            Log.d(TAG, "Setting up bluetooth");
        }
        if (!bluetoothReady) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                // Device does not support Bluetooth
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            } else if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                bluetoothReady = true;
                findDevices();
            }
        } else {
            findDevices();
        }
    }

    private void displayDevicesList(List<String> devices) {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item_device, devices);
        deviceListView.setAdapter(arrayAdapter);
    }

    /**
     * Attempts to set our name to NAME - MAC, but if NAME is null then only MAC
     */
    private void setAdapterName() {
        String name = mBluetoothAdapter.getName();
        if (name == null) {
            name = mBluetoothAdapter.getAddress();
        } else {
            name = name + " - " + mBluetoothAdapter.getAddress();
        }

        adapterName = name;
        MY_MAC_ADDRESS = mBluetoothAdapter.getAddress();
        macAddressTextView.setText(adapterName);
    }

    private void findDevices() {

        setAdapterName();

        if (DEBUG) {
            Log.d(TAG, "finding devices");
        }

        // MAKE US DISCOVERABLE
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        startActivity(discoverableIntent);

        // GET PAIRED DEVICES
        availableDevices = new ArrayList<>();
        for (BluetoothDevice btd : mBluetoothAdapter.getBondedDevices()) {
            availableDevices.add(btd);
        }
        displayableDevices = convertDevicesToStrings(availableDevices);
        displayDevicesList(displayableDevices);

        if (DEBUG) {
            logList("paired devices", displayableDevices);
        }

        // FIND DISCOVERABLE DEVICES
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        mBluetoothAdapter.startDiscovery();
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                String deviceStr = deviceToString(device);
                if (DEBUG) {
                    Log.d(TAG, "Discovered device: " + deviceStr);
                }
                availableDevices.add(device);
                displayableDevices.add(deviceToString(device));
                displayDevicesList(displayableDevices);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this, "Request Code: " + requestCode + " Result OK: " + (resultCode == RESULT_OK), Toast.LENGTH_SHORT).show();
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            bluetoothReady = true;
            setupBluetooth();
        }
    }

    /********************
     * STRING UTILITIES *
     ********************/

    /**
     * Returns a display string from a bluetooth device.
     *
     * @param device
     * @return
     */
    private String deviceToString(BluetoothDevice device) {
        return device.getName() + "\n" + device.getAddress();
    }

    /**
     * Returns a list of strings representing each device from a set of Bluetooth devices
     *
     * @param bluDevices
     * @return
     */
    private List<String> convertDevicesToStrings(List<BluetoothDevice> bluDevices) {
        List<String> devices = new ArrayList<>();
        for (BluetoothDevice device : bluDevices) {
            devices.add(deviceToString(device));
            Log.d(TAG, deviceToString(device));
        }
        return devices;
    }

    private void logList(String name, List<String> list) {
        if (list.size() < 1) {
            Log.d(TAG, "No items in list: " + name);
            return;
        }
        for (String s : list) {
            Log.d(TAG, s);
        }
    }

    /**************************
     * CONNECTING TO A DEVICE *
     **************************/

    /**
     * Connect as the client. This means we need to hold a BluetoothSocket
     * @param device
     */
    private void connectAsClient(BluetoothDevice device) {
        if (DEBUG) {
            Log.d(TAG, "Attempting to connect as client to device: " + deviceToString(device));
        }
        ConnectThread client = new ConnectThread(device, mBluetoothAdapter, MY_UUID);
        client.start();
    }

    /**
     * Connect as the server. This means we need to hold a BluetoothServerSocket
     * @param device
     */
    private void connectAsServer(BluetoothDevice device) {
        if (DEBUG) {
            Log.d(TAG, "Attempting to connect as server to device: " + deviceToString(device));
        }
        AcceptThread server = new AcceptThread(mBluetoothAdapter, APP_NAME, MY_UUID);
        server.start();
    }

    /**
     * Determine whether we should connect as client or server and start the connection process
     *
     * @param device
     */
    private void connectToDevice(BluetoothDevice device) {
        // Determine whether you should be the server or the client, based on lower first digit
        if (device.getAddress().charAt(0) < MY_MAC_ADDRESS.charAt(0)) {
            isServer = false;
            connectAsClient(device);
        } else {
            isServer = true;
            connectAsServer(device);
        }
    }
}
