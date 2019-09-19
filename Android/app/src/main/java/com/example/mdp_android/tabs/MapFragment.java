package com.example.mdp_android.tabs;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mdp_android.Constants;
import com.example.mdp_android.MainActivity;
import com.example.mdp_android.Maze;
import com.example.mdp_android.R;
import com.example.mdp_android.bluetooth.BluetoothManager;

public class MapFragment extends Fragment implements MainActivity.CallbackFragment
{
    private Maze maze;
    private Boolean _autoRefresh = false;
    private long fastestTime = 0;
    private long exploreTime = 0;
    private final Handler refreshHandler = new Handler();

    //DISPLAY ACTIVITY_MAP.XML
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState)
    {
       return inflater.inflate(R.layout.activity_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        //SET UP THE MAZE
        setupMaze();

        //SET UP THE CONTROLS (BUTTONS)
        initializeButtons();
        setupButtonListeners();
    }

    /**
     * CREATES 'MAZE' CONTAINER VIEWGROUP AND INITIALIZES IT
     * */
    private void setupMaze()
    {
        //GET MAZE VIEW VIA ID AND ADD
        RelativeLayout mazeLayout = getView().findViewById(R.id.mazeLayout);
        maze = new Maze(getActivity());
        mazeLayout.addView(maze);
    }

    /**
     * RESETS BUTTON STATES AND BOT STATUS TEXT
     * */
    private void initializeButtons()
    {
        getView().findViewById(R.id.coordBtn).setEnabled(true);
        getView().findViewById(R.id.waypoint_button).setEnabled(true);
        getView().findViewById(R.id.exploreBtn).setEnabled(true);
        getView().findViewById(R.id.manualBtn).setEnabled(true);
        getView().findViewById(R.id.fastestBtn).setEnabled(true);
        getView().findViewById(R.id.resetBtn).setEnabled(true);
        BluetoothManager.getInstance().sendMessage("SET_STATUS", "Ready!");
    }

    private void setupButtonListeners()
    {
        //SET START & END COORDINATES BUTTON
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

        //EXPLORE BUTTON FOR EXPLORATION OF MAZE
        getView().findViewById(R.id.exploreBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (maze.getState() == Constants.idleMode && maze.coordinatesSet())
                {
                    Toast.makeText(getActivity(), "Starting Exploration Now!", Toast.LENGTH_SHORT).show();
                    BluetoothManager.getInstance().sendMessage("SET_STATUS", "Exploring the maze...");
                    getView().findViewById(R.id.coordBtn).setEnabled(false);
                    BluetoothManager.getInstance().sendMessage("EX_START", "");
                    maze.setState(Constants.exploreMode);
                    // _exploreTime = System.nanoTime();
                }
            }
        });

        //SET WAY POINTS BUTTON
        getView().findViewById(R.id.waypoint_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (maze.getState() == Constants.idleMode /*&& maze.isExploreCompleted()*/)
                {
                    maze.resetWp();
                    maze.setState(Constants.waypointMode);
                    Toast.makeText(getActivity(), "Tap on Maze Tiles to set any Waypoints", Toast.LENGTH_SHORT).show();
                }
                else if (maze.getState() == Constants.waypointMode)
                {
                    maze.setState(Constants.idleMode);
                    Toast.makeText(getActivity(), "Cancelling Waypoints..", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //MANUAL CONTROL BUTTON
        getView().findViewById(R.id.manualBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //FOR TESTING OF FASTEST PATH (FP)
                // BluetoothManager.getInstance().sendMessage("MOVE", "fffffffcrffffffffffflffffffffffcrf");
                if (maze.coordinatesSet() && maze.getState() == Constants.idleMode)
                {
                    Toast.makeText(getActivity(), "Entering Manual Mode, tap again to exit.", Toast.LENGTH_SHORT).show();
                    maze.setState(Constants.manualMode);
                }
                else if (maze.getState() == Constants.manualMode)
                {
                    Toast.makeText(getActivity(), "Exiting Manual Mode!", Toast.LENGTH_SHORT).show();
                    maze.setState(Constants.idleMode);
                }
            }
        });

        //FASTEST PATH BUTTON
        getView().findViewById(R.id.fastestBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                    Toast.makeText(getActivity(), "Executing Fastest Path!", Toast.LENGTH_SHORT).show();
                    BluetoothManager.getInstance().sendMessage("SET_STATUS", "Travelling Fastest Path...");
                    getView().findViewById(R.id.waypoint_button).setEnabled(false);
                    BluetoothManager.getInstance().sendMessage("FP_START", "");
                    maze.setState(Constants.fastestPathMode);
                    // _fastestTime = System.nanoTime();
            }
        });

        //RESET BUTTON FOR MAZE
        getView().findViewById(R.id.resetBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                    Toast.makeText(getActivity(), "Maze Reset!", Toast.LENGTH_SHORT).show();
                    BluetoothManager.getInstance().sendMessage("RESET", "");
                    maze.reset();
                    initializeButtons();
            }
        });

        //DIRECTIONAL BUTTONS (UP, DOWN, LEFT, RIGHT)
        //LEFT BUTTON
        getView().findViewById(R.id.leftBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (maze.getState() == Constants.manualMode)
                {
                    maze.attemptMoveBot(Constants.WEST, true);
                }
            }
        });

        //RIGHT BUTTON
        getView().findViewById(R.id.rightBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (maze.getState() == Constants.manualMode)
                {
                    maze.attemptMoveBot(Constants.EAST, true);
                }
            }
        });

        //UP BUTTON
        getView().findViewById(R.id.upBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (maze.getState() == Constants.manualMode)
                {
                    maze.attemptMoveBot(Constants.NORTH, true);
                }
            }
        });

        //DOWN BUTTON
        getView().findViewById(R.id.downBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if (maze.getState() == Constants.manualMode) {
                    maze.attemptMoveBot(Constants.SOUTH, true);
                }
            }
        });

        //STATUS BUTTON TO GET ROBOT STATUS
        getView().findViewById(R.id.statusBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                BluetoothManager.getInstance().sendMessage("GET_STATUS", "");
            }
        });

        //MANUAL REFRESH BUTTON
        getView().findViewById(R.id.updateBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                BluetoothManager.getInstance().sendMessage("GET_DATA", "");
                BluetoothManager.getInstance().sendMessage("GET_STATUS", "");
            }
        });

        //AUTO UPDATE BUTTON
        getView().findViewById(R.id.autoBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                // on/off thread to auto call for maze updates
                _autoRefresh = !_autoRefresh;
                if(_autoRefresh)
                {
                    Toast.makeText(getActivity(), "Auto Update on!", Toast.LENGTH_SHORT).show();

                    refreshHandler.postDelayed(new Runnable()
                    {
                        public void run()
                        {
                            BluetoothManager.getInstance().sendMessage("GET_DATA","");
                            if(_autoRefresh) refreshHandler.postDelayed(this, 2000);
                        }
                    }, 2000);
                }
                else
                {
                    Toast.makeText(getActivity(), "Auto Refresh off!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //CALIBRATE BUTTON
        getView().findViewById(R.id.calibrateBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if(maze.getState() == Constants.idleMode){
                    BluetoothManager.getInstance().sendMessage("MOVE", "rrcrcr");
                }
            }
        });
    }

    //ALGORITHM - DISPLAY MDF STRINGS (HEX STRINGS)
    private String _mdfExplore = "";
    private String _mdfObstacle = "";

    /* Handle Bluetooth messages received */
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
                //RECEIVE MESSAGE
                if(key.equals("EXPLORED_DATA"))
                {
                    _mdfExplore = msg;
                    maze.handleExplore(msg);
                }
                else if(key.equals("EX_DONE"))
                {
                    maze.setExploreCompleted(true);

                    //DISPLAY MOST RECENT MDP STRINGS
                    MainActivity.updateMsgHistory("MDF_Explore: "+_mdfExplore);
                    MainActivity.updateMsgHistory("MDF_Obstacle: "+_mdfObstacle);
                    Log.d("MDF_Explore: ",_mdfExplore);
                    Log.d("MDF_Obstacle: ",_mdfObstacle);
                    maze.displayArrowBlockString();

                    //UPDATE TIME
                    /*
                    _exploreTime = System.nanoTime() - _exploreTime;
                    int seconds = Math.round(_exploreTime/1000000000);
                    int minutes = seconds/60;
                    seconds = seconds - minutes*60;
                    TextView tv = getView().findViewById(R.id.explTime);
                    tv.setText(minutes+"min "+ seconds +"s");
                    */

                    //UPDATE TEXT
                    getView().findViewById(R.id.exploreBtn).setEnabled(false);
                    maze.setState(Constants.idleMode);
                    BluetoothManager.getInstance().sendMessage("SET_STATUS", "Exploration Completed!");
                    Toast.makeText(getActivity(), "Exploration completed!", Toast.LENGTH_SHORT).show();
                }
                else if(key.equals("OBSTACLE_DATA"))
                {
                    _mdfObstacle = msg;
                    maze.handleObstacle(msg);
                }
                else if(key.equals("GRID"))
                {
                    //FOR AMD TOOL ONLY
                    maze.handleAMDGrid(msg);
                }
                else if(key.equals("FP_DONE"))
                {
                    //UPDATE TIME DISPLAY
                    /*
                    _fastestTime = System.nanoTime() - _fastestTime;
                    int seconds = Math.round(_fastestTime/1000000000);
                    int minutes = seconds/60;
                    seconds = seconds - minutes*60;
                    TextView tv = getView().findViewById(R.id.fastestTime);
                    tv.setText(minutes+"min "+ seconds +"s");
                    */

                    BluetoothManager.getInstance().sendMessage("SET_STATUS", "Fastest Path Completed!");
                    maze.setState(Constants.idleMode);
                    getView().findViewById(R.id.fastestBtn).setEnabled(false);
                    // Toast.makeText(getActivity(), "Fastest Path completed!", Toast.LENGTH_SHORT).show();
                }
                else if (key.equals("BOT"))
                {
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
                //RECEIVED MESSAGE
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
}