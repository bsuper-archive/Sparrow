package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.UUID;

// For Handling other threads
// UTIL
// SERVICE
// Storing this users UUID

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "me.bsu.MainActivity";

    // Shared preferences file
    public static final String PREFS_NAME = "MyPrefsFile";

    // UI
    TextView macAddressTextView;
    EditText messageEditText;
    Button sendMessageButton;

    // Bluetooth
    private ServiceConnection deviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            DeviceConnector deviceService = ((DeviceConnector.LocalBinder) binder).getInstance();
            deviceService.findDevices(connectedHandler, dataReceivedHandler, mBluetoothAdapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            sendMessage("Bluetooth Service Stopped");
        }
    };

    BluetoothAdapter mBluetoothAdapter;
    private boolean bluetoothReady = false;

    // Track open connections by mapping mac addresses to users
    private HashMap<String, UUID> openConnections = new HashMap<>();

    // This uniquely identifies our app on bluetooth connection
    UUID MY_UUID = null;
    String MY_UUID_KEY = "USER_UUID_KEY";

    // Our vector clock time
    String MY_VC_TIME_KEY = "VC_TIME_KEY";
    int MY_VECTOR_CLOCK_TIME = -1;

    // DEBUG MESSAGES
    Boolean DEBUG = true;

    /********
     * INIT *
     ********/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fetch our UUID if it exists
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        String uuidString = settings.getString(MY_UUID_KEY, null);
        if (uuidString == null) {
            generateNewUUID(settings);
        } else {
            MY_UUID = UUID.fromString(uuidString);
        }

        int vcTime = settings.getInt(MY_VC_TIME_KEY, -1);
        // Fetch our current vector clock time
        if (vcTime == -1) {
            generateNewVCTime(settings);
        } else {
            MY_VECTOR_CLOCK_TIME = vcTime;
        }

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
    }

    /**
     * Generate and store a new UUID for this user
     * @param prefs
     */
    private void generateNewUUID(SharedPreferences prefs) {
        MY_UUID = UUID.randomUUID();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(MY_UUID_KEY, MY_UUID.toString());
        editor.commit();
    }

    /**
     * Create a new vector clock time for the user
     */
    private void generateNewVCTime(SharedPreferences prefs) {
        MY_VECTOR_CLOCK_TIME = 0;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(MY_VC_TIME_KEY, MY_VECTOR_CLOCK_TIME);
        editor.commit();
    }

    private void sendMessage(String msg) {
        Toast.makeText(this, "Message Sent", Toast.LENGTH_SHORT).show();
    }

    /**
     * Start the bluetooth service and make ourselves discoverable
     */
    private void bindDeviceService() {
        mBluetoothAdapter.startDiscovery();
        bindService(new Intent(this,
                DeviceConnector.class), deviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindDeviceService() {
        unbindService(deviceConnection);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindDeviceService();
    }

    /************************************************
     * ENABLE BLUETOOTH AND START BLUETOOTH SERVICE *
     ************************************************/

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
                bindDeviceService();
            }
        } else {
            bindDeviceService();
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

    /*****************************
     * RECEIVE AND SEND MESSAGES *
     *****************************/

    /**
     * Adds an entry to the table mapping macAddress to a userId and data pair
     */
    public void processHandshake(ConnectedThread.ConnectionData dataObj) {
        //TODO: NEED PROTOBUFF CODE HERE FOR PARSING HANDSHAKE
        //NOTE: WE ONLY READ 1024 BYTES INTO OUR BUFFER A TIME SO MESSAGES COULD GET CUT OFF
    }

    /**
     * Should append the given data to the entry in openConnections
     */
    public void processData(UUID userUUID, byte[] data, int numBytes) {
        //TODO: NEED PROTOBUFF CODE HERE FOR PARSING DATA
        //NOTE: WE ONLY READ 1024 BYTES INTO OUR BUFFER A TIME SO MESSAGES COULD GET CUT OFF
    }


    /**
     * Receive data.
     * We can uniquely identify a connection by (mac address, UUID)
     */
    public void receiveData(ConnectedThread.ConnectionData dataObj) {

        // Mac address of device on the other side of the connection
        String macAddress = dataObj.getMacAddress();
        UUID userUUID = dataObj.getParentThread().getUUID();
        byte[] data = dataObj.getData();
        int numBytesRead = dataObj.getNumBytes();

        if (userUUID == null) {
            processHandshake(dataObj);
        } else {
            processData(userUUID, data, numBytesRead);
        }
    }

    /**
     * Start a new thread to listen on this socket
     */
    public void initiateHandshake(BluetoothSocket socket) {
        ConnectedThread openPort = new ConnectedThread(socket, dataReceivedHandler);
        openPort.start();
    }

    /**
     * Send the tweets that we have to the other end
     */
    public void sendTweets(ConnectedThread open) {
        open.write("Hello world".getBytes());
    }

    /**
     * Called by either the accept or connect thread when a connection is established
     * If success (what == 0) then msg.obj holds a connected bluetooth socket
     */
    Handler connectedHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                Log.d(TAG, "starting tweet exchange");
                initiateHandshake((BluetoothSocket) msg.obj);
            }
        }
    };

    /**
     * Called by the device service when it receives data on the open bluetooth socket
     */
    Handler dataReceivedHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                receiveData((ConnectedThread.ConnectionData) msg.obj);
            }
        }
    };
}
