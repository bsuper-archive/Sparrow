package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "me.bsu.MainActivity";

    EditText messageEditText;
    Button sendMessageButton;
    ListView deviceListView;
    ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item_device);

    BluetoothAdapter mBluetoothAdapter;
    private boolean bluetoothReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        if (!bluetoothReady) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                // Device does not support Bluetooth
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            }
            if (!mBluetoothAdapter.isEnabled()) {
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

    private void findDevices() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.d(TAG, "Number of paired devices: " + pairedDevices.size());
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            Log.d(TAG, "Went inside if statement");
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Log.d(TAG, device.getName() + "\n" + device.getAddress());
            }
//            deviceListView.setAdapter(mArrayAdapter);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this, "Result OK: " + (resultCode == RESULT_OK), Toast.LENGTH_SHORT).show();
        if (resultCode == RESULT_OK) {
            bluetoothReady = true;
        }
        setupBluetooth();
    }
}
