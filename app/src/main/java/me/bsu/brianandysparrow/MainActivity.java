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
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import me.bsu.proto.Feature;
import me.bsu.proto.Handshake;
import me.bsu.proto.TweetExchange;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "me.bsu.MainActivity";

    // UI
    FloatingActionButton fab;
    RecyclerView mRecyclerView;
    TextView mUsernameTextview;

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

    // Fields for constructing a new message
    String recipient = "";
    String msg = "";

    /************************************************
     * ACTIVITY LIFE CYCLE METHODS *
     ************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweets);

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

        mUsernameTextview = (TextView) findViewById(R.id.username_textview);
        mUsernameTextview.setText(String.format("Username: %s", Utils.getUsername(this)));
        mUsernameTextview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(MainActivity.this)
                .title("Enter new username")
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .input("Message text", "", new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                            }
                        })
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                Utils.setUsername(MainActivity.this, dialog.getInputEditText().getText().toString());
                                mUsernameTextview.setText(String.format("Username: %s", dialog.getInputEditText().getText().toString()));
                            }
                        })
                        .positiveText("Change").show();
            }
        });

        final MaterialDialog msgContent = new MaterialDialog.Builder(MainActivity.this)
                .title("Enter message text")
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("Message text", "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        Log.d(TAG, "Entered msg: " + input.toString());
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Utils.constructNewTweet(MainActivity.this, "", dialog.getInputEditText().getText().toString());
                        Utils.logAllTweetsInDB(MainActivity.this);
                        refreshListView();
                    }
                })
                .positiveText("Send").build();

        final MaterialDialog recipientDialog = new MaterialDialog.Builder(MainActivity.this)
                .title("Choose a recipient")
                .items(Utils.getUsers())
                .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        /**
                         * If you use alwaysCallSingleChoiceCallback(), which is discussed below,
                         * returning false here won't allow the newly selected radio button to actually be selected.
                         **/
                        return true;
                    }
                })
                .positiveText("Next")
                .autoDismiss(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Log.d(TAG, "Selected index: " + dialog.getSelectedIndex());
                        msgContent.show();
                        dialog.dismiss();
                    }
                })
                .build();

        fab = (FloatingActionButton) findViewById(R.id.new_tweet_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recipientDialog.show();
            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.tweets_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        refreshListView();

        setupBluetooth();
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

    private void unbindDeviceService() {
        if (deviceServiceBound) {
            unbindService(deviceConnection);
            deviceServiceBound = false;
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
    public void receiveTweetExchange(ConnectedThread.ConnectionData dataObj) {
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
        refreshListView();
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
            receiveTweetExchange(dataObj);
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

    /*************************
     * MENU AND OTHER METHODS *
     *************************/

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_all_db:
                // User chose the "Settings" item, show the app settings UI...
                Utils.removeAllItemsFromDB();
                Utils.logAllTweetsInDB(this);
                refreshListView();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void refreshListView() {
        mRecyclerView.setAdapter(new TweetsListAdapter(Utils.getAllDbTweets()));
    }
}
