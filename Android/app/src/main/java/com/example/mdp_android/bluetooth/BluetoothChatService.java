package com.example.mdp_android.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.mdp_android.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothChatService
{
    private static final String TAG = "BluetoothChatService";
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    private static UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private final BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private int mState;
    private boolean mAttemptReconnect = false;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_LOST = 4;
    private BluetoothDevice mConnectedDevice;

    public BluetoothChatService(Context context, Handler handler)
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    public String getDeviceName()
    {
        if(mConnectedDevice != null) return mConnectedDevice.getName();
        else return null;
    }

    public String getDeviceAddress()
    {
        if(mConnectedDevice != null) return mConnectedDevice.getAddress();
        else return null;
    }

    private Message prepareMsg(int msgType, String TAG , String value)
    {
        Message msg = mHandler.obtainMessage(msgType);
        Bundle bundle = new Bundle();
        bundle.putString(TAG, value);
        msg.setData(bundle);
        return msg;
    }

    private synchronized void updateUI()
    {
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mState, -1).sendToTarget();

        if(mState == STATE_LOST)
        {
            mState = STATE_NONE;
            try
            {
                wait(1000);
                updateUI();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    public synchronized int getState()
    {
        return mState;
    }

    public synchronized void start()
    {
        Log.d(TAG, "start");

        if (mConnectThread != null)
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread == null)
        {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }

        if (mInsecureAcceptThread == null)
        {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }

        updateUI();
    }

    public synchronized void connect(BluetoothDevice device, boolean secure)
    {
        if (mState == STATE_CONNECTING)
        {
            if (mConnectThread != null)
            {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectedDevice = device;
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        updateUI();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType)
    {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        if (mConnectThread != null)
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null)
        {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null)
        {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();
        mConnectedDevice = device;
        mHandler.sendMessage(prepareMsg(Constants.MESSAGE_DEVICE_NAME, Constants.DEVICE_NAME, device.getName()));
        updateUI();
    }

    public synchronized void stop()
    {
        Log.d(TAG, "BluetoothChatService stopped");
        mAttemptReconnect = false;

        if (mConnectThread != null)
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null)
        {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null)
        {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        mState = STATE_NONE;
        updateUI();
    }

    public void write(byte[] out)
    {
        ConnectedThread r;

        synchronized (this)
        {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        r.write(out);
    }

    private void connectionFailed()
    {
        if(!mAttemptReconnect)
        {
            mHandler.sendMessage(prepareMsg(Constants.MESSAGE_TOAST, Constants.TOAST, "Unable to connect to device"));
        }

        mState = STATE_NONE;
        updateUI();
        BluetoothChatService.this.start();
    }

    private synchronized void connectionLost()
    {
        mState = STATE_LOST;
        updateUI();

        if(mAttemptReconnect && BluetoothManager.getInstance().bluetoothAvailable())
        {
            mHandler.sendMessage(prepareMsg(Constants.MESSAGE_TOAST, Constants.TOAST, "Device connection was lost. Attempting to reconnect..."));
            int counter = 0;
            int maxTries = 3;

            while (mState != STATE_CONNECTED && counter < maxTries)
            {
                if (mConnectedDevice != null)
                {
                    counter++;
                    mHandler.sendMessage(prepareMsg(Constants.MESSAGE_TOAST, Constants.TOAST, "Attempting to reconnect (" + counter + ")"));
                    connect(mConnectedDevice, true);

                    try
                    {
                        wait(3000);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            if (mState != BluetoothChatService.STATE_CONNECTED)
            {
                BluetoothChatService.this.start();
                mHandler.sendMessage(prepareMsg(Constants.MESSAGE_TOAST, Constants.TOAST, "Failed to reconnect!"));
            }

            mAttemptReconnect = false;
        }
    }

    private class AcceptThread extends Thread
    {
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure)
        {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try
            {
                if (secure)
                {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
                }

                else
                {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);
                }
            }
            catch (IOException e)
            {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }

            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run()
        {
            Log.d(TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            while (mState != STATE_CONNECTED)
            {
                try
                {
                    socket = mmServerSocket.accept(3000);
                }
                catch (IOException e)
                {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                if (socket != null)
                {
                    synchronized (BluetoothChatService.this)
                    {
                        switch (mState)
                        {
                            case STATE_LISTEN:

                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice(), mSocketType);
                                break;

                            case STATE_NONE: //Nothing

                            case STATE_CONNECTED:
                                try
                                {
                                    socket.close();
                                }
                                catch (IOException e)
                                {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }

                                break;
                        }
                    }
                }
            }

            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);
        }

        public void cancel()
        {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);

            try
            {
                mmServerSocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;
        private final String TAG = "ConnectThread";

        public ConnectThread(BluetoothDevice device, boolean secure)
        {
            Log.d(TAG, "created: " + secure);
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try
            {
                if (secure)
                {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                }
                else
                {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            }
            catch (IOException e)
            {
                Log.e(TAG, "Failed to create Socket Type: " + mSocketType, e);
            }

            mmSocket = tmp;
            mState = STATE_CONNECTING;
            updateUI();
        }

        public void run()
        {
            Log.d(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            if(BluetoothManager.getInstance().bluetoothAvailable()) mBluetoothAdapter.cancelDiscovery();

            if(mmSocket != null)
            {
                try
                {
                    mmSocket.connect();
                }
                catch (IOException e)
                {
                    Log.e(TAG, "unable to connect() " + mSocketType, e);

                    try
                    {
                        mmSocket.close();
                    }
                    catch (IOException e2)
                    {
                        Log.e(TAG, "unable to close() " + mSocketType + " socket during connection failure", e2);
                    }

                    connectionFailed();
                    return;
                }

                synchronized (BluetoothChatService.this)
                {
                    mConnectThread = null;
                }

                connected(mmSocket, mmDevice, mSocketType);
            }
            else
            {
                Log.e(TAG, "Socket is null! Unable to invoke connected()");
                this.cancel();
            }
        }

        public void cancel()
        {
            try
            {
                Log.d(TAG, "cancelled!");
                mmSocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final String TAG = "ConnectedThread";

        public ConnectedThread(BluetoothSocket socket, String socketType)
        {
            Log.d(TAG, "created: " + socketType);

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                if(socket != null)
                {
                    tmpIn = socket.getInputStream();
                    tmpOut = socket.getOutputStream();
                }
                else
                {
                    Log.e(TAG, "Socket received is null! Connection closing!");
                    this.cancel();
                }
            }
            catch (IOException e)
            {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
            updateUI();
        }

        public void run()
        {
            Log.i(TAG, "running...");
            byte[] buffer = new byte[1024];
            int bytes;

            while (mState == STATE_CONNECTED)
            {
                try
                {
                    mAttemptReconnect = true;
                    bytes = mmInStream.read(buffer);
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    mAttemptReconnect = false;
                }
                catch (IOException e)
                {
                    Log.e(TAG, "disconnected while running", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer)
        {
            try
            {
                mmOutStream.write(buffer);
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            }
            catch (IOException e)
            {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel()
        {
            Log.d(TAG, "Cancelled!");

            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}