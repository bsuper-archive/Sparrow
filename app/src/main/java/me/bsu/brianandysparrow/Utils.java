package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import me.bsu.brianandysparrow.models.DBTweet;
import me.bsu.brianandysparrow.models.DBUser;
import me.bsu.brianandysparrow.models.DBVectorClockItem;
import me.bsu.proto.Tweet;
import me.bsu.proto.TweetExchange;
import me.bsu.proto.VectorClockItem;

/**
 * Methods for App-specific string manipulation and DB interactions
 */
public class Utils {

    private static String TAG = "me.bsu.Utils";

    /************************************************
     *
     *
     * METHODS FOR STORING AND ACCESSING MY UUID AND
     * VECTOR CLOCK TIME VALUES FROM SETTINGS
     *
     *
     ************************************************/

    public static final String MY_UUID_KEY = "MY_UUID_KEY";
    public static final String MY_VC_TIME_KEY = "MY_VC_TIME_KEY";
    public static final String MY_USERNAME_KEY = "MY_USERNAME_KEY";
    public static final String PREFS_NAME = "MY_PREFS";

    /************************************************
     *
     *
     * PREFERENCES
     *
     *
     ************************************************/

    /**
     * Gets my UUID, creating it if necessary
     */
    public static UUID getOrCreateNewUUID(Context context) {
        SharedPreferences settings = context.getSharedPreferences(Utils.PREFS_NAME, 0);

        String uuidString = settings.getString(Utils.MY_UUID_KEY, null);
        if (uuidString == null) {
            return Utils.generateNewUUID(settings);
        } else {
            return UUID.fromString(uuidString);
        }
    }

    /**
     * Generate and store a new UUID for this user
     */
    public static UUID generateNewUUID(SharedPreferences prefs) {
        UUID uuid = UUID.randomUUID();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(MY_UUID_KEY, uuid.toString());
        editor.commit();
        return uuid;
    }

    /**
     * Increments the VC time, creating it if necessary
     */
    public static int incrementVCTime(Context context) {
        SharedPreferences settings = context.getSharedPreferences(Utils.PREFS_NAME, 0);
        int vcTime = settings.getInt(Utils.MY_VC_TIME_KEY, -1);
        // Fetch our current vector clock time
        if (vcTime == -1) {
             vcTime = Utils.generateNewVCTime(settings);
        }
        settings.edit().putInt(MY_VC_TIME_KEY, vcTime + 1).commit();
        return vcTime + 1;
    }

    /**
     * Get the VC Time, creating it if necessary
     */
    public static int getOrCreateNewVCTime(Context context) {
        SharedPreferences settings = context.getSharedPreferences(Utils.PREFS_NAME, 0);
        int vcTime = settings.getInt(Utils.MY_VC_TIME_KEY, -1);
        // Fetch our current vector clock time
        if (vcTime == -1) {
            return Utils.generateNewVCTime(settings);
        } else {
            return vcTime;
        }
    }

    /**
     * Getter for Human readable username for identifying another person during encryption
     */
    public static String getUsername(Context context) {
        SharedPreferences settings = context.getSharedPreferences(Utils.PREFS_NAME, 0);
        String username = settings.getString(Utils.MY_USERNAME_KEY, "dummy_username");
        return username;
    }

    /**
     * Setter for
     * Human readable username for identifying another person during encryption
     */
    public static String setUsername(Context context, String username) {
        SharedPreferences settings = context.getSharedPreferences(Utils.PREFS_NAME, 0);
        settings.edit().putString(Utils.MY_USERNAME_KEY, username).commit();
        return username;
    }

    /**
     * Create a new vector clock time for the user
     */
    public static int generateNewVCTime(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(MY_VC_TIME_KEY, 0);
        editor.commit();
        return 0;
    }


    /************************************************
     *
     *
     * METHODS FOR WORKING WITH TWEET PROTOBUF
     * AND DBTWEET MODEL IN DATABASE
     *
     *
     ************************************************/


    /************************************************
     * ACCESSING
     ************************************************/

    /**
     * Gets all the tweets in the database, sorted by vector clocks in reverse order
     */
    public static List<DBTweet> getAllDbTweets() {
        List<DBTweet> dbTweets = new Select().from(DBTweet.class).execute();
        Collections.reverse(dbTweets);
        Log.d(TAG, "Number of items: " + dbTweets.size());
        return dbTweets;
    }

    /**
     * Creates a tweet exchange with all the tweets in the database
     */
    public static TweetExchange constructTweetExchangeWithAllTweets() {
        List<DBTweet> dbTweets = getAllDbTweets();

        List <Tweet> tweets = new ArrayList<>();
        for (DBTweet dbTweet : dbTweets) {
            tweets.add(dbTweet.createTweet());
        }
        return new TweetExchange.Builder().tweets(tweets).build();
    }

    /**
     * Get all tweets that can be sent to recipient
     */
    public static TweetExchange constructTweetExchangeForUser(String recipient) {
        List<DBTweet> dbTweets = new Select().from(DBTweet.class).where("recipient = ?", new String[] {recipient, ""}).execute();
        List <Tweet> tweets = new ArrayList<>();
        for (DBTweet dbTweet : dbTweets) {
            tweets.add(dbTweet.createTweet());
        }
        return new TweetExchange.Builder().tweets(tweets).build();
    }

    /************************************************
     * CONSTRUCTING NEW TWEET
     ************************************************/

    /**
     * Takes in a message that I created and stores it in the database
     */
    public static void constructNewTweet(Context context, String recipient, String msg) {
        Log.d(TAG, "Message is: " + msg);
        incrementVCTime(context);
        constructNewTweet(context, new Random().nextInt(), Utils.getOrCreateNewUUID(context).toString(), recipient, msg);
    }

    /**
     * Saves the new tweet into the database, including saving a DBTweet and the appropriate
     * DBVectorClockItems
     */
    public static void constructNewTweet(Context context, int tweetID, String author, String recipient, String msg) {
        String senderUUID = getOrCreateNewUUID(context).toString();

        // Construct database entry
        DBTweet dbTweet = new DBTweet(tweetID, author, msg, recipient, senderUUID);
        dbTweet.save();

        // Get Vector Clocks
        List<VectorClockItem> vectorClockItems = createVectorClockArrayForNewMessage(context);
        // Add my new vector clock
        vectorClockItems.add(new VectorClockItem.Builder().clock(incrementVCTime(context)).uuid(senderUUID).build());


        // Create a DBVectorClockItem item for each vector clock item and save it to db
        for (VectorClockItem vc : vectorClockItems) {
            new DBVectorClockItem(vc.uuid, vc.clock, dbTweet).save();
        }
    }

    /**
     * Returns a list of VectorClockItems used as the timestamp for a new tweet.
     * The new timestamp is equal to the max of all vector clocks authors currently in the table
     * with the addition of my current vector clock time + 1.
     */
    public static List<VectorClockItem> createVectorClockArrayForNewMessage(Context context) {
        String myUUID = getOrCreateNewUUID(context).toString();
        List<VectorClockItem> vectorClockItems = new ArrayList<>();
        List<DBVectorClockItem> dbVectorClockItems = SQLiteUtils.rawQuery(DBVectorClockItem.class,
                "SELECT *, MAX(clock) FROM DBVectorClockItems GROUP BY `UUID`", new String[]{});
        for (DBVectorClockItem dbVC : dbVectorClockItems) {
            if (!dbVC.uuid.equals(myUUID)){
                VectorClockItem vcItem = new VectorClockItem.Builder().clock(dbVC.clock).uuid(dbVC.uuid).build();
                vectorClockItems.add(vcItem);
            }
        }
        return vectorClockItems;
    }

    /************************************************
     * READING TWEET EXCHANGE
     ************************************************/

    /**
     * Saves all the tweets in the database, excluding duplicates
     */
    public static void readTweetExchangeSaveTweetsInDB(TweetExchange te) {
        HashMap<String, HashSet<Integer>> authorToIDs = new HashMap<>();
        for (DBTweet d : getAllDbTweets()) {
            if (!authorToIDs.containsKey(d.author)) {
                authorToIDs.put(d.author, new HashSet<Integer>());
            }
            HashSet<Integer> h = authorToIDs.get(d.author);
            h.add(d.tweetID);
        }
        List<Tweet> tweets = te.tweets;
        for (Tweet t : tweets) {
            if (!authorToIDs.containsKey(t.author) || !authorToIDs.get(t.author).contains(t.id)) {
                DBTweet dbTweet = new DBTweet(t.id, t.author, t.content, t.recipient, t.sender_uuid);
                dbTweet.save();

                for (VectorClockItem vc : t.vector_clocks) {
                    new DBVectorClockItem(vc.uuid, vc.clock, dbTweet).save();
                }
            } else {
                Log.d(TAG, "Already have message " + t.id + " from " + t.author);
            }
        }
    }

    /************************************************
     * REMOVAL
     ************************************************/

    /**
     * Removes all items from the tables but does not drop them
     */
    public static void removeAllItemsFromDB() {
        new Delete().from(DBVectorClockItem.class).execute();
        new Delete().from(DBTweet.class).execute();
    }

    /**
     * Drops all the tables
     * Used when we have to update the database
     */
    public static void dropTables() {
        SQLiteUtils.execSql("DROP TABLE IF EXISTS DBVectorClockItems");
        SQLiteUtils.execSql("DROP TABLE IF EXISTS DBTweets");
        SQLiteUtils.execSql("DROP TABLE IF EXISTS DBUsers");
        SQLiteUtils.execSql("DROP TABLE IF EXISTS UsersWithEncryption");
    }

    /************************************************
     * DEBUG
     ************************************************/

    /**
     * Logs all the tweets in the database, including their vector clocks.
     * For debugging purposes.
     */
    public static void logAllTweetsInDB(Context context) {
        List<DBTweet> dbTweets = getAllDbTweets();
        List <Tweet> tweets = new ArrayList<>();
        for (DBTweet dbTweet : dbTweets) {
            tweets.add(dbTweet.createTweet());
            Log.d(TAG, dbTweet.toString());
        }
    }

    /************************************************
     *
     *
     * ENCRYPTION UTILS
     *
     *
     ************************************************/

    public static boolean checkIfUserAcceptsEncryption(String username, String uuid) {
        return new Select()
                .from(DBUser.class)
                .where("username = ?", username)
                .where("uuid = ?", uuid)
                .execute()
                .size() > 0;
    }

    public static boolean markUserAcceptsEncryption(String username, String uuid, String publicKey) {
        new DBUser(username, uuid, publicKey, true).save();
        return true;
    }

    public static List<String> getUsers() {
        ArrayList<String> users = new ArrayList<>();
        List<DBUser> dbUsers = new Select().from(DBUser.class).execute();
        for (DBUser u : dbUsers) {
            users.add(u.toString());
        }
        users.add(0, "All");
        return users;
    }

    /************************************************
     *
     *
     * OTHER UTIL METHODS
     *
     *
     ************************************************/

    /**
     * Returns a display string from a bluetooth device.
     */
    public static String deviceToString(BluetoothDevice device) {
        return device.getName() + "\n" + device.getAddress();
    }

    /**
     * Returns a list of strings representing each device from a set of Bluetooth devices
     */
    public static String convertDevicesToString(List<BluetoothDevice> bluDevices) {
        String devices = "";
        for (BluetoothDevice device : bluDevices) {
            devices += Utils.deviceToString(device);
        }
        return devices;
    }

    /**
     * Read N bytes from the given input stream
     */
    public static byte[] readBytesFromStream(DataInputStream inStream, int n) throws IOException {
        byte[] result = new byte[n];
        for (int i = 0; i < n; ) {
            i += inStream.read(result, i, n-i);
        }
        return result;
    }


}
