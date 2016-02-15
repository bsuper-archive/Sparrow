package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import me.bsu.brianandysparrow.models.DBTweet;
import me.bsu.proto.Feature;
import me.bsu.proto.Handshake;
import me.bsu.proto.TweetExchange;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "me.bsu.MainActivity";

    // UI
    TextView macAddressTextView;
    EditText messageEditText;
    Button sendMessageButton;


    // Bluetooth
    private DeviceConnector deviceService;
    private ServiceConnection deviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "Device service connected");
            deviceService = ((DeviceConnector.LocalBinder) binder).getInstance();
            deviceService.findDevices(connectedHandler, dataReceivedHandler, mBluetoothAdapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
//            Utils.sendMessage("Bluetooth Service Stopped", MainActivity.this.MY_UUID.toString());
        }
    };
    BluetoothAdapter mBluetoothAdapter;
    private boolean bluetoothReady = false;
    private Handlers.ConnectedHandler connectedHandler;
    private Handlers.DataHandler dataReceivedHandler;
    private boolean deviceServiceBound = false;

    // Track open connections by mapping mac addresses to the features that they have
    private HashMap<String, ConnectedThread> openConnections = new HashMap<>();

    // This uniquely identifies our app on bluetooth connection
    UUID MY_UUID = null;

    // Our supported features
    private List<Feature> MY_FEATURES = Arrays.asList(Feature.BASIC, Feature.VECTOR_CLOCK);

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
        SharedPreferences settings = getSharedPreferences(Utils.PREFS_NAME, 0);

        String uuidString = settings.getString(Utils.MY_UUID_KEY, null);
        if (uuidString == null) {
            MY_UUID = Utils.generateNewUUID(settings);
            if (DEBUG) {
                Log.d(TAG, "Generating new uuid for user");
            }
        } else {
            MY_UUID = UUID.fromString(uuidString);
            if (DEBUG) {
                Log.d(TAG, "UUID for user already exists: " + MY_UUID);
            }
        }

//        clearTweetsCreate1Test();
//        return;

//        int vcTime = settings.getInt(Utils.MY_VC_TIME_KEY, -1);
//        // Fetch our current vector clock time
//        if (vcTime == -1) {
//            MY_VECTOR_CLOCK_TIME = Utils.generateNewVCTime(settings);
//        } else {
//            MY_VECTOR_CLOCK_TIME = vcTime;
//        }

        macAddressTextView = (TextView) findViewById(R.id.my_mac_address_text_view);
        messageEditText = (EditText) findViewById(R.id.message_edittext);
        sendMessageButton = (Button) findViewById(R.id.message_send_button);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = messageEditText.getText().toString();
//                Utils.sendMessage(msg, MainActivity.this.MY_UUID.toString());
            }
        });

        setupBluetooth();
    }

    // For Testing
    private void clearTweetsCreate1Test() {
        Utils.removeAllItemsFromDB();
        Log.d(TAG, "Removing all items");
        DBTweet tweet4 = new DBTweet(4, "brian", "hello world4", "", "brian");
        tweet4.save();

        TweetExchange tweetEx = Utils.constructTweetExchangeWithAllTweets();

        Log.d(TAG, "tweets in db: " + tweetEx.tweets);
    }

    private void unbindDeviceService() {
        if (deviceServiceBound) {
            unbindService(deviceConnection);
            deviceServiceBound = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // tell the device service to close all of our connections
        for (ConnectedThread conn : openConnections.values()) {
            deviceService.addToClose(conn);
        }

        unbindDeviceService();
    }

    /************************************************
     * ENABLE BLUETOOTH AND START BLUETOOTH SERVICE *
     ************************************************/

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_ENABLE_DISCOVERY = 2;

    private void setupBluetooth() {

        // Instantiate Handlers to receive data from threads/service
        connectedHandler = new Handlers.ConnectedHandler(this);
        dataReceivedHandler = new Handlers.DataHandler(this);

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

    /**
     * Start the bluetooth service and make ourselves discoverable
     */
    private void bindDeviceService() {
        if (!deviceServiceBound) {
            if (DEBUG) {
                Log.d(TAG, "Binding device service");

            }
            // MAKE US DISCOVERABLE
            Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivityForResult(discoverableIntent, REQUEST_ENABLE_DISCOVERY);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "On activity result called, request code: " + requestCode + ", result code: " + resultCode);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            bluetoothReady = true;
            setupBluetooth();
        }

        if (requestCode == REQUEST_ENABLE_DISCOVERY) {
            Log.d(TAG, "Binding service");
            bindService(new Intent(this,
                    DeviceConnector.class), deviceConnection, Context.BIND_AUTO_CREATE);
            deviceServiceBound = true;
        }
    }


    /**********************************
     * INITIATE AND RECEIVE HANDSHAKE *
     **********************************/

    /**
     * Start a new thread to listen on this socket
     * Send the handshake to the other device
     */
    public void initiateHandshake(BluetoothSocket socket) {
        ConnectedThread openPort = new ConnectedThread(socket, dataReceivedHandler);
        openPort.start();

        Log.d(TAG, "initiating handshake");

        Handshake handshakeMsg = new Handshake.Builder()
                .uuid(MY_UUID.toString())
                .features(MY_FEATURES)
                .build();

        byte[] handshakeBytes = Handshake.ADAPTER.encode(handshakeMsg);
        openPort.writeInt(handshakeBytes.length);
        openPort.write(handshakeBytes);
    }

    /**
     * Adds an entry to the table mapping mac address to the connection's thread
     */
    public void receiveHandshake(ConnectedThread.ConnectionData dataObj) {
        Handshake handshake = null;
        ConnectedThread connection = dataObj.getConnection();
        BluetoothDevice remoteDevice = connection.getSocket().getRemoteDevice();

        try {
            handshake = Handshake.ADAPTER.decode(dataObj.getData());
        } catch (IOException io) {
            Log.d(TAG, "Couldn't decode handshake from: " + remoteDevice.getAddress() + " dropping connection!");
            removeConnection(connection);
            return;
        }

        List<Feature> features = handshake.features;
        if (!validFeatures(features)) {
            Log.d(TAG, "Missing features. Invalid handshake from: " + remoteDevice.getAddress());
            removeConnection(connection);
            return;
        }

        // Store the features in the connection. May not need this
        connection.setFeatures(features);

        Log.d(TAG, "Successfully decoded handhshake from: " + remoteDevice.getAddress());

        String connectionID = connection.getID();

        // Store the threadID and the thread itself for sending later
        openConnections.put(connectionID, connection);

        // Check if the device supports vector clocks
        UUID userUUID = UUID.fromString(handshake.uuid);
        sendTweetExchange(connectionID, userUUID);
    }

    /**
     * Returns true iff the feature list provided by the handshake is valid
     *
     * @param features
     * @return
     */
    private Boolean validFeatures(List<Feature> features) {
        return (features.size() > 0) && (features.get(0) == Feature.BASIC);
    }

    /***********************************
     * SEND AND RECEIVE TWEET EXCHANGE *
     ***********************************/

    /**
     * Should append the given data to the entry in openConnections
     */
    public void receiveTweetExchagne(ConnectedThread.ConnectionData dataObj) {
        ConnectedThread connection = dataObj.getConnection();
        TweetExchange tweetEx = null;
        try {
            tweetEx = TweetExchange.ADAPTER.decode(dataObj.getData());
        } catch (IOException e) {
            Log.d(TAG, e.getLocalizedMessage());
            Log.d(TAG, e.getMessage());
            Log.d(TAG, "Couldn't parse tweet exchange from: " + connection.getID());
            Log.d(TAG, "Found length: " + dataObj.getData().length);
            Log.d(TAG, "Found data: \n" + Arrays.toString(dataObj.getData()));
            removeConnection(connection);
            return;
        }

        Log.d(TAG, "Successfully received tweet exchange from: " + connection.getID());
        Utils.readTweetExchangeSaveTweetsInDB(tweetEx);
        removeConnection(dataObj.getConnection());
    }

    /**
     * Send all of our tweets to the other connection
     *
     * @param connectionID
     */
    public void sendTweetExchange(String connectionID, UUID userUUID) {
        Log.d(TAG, "Sending basic tweet exchange to: " + connectionID);
        TweetExchange tweetEx = Utils.constructTweetExchangeWithAllTweets();
        byte[] tweetExBytes = TweetExchange.ADAPTER.encode(tweetEx);
        sendData(connectionID, tweetExBytes);
    }

    /*************************
     * RECEIVE AND SEND DATA *
     *************************/

    /**
     * Receive data
     *
     * @param dataObj
     */
    public void receiveData(ConnectedThread.ConnectionData dataObj) {
        if (DEBUG) {
            Log.d(TAG, "Data received");
        }
        // Mac address of device on the other side of the connection
        String threadID = dataObj.getConnection().getID();

        if (!openConnections.containsKey(threadID)) {
            receiveHandshake(dataObj);
        } else {
            receiveTweetExchagne(dataObj);
        }
    }

    /**
     * Send a byte array of data to a particular open connection
     *
     * @param connectionID
     * @param data
     */
    public void sendData(String connectionID, byte[] data) {
        ConnectedThread target = openConnections.get(connectionID);
        Log.d(TAG, "Send tweet exchange with length: " + data.length);
        Log.d(TAG, "Data" + Arrays.toString(data));
        target.writeInt(data.length);
        target.write(data);
    }

    /**
     * Removes a connection from me and also closes the connection
     *
     * @param connection
     */
    void removeConnection(ConnectedThread connection) {
        deviceService.addToClose(connection);
        openConnections.remove(connection.getID());
    }
}
