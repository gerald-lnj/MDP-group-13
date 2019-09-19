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

import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.example.mdp_android.bluetooth.BluetoothChatService;
import com.example.mdp_android.bluetooth.BluetoothManager;
import com.example.mdp_android.tabs.BluetoothFragment;
import com.example.mdp_android.tabs.CommFragment;
import com.example.mdp_android.tabs.SectionPageAdapter;

import java.util.ArrayList;

import static com.example.mdp_android.R.menu.menu_items;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{
    private BluetoothManager mBluetoothMgr;
    private SectionPageAdapter mSectionPageAdapter;
    private PagerAdapter pagerAdapter;
    private boolean _accelReady = false;
    final Handler handler = new Handler();

    //CREATE NEW ARRAYLIST FOR FRAGMENTS
    private static ArrayList<CallbackFragment> callbackFragList = new ArrayList<CallbackFragment>();

    // RPI SENDS COMPLETE BUFFERS OF STRINGS, SO SPLIT BY ';', AND STORE ANY LEFTOVER MESSAGES
    // FOR THE NEXT MESSAGE
    private String _storedMessage = "";

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ONLY ASK FOR THESE PERMISSIONS ON RUNTIME WHEN RUNNING ON ANDROID 6.0 OR HIGHER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }

        //TABLAYOUT TAB ITEMS
        Toolbar toolbar = findViewById(R.id.toolbar);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        TabItem tabMap = findViewById(R.id.map);
        TabItem tabComm = findViewById(R.id.comm);
        TabItem tabBluetooth = findViewById(R.id.bluetooth);
        TabItem tabImage = findViewById(R.id.imagecheck);
        final ViewPager mViewPager = findViewById(R.id.container);
        SectionPageAdapter pageAdapter = new SectionPageAdapter(getSupportFragmentManager(), tabLayout.getTabCount());

        mViewPager.setAdapter(pageAdapter);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                final InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mViewPager.getWindowToken(), 0);
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {

            }
        });

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        mViewPager.setCurrentItem(1);

        //BLUETOOTH
        mBluetoothMgr = new BluetoothManager(this, mHandler);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        //REQUEST BLUETOOTH TO BE SWITCHED ON
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, BluetoothManager.BT_REQUEST_CODE);

        SensorManager sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

//        //TOP MENU BAR
//        Toolbar top_menu_bar = (Toolbar)findViewById(R.id.toolbar);
//        setSupportActionBar(top_menu_bar);
//        if(getSupportActionBar() != null)
//        {
//            getSupportActionBar().setTitle("MDP Group 13");
//        }
//
//        toolbar.inflateMenu(menu_items);
    }

//    //OVERRIDE TO DISPLAY MENU FOR TOP MENU BAR
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu)
//    {
//        getMenuInflater().inflate(menu_items, menu);
//        return true;
//    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1)
    {

    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER)
        {
            if(_accelReady)
            {
                float ax=event.values[0];
                float ay=event.values[1];
                float az=event.values[2];
                String str = String.valueOf(ax)+' '+String.valueOf(ay)+' '+String.valueOf(az);

                int accel_dir = -1;
                if(ax <= 0 && ay <= -4 && az >= 5) accel_dir = Constants.up;
                else if(ax <= 2 && ay >= 4 && az >= 4) accel_dir = Constants.down;
                else if(ax >= 5 && ay >= -1 && az >= 5) accel_dir = Constants.left;
                else if(ax <= -5 && ay >= -1 && az >= 5) accel_dir = Constants.right;

                Message msg = mHandler.obtainMessage(Constants.ACCELERATE);
                Bundle bundle = new Bundle();
                bundle.putString("ACCEL_EVENT", String.valueOf(accel_dir));
                msg.setData(bundle);

                mHandler.handleMessage(msg);
                _accelReady = false;
                handler.postDelayed(new Runnable()
                {
                    public void run()
                    {
                        _accelReady = true;
                    }
                }, 1000);
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
     * RECEIVER FOR BROADCAST EVENTS FROM THE SYSTEM (MOSTLY BLUETOOTH RELATED)
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
                        //DO NOTHING
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
                        //DO NOTHING
                        break;
                }
            }
        }
    };

    /**
     * THE HANDLER THAT GETS INFO BACK FROM THE BLUETOOTHCHATSERVICE AND UPDATES MAINACTIVITY/BLUETOOTH FRAGMENT
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
                    //DON'T DO ANYTHING
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
                                    //ANDROID WILL NOT RECEIVE NO MESSAGE W/O TYPE IN OUR SYSTEM
                                    type = tmp[0] != "" ? tmp[0] : "";
                                    value = "";
                                }
                                else if(tmp.length == 2)
                                {
                                    //MESSAGE CONTAIN BOTH KEY AND VALUE
                                    // issue: sometimes message becomes: "F    BOT"|3,3,S;
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

    //CLEANUP METHODS
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mBluetoothMgr.stop();
    }

    //FOR HANDLING CALLBACKS FROM BLUETOOTHCHATSERVICE TO THE TAB FRAGMENTS
    public interface CallbackFragment
    {
        public void update(int type, String key, String msg);
    }

    /**
     * FOR PASSING MESSAGES/EVENTS FROM BLUETOOTHMANAGER
     * */
    public void notifyFragments(int type, String key, String msg)
    {
        for(CallbackFragment i:callbackFragList)
        {
            i.update(type, key, msg);
        }
    }

    //LEADERBOARD REQUIREMENT
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

//    //TOP MENU BAR - NAVIGATION (NOT USED)
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item)
//    {
//        switch(item.getItemId()) {
//            case R.id.controller:
//                Intent controllerIntent = new Intent(MainActivity.this, MainActivity.class);
//                startActivity(controllerIntent);
//                return false;
//
//            case R.id.stringprocessing:
//                Intent stringIntent = new Intent(MainActivity.this, CommFragment.class);
//                startActivity(stringIntent);
//                return false;
//
//            case R.id.bluetooth:
//                Intent bluetoothIntent = new Intent(MainActivity.this, BluetoothFragment.class);
//                startActivity(bluetoothIntent);
//                return false;
//
//            default:
//                break;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
}