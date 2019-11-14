package com.example.mdp_android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import java.util.HashMap;

public class MazeTile extends View
{
    private static int UNEXPLORED;
    private static int EXPLORED;
    private static int OBSTACLE;
    private static int START;
    private static int GOAL;
    private static int WAYPOINT;
    private static int ROBOT_HEAD;
    private static int ROBOT_BODY;
    private static final int red = Color.RED;
    private static HashMap<Integer, Integer> colorMap = null;

    private int _state = Constants.UNEXPLORED;
    private int _xPos = -1;
    private int _yPos = -1;

    public MazeTile(Context context, int XPos, int YPos)
    {
        super(context);
        _xPos = XPos;
        _yPos = YPos;

        if (colorMap == null)
        {
            UNEXPLORED = 0xFFA9A9A9; // light grey 0xFFD3D3D3; // Color.GRAY;
            EXPLORED = Color.LTGRAY;
            OBSTACLE = Color.BLACK;
            ROBOT_BODY = getResources().getColor(R.color.darkOrange);
            ROBOT_HEAD = getResources().getColor(R.color.colorPrimaryDark);
            START = getResources().getColor(R.color.orange);
            GOAL = getResources().getColor(R.color.yellow);
            WAYPOINT = getResources().getColor(R.color.green); // 0xFF96deff;

            colorMap = new HashMap<Integer, Integer>();
            colorMap.put(Constants.UNEXPLORED, UNEXPLORED);
            colorMap.put(Constants.EXPLORED, EXPLORED);
            colorMap.put(Constants.OBSTACLE, OBSTACLE);
            colorMap.put(Constants.START, START);
            colorMap.put(Constants.GOAL, GOAL);
            colorMap.put(Constants.WAYPOINT, WAYPOINT);
            colorMap.put(Constants.ROBOT_HEAD, ROBOT_HEAD);
            colorMap.put(Constants.ROBOT_BODY, ROBOT_BODY);
            colorMap.put(999, red);
        }
    }

    /**
     * Required Android function.
     * Draws the tile everytime Android system does a re-render
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        if (_state >= Constants.UNEXPLORED && _state <= Constants.OBSTACLE /*|| _state ==999*/)
        {
            Rect rectangle = new Rect(0, 0, Maze.TILESIZE-Constants.tilePadding, Maze.TILESIZE-Constants.tilePadding);
            Paint paint = new Paint();
            paint.setColor(colorMap.get(_state));
            canvas.drawRect(rectangle, paint);
        }

        // red
        else if (_state >= Constants.NORTH && _state <= Constants.WEST)
        {
            Bitmap bitmap = null;
            bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.up_arrow_foreground);
            canvas.drawBitmap(bitmap, null, new RectF(0, 0, Maze.TILESIZE-Constants.tilePadding, Maze.TILESIZE-Constants.tilePadding), null);
        }

        // Added for Arrow Blocks
        else if (_state >= 21 && _state <= 35)
        {
            Bitmap bitmap = null;

            switch (_state){
                case 21:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_1);
                    break;
                case 22:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_2);
                    break;
                case 23:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_3);
                    break;
                case 24:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_4);
                    break;
                case 25:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_5);
                    break;
                case 26:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_6);
                    break;
                case 27:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_7);
                    break;
                case 28:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_8);
                    break;
                case 29:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_9);
                    break;
                case 30:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_10);
                    break;
                case 31:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_11);
                    break;
                case 32:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_12);
                    break;
                case 33:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_13);
                    break;
                case 34:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_14);
                    break;
                case 35:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.image_15);
                    break;
                default:
                    break;
            }

            canvas.drawBitmap(bitmap, null, new RectF(0, 0, Maze.TILESIZE-Constants.tilePadding, Maze.TILESIZE-Constants.tilePadding), null);
        }
    }

    public int getState()
    {
        return _state;
    }

    public void setState(int newState)
    {
        _state = newState;
        invalidate();
    }

    public int get_xPos()
    {
        return _xPos;
    }

    public int get_yPos()
    {
        return _yPos;
    }

    public void reset()
    {
        _state = Constants.UNEXPLORED;
        invalidate();
    }
}
