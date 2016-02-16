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
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

import me.bsu.brianandysparrow.models.DBTweet;
import me.bsu.brianandysparrow.models.DBVectorClockItem;
import me.bsu.proto.Tweet;
import me.bsu.proto.TweetExchange;
import me.bsu.proto.VectorClockItem;

public class Utils {

    private static String TAG = "me.bsu.Utils";

    /**
     * Returns a display string from a bluetooth device.
     *
     * @param device
     * @return
     */
    public static String deviceToString(BluetoothDevice device) {
        return device.getName() + "\n" + device.getAddress();
    }

    /**
     * Returns a list of strings representing each device from a set of Bluetooth devices
     *
     * @param bluDevices
     * @return
     */
    public static String convertDevicesToString(List<BluetoothDevice> bluDevices) {
        String devices = "";
        for (BluetoothDevice device : bluDevices) {
            devices += Utils.deviceToString(device);
        }
        return devices;
    }

    public static String deviceMapToString(HashMap<BluetoothDevice, DeviceConnector.DeviceTriplet> deviceMap) {
        String result = "\n";

        for (BluetoothDevice btd : deviceMap.keySet()) {
            result += deviceToString(btd) + " - " + deviceMap.get(btd) + "\n";
        }

        return result;
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
