package com.sanmiao.bluetoothdemo.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.sanmiao.bluetoothdemo.operation.ConnectOperation;
import com.sanmiao.bluetoothdemo.operation.DisconnectOperation;
import com.sanmiao.bluetoothdemo.operation.OperationQueue;
import com.sanmiao.bluetoothdemo.operation.WriteOperation;
import com.sanmiao.bluetoothdemo.utils.Constants;
import com.sanmiao.bluetoothdemo.utils.LLog;
import com.sanmiao.bluetoothdemo.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 刚桥恕
 * @功能描述 描述
 * @date 2016/11/16 0016 15:45.
 */

public class AutoManageService extends Service {

    public static final String ACTION_GATT_SERVICES_CONNECTION_STATE =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_CONNECTION_STATE";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_CONNECT_STATE = "connect_state";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    private IBinder mIBinder = new LocalBinder();
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private List<BluetoothGatt> mGatts = new ArrayList<>();
    private OperationQueue mOperationQueue;
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            LLog.d(AutoManageService.this.getClass(), "char change---address---" + gatt.getDevice().getAddress());
            broadcastReceivedData(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            LLog.d(AutoManageService.this.getClass(), "char read---status---" + status + "address---" + gatt.getDevice().getAddress());
            broadcastReceivedData(gatt, characteristic);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            LLog.d(AutoManageService.this.getClass(), "service discovered---address" + gatt.getDevice().getAddress() + "status-" + status);
            sendCmdDelay(gatt, Constants.CMD_TURN_ON);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            LLog.d(getClass(), "connect-state- mGatts contains gatt-address" + gatt.getDevice().getAddress() + mGatts.contains(gatt));
            LLog.d(AutoManageService.this.getClass(), "connect-state---" + newState + "address---" + gatt.getDevice().getAddress());
            Intent intent = new Intent(ACTION_GATT_SERVICES_CONNECTION_STATE);
            intent.putExtra(EXTRA_DEVICE_ADDRESS, gatt.getDevice().getAddress());
            intent.putExtra(EXTRA_CONNECT_STATE, newState);
            sendBroadcast(intent);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //  when connected, discover services
                LLog.d(getClass(), "connected, attempt to discover services of address" + gatt.getDevice().getAddress());
                gatt.discoverServices();
            }

            mOperationQueue.satisfy(ConnectOperation.SATISFY_TYPE, gatt.getDevice().getAddress());
            mOperationQueue.satisfy(DisconnectOperation.SATISFY_TYPE, gatt.getDevice().getAddress());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            LLog.d(AutoManageService.this.getClass(), "char write-status---" + status + "address" + gatt.getDevice().getAddress());
            broadcastReceivedData(gatt, characteristic);
            mOperationQueue.satisfy(WriteOperation.SATISFY_TYPE, gatt.getDevice().getAddress());
        }
    };

    private void broadcastReceivedData(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        String data = decodeResult(characteristic);
        Intent intent = new Intent(ACTION_DATA_AVAILABLE);
        intent.putExtra(EXTRA_DATA, data);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, gatt.getDevice().getAddress());
        sendBroadcast(intent);
    }

    private void sendCmdDelay(final BluetoothGatt gatt, final String cmd) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                write(gatt, cmd);
            }
        }, Constants.CMD_INTERVAL);
    }

    private String decodeResult(BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        final StringBuilder stringBuilder = new StringBuilder();
        if (data != null && data.length > 0) {
            for (byte byteChar : data) {
                stringBuilder.append(String.format("%02X ", byteChar));
            }
        }
        return stringBuilder.toString();
    }

    public void write(String address, String cmd) {
        LLog.d(getClass(), "attempt to send cmd to address" + address);

        BluetoothGatt gatt = Utils.getListElement(mGatts, address, Utils.BluetoothGattAndAddressEquals);

        if (gatt == null) {
            //  if this address is not in the list, try to connect it
            connect(address);
            return;
        }
        write(gatt, cmd);
    }

    private boolean writeImpl(final BluetoothGatt gatt, final String cmd) {
        if (gatt == null) {
            LLog.d(getClass(), "send cmd gatt == null");
            return false;
        }
        LLog.d(getClass(), "send cmd address-" + gatt.getDevice().getAddress());
        if (!(mBluetoothManager.getConnectionState(gatt.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED)) {
            LLog.d(getClass(), "attempt to send cmd to a device that is not connected");
            return false;
        }
        if (TextUtils.isEmpty(cmd)) {
            LLog.d(getClass(), "send cmd cmd == null");
            return false;
        }
        BluetoothGattService service = gatt.getService(Constants.UUID_CONNECT);
        if (service == null) {
            LLog.d(getClass(), "send cmd service == null");
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.UUID_SEND_CMD);
        if (characteristic == null) {
            LLog.d(getClass(), "send cmd char == null");
            return false;
        }
        characteristic.setValue(Utils.hexStringToBytes(cmd));
        boolean write = gatt.writeCharacteristic(characteristic);
        LLog.d(getClass(), "write result " + write);
        gatt.setCharacteristicNotification(characteristic, true);
        return write;
    }

    private void write(final BluetoothGatt gatt, final String cmd) {
        if (gatt == null || TextUtils.isEmpty(cmd)) {
            return;
        }
        mOperationQueue.add(new WriteOperation() {
            @Override
            protected boolean operate() {
                ID = gatt.getDevice().getAddress();
                return writeImpl(gatt, cmd);
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mOperationQueue = new OperationQueue(mHandler);
        LLog.d(getClass(), "service onCreate");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LLog.d(getClass(), "service on bind");
        return mIBinder;
    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                LLog.d(getClass(), "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            LLog.d(getClass(), "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LLog.d(getClass(), "service on destroy");
        //  close all
        for (int i = 0; i < mGatts.size(); i++) {
            BluetoothGatt gatt = mGatts.get(i);
            gatt.disconnect();
        }
        for (int i = 0; i < mGatts.size(); i++) {
            BluetoothGatt gatt = mGatts.get(i);
            gatt.close();
            mGatts.set(i, null);
        }
    }

    public void queryConnectState(String address) {
        int state = mBluetoothManager.getConnectionState(mBluetoothAdapter.getRemoteDevice(address), BluetoothProfile.GATT);
        Toast.makeText(
                this,
                address + "---" + (state == BluetoothProfile.STATE_CONNECTED ? "已连接" : "已断开"),
                Toast.LENGTH_LONG
        ).show();
    }

    public void disconnect(final String address) {
        LLog.d(getClass(), "attempt to disconnect address" + address);
        if (TextUtils.isEmpty(address)) {
            return;
        }

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        int state = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        LLog.d(getClass(), "query state address" + address + "state" + state);
        if (state == BluetoothProfile.STATE_DISCONNECTED || state == BluetoothProfile.STATE_DISCONNECTING) {
            return;
        }

        //  queue it
        mOperationQueue.add(new DisconnectOperation() {
            @Override
            protected boolean operate() {
                ID = address;
                return disconnectImpl(address);
            }
        });
        //  no queue
//        disconnectImpl(address);
    }

    private boolean disconnectImpl(String address) {
        int position = Utils.getListPosition(mGatts, address, Utils.BluetoothGattAndAddressEquals);
        BluetoothGatt gatt = mGatts.get(position);
        if (gatt != null) {
            gatt.disconnect();
//            gatt.close();
//            mGatts.remove(position);
//            mGattsNotEchoed.remove(gatt);
            LLog.d(getClass(), "disconnect impl result true address-" + address);
            return true;
        }
        LLog.d(getClass(), "disconnect impl result false address-" + address);
        return false;
    }

    public void connect(final String address) {
        LLog.d(getClass(), "attempt to connect address" + address);
        if (TextUtils.isEmpty(address)) {
            return;
        }

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        int state = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        LLog.d(getClass(), "query state address" + address + "state" + state);
        if (state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_CONNECTING) {
            if (!Utils.contains(mGatts, address, Utils.BluetoothGattAndAddressEquals)) {
                connectImpl(address);
            }
            return;
        }

        mOperationQueue.add(new ConnectOperation() {
            @Override
            protected boolean operate() {
                ID = address;
                return connectImpl(address);
            }
        });
        //  no queue
//        connectImpl(address);
    }

    private boolean connectImpl(String address) {
        if (TextUtils.isEmpty(address)) {
            LLog.d(getClass(), "connect impl result false address" + address);
            return false;
        }
        //  new a gatt when connect
        int position = Utils.getListPosition(mGatts, address, Utils.BluetoothGattAndAddressEquals);
        if (position >= 0 && position < mGatts.size()) {
            BluetoothGatt gatt = mGatts.remove(position);
            gatt.close();
        }
        BluetoothGatt gatt = mBluetoothAdapter.getRemoteDevice(address).connectGatt(this, false, mGattCallback);
        mGatts.add(gatt);

        //  use current gatt
//        BluetoothGatt gatt = Utils.getListElement(mGatts, address, Utils.BluetoothGattAndAddressEquals);
//        if (gatt == null) {
//            gatt = mBluetoothAdapter.getRemoteDevice(address).connectGatt(this, false, mGattCallback);
//            mGatts.add(gatt);
//            mGattsNotEchoed.add(gatt);
//        } else {
//            gatt.connect();
//        }
        LLog.d(getClass(), "connect impl result true address" + address);
        return true;
    }

    public class LocalBinder extends Binder {
        public AutoManageService getService() {
            return AutoManageService.this;
        }
    }
}
