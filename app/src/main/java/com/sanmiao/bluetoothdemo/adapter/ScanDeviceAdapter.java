package com.sanmiao.bluetoothdemo.adapter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.sanmiao.bluetoothdemo.R;
import com.sanmiao.bluetoothdemo.activity.DeviceControlActivity;
import com.sanmiao.bluetoothdemo.activity.MainActivity;
import com.sanmiao.bluetoothdemo.service.AutoManageService;
import com.sanmiao.bluetoothdemo.utils.LLog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author 刚桥恕
 * @功能描述 描述
 * @date 2016/11/15 0015 15:34.
 */

public class ScanDeviceAdapter extends RecyclerView.Adapter<ScanDeviceAdapter.DeviceHolder> {

    private List<DeviceWrapper> mDevices = new ArrayList<>();
    private MainActivity mMainActivity;

    public ScanDeviceAdapter(MainActivity mainActivity) {
        mMainActivity = mainActivity;
    }

    public static String getStateString(int connectState) {
        String s_connect_state = "未知状态";
        switch (connectState) {
            case BluetoothProfile.STATE_CONNECTED:
                s_connect_state = "已连接";
                break;
            case BluetoothProfile.STATE_CONNECTING:
                s_connect_state = "正在连接";
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                s_connect_state = "已断开";
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                s_connect_state = "正在断开";
                break;
        }
        return s_connect_state;
    }

    @Override
    public DeviceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DeviceHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan_device, parent, false));
    }

    @Override
    public void onBindViewHolder(DeviceHolder holder, int position) {
        BluetoothDevice device = mDevices.get(position).getDevice();
        holder.tv_name.setText(device.getName());
        holder.tv_address.setText(device.getAddress());
        int connectState = mDevices.get(position).getState();
        holder.tv_connection_state.setText(getStateString(connectState));
        holder.btn_connect.setText(connectState == BluetoothProfile.STATE_CONNECTED ? "断开" : "连接");
    }

    public void updateConnectionState(BluetoothDevice device, int newState) {
//        int position = Utils.getListPosition(mDevices, device, Utils.BluetoothDeviceEquals);
        int position = mDevices.indexOf(new DeviceWrapper(device, BluetoothProfile.STATE_DISCONNECTED));
        if (position < 0 || position > mDevices.size() - 1) {
            return;
        }
        updateConnectionState(position, newState);
    }

    public void updateConnectionState(int position, int newState) {
        DeviceWrapper deviceWrapper = mDevices.get(position);
        if(newState==BluetoothProfile.STATE_CONNECTED){
            deviceWrapper.setUserWantDisconnect(false);
        }
        BluetoothDevice device = deviceWrapper.getDevice();
        LLog.d(getClass(), "update connect state:" + device.getName() + device.getAddress() + "state" + newState + "position" + position);
        if (deviceWrapper.getState() == newState) {
            return;
        }
        deviceWrapper.setState(newState);
        notifyItemChanged(position);
    }

    public void addDevice(BluetoothDevice device) {
        if (mDevices == null) {
            mDevices = new ArrayList<>();
        }
        DeviceWrapper deviceWrapper = new DeviceWrapper(device, BluetoothProfile.STATE_DISCONNECTED);
        if (mDevices.contains(deviceWrapper)) {
            return;
        }
        mDevices.add(deviceWrapper);
        notifyItemInserted(mDevices.size() - 1);
    }

    public BluetoothDevice getDevice(int position) {
        return mDevices.get(position).getDevice();
    }

    public List<DeviceWrapper> getDevices() {
        return mDevices;
    }

    @Override
    public int getItemCount() {
        return mDevices == null ? 0 : mDevices.size();
    }

    public class DeviceWrapper {
        private BluetoothDevice mDevice;
        private int mState;
        private boolean mUserWantDisconnect = false;

        public DeviceWrapper(BluetoothDevice device, int state) {
            mDevice = device;
            mState = state;
        }

        public boolean isUserWantDisconnect() {
            return mUserWantDisconnect;
        }

        public void setUserWantDisconnect(boolean userWantDisconnect) {
            mUserWantDisconnect = userWantDisconnect;
        }

        public BluetoothDevice getDevice() {
            return mDevice;
        }

        public void setDevice(BluetoothDevice device) {
            mDevice = device;
        }

        public int getState() {
            return mState;
        }

        public void setState(int state) {
            mState = state;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DeviceWrapper) {
                return mDevice.equals(((DeviceWrapper) obj).getDevice());
            }
            return false;
        }
    }

    class DeviceHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tv_connection)
        TextView tv_connection_state;
        @BindView(R.id.device_name)
        TextView tv_name;
        @BindView(R.id.device_address)
        TextView tv_address;
        @BindView(R.id.btn_connect)
        Button btn_connect;

        public DeviceHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //  start　DeviceControlActivity
                    DeviceWrapper deviceWrapper = mDevices.get(getAdapterPosition());
                    BluetoothDevice device = deviceWrapper.getDevice();
                    DeviceControlActivity.start(mMainActivity, device.getAddress(), device.getName(), deviceWrapper.getState());
                }
            });
        }

        @OnClick(R.id.btn_connect)
        public void onClick(View v) {
            LLog.d(getClass(), "connect clicked");
            int position = getAdapterPosition();
            DeviceWrapper deviceWrapper = mDevices.get(position);
            BluetoothDevice device = deviceWrapper.getDevice();
            AutoManageService service = mMainActivity.getAutoManageService();
            if (service != null) {
                if (deviceWrapper.getState() == BluetoothProfile.STATE_CONNECTED) {
                    service.disconnect(device.getAddress());
                    deviceWrapper.setUserWantDisconnect(true);
                } else if (deviceWrapper.getState() == BluetoothProfile.STATE_DISCONNECTED) {
                    deviceWrapper.setUserWantDisconnect(false);
                    service.connect(device.getAddress());
                }
            }
        }
    }
}
