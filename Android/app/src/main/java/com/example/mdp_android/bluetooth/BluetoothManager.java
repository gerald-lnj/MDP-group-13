package com.example.mdp_android.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.example.mdp_android.Constants;

public class BluetoothManager
{
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothChatService mChatService;
    private static BluetoothManager _instance;
    private static Handler _mHandler;
    private Activity mActivity;
    public static final int BT_REQUEST_CODE = 0;

    private final String LAST_DEVICE = "storedRecord";
    private final String LAST_DEVICE_NAME = "storedName";
    private final String LAST_DEVICE_ADDRESS = "storedAddress";

    public BluetoothManager(Activity activity, Handler mHandler)
    {
        mActivity = activity;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mChatService = new BluetoothChatService(activity, nHandler);
        _mHandler = mHandler;
        _instance = this;
    }

    public static BluetoothManager getInstance()
    {
        return _instance;
    }

    public Boolean bluetoothAvailable()
    {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public void bluetoothErrorMsg()
    {
        Toast.makeText(mActivity, "Please turn on Bluetooth!", Toast.LENGTH_SHORT).show();
    }

    public void listBluetoothDevices()
    {
        if(bluetoothAvailable())
        {
            if (mBluetoothAdapter.isDiscovering())
            {
                mBluetoothAdapter.cancelDiscovery();
            }

            mBluetoothAdapter.startDiscovery();
        }
        else
        {
            BluetoothManager.getInstance().bluetoothErrorMsg();
        }
    }

    public void connectDevice(String address, boolean secure)
    {
        if(bluetoothAvailable() && mChatService != null)
        {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            mChatService.connect(device, secure);
        }
        else
        {
            bluetoothErrorMsg();
        }
    }

    public void stop()
    {
        mChatService.stop();
    }

    public void sendMessage(String type, int num)
    {
        sendMessage(type, String.valueOf(num));
    }

    public void sendMessage(String type, String msg)
    {
        if(mChatService != null && mChatService.getState() == BluetoothChatService.STATE_CONNECTED)
        {
            String toSend = msg;

            if (type != null)
            {
                toSend = type+':'+msg;
            }

            Log.d("comms_send", toSend);

            mChatService.write(toSend.getBytes());
        }
        else
        {
            Toast.makeText(mActivity, "Bluetooth is unavailable! Unable to send message.", Toast.LENGTH_SHORT);
        }
    }

    public String getDeviceName()
    {
        return mChatService.getDeviceName();
    }

    public String getDeviceAddress()
    {
        return mChatService.getDeviceAddress();
    }

    public Boolean isConnected()
    {
        return mChatService != null && mChatService.getState() == mChatService.STATE_CONNECTED;
    }

    public void notConnectedMsg()
    {
        Toast.makeText(mActivity, "Bluetooth is not connected to any device!", Toast.LENGTH_SHORT);
    }

    private final Handler nHandler = new Handler(new Handler.Callback()
    {
        @Override
        public boolean handleMessage(Message msg)
        {
            if(msg.what == Constants.MESSAGE_STATE_CHANGE && msg.arg1 == BluetoothChatService.STATE_CONNECTED)
            {
                if(mChatService.getDeviceName() != null && mChatService.getDeviceAddress() != null)
                    storeDeviceRecord(mChatService.getDeviceName(), mChatService.getDeviceAddress());
            }

            Message newMsg = _mHandler.obtainMessage(msg.what);
            newMsg.copyFrom(msg);
            _mHandler.sendMessage(newMsg);
            return true;
        }
    });

    private void storeDeviceRecord(String deviceName, String deviceAddress)
    {
        SharedPreferences settings = mActivity.getSharedPreferences(LAST_DEVICE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(LAST_DEVICE_NAME, deviceName);
        editor.putString(LAST_DEVICE_ADDRESS, deviceAddress);
        editor.apply();
    }

    public String[] retrieveDeviceRecord()
    {
        SharedPreferences settings = mActivity.getSharedPreferences(LAST_DEVICE, 0);
        String deviceName = settings.getString(LAST_DEVICE_NAME, null);
        String deviceAddress = settings.getString(LAST_DEVICE_ADDRESS, null);
        String[] returnVal = {deviceName, deviceAddress};
        return returnVal;
    }
}