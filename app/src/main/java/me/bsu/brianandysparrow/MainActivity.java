package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.widget.ArrayAdapter;

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
    private boolean bluetoothReady = false;
    Set<BluetoothDevice> availableDevices;
    List<String> displayableDevices;
    Boolean DEBUG = true;

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

        setupBluetooth();

    }

    /**
     * This gets the wifi mac address which is probably not what we want
     *
     * @return
     */
    private String getMacAddress() {
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        String address = info.getMacAddress();
        return address;
    }

    private void sendMessage(String msg) {
        Toast.makeText(this, "Message Sent", Toast.LENGTH_SHORT).show();
    }

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
        availableDevices = new HashSet<>();
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
    private List<String> convertDevicesToStrings(Set<BluetoothDevice> bluDevices) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this, "Request Code: " + requestCode + " Result OK: " + (resultCode == RESULT_OK), Toast.LENGTH_SHORT).show();
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            bluetoothReady = true;
            setupBluetooth();
        }
    }

}
