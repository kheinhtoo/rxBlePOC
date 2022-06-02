package com.viatom.a20ftest;

import android.util.Log;

import java.util.ArrayList;

public class DeviceController {
    private static ArrayList<Bluetooth> bleDevices = new ArrayList<Bluetooth>();

    synchronized public static boolean addDevice(Bluetooth b) {
        boolean needNotify = false;

        if (!bleDevices.contains(b)) {
            bleDevices.add(b);
            needNotify = true;

        }

        return needNotify;
    }

    synchronized public static ArrayList<Bluetooth> getDevices() {
        return bleDevices;
    }

    synchronized public static ArrayList<Bluetooth> getDevicesWithFilter(String name, int minRssi) {
        ArrayList<Bluetooth> list = new ArrayList<Bluetooth>();

        if (name.length() == 0) {
            for (Bluetooth b : bleDevices) {
                if (b.getRssi() >= minRssi) {
                    list.add(b);
                }
            }
        } else {

            for (Bluetooth b : bleDevices) {
                if (b.getName().contains(name) && b.getRssi() >= minRssi) {
                    list.add(b);
                }
            }
        }

        Log.d("20F", "list length: " + list.size() + ", befor filter:" + bleDevices.size());
        return list;
    }

    synchronized static public void clear() {
        bleDevices = new ArrayList<Bluetooth>();
    }
}
