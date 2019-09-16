package com.example.fel.mdp_group13;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import com.example.fel.mdp_group13.DeviceCommands;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.BLUE;

enum ArrowFace {
    NORTH,
    SOUTH,
    EAST,
    WEST,
    NULL,
}

public class ArenaGrid extends View {
    

    public static final String TAG = "MDP GROUP 13";
    int orange = ContextCompat.getColor(getContext(), R.color.colorPrimary);
    /* private static final int COLS = 15;
     private static final int ROWS = 20; */
    private int numColumns, numRows;
    private static final float WALL_THICKNESS = 4;
    //private float cellSize, hMargin, vMargin;
    private float cellWidth, cellHeight;
    private Paint wallPaint;
    private Paint blackPaint = new Paint();
    private Paint robotPaint = new Paint();
    private Paint robotPosPaint = new Paint();
    private Paint arrowPaint = new Paint();
    private Paint waypointPaint = new Paint();
    private Paint endPointPaint = new Paint();
    private Paint exploredPaint = new Paint();
    private boolean[][] cellChecked;
    private ArrowFace[][] cellArrowUp;
    private ArrowFace[][] cellArrowDown;
    private ArrowFace[][] cellArrowLeft;
    private ArrowFace[][] cellArrowRight;
    private boolean[][] cellExplored;
    private boolean[][] robot;
    private boolean robotStart = false;
    private int robotX = 0, robotY = 17;
    private DeviceCommands.Direction robotDirection = DeviceCommands.Direction.UP;
    private int wpX = 99, wpY = 99;

    public ArenaGrid(Context context) {
        this(context, null);
    }

    public ArenaGrid(Context context, AttributeSet attrs) {
        super(context, attrs);

       /*wallPaint = new Paint();
        wallPaint.setColor(Color.BLACK);
        wallPaint.setStrokeWidth(WALL_THICKNESS); */

        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        robotPosPaint.setColor(BLUE);
        robotPaint.setColor(BLACK);
        arrowPaint.setColor(Color.YELLOW);
        endPointPaint.setColor(Color.GREEN);
        exploredPaint.setColor(Color.GRAY);
        waypointPaint.setColor(Color.RED);

      //  createMaze();
    }

    public int getNumColumns(){
        return numColumns;
    }

    public void setNumColumns(int COLS) {
        this.numColumns = numColumns;
        redrawArenaGrid();
    }

    public int getNumRows() {
        return numRows;
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
        redrawArenaGrid();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        redrawArenaGrid();
    }

    public void redrawArenaGrid() {
        if (numColumns < 1 || numRows < 1) {
            return;
        }
        cellWidth = ((float) getWidth() / numColumns);
        cellHeight = ((float) getHeight() / numRows);
        resetGrid();
        resetArrows();
        resetRobot();
        cellExplored = new boolean[numRows][numColumns];
        invalidate();
    }

    public float getCellWidth() {
        cellWidth = ((float) getWidth() / numColumns);
        return cellWidth;
    }

    public float getCellHeight() {
        cellHeight = ((float) getHeight() / numRows);
        return cellHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);

        if (numColumns == 0 || numRows == 0) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                if (cellChecked[i][j]) {
                    canvas.drawRect(j * cellWidth, i * cellHeight,
                            (j + 1) * cellWidth, (i + 1) * cellHeight,
                            blackPaint);
                }
                if (cellArrowUp[i][j] != ArrowFace.NULL) {
                    drawTriangle(canvas, arrowPaint, j * cellWidth, i * cellHeight, cellWidth, DeviceCommands.Direction.UP);
                }
                if (cellArrowDown[i][j] != ArrowFace.NULL) {
                    drawTriangle(canvas, arrowPaint, j * cellWidth, i * cellHeight, cellWidth, DeviceCommands.Direction.DOWN);
                }
                if (cellArrowLeft[i][j] != ArrowFace.NULL) {
                    drawTriangle(canvas, arrowPaint, j * cellWidth, i * cellHeight, cellWidth, DeviceCommands.Direction.LEFT);
                }
                if (cellArrowRight[i][j] != ArrowFace.NULL) {
                    drawTriangle(canvas, arrowPaint, j * cellWidth, i * cellHeight, cellWidth, DeviceCommands.Direction.RIGHT);
                }
                if ((j == robotX) && (i == robotY)) {
                    for (int m = 0; m < 3; m++) {
                        for (int n = 0; n < 3; n++) {
                            canvas.drawRect((robotX + n) * cellWidth, (robotY + m) * cellHeight,
                                    (robotX + n + 1) * cellWidth, (robotY + m + 1) * cellHeight, robotPaint);
                        }
                    }
                    float robotwidth = cellWidth * 3;
                    drawTriangle(canvas, robotPosPaint, robotX * cellWidth, robotY * cellHeight, robotwidth, robotDirection);

                }

                if (cellExplored[i][j] == true && robot[i][j] == false) {
                    canvas.drawRect(j * cellWidth, i * cellHeight,
                            (j + 1) * cellWidth, (i + 1) * cellHeight,
                            exploredPaint);
                }

                if ((j == wpX) && (i == wpY)) {
                    canvas.drawRect(wpX * cellWidth, wpY * cellHeight,
                            (wpX + 1) * cellWidth, (wpY + 1) * cellHeight, waypointPaint);
                }

                for (int m = 0; m < 3; m++) {
                    for (int n = 0; n < 3; n++) {
                        if (robot[m][14 - n] == false)
                            canvas.drawRect((14 - n) * cellWidth, (m) * cellHeight,
                                    (14 - n + 1) * cellWidth, (m + 1) * cellHeight, endPointPaint);
                    }
                }
            }
        }

        for (int i = 1; i < numColumns; i++) {
            canvas.drawLine(i * cellWidth, 0, i * cellWidth, width + 400, blackPaint);
        }

        for (int i = 1; i < numRows; i++) {
            canvas.drawLine(0, i * cellHeight, height, i * cellHeight, blackPaint);
        }
        blackPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(1, 1, canvas.getWidth(), canvas.getHeight(), blackPaint);
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public void resetGrid() {
        cellChecked = new boolean[numRows][numColumns];
    }

    public void updateGrid(byte[] arenaDefinition) {
        int i = 0;
        for (int j = 0; j < numRows; j++) {
            for (int k = 0; k < numColumns; k++) {
                cellChecked[j][k] = arenaDefinition[i] !=0;
                i++;
            }
        }

    }

    public void resetRobot() {
        robot = new boolean[numRows][numColumns];
    }

    public void updateRobot(int x, int y, DeviceCommands.Direction direction) {
        resetRobot();
        robotX = x;
        robotY = y;
        robotDirection = direction;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                robot[y + i][x + j] = true;
                if (robotStart == true)
                    updateExploration(robotX, robotY);
            }
        }
    }

    public void updateExploration(int x, int y) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                cellExplored[y + i][x + j] = true;
            }
        }
    }

    public DeviceCommands.Direction getRobotDirection() {
        return robotDirection;
    }

    public void resetArrows() {
        cellArrowUp = new ArrowFace[numRows][numColumns];
        cellArrowDown = new ArrowFace[numRows][numColumns];
        cellArrowLeft = new ArrowFace[numRows][numColumns];
        cellArrowRight = new ArrowFace[numRows][numColumns];
        for (int i=0; i<numRows; i++) {
            Arrays.fill(cellArrowUp[i], ArrowFace.NULL);
            Arrays.fill(cellArrowLeft[i], ArrowFace.NULL);
            Arrays.fill(cellArrowDown[i], ArrowFace.NULL);
            Arrays.fill(cellArrowRight[i], ArrowFace.NULL);
        }
    }

    /**
     * Add Arrow to the current Robot State
     *
     * @param x
     * @param y
     * @param direction
     */
    public void addArrow(int x, int y, DeviceCommands.Direction direction, ArrowFace arrowFace) {
        switch (direction) {
            case UP:
                cellArrowUp[y][x] = arrowFace;
                break;

            case DOWN:
                cellArrowDown[y][x] = arrowFace;
                break;

            case LEFT:
                cellArrowLeft[y][x] = arrowFace;
                break;

            case RIGHT:
                cellArrowRight[y][x] = arrowFace;
                break;
        }
    }

    public void addArrow(ByteString arrowsByteString, DeviceCommands.Direction direction) {
        byte[] arrows = arrowsByteString.toByteArray();
        int i=0;
        if (arrows.length == 0) {
            return;
        }
        for (int row = 0; row < numRows; row++) {
            for (int col=0; col < numColumns; col++) {
                byte curr = arrows[i];
                if (curr == 1) {
                    addArrow(col, row, direction, ArrowFace.NORTH);
                } else if (curr == 2) {
                    addArrow(col, row, direction, ArrowFace.SOUTH);
                } else if (curr == 3) {
                    addArrow(col, row, direction, ArrowFace.WEST);
                } else if (curr == 4) {
                    addArrow(col, row, direction, ArrowFace.EAST);
                }
                i++;
            }
        }
    }

    public String arrowText() {
        String text = "";
        int i = 0;
        for (int j = 0; j < numRows; j++) {
            for (int k = 0; k < numColumns; k++) {
                if (cellArrowUp[j][k] != ArrowFace.NULL) {
                    text = text + (String.format("%s: %d, %d, %s\n", i + 1, k, 19 - j, cellArrowUp[j][k].name()));
                    i++;
                }
                if (cellArrowDown[j][k] != ArrowFace.NULL) {
                    text = text + (String.format("%s: %d, %d, %s\n", i + 1, k, 19 - j, cellArrowDown[j][k].name()));
                    i++;
                }
                if (cellArrowLeft[j][k] != ArrowFace.NULL) {
                    text = text + (String.format("%s: %d, %d, %s\n", i + 1, k, 19 - j, cellArrowLeft[j][k].name()));
                    i++;
                }
                if (cellArrowRight[j][k] != ArrowFace.NULL) {
                    text = text + (String.format("%s: %d, %d, %s\n", i + 1, k, 19 - j, cellArrowRight[j][k].name()));
                    i++;
                }
            }
        }
        return text;
    }

    public void drawTriangle(Canvas canvas, Paint paint, float x, float y, float width, DeviceCommands.Direction direction) {
        float halfWidth = width / 2;
        Path path = new Path();
        switch (direction) {
            case UP:
                path.moveTo(x + halfWidth, y); // Top
                path.lineTo(x, y + width); // Bottom left
                path.lineTo(x + width, y + width); // Bottom right
                path.lineTo(x + halfWidth, y); // Back to Top
                path.close();
                canvas.drawPath(path, paint);
                break;

            case RIGHT:
                path.moveTo(x + width, y + halfWidth); // Top
                path.lineTo(x, y); // Bottom left
                path.lineTo(x, y + width); // Bottom right
                path.lineTo(x + width, y + halfWidth); // Back to Top
                path.close();
                canvas.drawPath(path, paint);
                break;

            case DOWN:
                path.moveTo(x + halfWidth, y + width); // Top
                path.lineTo(x + width, y); // Bottom left
                path.lineTo(x, y); // Bottom right
                path.lineTo(x + halfWidth, y + width); // Back to Top
                path.close();
                canvas.drawPath(path, paint);
                break;

            case LEFT:
                path.moveTo(x, y + halfWidth); // Top
                path.lineTo(x + width, y + width); // Bottom left
                path.lineTo(x + width, y); // Bottom right
                path.lineTo(x, y + halfWidth); // Back to Top
                path.close();
                canvas.drawPath(path, paint);
                break;
            case UNRECOGNIZED:
                //
                break;
            default:
                //
                break;
        }
    }

    public void manualUpdateGrid(final byte[] arenaDefinition) {
        updateGrid(arenaDefinition);
    }

    public void manualUpdateRobot(final int x, final int y, final DeviceCommands.Direction direction) {
        updateRobot(x, y, direction);
    }

    public void manualUpdateArrow(final int x, final int y, final DeviceCommands.Direction direction) {
        addArrow(x, y, direction, ArrowFace.NULL);
    }

    public void manualUpdateAll() {
        invalidate();
    }

    public void updateWaypoint(int x, int y) {
        wpX = x;
        wpY = y;
    }

    public void updateRobotStart(boolean start) {
        robotStart = start;
    }
}

