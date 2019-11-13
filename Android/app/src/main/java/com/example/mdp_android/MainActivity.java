package com.example.mdp_android;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabItem;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.example.mdp_android.bluetooth.BluetoothChatService;
import com.example.mdp_android.bluetooth.BluetoothManager;
import com.example.mdp_android.tabs.BluetoothFragment;
import com.example.mdp_android.tabs.CommFragment;
import com.example.mdp_android.tabs.MapFragment;
import com.example.mdp_android.tabs.SectionPageAdapter;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{
    private BluetoothManager mBluetoothMgr;
    private SectionPageAdapter mSectionPageAdapter;
    private PagerAdapter pagerAdapter;
    private boolean _accelReady = false;
    final Handler handler = new Handler();
    private Maze maze; //added on

    //Create new arraylist for fragments
    private static ArrayList<CallbackFragment> callbackFragList = new ArrayList<CallbackFragment>();

    //RPI sends complete buffers of strings, so split by ";", and store any leftover message
    private String _storedMessage = "";

    private static final String TAG = "MainActivity";

    //Accelerometer Controls
    Switch controlswitch;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private float xAccel = 0.0f;
    private float yAccel = 0.0f;
    private float zAccel = 0.0f;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        controlswitch = findViewById(R.id.acceSwitch);

        //Only ask for these permissions on runtime when running on android 6.0 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }

        //Bluetooth
        mBluetoothMgr = new BluetoothManager(this, mHandler);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        //Request Bluetooth to be switched on
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, BluetoothManager.BT_REQUEST_CODE);

        //Sensors initialization for Accelerometer
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(MainActivity.this, mSensor,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1)
    {

    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        int accel_dir  = 0;
        controlswitch = (Switch)findViewById(R.id.acceSwitch);

        if(controlswitch!=null)
        {
            if (controlswitch.isChecked())
            {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                {
                    xAccel = event.values[0];
                    Log.d("X", Float.toString(xAccel));

                    yAccel = event.values[1];
                    Log.d("Y", Float.toString(yAccel));

                    zAccel = event.values[2];
                    Log.d("Z", Float.toString(zAccel));

                    if (xAccel <= 0 && yAccel <= -2.5 && zAccel >= 1.1)
                    {
                        accel_dir = Constants.up;
                        Log.d(TAG, "tilting up");
                        MapFragment.getMaze().attemptMoveBot(Constants.NORTH, true);
                    }
                    else if (xAccel <= 0 && yAccel >= 2.5 && zAccel >= 1.1)
                    {
                        accel_dir = Constants.down;
                        Log.d(TAG, "tilting down");
                        MapFragment.getMaze().attemptMoveBot(Constants.SOUTH, true);
                    }
                    else if (xAccel >= 2.5 && yAccel >= 0 && zAccel >= 1.1)
                    {
                        accel_dir = Constants.left;
                        Log.d(TAG, "tilting left");
                        MapFragment.getMaze().attemptMoveBot(Constants.WEST, true);
                    }
                    else if (xAccel <= -2.5 && yAccel >= 0 && zAccel >= 1.1)
                    {
                        accel_dir = Constants.right;
                        Log.d(TAG, "tilting right");
                        MapFragment.getMaze().attemptMoveBot(Constants.EAST, true);
                    }
                }
            }
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment)
    {
        super.onAttachFragment(fragment);
        if(fragment instanceof CallbackFragment)
        {
            callbackFragList.add((CallbackFragment) fragment);
        }
    }

    /**
     * Receiver for broadcast events from the system (Mostly bluetooth related)
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state)
                {
                    case BluetoothAdapter.STATE_OFF:
                        mBluetoothMgr.stop();
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //Do nothing
                        break;

                    case BluetoothAdapter.STATE_ON:
                        String[] deviceRecord = mBluetoothMgr.retrieveDeviceRecord();
                        if (deviceRecord[0] != null && deviceRecord[1] != null)
                        {
                            mBluetoothMgr.connectDevice(deviceRecord[1], true);
                            Toast.makeText(getApplicationContext(), "Bluetooth turned on! Auto-connecting...", Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        //Do nothing
                        break;
                }
            }
        }
    };

    /**
     * The handler that gets info back from the bluetoothchatservice and updates main activity/bluetooth fragment
     */
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1)
                    {
                        case BluetoothChatService.STATE_CONNECTED:
                            String tmp = "Connected to: " + mBluetoothMgr.getDeviceName();
                            Toast.makeText(MainActivity.this, tmp, Toast.LENGTH_SHORT).show();
                            notifyFragments(Constants.MESSAGE_STATE_CHANGE, null, tmp);
                            break;

                        case BluetoothChatService.STATE_CONNECTING:
                            notifyFragments(Constants.MESSAGE_STATE_CHANGE, null,"Connecting...");
                            break;

                        case BluetoothChatService.STATE_LOST:
                            notifyFragments(Constants.MESSAGE_STATE_CHANGE, null, "Connection Lost!");
                            Toast.makeText(MainActivity.this, "Connection Lost!", Toast.LENGTH_SHORT).show();
                            break;

                        //case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            notifyFragments(Constants.MESSAGE_STATE_CHANGE, null, "Not Connected");
                            break;
                    }
                    break;

                case Constants.MESSAGE_WRITE:
                    //Don't do anything
                    break;

                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    if(readMessage == null || readMessage == "")
                    {
                        return;
                    }
                    else if(readMessage.contains(";"))
                    {
                        String[] msgList = readMessage.split(";");
                        for (int i = 0; i < msgList.length; i++)
                        {
                            String processedMsg;
                            if (i == 0)
                            {
                                processedMsg = _storedMessage + msgList[i];
                                _storedMessage = "";
                            }
                            else if (i == msgList.length - 1)
                            {
                                if(readMessage.charAt(readMessage.length()-1) == ';')
                                {
                                    processedMsg = msgList[i];
                                }
                                else
                                {
                                    _storedMessage += msgList[i];
                                    continue;
                                }
                            }
                            else
                            {
                                processedMsg = msgList[i];
                            }

                            String type = "";
                            String value = processedMsg.trim();

                            if (value != null && value.contains("|"))
                            {
                                String[] tmp = value.split("\\|");

                                if(tmp.length == 1)
                                {
                                    //Android will not receive no message w/o type in our system
                                    type = tmp[0] != "" ? tmp[0] : "";
                                    value = "";
                                }
                                else if(tmp.length == 2)
                                {
                                    //Message contain both key and value
                                    if (tmp[0].contains("BOT"))
                                    {
                                        type = "BOT";
                                    }
                                    else
                                    {
                                      type = tmp[0] != "" ? tmp[0] : "";
                                    }

                                    value = tmp[1] != "" ? tmp[1] : "";
                                }
                            }

                            notifyFragments(Constants.MESSAGE_READ, type,  value);
                        }
                    }
                    else
                    {
                        _storedMessage += readMessage;
                        notifyFragments(Constants.MESSAGE_READ, "REC", readMessage);

                        //Added for robot status display on AMD Tool
                        if (readMessage.length() > 13) {
                            if (readMessage.substring(2, 8).equals("status"))
                            {
                                String robotstatus = readMessage.substring(11, readMessage.length() - 2);
                                notifyFragments(Constants.MESSAGE_READ, "STATUS", robotstatus);
                            }
                        }

                        //Added for obstacle in AMD
                        if (readMessage.length() > 20)
                        {
                            if (readMessage.substring(2, 6).equals("grid"))
                            {
                                String gridobstacles = readMessage.substring(11, readMessage.length() - 2);
                                notifyFragments(Constants.MESSAGE_READ, "GRID", gridobstacles);
                            }
                        }

                        //Added for arrowblocks in AMD
                        if (readMessage.length() > 6)
                        {
                            if (readMessage.substring(0, 5).equals("IMAGE"))
                            {
                                String imageblock = readMessage.substring(6, readMessage.length());
                                notifyFragments(Constants.MESSAGE_READ, "IMAGE", imageblock);
                            }
                        }

                        //Added for STOP for EXP and FSP
                        if (readMessage.equals("STOP"))
                        {
                            Log.d("Stop", "Stop");
                            String stopmsg = readMessage.substring(0,4);
                            notifyFragments(Constants.MESSAGE_READ,"STOP", stopmsg);
                            notifyFragments(Constants.MESSAGE_READ,"RESIMG", "ResolveImages");
                        }

                        //Added
                        if (readMessage.length() > 8)
                        //if(readMessage.substring(0,8).equals("MOVEMENT"))
                        {
                            if (readMessage.substring(0, 8).equals("MOVEMENT"))
                            {
                                String received = readMessage;
                                Log.d("Received", received);
                                String movement[] = received.split("\\|");

                                if (movement.length < 5)
                                {
                                    for (int i = 0; i < movement.length; i++)
                                    {
                                        Log.d("Movement String Error", movement[i]);
                                    }
                                }

                                if (movement.length == 5)
                                {
                                    String movementstr = movement[0];
                                    Log.d("Movement", movementstr);

                                    String mdf1 = movement[1];
                                    Log.d("mdf1", mdf1);
                                    notifyFragments(Constants.MESSAGE_READ, "MDF1", mdf1);

                                    String mdf2 = movement[2];
                                    Log.d("mdf2", mdf2);
                                    notifyFragments(Constants.MESSAGE_READ, "MDF2", mdf2);

                                    String reccoords = movement[3];
                                    Log.d("coords", reccoords);
                                    String coords = reccoords.substring(1, reccoords.length() - 1);

                                    int mid = coords.length() / 2;
                                    String y_coord = coords.substring(0, mid);
                                    y_coord = y_coord.trim();
                                    String x_coord = coords.substring(mid);
                                    x_coord = x_coord.trim();

                                    String orientation = movement[4];
                                    Log.d("orientation", orientation);

                                    String coords_dir = x_coord + " " + y_coord + " " + orientation;
                                    Log.d("Final", coords_dir);
                                    notifyFragments(Constants.MESSAGE_READ, "COORD", coords_dir);
                                }
                            }

                            if (readMessage.substring(0, 7).equals("FASTEST")) {
                                String recfastest = readMessage;
                                Log.d("Received Fastest", recfastest);

                                String fastestpath = recfastest.substring(8, recfastest.length());
                                Log.d("Fastest Path", fastestpath);

                                notifyFragments(Constants.MESSAGE_READ, "FASTEST", fastestpath);
                            }
                        }
                    }
                    break;

                case Constants.MESSAGE_TOAST:
                    Toast.makeText(MainActivity.this, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    break;

                case Constants.ACCELERATE:
                    notifyFragments(Constants.ACCELERATE, null, msg.getData().getString("ACCEL_EVENT"));
                    break;
            }
        }
    };

    //Cleanup Methods
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mBluetoothMgr.stop();
    }

    //For handling callbacks from bluetoothchatservice to the tab fragments
    public interface CallbackFragment
    {
        public void update(int type, String key, String msg);
    }

    /**
     * For passing messages/events from bluetoothmanager
     * */
    public void notifyFragments(int type, String key, String msg)
    {
        for(CallbackFragment i:callbackFragList)
        {
            i.update(type, key, msg);
        }
    }

    //Leaderboard requirement
    private static ArrayList<String> msgHistory = new ArrayList<String>();

    public static void updateMsgHistory(String text)
    {
        msgHistory.add(text);
    }

    public static ArrayList<String> getMsgHistory()
    {
        return msgHistory;
    }

    public static void resetMsgHistory()
    {
        msgHistory = new ArrayList<String>();
    }
}
