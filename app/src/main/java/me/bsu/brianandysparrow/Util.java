package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothDevice;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.HashMap;

/**
 * Created by aschmitt on 2/5/16.
 */
public class Util {


    /**
     * Read N bytes from the given input stream
     */
    public static byte[] readBytesFromStream(DataInputStream inStream, int n) throws IOException {
        byte[] result = new byte[n];
        for (int i = 0; i < n; ) {
            i += inStream.read(result);
        }
        return result;
    }

    /**
     * Returns a display string from a bluetooth device.
     *
     * @param device
     * @return
     */
    public static String deviceToString(BluetoothDevice device) {
        return device.getAddress();
    }

    /**
     * Returns a list of strings representing each device from a set of Bluetooth devices
     *
     * @param bluDevices
     * @return
     */
    public static String deviceListToString(HashSet<BluetoothDevice> bluDevices) {
        String devices = "\n";
        for (BluetoothDevice device : bluDevices) {
            devices += Util.deviceToString(device) + "\n";
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

}
