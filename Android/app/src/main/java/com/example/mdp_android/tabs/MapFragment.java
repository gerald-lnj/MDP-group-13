package com.example.mdp_android.tabs;

import android.graphics.Color;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mdp_android.Constants;
import com.example.mdp_android.MainActivity;
import com.example.mdp_android.Maze;
import com.example.mdp_android.MazeTile;
import com.example.mdp_android.R;
import com.example.mdp_android.bluetooth.BluetoothManager;

import java.util.ArrayList;

public class MapFragment extends Fragment implements MainActivity.CallbackFragment
{
    private static Maze maze;
    private Boolean _autoRefresh = true;
    private long fastestTime = 0;
    private long fastestpathtime = 0;
    private long exploreTime = 0;
    private long exploredTime = 0;
    private final Handler refreshHandler = new Handler();

    Switch updateswitch;
    Button updatebutton;
    Boolean firstImage = false;

    //Added for AMD Tool Only
    private static String _storekey = "";
    private String _testmsg;

    private static int img_coord;

    /**
     * Display Map Activity
     * */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.activity_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        //Set up the Maze
        setupMaze();

        //Set up the buttons
        initializeButtons();
        setupButtonListeners();
    }

    /**
     * Create "Maze" container viewgroup and initializes it
     * */
    private void setupMaze()
    {
        //Ger maze view via ID
        RelativeLayout mazeLayout = getView().findViewById(R.id.mazeLayout);
        maze = new Maze(getActivity());
        mazeLayout.addView(maze);
    }

    /**
     * Resets button states and bot status text
     * */
    private void initializeButtons()
    {
        getView().findViewById(R.id.coordBtn).setEnabled(true);
        getView().findViewById(R.id.waypoint_button).setEnabled(true);
        getView().findViewById(R.id.exploreBtn).setEnabled(true);
        getView().findViewById(R.id.fastestBtn).setEnabled(true);
        getView().findViewById(R.id.resetBtn).setEnabled(true);
        //getView().findViewById(R.id.manualBtn).setEnabled(true);

        updateswitch = getView().findViewById(R.id.updateSwitch);
        updateswitch.setEnabled(true);

        getView().findViewById(R.id.updateBtn).setEnabled(false);
        updatebutton = getView().findViewById(R.id.updateBtn);

        TextView sv = getView().findViewById(R.id.statusText);
        sv.setText("Ready!");

        TextView fsptime = getView().findViewById(R.id.fastestTime);
        fsptime.setText("0 mins 0 s");

        TextView expltime = getView().findViewById(R.id.explTime);
        expltime.setText("0 mins 0 s");

        TextView mdf1view = getView().findViewById(R.id.MDF1String);
        mdf1view.setText("000000000000000000000000000000000000000000000000000000000000000000000000000");

        TextView mdf2view = getView().findViewById(R.id.MDF2String);
        mdf2view.setText("000000000000000000000000000000000000000000000000000000000000000000000000000");

        BluetoothManager.getInstance().sendMessage("SET_STATUS", "Ready!");
    }

    private void setupButtonListeners()
    {
        //Set start and end coordinates
        getView().findViewById(R.id.coordBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (maze.getState() == Constants.idleMode)
                {
                    maze.resetStartEnd();
                    Toast.makeText(getActivity(), "Tap on Maze Tiles to set Start & End coordinates", Toast.LENGTH_SHORT).show();
                    maze.setState(Constants.coordinateMode);
                }
            }
        });

        //Exploration of the Map (Only can run when coordinates are set)
        getView().findViewById(R.id.exploreBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (maze.getState() == Constants.idleMode /*&& maze.coordinatesSet()*/)
                {
                    Toast.makeText(getActivity(), "Starting Exploration Now!", Toast.LENGTH_SHORT).show();
                    BluetoothManager.getInstance().sendMessage("al", "EXPLORE");
                    getView().findViewById(R.id.coordBtn).setEnabled(false);
                    maze.setState(Constants.exploreMode);

                    TextView sv = getView().findViewById(R.id.statusText);
                    sv.setText("Exploration");

                    exploreTime = System.nanoTime();

                    //BluetoothManager.getInstance().sendMessage("SET_STATUS", "Exploring the Maze");
                }
            }
        });

        //Set waypoint for the maze (Need to set waypoint first)
        getView().findViewById(R.id.waypoint_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (maze.getState() == Constants.idleMode /*&& maze.isExploreCompleted()*/)
                {
                    maze.resetWp();
                    maze.setState(Constants.waypointMode);
                    Toast.makeText(getActivity(), "Tap on Maze Tiles to set any Waypoint", Toast.LENGTH_SHORT).show();
                }
                else if (maze.getState() == Constants.waypointMode)
                {
                    maze.setState(Constants.idleMode);
                    Toast.makeText(getActivity(), "Cancelling Waypoint...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Fastest Path
        getView().findViewById(R.id.fastestBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(getActivity(), "Executing Fastest Path!", Toast.LENGTH_SHORT).show();
                getView().findViewById(R.id.waypoint_button).setEnabled(false);
                BluetoothManager.getInstance().sendMessage("al", "FASTEST");
                maze.setState(Constants.fastestPathMode);

                TextView sv = getView().findViewById(R.id.statusText);
                sv.setText("Fastest Path");

                fastestTime = System.nanoTime();

                //BluetoothManager.getInstance().sendMessage("SET_STATUS", "Travelling Fastest Path");
            }
        });

        //Reset the Maze
        getView().findViewById(R.id.resetBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(getActivity(), "Maze Reset!", Toast.LENGTH_SHORT).show();
                BluetoothManager.getInstance().sendMessage("reset", "");
                maze.reset();
                initializeButtons();
            }
        });

        //Up/North Button - Directional
        getView().findViewById(R.id.upBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                maze.attemptMoveBot(Constants.NORTH, true);

            }
        });

        //Down Button - Directional
        getView().findViewById(R.id.downBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                maze.attemptMoveBot(Constants.SOUTH, true);
            }
        });

        //Right/East Button - Directional
        getView().findViewById(R.id.rightBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                maze.attemptMoveBot(Constants.EAST, true);
            }
        });

        //Left/West Button - Directional
        getView().findViewById(R.id.leftBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                        maze.attemptMoveBot(Constants.WEST, true);
            }
        });

        //Get robot status (Acutally not needed for this button cause status will change automatically)
//        getView().findViewById(R.id.statusBtn).setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                BluetoothManager.getInstance().sendMessage("GET_STATUS", "statusss");
//            }
//        });

        //Manual Update Button
        getView().findViewById(R.id.updateBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(getActivity(), "Updating the Map!", Toast.LENGTH_SHORT).show();

                //Added for AMD Tool
                if(_storekey.equals("GRID"))
                {
                    maze.handleAMDGrid(_testmsg);
                }

                maze.renderMaze();

//                //Below will only work with algo
//                BluetoothManager.getInstance().sendMessage("GET_DATA", "");
//                BluetoothManager.getInstance().sendMessage("GET_STATUS", "");
            }
        });

        //Auto Update switch - turned on as default
        updateswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if(isChecked)
                {
                    Toast.makeText(getActivity(), "Auto Update On!", Toast.LENGTH_SHORT).show();
                    updatebutton.setVisibility(View.INVISIBLE);
                    updatebutton.setEnabled(false);

                    Maze.setAuto(); //Added for auto mode

                    // On/off thread to auto call for maze updates
                    _autoRefresh = !_autoRefresh;
                    if (_autoRefresh)
                    {
                        refreshHandler.postDelayed(new Runnable()
                        {
                            public void run()
                            {
                                //BluetoothManager.getInstance().sendMessage("GET_DATA", "");

                                if (_autoRefresh)
                                {
                                    refreshHandler.postDelayed(this, 2000);
                                }
                            }
                        }, 2000);
                    }
                }
                else
                {
                    Toast.makeText(getActivity(), "Auto Update Off!", Toast.LENGTH_SHORT).show();
                    updatebutton.setVisibility(buttonView.VISIBLE);

                    Maze.setAuto();
                    _autoRefresh = false;
                    updatebutton.setEnabled(true);
                }
            }
        });
    }

//        //CALIBRATE BUTTON - Not needed for Android
//        getView().findViewById(R.id.calibrateBtn).setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v) {
//                if(maze.getState() == Constants.idleMode){
//                    Toast.makeText(getActivity(), "Auto Calibrating", Toast.LENGTH_SHORT).show();
//                    BluetoothManager.getInstance().sendMessage("MOVE", "rrcrcr");
//                }
//            }
//        });

    //Algorithm - Display MDF Strings (Hex Strings)
    private String MDF1 = ""; //need to change to _mdfExplore
    private String MDF2 = ""; //need to change to _mdfObstacles

    //Handle Bluetooth messages received
    public void update(int type, String key, String msg)
    {
        if(key != null)
        {
            key = key.trim();
        }

        if(msg != null)
        {
            msg = msg.trim();
        }

        switch (type)
        {
            case Constants.MESSAGE_STATE_CHANGE:
                //BLUETOOTH STATE CHANGE (NOT REQUIRED)
                // Log.d("MESSAGE_STATE_CHANGE", msg);
                break;

            case Constants.MESSAGE_READ:

                //Received Messages
                if(key.equals("MDF1")) //Explored Data
                {
                    MDF1 = msg;
                    Log.d("MDF1", MDF1);

                    TextView mdf1view = getView().findViewById(R.id.MDF1String);
                    mdf1view.setText(MDF1);

                    maze.handleExplore(msg);
                }

                else if(key.equals("MDF2")) //Obstacle Data
                {
                    //For AMD Tool only
                    _storekey = key;
                    MDF2 = msg;

                    int count = 0;

                    for(int i = 0; i < MDF2.length(); i++) {
                        if(MDF2.charAt(i) != ' ')
                            count++;
                    }

                    Log.d("Count", Integer.toString(count));

                    if (count%2 != 0)
                    {
//                        int pad = count % 8;
//                        int padnums = 8 - pad;

//                        for(int j=0; j<padnums; j++)
//                        {
                            MDF2 = MDF2 + "0";
//                        }
                    }

                    Log.d("MDF2", MDF2);

                    TextView mdf2view = getView().findViewById(R.id.MDF2String);
                    mdf2view.setText(MDF2);

                    if (_autoRefresh)
                    {
                        maze.handleAMDGrid(msg);
                    }

                    Log.d("data",msg);
                }

//                else if(key.equals("MDF2")) //Obstacle Data //testing only
//                {
//                    MDF2 = msg;
//                    maze.handleObstacle(msg);
//                }

                else if(key.equals("STOP"))
                {
                    if (maze.getState() == Constants.exploreMode) //Exploration Done
                    {
                        maze.setExploreCompleted(true);

                        //Display Most Recent MDP Strings
                        MainActivity.updateMsgHistory("MDF1: "+ MDF1);
                        Log.d("MDF1: ", MDF1);

                        MainActivity.updateMsgHistory("MDF2: "+ MDF2);
                        Log.d("MDF2: ", MDF2);

                        maze.displayArrowBlockString();

                        //Update Time
                        exploredTime = System.nanoTime() - exploreTime;
                        int seconds = Math.round(exploredTime/1000000000);
                        int minutes = seconds/60;
                        seconds = seconds - minutes * 60;

                        TextView expltime = getView().findViewById(R.id.explTime);
                        expltime.setText(minutes+ " mins "+ seconds + " s");

                        //Update Grid Mode
                        getView().findViewById(R.id.exploreBtn).setEnabled(false);
                        maze.setState(Constants.idleMode);

                        //Update Text and Status
                        Toast.makeText(getActivity(), "Exploration completed!", Toast.LENGTH_SHORT).show();
                        TextView sv = getView().findViewById(R.id.statusText);
                        sv.setText("EXP Completed");
                    }

                    else if (maze.getState() == Constants.fastestPathMode)
                    {
                        //Update Grid Mode
                        getView().findViewById(R.id.fastestBtn).setEnabled(false);
                        maze.setState(Constants.idleMode);

                        //Update Time
                        fastestpathtime = System.nanoTime() - fastestTime;
                        int seconds = Math.round(fastestpathtime/1000000000);
                        int minutes = seconds/60;
                        seconds = seconds - minutes*60;
                        TextView fsptime = getView().findViewById(R.id.fastestTime);
                        fsptime.setText(minutes + " mins " + seconds + " s");

                        //Update Text and Status
                        Toast.makeText(getActivity(), "Fastest Path completed!", Toast.LENGTH_SHORT).show();
                        TextView sv = getView().findViewById(R.id.statusText);
                        sv.setText("FSP Completed");
                    }
                }

//                else if(key.equals("EX_DONE"))
//                {
//                    maze.setExploreCompleted(true);
//
//                    //DISPLAY MOST RECENT MDP STRINGS
//                    MainActivity.updateMsgHistory("MDF_Explore: "+ MDF1);
//                    Log.d("MDF_Explore: ", MDF1);
//
//                    MainActivity.updateMsgHistory("MDF_Obstacle: "+ MDF2);
//                    Log.d("MDF_Obstacle: ", MDF2);
//
//                    maze.displayArrowBlockString();
//
//                    //UPDATE TIME
//                    /*
//                    _exploreTime = System.nanoTime() - _exploreTime;
//                    int seconds = Math.round(_exploreTime/1000000000);
//                    int minutes = seconds/60;
//                    seconds = seconds - minutes*60;
//                    TextView tv = getView().findViewById(R.id.explTime);
//                    tv.setText(minutes+"min "+ seconds +"s");
//                    */
//
//                    //UPDATE TEXT
//                    getView().findViewById(R.id.exploreBtn).setEnabled(false);
//                    maze.setState(Constants.idleMode);
//                    //BluetoothManager.getInstance().sendMessage("SET_STATUS", "Exploration Completed!");
//                    Toast.makeText(getActivity(), "Exploration completed!", Toast.LENGTH_SHORT).show();
//                }

//                else if(key.equals("FP_DONE"))
//                {
//                    BluetoothManager.getInstance().sendMessage("SET_STATUS", "Fastest Path Completed!");
//                    maze.setState(Constants.idleMode);
//                    getView().findViewById(R.id.fastestBtn).setEnabled(false);
//                    Toast.makeText(getActivity(), "Fastest Path completed!", Toast.LENGTH_SHORT).show();
//
//                    //UPDATE TIME DISPLAY
//                    /*
//                    _fastestTime = System.nanoTime() - _fastestTime;
//                    int seconds = Math.round(_fastestTime/1000000000);
//                    int minutes = seconds/60;
//                    seconds = seconds - minutes*60;
//                    TextView tv = getView().findViewById(R.id.fastestTime);
//                    tv.setText(minutes+"min "+ seconds +"s");
//                    */
//                }

                else if(key.equals("IMAGE"))
                {
                    String[] tmp = msg.split("-");
                    Log.d("Image ID", tmp[0]);
                    Log.d("X-Axis", tmp[1]);
                    Log.d("Y-Axis", tmp[2]);
                    Log.d("Orientation", tmp[3]);

                    int img_xpos = Integer.parseInt(tmp[1]);
                    int img_ypos = Integer.parseInt(tmp[2]);

                    img_coord = img_xpos + (15 * img_ypos);

                    int[] intArray;

                    intArray = Maze.getImageData();
                    intArray[img_coord] = 1;

//                    if (!firstImage)
//                    {
//                        for (int i = 0; i < 300; i++)
//                        {
//                            intArray[i] = 0;
//                        }
//
//                        firstImage = true;
//                    }
//
//                    maze._[img_coord] = 1;

                    StringBuilder stringBuilder = new StringBuilder();

                    for (int i = 0; i < intArray.length; i++)
                    {
                        stringBuilder.append(intArray[i]);
                    }

                    maze.handleImageBlock(stringBuilder.toString(), Integer.parseInt(tmp[0]));
                    Log.d("data", msg);
                }

                else if (key.equals("COORD")) ///robot position
                {
                    Log.d("Robot Position", msg);
                    maze.updateBotPosDir(msg);
                }

                else if(key.equals("MOVE"))
                {
                    maze.handleBotData(msg);
                }
                else if (key.equals("STATUS"))
                {
                    TextView tv = getView().findViewById(R.id.statusText);
                    tv.setText(msg);
                }
                else if (key.equals("AU"))
                {
                    Log.d("arrow",msg);
                    Log.d("arrowBotPos",maze._botCoord[0]+" "+maze._botCoord[1]);
                    maze.handleArrowBlock(Constants.NORTH, msg);
                }
                break;

            case Constants.ACCELERATE:

            //Received Messages
            if(maze.getState() != Constants.manualMode)
            {
                return;
            }

            int accelDir = Integer.parseInt(msg);

            if(accelDir == Constants.up)
            {
                maze.attemptMoveBot(Constants.NORTH, true);
            }
            else if(accelDir == Constants.down)
            {
                maze.attemptMoveBot(Constants.SOUTH, true);
            }
            else if(accelDir == Constants.right)
            {
                maze.attemptMoveBot(Constants.EAST, true);
            }
            else if(accelDir == Constants.left)
            {
                maze.attemptMoveBot(Constants.WEST, true);
            }
        }
    }

    public static Maze getMaze()
    {
        return maze;
    }

    public static int getImageCoord() { return img_coord;}
}