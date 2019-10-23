package com.example.mdp_android;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.example.mdp_android.bluetooth.BluetoothManager;
import com.example.mdp_android.tabs.MapFragment;
import com.example.mdp_android.tabs.ImageCheckFragment;
import com.example.mdp_android.MainActivity;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;


public class Maze extends ViewGroup
{
    private final Handler refreshHandler = new Handler();

    //Maze Constants
    private static final int MAZE_WIDTH = 15;
    private static final int MAZE_HEIGHT = 20;
    public static int TILESIZE = 0;
    private final int[] _emptyArray = new int[MAZE_HEIGHT * MAZE_WIDTH];
    private final int[] _emptyimageData = new int[MAZE_HEIGHT * MAZE_WIDTH];
    private static final String AR = "ar:";
    public boolean isFastest = true;

    //Maze Entities
    private ArrayList<MazeTile> _tileList;

    //Maze Data
    public int[] _botCoord = {0, 0};
    private int[] _headCoord = {0, 0};
    private int _direction = Constants.NORTH;
    private int[] _startCoord = {0, 0};
    private int[] _endCoord = {0, 0};
    private int[] _wpCoord = {0, 0};
    private ArrayList<Integer[]> _arrowBlockList = new ArrayList<Integer[]>();
    private ArrayList<Integer[]> _trueArrowBlockList = new ArrayList<Integer[]>();
    private int[] _obstacleData = new int[MAZE_HEIGHT * MAZE_WIDTH];
    private int[] _exploreData = new int[MAZE_HEIGHT * MAZE_WIDTH];
    private static int[] _imageData = new int[MAZE_HEIGHT * MAZE_WIDTH];
    private int _imageID;

    private String[] _unresolvedImageDataArr = new String[]{"0"};
    private int[] _receivedImagePosLog = new int[]{0};
    private ArrayList<Integer[]> _arrowBlockBanList = new ArrayList<Integer[]>();

    //Managing Input State
    private int _coordCount = -1;
    private boolean _wpSet = false;
    private int _inputState = Constants.idleMode;
    private Boolean _exploreCompleted = false;

    //Added for auto refresh
    private static Boolean _isAuto = true;

    private final String TAG = "ConnectedThread";
    /**
     * CONSTRUCTOR FOR MAZE (15 X 20 TILES AND STORES IN ARRAYLIST '_TILELIST')
     *
     * @param context
     */
    public Maze(Context context)
    {
        super(context);
        _tileList = new ArrayList<MazeTile>(MAZE_WIDTH * MAZE_HEIGHT);

        //Generate Mazetiles and save to arraylist
        int i, j;
        for (i = 0; i < MAZE_HEIGHT; i++)
        {
            for (j = 0; j < MAZE_WIDTH; j++)
            {
                MazeTile mazeTile = new MazeTile(context, j, i);
                this.addView(mazeTile);
                _tileList.add(mazeTile);
                mazeTile.setOnClickListener(_tileListener);
            }
        }

        for (int k = 0; k < MAZE_WIDTH * MAZE_HEIGHT; k++)
        {
            _emptyArray[k] = Constants.UNEXPLORED;
        }

        reset();

        // handleCoordinatesInput(getTargetTiles(1,1,3).get(0));
        // handleCoordinatesInput(getTargetTiles(13,18,3).get(0));
    }

    public static int[] getImageData()
    {
        return _imageData;
    }

    public int getState()
    {
        return _inputState;
    }

    public void setState(int newState)
    {
        _inputState = newState;
    }

    public boolean coordinatesSet()
    {
        return _coordCount == 1;
    }

    public boolean isExploreCompleted()
    {
        return _exploreCompleted;
    }

    public void setExploreCompleted(Boolean value)
    {
        _exploreCompleted = value;
    }

    public void handleExplore(String binaryData)
    {
        // binaryData is Maze Size 300/4 = 75 hex characters
        // for explored data, algo requirement is to pad first and last 2 bits with 0s
        // so we will drop those
        //String tmp2 = tmp.substring(2, tmp.length() - 2);

        String tmp = parseHexCharToBinary(binaryData);
        Log.d("TEMP1", tmp);
        String tmp2 = tmp.substring(2, tmp.length() - 2);
        Log.d("TEMP2", tmp2);
        _exploreData = convertStrToIntArray(tmp2);
        renderMaze();
    }

    private void addToTrueArrowBlockList(Integer [] val)
    {
        for (Integer[] a : _trueArrowBlockList)
        {
            if (a[0] == val[0] && a[1] == val[1] && a[2] == val[2])
            {
                return;
            }
        }

        _trueArrowBlockList.add(val);
    }

    public void handleObstacle(String binaryData)
    {
        //Obstacle Data is mapped to explored tiles in _Exploreddata
        String tmp = parseHexCharToBinary(binaryData);

        int count = 0;
        int mazeSize = _exploreData.length;
        int[] result = _emptyArray.clone();

        for (int j = 0; j < tmp.length(); j++)
        {
            int myChar = Character.getNumericValue(tmp.charAt(j));

            while (count < mazeSize && _exploreData[count] == Constants.UNEXPLORED) count++;
            if (count >= mazeSize) break;
            result[count] = myChar;
            count++;
        }

        _obstacleData = result;

        //Clear true arrowblock if phantom used to be here
        clearIfPhantom();

            //Iterate thru arrowblocklist, get true blocks and delete from arrowblocklist
            for (int k= _arrowBlockList.size()-1; k >= 0; k--)
            {
                Integer [] a = _arrowBlockList.get(k);
                int index = a[0]+a[1]*MAZE_WIDTH;

                //Bring forward
                Integer [] before = a.clone();

            switch(_direction)
            {
                case Constants.NORTH:
                    before[1] -= 1;
                    break;

                case Constants.SOUTH:
                    before[1] += 1;
                    break;

                case Constants.EAST:
                    before[0] -= 1;
                    break;

                case Constants.WEST:
                    before[0] += 1;
                    break;
            }

            //Push behind
            Integer [] after = a.clone();

            switch(_direction)
            {
                case Constants.NORTH:
                    after[1] += 1;
                    break;

                case Constants.SOUTH:
                    after[1] -= 1;
                    break;

                case Constants.EAST:
                    after[0] += 1;
                    break;

                case Constants.WEST:
                    after[0] -= 1;
                    break;
            }

            int index2 = before[0]+before[1]*MAZE_WIDTH;

            if(index2 > -1 && index2 < _obstacleData.length && _obstacleData[index2] == 1)
            {
                addToTrueArrowBlockList(before);
                _arrowBlockList.remove(k);
                _arrowBlockBanList.add(before);
                _arrowBlockBanList.add(a);
                _arrowBlockBanList.add(after);
                continue;
            }

            //Correct position
            if(index > -1 && index < _obstacleData.length && _obstacleData[index] == 1)
            {
                addToTrueArrowBlockList(a);
                _arrowBlockList.remove(k);
                _arrowBlockBanList.add(before);
                _arrowBlockBanList.add(a);
                _arrowBlockBanList.add(after);
                continue;
            }

            int index3 = after[0]+after[1]*MAZE_WIDTH;
            if(index3 > -1 && index3 < _obstacleData.length && _obstacleData[index3] == 1)
            {
                addToTrueArrowBlockList(after);
                _arrowBlockList.remove(k);
                _arrowBlockBanList.add(before);
                _arrowBlockBanList.add(a);
                _arrowBlockBanList.add(after);
                continue;
            }
        }
    }

    //notamd
    // for returning during exploration, and fastest path
    // update handle movement ourselves based on movement string received
    // instead of receiving position and direction directly from algo
    private Handler _botPosHandler = new Handler();
    private int _botPosDelay = 500;
    private int _botStrCount = 0;
    private boolean _botHandlerRunning = false;
    private ArrayList<String> _botMoveStrArray = new ArrayList<String>();

    public void handleBotData(String moveData)
    {
        if (moveData != null && moveData != "" && moveData.contains("f"))
        {
            String [] tmp = moveData.replace("c", "").replace("f", "f-").split("-");

            for (String a:tmp)
            {
                _botMoveStrArray.add(a);
            }

            if(!_botHandlerRunning)
            {
                _botHandlerRunning = true;
                _botPosHandler.postDelayed(new Runnable()
                {
                    public void run()
                    {
                        int newDir = _direction;
                        String nextStep = _botMoveStrArray.get(_botStrCount);
                        for (char ch : nextStep.toCharArray())
                        {
                            if (ch == 'r' || ch == 'l') newDir = calcNewDir(newDir, ch);
                            else if (ch == 'f')
                            {
                                attemptMoveBot(newDir, false);
                            }
                        }
                        _botStrCount++;
                        if (_botStrCount < _botMoveStrArray.size())
                        {
                            _botPosHandler.postDelayed(this, _botPosDelay);
                        }
                        else
                        {
                            _botHandlerRunning = false;
                            _botStrCount = 0;
                            _botMoveStrArray = new ArrayList<String>();
                        }
                    }
                }, _botPosDelay);
            }
        }
    }

    boolean tryParseInt(String value)
    {
        try
        {
            Integer.parseInt(value);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    // fastest path
    public void handleFastestPath(String fasteststr)
    {
        Log.d("I'm here!", fasteststr);
        Integer totaldelay = 0;
        Integer currentdir = Constants.NORTH;
        String fastestarr[] = fasteststr.split("\\|");
        Log.d("Slot 1", fastestarr[0]);

        for (int i=0; i < fastestarr.length; i++)
        {
            final String movement = fastestarr[i];
            Log.d("Fastest Move", fastestarr[i]);

            if (tryParseInt(movement))
            {
                int j;

                for (j=0; j < Integer.parseInt(movement); j++)
                {
                    refreshHandler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (_direction == Constants.NORTH)
                            {
                                attemptMoveBot(Constants.NORTH, true);
                            }
                            else if(_direction == Constants.SOUTH)
                            {
                                attemptMoveBot(Constants.SOUTH, true);
                            }
                            else if (_direction == Constants.EAST)
                            {
                                attemptMoveBot(Constants.EAST, true);
                            }
                            else if (_direction == Constants.WEST)
                            {
                                attemptMoveBot(Constants.WEST, true);
                            }
                        }
                    }, totaldelay + (500 * j));
                }

                totaldelay = totaldelay + (500 * j);
            }

            else {
                switch (movement)
                {
                    case "W":
                        refreshHandler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (_direction == Constants.NORTH)
                                {
                                    attemptMoveBot(Constants.NORTH, true);
                                }
                                else if (_direction == Constants.SOUTH)
                                {
                                    attemptMoveBot(Constants.SOUTH, true);
                                }
                                else if (_direction == Constants.EAST)
                                {
                                    attemptMoveBot(Constants.EAST, true);
                                }
                                else if (_direction == Constants.WEST)
                                {
                                    attemptMoveBot(Constants.WEST, true);
                                }
                            }
                        }, totaldelay + 500);
                        totaldelay = totaldelay + 500;
                        break;

                    case "D":
                        refreshHandler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (_direction == Constants.NORTH)
                                {
                                    attemptMoveBot(Constants.EAST, true);
                                }
                                else if (_direction == Constants.EAST)
                                {
                                    attemptMoveBot(Constants.SOUTH, true);
                                }
                                else if (_direction == Constants.SOUTH)
                                {
                                    attemptMoveBot(Constants.WEST, true);
                                }
                                else if (_direction == Constants.WEST)
                                {
                                    attemptMoveBot(Constants.NORTH, true);
                                }
                            }
                        }, totaldelay + 500);
                        totaldelay = totaldelay + 500;
                        break;

                    case "A":
                        refreshHandler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (_direction == Constants.NORTH)
                                {
                                    attemptMoveBot(Constants.WEST, true);
                                }
                                else if (_direction == Constants.WEST)
                                {
                                    attemptMoveBot(Constants.SOUTH, true);
                                }
                                else if (_direction == Constants.SOUTH)
                                {
                                    attemptMoveBot(Constants.EAST, true);
                                }
                                else if (_direction == Constants.EAST)
                                {
                                    attemptMoveBot(Constants.NORTH, true);
                                }
                            }
                        }, totaldelay + 500);
                        totaldelay = totaldelay + 500;
                        break;
                }
            }
        }
    }

    public void handleGridObstacle(String binaryData)
    {
        //Obstacle Data is mapped to explored tiles in _Exploreddata
        int[] exploredObstacleData = convertStrToIntArray(parseHexCharToBinary(binaryData));
        int [] mappedObstacleData = _exploreData.clone();
        int j = 0;
        for (int i = 0; i < mappedObstacleData.length; i++)
        {
            if (mappedObstacleData[i] == 1)
            {
                try
                {
                    mappedObstacleData[i] = exploredObstacleData[j];
                    j++;
                }
                catch (Exception e)
                {
                    mappedObstacleData[i] = 0;
                }
            };
        }
        _obstacleData = mappedObstacleData;
        Log.d("Obstacle Data", Arrays.toString(_obstacleData));
        renderMaze();
//        Log.d("Explored Data", Arrays.toString(_exploreData));
//        //_exploreData = convertStrToIntArray(parseHexCharToBinary("fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));
//        _obstacleData = convertStrToIntArray(parseHexCharToBinary(binaryData));
//        renderMaze();
    }

    //amd tool only (Show arrow block Position)
    public void handleImageBlock(String binaryData, int imgID)
    {
       //_exploreData = convertStrToIntArray(parseHexCharToBinary("fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));
        _imageData = convertStrToIntArray(binaryData);
        _imageID = imgID;
        Log.d("Image ID", Integer.toString(imgID));
        renderMaze();
    }

    public void updateBotPosDir(String data)
    {
        try
        {
            String tmp[] = data.split(" ");
            Log.d("Y-coord", tmp[0]);
            Log.d("X-coord", tmp[1]);
            Log.d("Direction", tmp[2]);

            if (tmp.length == 3)
            {
//                int xPos = Integer.parseInt(tmp[0]);
//                int yPos = Integer.parseInt(tmp[1]);

                int xPos = Integer.parseInt(tmp[0]);
                int yPos = 19-Integer.parseInt(tmp[1]);
                int direction = Integer.parseInt(tmp[2]);
                String dir = tmp[2];

                _botCoord[0] = xPos;
                _botCoord[1] = yPos;

                if (tmp[2].equals("1"))
                {
                    direction = Constants.NORTH;
                }
                else if (tmp[2].equals("2"))
                {
                    direction = Constants.EAST;
                }
                else if (tmp[2].equals("3"))
                {
                    direction = Constants.SOUTH;
                }
                else  if (tmp[2].equals("4"))
                {
                    direction = Constants.WEST;
                }

//                _botCoord[0] = xPos + 1;
//                _botCoord[1] = yPos - 1;

               _direction = direction;
               // _direction = convertDirStrToNum(dir);

                Log.d("Bot Coord X", Integer.toString(-_botCoord[0]));
                Log.d("Bot Coord Y", Integer.toString(-_botCoord[1]));
                Log.d("Bot Direction", Integer.toString(_direction));

//                int xPos = Integer.parseInt(tmp[0]);
//                int yPos = Integer.parseInt(tmp[1]);
//                String dir = tmp[2];
//                _botCoord[0] = xPos + 1;
//                _botCoord[1] = yPos - 1;
//                _direction = convertDirStrToNum(dir);

                renderMaze();
            }
        }
        catch (NumberFormatException e)
        {
            Log.e("robotPos", e.getMessage());
        }
     }

    //Reset the waypoint
    public void resetWp()
    {
        _wpCoord[0] = -1;
        _wpCoord[1] = -1;
        _wpSet = false;
        renderMaze();
    }

    //Reset start and end coordinates
    public void resetStartEnd()
    {
        _coordCount = -1;
        renderMaze();
    }

    //Reset everything
    public void reset()
    {
        _obstacleData = _emptyArray.clone();
        _exploreData = _emptyArray.clone();
        _arrowBlockList = new ArrayList<Integer[]>();
        _trueArrowBlockList = new ArrayList<Integer[]>();
        _arrowBlockBanList = new ArrayList<Integer[]>();
        resetWp();
        _inputState = Constants.idleMode;
        resetStartEnd();
        _exploreCompleted = false;
        _direction = Constants.NORTH;

        for (MazeTile i : _tileList)
        {
            i.reset();
        }

        renderMaze();
        MainActivity.resetMsgHistory();
        _botMoveStrArray = new ArrayList<String>();
    }

    /**
     * Data from algo is 4 characters of 0/1 is converted to 1 hex char
     */
    private String parseHexCharToBinary(String hexStr)
    {
        String fullString = "";
        for (int i = 0; i < hexStr.length(); i++)
        {
            String hexChar = Character.toString(hexStr.charAt(i));
            int hexValue = 0;
            try
            {
                hexValue = Integer.parseInt(hexChar, 16);
            }
            catch (NumberFormatException e)
            {
                Log.e("ParseHexChar", e.getMessage());
            }
            String binary = String.format("%4s", Integer.toString(hexValue, 2)).replace(' ', '0');
            fullString += binary;
        }
        return fullString;
    }

    private int[] convertStrToIntArray(String data)
    {
        int[] result = new int[data.length()];
        for (int j = 0; j < data.length(); j++)
        {
            result[j] = Character.getNumericValue(data.charAt(j));
        }
        return result;
    }

    private void printIntArrayAsString(int[] data)
    {
        String tmp = "";
        for (int i = 0; i < data.length; i++)
        {
            tmp += data[i];
        }
    }

    private void clearIfPhantom()
    {
        // if arrow block on phantom block that was cleared, remove it
        for (int k=_trueArrowBlockList.size()-1; k >= 0; k--)
        {
            Integer [] a = _trueArrowBlockList.get(k);
            int index = a[0]+a[1]*MAZE_WIDTH;
            if(index >= 0 && index < _obstacleData.length && _obstacleData[index] != 1)
            {
                _trueArrowBlockList.remove(k);
            }
        }
    }

    public void handleArrowBlock(int type, String data)
    {
        clearIfPhantom();

        if (coordinatesSet() /*&& _inputState == Constants.exploreMode*/)
        {

            if (!data.contains(","))
            {
                return;
            }

            String[] tmp = data.split(","); //original - split by ","

            if (tmp.length != 3)
            {
                return;
            }

            int blockDistL = 0;
            int blockDistV = 0;

            try
            {
                blockDistL = Integer.parseInt(tmp[0]);
                blockDistV = Integer.parseInt(tmp[1]);
            }
            catch (Exception e)
            {
                return;
            }

            Integer[] blockCoord = new Integer[3];
            blockCoord[0] = _botCoord[0]; //x
            blockCoord[1] = _botCoord[1]; //y
            blockCoord[2] = _direction; // inverted later

            Integer[] banCoord = new Integer[3];
            banCoord[2] = blockCoord[2]; //direction

            Log.d("BLOCKCOORD 1", Integer.toString(blockCoord[0]));
            Log.d("BLOCKCOORD 2", Integer.toString(blockCoord[1]));
            Log.d("DIRECTION", Integer.toString(blockCoord[2]));

            if (_direction == Constants.NORTH)
            {
                blockCoord[0] += blockDistL - 2;
                blockCoord[1] += blockDistV ; // originally + 2
                banCoord[0] = blockCoord[0];
                banCoord[1] = blockCoord[1]+1;
            }
            else if (_direction == Constants.SOUTH)
            {
                blockCoord[0] -= blockDistL - 2;
                blockCoord[1] -= blockDistV; // originally + 2
                banCoord[0] = blockCoord[0];
                banCoord[1] = blockCoord[1]-1;
            }
            else if (_direction == Constants.EAST)
            {
                blockCoord[0] += blockDistV; // originally + 2
                blockCoord[1] -= blockDistL - 2;
                banCoord[0] = blockCoord[0]+1;
                banCoord[1] = blockCoord[1];
            }
            else if (_direction == Constants.WEST)
            {
                blockCoord[0] -= blockDistV; // originally + 2
                blockCoord[1] += blockDistL - 2;
                banCoord[0] = blockCoord[0]-1;
                banCoord[1] = blockCoord[1];
            }

            // outside maze: by right, we dont need this, but just in case
//            if (blockCoord[0] < 0 || blockCoord[0] >= MAZE_WIDTH || blockCoord[1] < 0 || blockCoord[1] >= MAZE_HEIGHT)
//            {
//                return;
//            }

            // check unique
            for (Integer[] a : _arrowBlockList)
            {
                if (a[0] == blockCoord[0] && a[1] == blockCoord[1] && a[2] == blockCoord[2])
                {
                    return;
                }
            }

            // if not in ban list, add
            for (Integer[] a : _arrowBlockBanList)
            {
                if (a[0] == blockCoord[0] && a[1] == blockCoord[1] && a[2] == blockCoord[2])
                {
                    return;
                }
            }

            _arrowBlockList.add(blockCoord);
            _arrowBlockBanList.add(blockCoord);
            renderMaze();
    }
    }

    public void displayArrowBlockString()
    {
        // invert bot's dir to get direction arrow block is facing relative to start point
        for (Integer[] a : _trueArrowBlockList)
        {
            int x = a[0];
            int y = a[1];
            int dir = a[2];
            String blockDir = "";
            if (dir == Constants.NORTH) blockDir = "D";
            else if (dir == Constants.SOUTH) blockDir = "U";
            else if (dir == Constants.EAST) blockDir = "L";
            else if (dir == Constants.WEST) blockDir = "R";
            MainActivity.updateMsgHistory("Arrow Block detected at: x:" + x + " y:" + y + ", facing: " + blockDir);
        }
    }

    private View.OnClickListener _tileListener = new View.OnClickListener()
    {
        public void onClick(View v)
        {
            if (v instanceof MazeTile)
            {
                MazeTile mazeTile = (MazeTile) v;
                if (_inputState == Constants.coordinateMode)
                {
                    handleCoordinatesInput(mazeTile);
                }
                else if (_inputState == Constants.waypointMode)
                {
                    handleWaypointInput(mazeTile);
                }
                else if (_inputState == Constants.manualMode)
                {
                    handleManualInput(mazeTile);
                }
            }
        }
    };

    //For start and end coordinates
    private void handleCoordinatesInput(MazeTile mazeTile)
    {
        //Set start point
        if (_coordCount == -1 || _coordCount == 1)
        {
            _startCoord = correctSelectedTile(mazeTile.get_xPos(), mazeTile.get_yPos(), 0);
            _botCoord = _startCoord.clone();
            _coordCount = 0;
            _direction = Constants.NORTH;
            BluetoothManager.getInstance().sendMessage("START_POS", _startCoord[0] + "," + _startCoord[1]);
        }

        //Set end point
        else if (_coordCount == 0)
        {
            _endCoord = correctSelectedTile(mazeTile.get_xPos(), mazeTile.get_yPos(), 0);
            _coordCount = 1;
            BluetoothManager.getInstance().sendMessage("END_POS", _endCoord[0] + "," + _endCoord[1]);

            //CLEAN UP
            setState(Constants.idleMode);
        }
        renderMaze();
    }

    //For waypoint coordinate
    private void handleWaypointInput(MazeTile mazeTile)
    {
        int[] waypointCoord = correctSelectedTile(mazeTile.get_xPos(), mazeTile.get_yPos(), 3);
        ArrayList<MazeTile> targetMazeTiles = getTargetTiles(waypointCoord[0], waypointCoord[1], 3);

        // check that target tiles are not occupied by obstacle or arrow block
        for (MazeTile a : targetMazeTiles)
        {
            if (isObstacle(a)) return;
        }
        _wpCoord = waypointCoord;
        _wpSet = true;
        renderMaze();
        BluetoothManager.getInstance().sendMessage("al", "WAYPOINT:" + _wpCoord[1] + ":" + _wpCoord[0]);

        //CLEAN UP
        setState(Constants.idleMode);
    }

    private void handleManualInput(MazeTile mazeTile)
    {
        if (mazeTile.get_xPos() == _botCoord[0])
        {
            if (mazeTile.get_yPos() == _botCoord[1] + 2)
            {
                attemptMoveBot(Constants.NORTH, mazeTile, true);
            }
            else if (mazeTile.get_yPos() == _botCoord[1] - 2)
            {
                attemptMoveBot(Constants.SOUTH, mazeTile, true);
            }
        }
        else if (mazeTile.get_yPos() == _botCoord[1])
        {
            if (mazeTile.get_xPos() == _botCoord[0] + 2)
            {
                attemptMoveBot(Constants.EAST, mazeTile, true);
            }
            else if (mazeTile.get_xPos() == _botCoord[0] - 2)
            {
                attemptMoveBot(Constants.WEST, mazeTile, true);
            }
        }
    }

    // doMove is true only for manual mode. for updates from algo, doMove = false
    public void attemptMoveBot(int dir, boolean doMove)
    {
        attemptMoveBot(dir, null, doMove);
    }

    public void attemptMoveBot(int dir, MazeTile mazeTile, boolean doMove)
    {
        if (canMove(dir, mazeTile))
        {
            if (doMove) {
                if (dir != _direction)
                {
                    if (dir == Constants.WEST && _direction == Constants.NORTH)
                    {
                        if(!isFastest)
                        {
                            BluetoothManager.getInstance().sendMessage("ar", "L");
                        }
                    }
                    else if (dir == Constants.SOUTH && _direction == Constants.WEST)
                    {
                        if(!isFastest)
                        {
                            BluetoothManager.getInstance().sendMessage("ar", "L");
                        }
                    }
                    else if (dir == Constants.EAST && _direction == Constants.SOUTH)
                    {
                        if(!isFastest)
                        {
                            BluetoothManager.getInstance().sendMessage("ar", "L");
                        }
                    }
                    else if (dir == Constants.NORTH && _direction == Constants.EAST)
                    {
                        if(!isFastest)
                        {
                            BluetoothManager.getInstance().sendMessage("ar", "L");
                        }
                    }
                    else if (dir == Constants.EAST && _direction == Constants.NORTH)
                    {
                        if(!isFastest)
                        {
                            BluetoothManager.getInstance().sendMessage("ar", "R");
                        }
                    }
                    else if (dir == Constants.SOUTH && _direction == Constants.EAST)
                    {
                        if(!isFastest)
                        {
                            BluetoothManager.getInstance().sendMessage("ar", "R");
                        }
                    }
                    else if (dir == Constants.WEST && _direction == Constants.SOUTH)
                    {
                        if(!isFastest)
                        {
                            BluetoothManager.getInstance().sendMessage("ar", "R");
                        }
                    }
                    else if (dir == Constants.NORTH && _direction == Constants.WEST)
                    {
                        if(!isFastest)
                        {
                            BluetoothManager.getInstance().sendMessage("ar", "R");
                        }
                    }
                }
                else if (dir == _direction)
                {
                    if (dir == Constants.NORTH)
                    {
                        if (!isFastest)
                        {
                            BluetoothManager.getInstance().sendMessage("ar", "F");
                        }
                        _botCoord[1] += 1;
                        _direction = Constants.NORTH;
                    }
                    else if (dir == Constants.SOUTH)
                    {
                        if (!isFastest)
                        {
                            BluetoothManager.getInstance().sendMessage("ar", "F");
                        }
                        _botCoord[1] -= 1;
                        _direction = Constants.SOUTH;
                    }
                    else if (dir == Constants.EAST)
                    {
                        if (!isFastest)
                        {
                            BluetoothManager.getInstance().sendMessage("ar", "F");
                        }
                        _botCoord[0] += 1;
                        _direction = Constants.EAST;
                    }
                    else if (dir == Constants.WEST)
                    {
                        if (!isFastest)
                        {
                            BluetoothManager.getInstance().sendMessage("ar", "F");
                        }
                        _botCoord[0] -= 1;
                        _direction = Constants.WEST;
                    }
                }
            }
           moveBot(dir);
        }
    }

    private Boolean canMove(int dir, MazeTile mazeTile)
    {
        int newX = _botCoord[0];
        int newY = _botCoord[1];
        if (mazeTile == null)
        { // from directional button
            // calc new bot head position
            if (dir == Constants.WEST)
            {
                newX -= 2;
            } else if (dir == Constants.EAST)
            {
                newX += 2;
            } else if (dir == Constants.NORTH)
            {
                newY += 2;
            } else if (dir == Constants.SOUTH)
            {
                newY -= 2;
            }
            // check if new spot for bot's head is within maze
            if (newY < 0 || newY >= MAZE_HEIGHT || newX < 0 || newX >= MAZE_WIDTH)
            {
                return false;
            }
        }
        else
        {
            newX = mazeTile.get_xPos();
            newY = mazeTile.get_yPos();
        }

        return true;
    }

    /**
     * Only for manual mode
     * Always check whether the bot can move in the specified direction using canMove() first
     */
    private void moveBot(int dir)
    {
        if (dir == Constants.WEST)
        {
            //_botCoord[0] -= 1;
            _direction = Constants.WEST;
        }
        else if (dir == Constants.EAST)
        {
          // _botCoord[0] += 1;
            _direction = Constants.EAST;
        }
        else if (dir == Constants.NORTH)
        {
            // _botCoord[1] += 1;
            _direction = Constants.NORTH;
        }
        else if (dir == Constants.SOUTH)
        {
           //_botCoord[1] -= 1;
            _direction = Constants.SOUTH;
        }
        if(_isAuto)
        {
            renderMaze();
        }
    }

    private void updateBotHead()
    {
        _headCoord = _botCoord.clone();
        if (_direction == Constants.NORTH)
        {
            _headCoord[1] += 1;
        }
        else if (_direction == Constants.SOUTH)
        {
            _headCoord[1] -= 1;
        }
        else if (_direction == Constants.EAST)
        {
            _headCoord[0] += 1;
        }
        else
        {
            _headCoord[0] -= 1;
        }
    }

    // CALLED AFTER EVERY UPDATE TO MAZE DATA
    public void renderMaze()
    {
        Log.d(null, "This is RenderMaze");
        Log.d("Coord Count", Integer.toString(_coordCount));

        // for testing
        // handleExplore("ff007e00fc01f803f007e00fe01fc03f807f00f8004000000000000000000000000000000003");
        // handleObstacle("041041041060c1e3f3");

        //UNEXPLORED TILES
        for (int i = 0; i < _exploreData.length; i++)
        {
            if (_exploreData[i] == Constants.UNEXPLORED)
            {
                _tileList.get(i).setState(Constants.UNEXPLORED);
            }
            else if (_exploreData[i] == _imageData[i] && _imageData[i] == 1)
            {
                _obstacleData[i] = 1;
            }
            else
            {
                _tileList.get(i).setState(Constants.EXPLORED);
            }
        }

        //SET START TILES
        if (_coordCount >= 0)
        {
            ArrayList<MazeTile> targetTiles = getTargetTiles(_startCoord[0], _startCoord[1], 0);
            setTile(targetTiles, Constants.START);
        }

        //SET END TILES
        if (_coordCount == 1)
        {
            ArrayList<MazeTile> targetTiles = getTargetTiles(_endCoord[0], _endCoord[1], 0);
            setTile(targetTiles, Constants.GOAL);
        }

        //SET WAYPOINT TILE
        if (_wpSet)
        {
            ArrayList<MazeTile> targetTiles = getTargetTiles(_wpCoord[0], _wpCoord[1], 3);
            setTile(targetTiles, Constants.WAYPOINT);
        }

        //OBSTACLES
        for (int i = 0; i < _obstacleData.length; i++)
        {

            if (_obstacleData[i] == 1 && _obstacleData[i] != _imageData[i])
            {
                _tileList.get(i).setState(Constants.OBSTACLE);
            }
        }

        if (MapFragment.getImagePos() != 0)
        {
            _tileList.get(MapFragment.getImagePos()).setState(_imageID + 20);
            int[] newImagePosEntry = new int[]{MapFragment.getImagePos(), _imageID};
            _receivedImagePosLog = joinIntArray(_receivedImagePosLog, newImagePosEntry);

            if (_receivedImagePosLog.length > 2)
            {
                for (int j = 2; j < _receivedImagePosLog.length-2; j += 2)
                {
                    if (newImagePosEntry[1] == _receivedImagePosLog[j] && newImagePosEntry[0] != _receivedImagePosLog[j-1])
                    {
                        _tileList.get(_receivedImagePosLog[j-1]).setState(Constants.EXPLORED);
                    }
                }
            }
        }

        //ARROW BLOCKS
        if (_trueArrowBlockList.size() > 0)
        {
            for (Integer[] a : _trueArrowBlockList)
            {
                ArrayList<MazeTile> targetTiles = getTargetTiles(a[0], a[1], 3);
                setTile(targetTiles, a[2]); // arrow direction
            }
        }

        // set new robot tiles & head
        if (_coordCount >= 0)
        {
            Log.d("hello", "HELLO I AM NEW ROBOT TILES");
            ArrayList<MazeTile> botTiles = getTargetTiles(_botCoord[0], _botCoord[1], 0);
            setTile(botTiles, Constants.ROBOT_BODY);
            updateBotHead();
            ArrayList<MazeTile> headTile = getTargetTiles(_headCoord[0], _headCoord[1], 3);
            setTile(headTile, Constants.ROBOT_HEAD);

            /* leaves explored tiles where the bot was after reset
            // buggy
            // for manual mode, update _exploredData array after moving
            if (_inputState == Constants.manualMode) {
                for (MazeTile a : botTiles) {
                    _exploreData[a.get_xPos() + a.get_yPos() * MAZE_WIDTH] = Constants.EXPLORED;
                }
            }
            */
        }

        /*
        // plot red tiles for original arrow readings
        if (_arrowBlockList.size() > 0) {
            for (Integer[] a : _arrowBlockList) {
                ArrayList<MazeTile> targetTiles = getTargetTiles(a[0], a[1], 3);
                setTile(targetTiles, 999); // arrow direction
            }
        }
        */

    }

    /* ====== helper functions ========= */
    // updates tile state(s)
    private void setTile(ArrayList<MazeTile> targetTiles, int newState)
    {
        if (targetTiles.size() == 0) return;
        for (MazeTile a : targetTiles) setTile(a, newState);
    }

    private void setTile(MazeTile a, int newState)
    {
        a.setState(newState);
    }

    /**
     * from a selected tile, get the tiles surrounding it, depending on the mode specified
     * mode: 0 -> Block of 9 tiles
     * mode: 1 -> 3 horizontal tiles
     * mode: 2 -> 3 vertical tiles
     * mode: 3 -> single block
     */
    private ArrayList<MazeTile> getTargetTiles(int centerX, int centerY, int mode)
    {
        // get surrounding tiles
        int _center = centerX + centerY * MAZE_WIDTH;
        ArrayList<MazeTile> _tempList = new ArrayList<MazeTile>();
        try
        {
            if (mode == 0)
            {
                _tempList.add(_tileList.get(_center));
                _tempList.add(_tileList.get(_center + 1));
                _tempList.add(_tileList.get(_center - 1));
                _center -= MAZE_WIDTH;

                _tempList.add(_tileList.get(_center));
                _tempList.add(_tileList.get(_center + 1));
                _tempList.add(_tileList.get(_center - 1));
                _center += MAZE_WIDTH * 2;

                _tempList.add(_tileList.get(_center));
                _tempList.add(_tileList.get(_center + 1));
                _tempList.add(_tileList.get(_center - 1));
            }
            else if (mode == 1)
            {
                _tempList.add(_tileList.get(_center));
                _tempList.add(_tileList.get(_center + 1));
                _tempList.add(_tileList.get(_center - 1));
            }
            else if (mode == 2)
            {
                _tempList.add(_tileList.get(_center));
                _tempList.add(_tileList.get(_center + MAZE_WIDTH));
                _tempList.add(_tileList.get(_center - MAZE_WIDTH));
            }
            else if (mode == 3)
            {
                _tempList.add(_tileList.get(_center));
            }
        }
        catch (Exception e)
        {
            Log.e("MissingTileIndex", e.getMessage());
        }
        return _tempList;
    }

    // CORRECT SELECTED TILES - SHIFT INWARDS IF MAZE EDGE IS SELECTED
    private int[] correctSelectedTile(int centerX, int centerY, int mode)
    {
        if (mode == 0 || mode == 1)
        {
            if (centerX == 0) centerX += 1;
            if (centerX == MAZE_WIDTH - 1) centerX -= 1;
        }

        if (mode == 0 || mode == 2)
        {
            if (centerY == 0) centerY += 1;
            if (centerY == MAZE_HEIGHT - 1) centerY -= 1;
        }


        int[] result = {centerX, centerY};
        return result;
    }

    private Boolean isObstacle(MazeTile mazeTile)
    {
        return mazeTile != null && mazeTile.getState() >= Constants.OBSTACLE && mazeTile.getState() <= Constants.WEST;
    }


    // just for converting directions from algo, "N/S/E/W" to our constants
    private int convertDirStrToNum(String dir)
    {
        int dirNum = 0;
        switch (dir)
        {
            case "S":
                dirNum = Constants.SOUTH;
                break;

            case "E":
                dirNum = Constants.EAST;
                break;

            case "W":
                dirNum = Constants.WEST;
                break;

            case "N":
                dirNum = Constants.NORTH;
                break;

            default:
                dirNum = Constants.NORTH;
                Log.e("robotDir", "Unknown direction: " + dir);
                break;
        }
        return dirNum;
    }

    /**
     * Required Android function for positioning child views, don't change this
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        if (TILESIZE == 0)
        {
            int width = this.getWidth();
            int height = this.getHeight();
            TILESIZE = Math.min(width / MAZE_WIDTH, height / MAZE_HEIGHT);
        }

        int i;
        int count = _tileList.size();
        for (i = 0; i < count; i++)
        {
            int xPos = i % MAZE_WIDTH * TILESIZE;
            int yPos = (MAZE_HEIGHT - 1 - i / MAZE_WIDTH) * TILESIZE;
            _tileList.get(i).layout(xPos, yPos, xPos + TILESIZE, yPos + TILESIZE);
            _tileList.get(i).setPadding(Constants.tilePadding, Constants.tilePadding, Constants.tilePadding, Constants.tilePadding);
        }
    }

    // move = 'l'/'r'
    private int calcNewDir(int _direction, char move)
    {
        switch (_direction)
        {
            case Constants.SOUTH:
                if (move == 'l') return Constants.EAST;
                else if (move == 'r') return Constants.WEST;
                break;

            case Constants.EAST:
                if (move == 'l') return Constants.NORTH;
                else if (move == 'r') return Constants.SOUTH;
                break;

            case Constants.WEST:
                if (move == 'l') return Constants.SOUTH;
                else if (move == 'r') return Constants.NORTH;
                break;

            case Constants.NORTH:
                if (move == 'l') return Constants.WEST;
                else if (move == 'r') return Constants.EAST;
                break;

            default:
                return Constants.NORTH;
        }
        return Constants.NORTH;
    }

//    public String[] convertImgCoord(String[] recImgStr)
//    {
//        int robot_x_coord = Integer.parseInt(recImgStr[2]);
//        int robot_y_coord = Integer.parseInt(recImgStr[1]);
//        String img_captured_orient = recImgStr[3];
//
//        //initialise image coordinates to be robot coordinates
//        int img_x_coord = robot_x_coord;
//        int img_y_coord = robot_y_coord;
//
//        //map coordinates to mdp string binary format position
//        int real_img_pos = img_x_coord + (15 * img_y_coord);
//
//        while (_obstacleData[real_img_pos] != 1
//                && img_x_coord+1 > 0 && img_x_coord < 15
//                && img_y_coord+1 > 0 && img_y_coord < 20)
//        {
//            if (img_captured_orient.equals("N")){
//                real_img_pos += 15;
//                img_y_coord++;
//            }
//            else if (img_captured_orient.equals("S")){
//                real_img_pos -= 15;
//                img_y_coord--;
//            }
//            else if (img_captured_orient.equals("E")){
//                real_img_pos += 1;
//                img_x_coord++;
//            }
//            else if (img_captured_orient.equals("W")){
//                real_img_pos -= 1;
//                img_x_coord--;
//            }
//        }
//
//        if (img_x_coord < 0 || img_x_coord > 14 || img_y_coord < 0 || img_y_coord > 19){
//            img_x_coord = -1;
//            img_y_coord = -1;
//            _unresolvedImageDataArr = ImageCheckFragment.joinArray(_unresolvedImageDataArr, recImgStr);
//        }
//
//        String[] _convertedImgStr = new String[]{recImgStr[0], Integer.toString(img_y_coord), Integer.toString(img_x_coord), recImgStr[3]};
//        return _convertedImgStr;
//    }

//    public void resolveMisplacedImages()
//    {
//        _unresolvedImageDataArr[1]
//        _unresolvedImageDataArr[2]
//        _unresolvedImageDataArr[3]
//        _unresolvedImageDataArr[4]_receivedImagePosLog
//        handleImageBlock();
//    }

    private static int[] joinIntArray(int[]... arrays)
    {
        int length = 0;
        for (int[] array : arrays) {
            length += array.length;
        }

        final int[] result = new int[length];

        int offset = 0;
        for (int[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }

        return result;
    }

    public static void setAuto(){
        _isAuto = !_isAuto;
    }
}