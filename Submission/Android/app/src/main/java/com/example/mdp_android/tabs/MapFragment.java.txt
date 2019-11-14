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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mdp_android.Constants;
import com.example.mdp_android.MainActivity;
import com.example.mdp_android.Maze;
import com.example.mdp_android.R;
import com.example.mdp_android.bluetooth.BluetoothManager;

public class MapFragment extends Fragment implements MainActivity.CallbackFragment
{
    public static Maze maze;
    private Boolean _autoRefresh = true;
    private long fastestTime = 0;
    private long fastestpathtime = 0;
    private long exploreTime = 0;
    private long exploredTime = 0;
    private final Handler refreshHandler = new Handler();

    Switch updateswitch;
    Button updatebutton;
    Boolean firstImage = false;

    private static String _storekey = "";
    private String _testmsg;

    private static int img_pos;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.activity_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setupMaze();
        initializeButtons();
        setupButtonListeners();
    }

    private void setupMaze()
    {
        RelativeLayout mazeLayout = getView().findViewById(R.id.mazeLayout);
        maze = new Maze(getActivity());
        mazeLayout.addView(maze);
    }

    private void initializeButtons()
    {
        getView().findViewById(R.id.coordBtn).setEnabled(true);
        getView().findViewById(R.id.waypoint_button).setEnabled(true);
        getView().findViewById(R.id.exploreBtn).setEnabled(true);
        getView().findViewById(R.id.fastestBtn).setEnabled(true);
        getView().findViewById(R.id.resetBtn).setEnabled(true);

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

        getView().findViewById(R.id.exploreBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (maze.getState() == Constants.idleMode)
                {
                    Toast.makeText(getActivity(), "Starting Exploration Now!", Toast.LENGTH_SHORT).show();
                    BluetoothManager.getInstance().sendMessage("al", "EXPLORE");
                    getView().findViewById(R.id.coordBtn).setEnabled(false);
                    maze.setState(Constants.exploreMode);

                    TextView sv = getView().findViewById(R.id.statusText);
                    sv.setText("Exploration");

                    exploreTime = System.nanoTime();
                }
            }
        });

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
            }
        });

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

        getView().findViewById(R.id.upBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                maze.attemptMoveBot(Constants.NORTH, true);

            }
        });

        getView().findViewById(R.id.downBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                maze.attemptMoveBot(Constants.SOUTH, true);
            }
        });

        getView().findViewById(R.id.rightBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                maze.attemptMoveBot(Constants.EAST, true);
            }
        });

        getView().findViewById(R.id.leftBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                maze.attemptMoveBot(Constants.WEST, true);
            }
        });

        getView().findViewById(R.id.updateBtn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(getActivity(), "Updating the Map!", Toast.LENGTH_SHORT).show();

                if(_storekey.equals("GRID"))
                {
                    maze.handleGridObstacle(_testmsg);
                }

                maze.renderMaze();
            }
        });

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

                    Maze.setAuto();

                    _autoRefresh = !_autoRefresh;
                    if (_autoRefresh)
                    {
                        refreshHandler.postDelayed(new Runnable()
                        {
                            public void run()
                            {
                                if (_autoRefresh)
                                {
                                    refreshHandler.postDelayed(this, 1000);
                                }
                            }
                        }, 1000);
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

    private String MDF1 = "";
    private String MDF2 = "";

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
                break;

            case Constants.MESSAGE_READ:

                if(key.equals("MDF1"))
                {
                    MDF1 = msg;
                    Log.d("MDF1", MDF1);

                    TextView mdf1view = getView().findViewById(R.id.MDF1String);
                    mdf1view.setText(MDF1);

                    maze.handleExplore(msg);
                }

                else if(key.equals("MDF2"))
                {
                    _storekey = key;
                    MDF2 = msg;

                    int count = 0;

                    for(int i = 0; i < MDF2.length(); i++)
                    {
                        if(MDF2.charAt(i) != ' ')
                            count++;
                    }

                    Log.d("Count", Integer.toString(count));

                    if (count%2 != 0)
                    {
                        MDF2 = MDF2 + "0";
                    }

                    Log.d("MDF2", MDF2);

                    TextView mdf2view = getView().findViewById(R.id.MDF2String);
                    mdf2view.setText(MDF2);

                    if (_autoRefresh)
                    {
                        maze.handleGridObstacle(msg);
                    }

                    Log.d("data",msg);
                }

                else if(key.equals("STOP"))
                {
                    if (maze.getState() == Constants.exploreMode)
                    {
                        maze.setExploreCompleted(true);

                        MainActivity.updateMsgHistory("MDF1: "+ MDF1);
                        Log.d("MDF1: ", MDF1);

                        MainActivity.updateMsgHistory("MDF2: "+ MDF2);
                        Log.d("MDF2: ", MDF2);

                        exploredTime = System.nanoTime() - exploreTime;
                        int seconds = Math.round(exploredTime/1000000000);
                        int minutes = seconds/60;
                        seconds = seconds - minutes * 60;

                        TextView expltime = getView().findViewById(R.id.explTime);
                        expltime.setText(minutes+ " mins "+ seconds + " s");

                        getView().findViewById(R.id.exploreBtn).setEnabled(false);
                        maze.setState(Constants.idleMode);

                        Toast.makeText(getActivity(), "Exploration completed!", Toast.LENGTH_SHORT).show();
                        TextView sv = getView().findViewById(R.id.statusText);
                        sv.setText("EXP Completed");
                    }

                    else if (maze.getState() == Constants.fastestPathMode)
                    {
                        getView().findViewById(R.id.fastestBtn).setEnabled(false);
                        maze.setState(Constants.idleMode);

                        fastestpathtime = System.nanoTime() - fastestTime;
                        int fseconds = Math.round(fastestpathtime/1000000000);
                        int fminutes = fseconds/60;
                        fseconds = fseconds - fminutes*60;

                        TextView fsptime = getView().findViewById(R.id.fastestTime);
                        fsptime.setText(fminutes + " mins " + fseconds + " s");

                        Toast.makeText(getActivity(), "Fastest Path completed!", Toast.LENGTH_SHORT).show();
                        TextView sv = getView().findViewById(R.id.statusText);
                        sv.setText("FSP Completed");
                    }
                }

                else if(key.equals("IMAGE"))
                {
                    String[] tmp = msg.split("-");
                    String[] imgStr = maze.convertImgCoord(tmp).clone();

                    int img_xcoord = Integer.parseInt(imgStr[2]);
                    int img_ycoord = Integer.parseInt(imgStr[1]); //added 19- just to changed
                    if (img_xcoord != -1 && img_ycoord != -1){
                        img_pos = img_xcoord + 15 * img_ycoord;

                        int[] imgCoordArr = Maze.getImageData();
                        imgCoordArr[img_pos] = 1;
                        StringBuilder imgCoordStr = new StringBuilder();
                        for (int i = 0; i < imgCoordArr.length; i++) {imgCoordStr.append(imgCoordArr[i]);}

                        maze.handleImageBlock(imgCoordStr.toString(), Integer.parseInt(imgStr[0]));
                    }
                }

                else if(key.equals("RESIMG"))
                {
                    Log.d("Resolved String", "Resolved String Received!");
                    String[] unresolvedImageDataArr = Maze.resolveMisplacedImages();
                    if (unresolvedImageDataArr.length > 4) {
                        String[][] imgStringArr = new String[(unresolvedImageDataArr.length - 1) / 4][4];
                        int j = 1;
                        for (int i = 0; i < (unresolvedImageDataArr.length - 1) / 4; i++) {
                            imgStringArr[i][0] = unresolvedImageDataArr[j];
                            imgStringArr[i][1] = unresolvedImageDataArr[j + 1];
                            imgStringArr[i][2] = unresolvedImageDataArr[j + 2];
                            imgStringArr[i][3] = unresolvedImageDataArr[j + 3];
                            String[] imgStr = maze.convertImgCoord(imgStringArr[i]).clone();

                            int img_xcoord = Integer.parseInt(imgStr[2]);
                            int img_ycoord = Integer.parseInt(imgStr[1]);
                            if (img_xcoord != -1 && img_ycoord != -1) {
                                img_pos = img_xcoord + 15 * img_ycoord;
                                int[] imgCoordArr = Maze.getImageData();
                                imgCoordArr[img_pos] = 1;
                                StringBuilder imgCoordStr = new StringBuilder();
                                for (int k = 0; k < imgCoordArr.length; k++) {
                                    imgCoordStr.append(imgCoordArr[k]);
                                }
                                maze.handleImageBlock(imgCoordStr.toString(), Integer.parseInt(imgStr[0]));
                            }
                            j += 4;
                        }
                    }
                }

                else if (key.equals("COORD"))
                {
                    Log.d("Robot Position", msg);
                    maze.updateBotPosDir(msg);
                }

                else if (key.equals("FASTEST"))
                {
                    Log.d("Fastest Path Route", msg);
                    maze.handleFastestPath(msg);
                }

                else if (key.equals("STATUS"))
                {
                    TextView tv = getView().findViewById(R.id.statusText);
                    tv.setText(msg);
                }
                break;

            case Constants.ACCELERATE:

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

    public static int getImagePos() { return img_pos;}
}