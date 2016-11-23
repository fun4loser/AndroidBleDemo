package com.sanmiao.bluetoothdemo.activity;

import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.sanmiao.bluetoothdemo.R;
import com.sanmiao.bluetoothdemo.adapter.ScanDeviceAdapter;
import com.sanmiao.bluetoothdemo.service.AutoManageService;
import com.sanmiao.bluetoothdemo.utils.LLog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeviceControlActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_DEVICE_STATE = "device_state";

    @BindView(R.id.et_cmd)
    EditText et_cmd;
    @BindView(R.id.btn_send_cmd)
    Button btn_send_cmd;
    @BindView(R.id.scroll_cmd)
    ScrollView scroll_cmd;
    @BindView(R.id.tv_cmd)
    TextView tv_cmd;
    @BindView(R.id.device_name)
    TextView tv_name;
    @BindView(R.id.device_address)
    TextView tv_address;
    @BindView(R.id.tv_connect_state)
    TextView tv_connect_state;
    @BindView(R.id.btn_connect)
    Button btn_connect;
    @BindView(R.id.btn_query_state)
    Button btn_query_state;//查询连接状态

    private String mAddress = "";
    private String mName = "";
    private int mConnectState = BluetoothProfile.STATE_DISCONNECTED;
    private AutoManageService mAutoManageService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mAutoManageService = ((AutoManageService.LocalBinder) service).getService();
            if (mAutoManageService.initialize()) {
                btn_connect.setEnabled(true);
                btn_send_cmd.setEnabled(true);
                btn_query_state.setEnabled(true);
            } else {
                LLog.d(getClass(), "设备不支持蓝牙");
                Toast.makeText(DeviceControlActivity.this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceConnection = null;
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String address = intent.getStringExtra(AutoManageService.EXTRA_DEVICE_ADDRESS);
            if (!address.equals(mAddress)) {
                return;
            }
            String action = intent.getAction();
            if (action.equals(AutoManageService.ACTION_GATT_SERVICES_CONNECTION_STATE)) {
                mConnectState = intent.getIntExtra(AutoManageService.EXTRA_CONNECT_STATE, BluetoothProfile.STATE_DISCONNECTED);
                tv_connect_state.setText("连接状态：" + ScanDeviceAdapter.getStateString(mConnectState));
                btn_connect.setText(mConnectState == BluetoothProfile.STATE_CONNECTED ? "断开" : "连接");
            } else if (action.equals(AutoManageService.ACTION_DATA_AVAILABLE)) {
                StringBuilder data = new StringBuilder("\n设备名称:").append(mName).append("设备地址:").append(mAddress);
                data.append("数据:");
                data.append(intent.getStringExtra(AutoManageService.EXTRA_DATA));
                tv_cmd.append(data);
                scroll_cmd.fullScroll(View.FOCUS_DOWN);
            }
        }
    };

    public static void start(Context context, String address, String name, int state) {
        Intent intent = new Intent(context, DeviceControlActivity.class);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
        intent.putExtra(EXTRA_DEVICE_NAME, name);
        intent.putExtra(EXTRA_DEVICE_STATE, state);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        mAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mConnectState = intent.getIntExtra(EXTRA_DEVICE_STATE, BluetoothProfile.STATE_DISCONNECTED);
        tv_name.append(mName);
        tv_address.append(mAddress);
        tv_connect_state.append(ScanDeviceAdapter.getStateString(mConnectState));
        btn_connect.setText(mConnectState == BluetoothProfile.STATE_CONNECTED ? "断开" : "连接");

        intent = new Intent(this, AutoManageService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter(AutoManageService.ACTION_GATT_SERVICES_CONNECTION_STATE);
        intentFilter.addAction(AutoManageService.ACTION_DATA_AVAILABLE);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        unregisterReceiver(mReceiver);
    }

    @OnClick({R.id.btn_send_cmd, R.id.btn_connect, R.id.btn_query_state})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send_cmd:
                mAutoManageService.write(mAddress, et_cmd.getText().toString());
                break;
            case R.id.btn_connect:
                if (mConnectState == BluetoothProfile.STATE_CONNECTED) {
                    mAutoManageService.disconnect(mAddress);
                } else if (mConnectState == BluetoothProfile.STATE_DISCONNECTED) {
                    mAutoManageService.connect(mAddress);
                }
                break;
            case R.id.btn_query_state:
                mAutoManageService.queryConnectState(mAddress);
                break;
        }
    }
}
