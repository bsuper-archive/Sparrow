package me.bsu.brianandysparrow;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aschmitt on 2/5/16.
 */
public class Util {



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
            devices += Util.deviceToString(device);
        }
        return devices;
    }


}
