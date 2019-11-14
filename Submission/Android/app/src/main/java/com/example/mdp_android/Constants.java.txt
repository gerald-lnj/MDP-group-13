package com.example.mdp_android;

import com.example.mdp_android.bluetooth.BluetoothChatService;

/**
 * Constants between {@link BluetoothChatService} and interface
 */
public interface Constants
{
    //Message types sent from BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_DEVICE_ADDRESS = 5;
    int MESSAGE_TOAST = 6;
    int ACCELERATE = 7;

    //Key names received form the BluetoothChatService Handler
    String DEVICE_NAME = "device_name";
    String DEVICE_ADDRESS = "device_address";
    String TOAST = "toast";

    //Mazetile states
    int UNEXPLORED = 0;
    int EXPLORED = 1;
    int START = 2;
    int GOAL = 3;
    int WAYPOINT = 4;

    //Maze Robot
    int ROBOT_HEAD = 5;
    int ROBOT_BODY = 6;

    //Used for directions as well
    int OBSTACLE = 7;
    int NORTH = 8;
    int EAST = 9;
    int SOUTH = 10;
    int WEST = 11;

    //Map Fragment Input States
    int idleMode = -1;
    int coordinateMode = 0;
    int waypointMode = 1;
    int exploreMode = 2;
    int fastestPathMode = 3;
    int manualMode = 4;

    //For Accelerometer
    int up = 0;
    int down = 1;
    int right = 2;
    int left = 3;

    //For maze tile
    int tilePadding = 1;
}
