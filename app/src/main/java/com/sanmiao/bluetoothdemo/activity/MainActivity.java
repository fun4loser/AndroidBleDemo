package com.sanmiao.bluetoothdemo.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.sanmiao.bluetoothdemo.R;
import com.sanmiao.bluetoothdemo.adapter.ScanDeviceAdapter;
import com.sanmiao.bluetoothdemo.service.AutoManageService;
import com.sanmiao.bluetoothdemo.utils.Constants;
import com.sanmiao.bluetoothdemo.utils.LLog;
import com.sanmiao.bluetoothdemo.utils.Utils;
import com.sanmiao.bluetoothdemo.views.DividerItemDecoration;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 8888;
    public ArrayDeque<BluetoothDevice> mDevicesToAdd = new ArrayDeque<>();
    @BindView(R.id.rv_devices)
    RecyclerView mRvDevices;
    @BindView(R.id.btn_scan)
    Button mBtnScan;
    @BindView(R.id.tv_cmd)
    TextView mTvCmd;
    @BindView(R.id.scroll_cmd)
    ScrollView mScrollCmd;
    @BindView(R.id.btn_on)
    Button btn_on;
    @BindView(R.id.btn_off)
    Button btn_off;
    private BluetoothAdapter mBluetoothAdapter;
    private ScanDeviceAdapter mScanDeviceAdapter;
    private Handler mHandler;
    private boolean mScanning = false;
    private AutoManageService mAutoManageService;

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    LLog.d(getClass(), "scan-found" + "name" + device.getName() + "addr" + device.getAddress());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mScanDeviceAdapter.addDevice(device);
                            if (mAutoManageService != null) {
                                mAutoManageService.connect(device.getAddress());
                            } else {
                                //  如果AutoManageService还没准备好，就先存下
                                mDevicesToAdd.add(device);
                            }
                        }
                    });
                }
            };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String device_address = intent.getStringExtra(AutoManageService.EXTRA_DEVICE_ADDRESS);
            int position = Utils.getListPosition(mScanDeviceAdapter.getDevices(), device_address, Utils.DeviceWrapperAddressEquals);
            BluetoothDevice device = mScanDeviceAdapter.getDevice(position);
            if (action.equals(AutoManageService.ACTION_DATA_AVAILABLE)) {
                //  show data
                StringBuilder data = new StringBuilder("\n设备名称:").append(device.getName()).append("设备地址:").append(device.getAddress());
                data.append("数据:");
                data.append(intent.getStringExtra(AutoManageService.EXTRA_DATA));
                mTvCmd.append(data);
                mScrollCmd.fullScroll(View.FOCUS_DOWN);
            } else if (action.equals(AutoManageService.ACTION_GATT_SERVICES_CONNECTION_STATE)) {
                //  update connection state
                int newState = intent.getIntExtra(AutoManageService.EXTRA_CONNECT_STATE, BluetoothProfile.STATE_DISCONNECTED);
                mScanDeviceAdapter.updateConnectionState(position, newState);
                if(newState == BluetoothProfile.STATE_DISCONNECTED && !mScanDeviceAdapter.getDevices().get(position).isUserWantDisconnect()){
                    mAutoManageService.connect(device_address);
                }
            }
        }
    };
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mAutoManageService = ((AutoManageService.LocalBinder) service).getService();
            if (mAutoManageService.initialize()) {
                while (!mDevicesToAdd.isEmpty()) {
                    BluetoothDevice device = mDevicesToAdd.poll();
                    mAutoManageService.connect(device.getAddress());
                }
            } else {
                LLog.d(getClass(), "设备不支持蓝牙");
                Toast.makeText(MainActivity.this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceConnection = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ButterKnife.bind(this);

        mHandler = new Handler();

        mScanDeviceAdapter = new ScanDeviceAdapter(this);
        mRvDevices.setAdapter(mScanDeviceAdapter);
        mRvDevices.setLayoutManager(new LinearLayoutManager(this));
        mRvDevices.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        mRvDevices.setItemAnimator(new DefaultItemAnimator());

        Intent intent = new Intent(this, AutoManageService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(AutoManageService.ACTION_DATA_AVAILABLE);
        filter.addAction(AutoManageService.ACTION_GATT_SERVICES_CONNECTION_STATE);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAutoManageService != null) {
            mAutoManageService.unbindService(mServiceConnection);
        }
        unregisterReceiver(mReceiver);
        //  close bluetooth
        mBluetoothAdapter.disable();
    }

    @OnClick({R.id.btn_scan, R.id.btn_on, R.id.btn_off})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                if (!mScanning) {
                    scanLeDevice(true);
                }
                break;
            case R.id.btn_on:
                for (ScanDeviceAdapter.DeviceWrapper dev :
                        mScanDeviceAdapter.getDevices()) {
                    if (mAutoManageService != null) {
                        mAutoManageService.write(dev.getDevice().getAddress(), Constants.CMD_TURN_ON);
                    } else {
                        mDevicesToAdd.add(dev.getDevice());
                    }
                }
                break;
            case R.id.btn_off:
                for (ScanDeviceAdapter.DeviceWrapper dev :
                        mScanDeviceAdapter.getDevices()) {
                    if (mAutoManageService != null) {
                        mAutoManageService.write(dev.getDevice().getAddress(), Constants.CMD_TURN_OFF);
                    } else {
                        mDevicesToAdd.add(dev.getDevice());
                    }
                }
                break;
        }
    }

    public AutoManageService getAutoManageService() {
        return mAutoManageService;
    }

    private void scanLeDevice(boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mBtnScan.setText("扫描");
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBtnScan.setText("扫描中...");
            mBluetoothAdapter.startLeScan(/*new UUID[]{Constants.UUID_CONNECT}, */mLeScanCallback);
        } else {
            mScanning = false;
            mBtnScan.setText("扫描");
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }
}
