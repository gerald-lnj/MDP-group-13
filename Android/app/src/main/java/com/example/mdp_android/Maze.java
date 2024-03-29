package com.example.mdp_android;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.example.mdp_android.bluetooth.BluetoothManager;
import com.example.mdp_android.tabs.ImageCheckFragment;
import com.example.mdp_android.tabs.MapFragment;

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
    private int[] _obstacleData = new int[MAZE_HEIGHT * MAZE_WIDTH];
    private int[] _exploreData = new int[MAZE_HEIGHT * MAZE_WIDTH];
    private static int[] _imageData = new int[MAZE_HEIGHT * MAZE_WIDTH];
    private int _imageID;

    private static String[] _allImageDataArr = new String[]{"0"};
    private int[] _receivedImagePosLog = new int[]{0};

    //Managing Input State
    private int _coordCount = -1;
    private boolean _wpSet = false;
    private int _inputState = Constants.idleMode;
    private Boolean _exploreCompleted = false;

    //Added for auto refresh
    private static Boolean _isAuto = true;

    private final String TAG = "ConnectedThread";

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
        String tmp = parseHexCharToBinary(binaryData);
        Log.d("TEMP1", tmp);

        String tmp2 = tmp.substring(2, tmp.length() - 2);
        Log.d("TEMP2", tmp2);

        _exploreData = convertStrToIntArray(tmp2);
        renderMaze();
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
        int[] mappedObstacleData = _exploreData.clone();
        int j = 0;
        for (int i = 0; i < mappedObstacleData.length; i++)
        {
            if (mappedObstacleData[i] == 1){
                try{
                mappedObstacleData[i] = exploredObstacleData[j];
                j++;}
                catch (Exception e)
                {
                    mappedObstacleData[i] = 0;
                }
            };
        }
        _obstacleData = mappedObstacleData;
        Log.d("Obstacle Data", Arrays.toString(_obstacleData));
        renderMaze();
    }

    //amd tool - Show arrow block Position
    public void handleImageBlock(String binaryData, int imgID)
    {
        _imageData = convertStrToIntArray(binaryData);
        _imageID = imgID;
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
                int xPos = Integer.parseInt(tmp[0]);
                int yPos = 19-Integer.parseInt(tmp[1]);
                int direction = Integer.parseInt(tmp[2]);

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
                else if (tmp[2].equals("4"))
                {
                    direction = Constants.WEST;
                }

               _direction = direction;

                Log.d("Bot Coord X", Integer.toString(-_botCoord[0]));
                Log.d("Bot Coord Y", Integer.toString(-_botCoord[1]));
                Log.d("Bot Direction", Integer.toString(_direction));

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
    }

    // Convert hex to binary
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

    private void moveBot(int dir)
    {
        if (dir == Constants.WEST)
        {
            _direction = Constants.WEST;
        }
        else if (dir == Constants.EAST)
        {
            _direction = Constants.EAST;
        }
        else if (dir == Constants.NORTH)
        {
            _direction = Constants.NORTH;
        }
        else if (dir == Constants.SOUTH)
        {
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

    // Needed to update maze
    public void renderMaze()
    {
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

        // Start Tiles
        if (_coordCount >= 0)
        {
            ArrayList<MazeTile> targetTiles = getTargetTiles(_startCoord[0], _startCoord[1], 0);
            setTile(targetTiles, Constants.START);
        }

        // End Tiles
        if (_coordCount == 1)
        {
            ArrayList<MazeTile> targetTiles = getTargetTiles(_endCoord[0], _endCoord[1], 0);
            setTile(targetTiles, Constants.GOAL);
        }

        // Waypoint Tile
        if (_wpSet)
        {
            ArrayList<MazeTile> targetTiles = getTargetTiles(_wpCoord[0], _wpCoord[1], 3);
            setTile(targetTiles, Constants.WAYPOINT);
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
                        if (_obstacleData[_receivedImagePosLog[j-1]] == 1)
                        {
                            _tileList.get(_receivedImagePosLog[j-1]).setState(Constants.OBSTACLE);
                        }
                        else if (_exploreData[_receivedImagePosLog[j-1]] == Constants.EXPLORED)
                        {
                            _tileList.get(_receivedImagePosLog[j-1]).setState(Constants.EXPLORED);
                        }
                        else _tileList.get(_receivedImagePosLog[j-1]).setState(Constants.UNEXPLORED);
                    }
                }
            }
        }

        // Obstacles
        for (int i = 0; i < _obstacleData.length; i++)
        {
            if (_obstacleData[i] == 1 && _obstacleData[i] != _imageData[i])
            {
                _tileList.get(i).setState(Constants.OBSTACLE);
            }
        }

        // New robot tiles & head
        if (_coordCount >= 0)
        {
            Log.d("hello", "HELLO I AM NEW ROBOT TILES");
            ArrayList<MazeTile> botTiles = getTargetTiles(_botCoord[0], _botCoord[1], 0);
            setTile(botTiles, Constants.ROBOT_BODY);
            updateBotHead();
            ArrayList<MazeTile> headTile = getTargetTiles(_headCoord[0], _headCoord[1], 3);
            setTile(headTile, Constants.ROBOT_HEAD);
        }
    }

    /* ====== helper functions ========= */
    // Updates tile state(s)
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
     * mode: 0 -> Block of 9 tile
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

    public String[] convertImgCoord(String[] recImgStr)
    {
        int robot_x_coord = Integer.parseInt(recImgStr[2]);
        int robot_y_coord = 19-Integer.parseInt(recImgStr[1]);

        String img_captured_orient = "";

        if (recImgStr[3].equals("1")){
            img_captured_orient = "2";
        }
        else if (recImgStr[3].equals("2")){
            img_captured_orient = "3";
        }
        else if (recImgStr[3].equals("3")){
            img_captured_orient = "4";
        }
        else if (recImgStr[3].equals("4")){
            img_captured_orient = "1";
        }

        //initialise image coordinates to be robot coordinates
        int img_x_coord = robot_x_coord;
        int img_y_coord = robot_y_coord;

        //12 - 3 correct 3
        //8 - 1 correct 2
        //6 - 1 correct 2
        //13 - 4 correct 1

        //map coordinates to mdp string binary format position
        int real_img_pos = img_x_coord + (15 * img_y_coord);

        while (_obstacleData[real_img_pos] != 1
                && img_x_coord+1 > 0 && img_x_coord < 15
                && img_y_coord+1 > 0 && img_y_coord < 20)
        {
            if (img_captured_orient.equals("1"))
            {
                real_img_pos += 15;
                img_y_coord++;
            }
            else if (img_captured_orient.equals("2"))
            {
                real_img_pos += 1;
                img_x_coord++;
            }
            else if (img_captured_orient.equals("3"))
            {
                real_img_pos -= 15;
                img_y_coord--;
            }
            else if (img_captured_orient.equals("4"))
            {
                real_img_pos -= 1;
                img_x_coord--;
            }
            if (real_img_pos < 0 || real_img_pos > 300)
            {
                break;
            }
        }

        if (img_x_coord < 0 || img_x_coord > 14 || img_y_coord < 0 || img_y_coord > 19){
            img_x_coord = -1;
            img_y_coord = -1;
        }

        _allImageDataArr = ImageCheckFragment.joinArray(_allImageDataArr, recImgStr);
        String[] _convertedImgStr = new String[]{recImgStr[0], Integer.toString(img_y_coord), Integer.toString(img_x_coord), recImgStr[3]};
        return _convertedImgStr;
    }

    public static String[] resolveMisplacedImages()
    {
        return _allImageDataArr;
    }

    private static int[] joinIntArray(int[]... arrays) {
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