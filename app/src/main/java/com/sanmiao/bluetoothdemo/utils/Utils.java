package com.sanmiao.bluetoothdemo.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.sanmiao.bluetoothdemo.adapter.ScanDeviceAdapter;
import com.sanmiao.bluetoothdemo.service.AutoManageService;

import java.util.List;

/**
 * @author 刚桥恕
 * @功能描述 描述
 * @date 2016/11/14 0014 11:06.
 */

public class Utils {

    public static final Equals<BluetoothDevice, String> BluetoothDeviceAndAddressEquals = new Equals<BluetoothDevice, String>() {
        @Override
        public boolean equals(BluetoothDevice device, String s) {
            if (device == null || s == null) {
                return false;
            }
            return device.getAddress().equals(s);
        }
    };
    public static final Equals<BluetoothDevice, BluetoothDevice> BluetoothDeviceEquals = new Equals<BluetoothDevice, BluetoothDevice>() {
        @Override
        public boolean equals(BluetoothDevice device, BluetoothDevice device2) {
            if (device == null || device2 == null) {
                return false;
            }
            return device.getAddress().equals(device2.getAddress());
        }
    };
    public static final Equals<BluetoothGatt, BluetoothGatt> BluetoothGattEquals = new Equals<BluetoothGatt, BluetoothGatt>() {
        @Override
        public boolean equals(BluetoothGatt gatt, BluetoothGatt gatt2) {
            if (gatt == null || gatt2 == null) {
                return false;
            }
            return gatt.getDevice().getAddress().equals(gatt2.getDevice().getAddress());
        }
    };
    public static final Equals<BluetoothGatt, String> BluetoothGattAndAddressEquals = new Equals<BluetoothGatt, String>() {
        @Override
        public boolean equals(BluetoothGatt gatt, String s) {
            if (gatt == null || s == null) {
                return false;
            }
            return gatt.getDevice().getAddress().equals(s);
        }
    };

    /**
     * 16进制字符串转字节数组
     */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    public static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static <M, N> int getListPosition(List<M> list, N n, Equals<M, N> equals) {
        if (n == null || list == null || list.size() == 0 || equals == null) {
            return -1;
        }
        for (int i = 0; i < list.size(); i++) {
            M m = list.get(i);
            if (equals.equals(m, n)) {
                return i;
            }
        }
        return -1;
    }

    public static <M, N> M getListElement(List<M> list, N n, Equals<M, N> equals) {
        if (list == null || n == null || equals == null) {
            return null;
        }
        for (M m :
                list) {
            if (equals.equals(m, n)) {
                return m;
            }
        }
        return null;
    }

    public static <M, N> boolean contains(List<M> list, N n, Equals<M, N> equals) {
        if (list == null || list.size() == 0 || n == null || equals == null) {
            return false;
        }
        for (M m :
                list) {
            if (equals.equals(m, n)) {
                return true;
            }
        }
        return false;
    }

    public static final Equals<ScanDeviceAdapter.DeviceWrapper, String> DeviceWrapperAddressEquals = new Equals<ScanDeviceAdapter.DeviceWrapper, String>() {
        @Override
        public boolean equals(ScanDeviceAdapter.DeviceWrapper deviceWrapper, String s) {
            if(deviceWrapper == null || s == null){
                return false;
            }
            return deviceWrapper.getDevice().getAddress().equals(s);
        }
    };
}
