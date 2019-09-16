package com.example.fel.mdp_group13;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

//import com.example.myapplication.Bluetooth.BluetoothChatFragment;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.SENSOR_SERVICE;

public class ArenaFragment extends Fragment implements SensorEventListener {

    public static final float EPSILON = 0.000000001f;
    public static final int TIME_CONSTANT = 30;
    public static final float FILTER_COEFFICIENT = 0.98f;
    public static final String TAG = "MDP_GRPxx";
    private static final float NS2S = 1.0f / 1000000000.0f;
    public Handler mHandler;
    DecimalFormat d = new DecimalFormat("#.##");
    private Button fwdBtn;
    private Button backBtn;
    private Button leftBtn;
    private Button rightBtn;
    public Button exploreBtn;
    public Button fastestPathBtn;
    private Button startRobotBtn;
    private Button waypointBtn;
    private Button clearGridBtn;
//    private Button calibrateBtn;
    private View grid;
    private TextView robot_status;
    private TextView robot_coordinates;
    private TextView arrowText;
    private SensorManager mSensorManager = null;
    private String pitchValue;
    private String rollValue;
    private int setwp = 0;
    private int robotX = 0;
    private int robotY = 17;
    private ToggleButton toggleBtn;
    private Button updateBtn;
    private boolean started = false;
    private boolean gyroOnOff = false;
    private Button gyroBtn;
    private TextView mPitchView;
    private TextView mRollView;
    // angular speeds from gyro
    private float[] gyro = new float[3];
    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];
    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
    // magnetic field vector
    private float[] magnet = new float[3];
    // accelerometer vector
    private float[] accel = new float[3];
    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];
    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];
    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];
    private float timestamp;
    private boolean initState = true;
    private Timer fuseTimer = new Timer();
    private ArenaGrid arenaGrid;
    private TextView robotPos;
    private TextView waypointPos;
    private TextView startPointPos;
    private String startPointCoordinates = "(0,17)";
    private String coordinates = "";
    private String wpCoordinates = "(0,17)";
    private int wpX = 0;
    private int wpY = 17;
    private DeviceCommands.DeviceCommand.Command acceleratorDirection = DeviceCommands.DeviceCommand.Command.STOP;
    private Map<DeviceCommands.DeviceCommand.Command, String> commandStatusTextMapping;

    private Runnable updateOrientationDisplayTask = new Runnable() {
        public void run() {
            updateOrientationDisplay();
        }
    };

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f;
        gyroMatrix[1] = 0.0f;
        gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f;
        gyroMatrix[4] = 1.0f;
        gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f;
        gyroMatrix[7] = 0.0f;
        gyroMatrix[8] = 1.0f;

        // get sensorManager and initialise sensor listeners
        mSensorManager = (SensorManager) this.getContext().getSystemService(SENSOR_SERVICE);
        initListeners();

        commandStatusTextMapping = new HashMap();

        commandStatusTextMapping.put(DeviceCommands.DeviceCommand.Command.FORWARD, "Robot moving forward");
        commandStatusTextMapping.put(DeviceCommands.DeviceCommand.Command.BACKWARD, "Robot moving backward");
        commandStatusTextMapping.put(DeviceCommands.DeviceCommand.Command.RIGHT, "Robot moving right ");
        commandStatusTextMapping.put(DeviceCommands.DeviceCommand.Command.LEFT, "Robot moving left");


        // wait for one second until gyroscope and magnetometer/accelerometer
        // data is initialised then scedule the complementary filter task
        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                1000, TIME_CONSTANT);

       // View rootView = inflater.inflate(R.layout.fragment_arenacontrol, container, false);
       // arenaGrid = (ArenaGrid) rootView.findViewById(R.id.grid);
      //  arenaGrid.setNumColumns(15);
      // arenaGrid.setNumRows(20);
       // arenaGrid.invalidate();
      //  return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

       /* fwdBtn = (Button) view.findViewById(R.id.upArrowBtn);
        backBtn = (Button) view.findViewById(R.id.downArrowBtn);
        leftBtn = (Button) view.findViewById(R.id.leftArrowBtn);
        rightBtn = (Button) view.findViewById(R.id.rightArrowBtn);
        exploreBtn = (Button) view.findViewById(R.id.explorationBtn);
        fastestPathBtn = (Button) view.findViewById(R.id.fastestPathBtn);
        startRobotBtn = (Button) view.findViewById(R.id.startRobotBtn);
        waypointBtn = (Button) view.findViewById(R.id.waypointBtn);
        waypointBtn.setEnabled(false);
        clearGridBtn = (Button) view.findViewById(R.id.clearGridBtn);
        robot_status = (TextView) view.findViewById(R.id.robot_status);
        robot_coordinates = (TextView) view.findViewById(R.id.robot_coordinates);
//        calibrateBtn = (Button) view.findViewById(R.id.calibrateBtn);
        arrowText = (TextView) view.findViewById(R.id.arrowTv);
        grid = view.findViewById(R.id.grid);

        gyroBtn = (Button) view.findViewById(R.id.gyroBtn);

        mHandler = new Handler();
        d.setRoundingMode(RoundingMode.HALF_UP);
        d.setMaximumFractionDigits(3);
        d.setMinimumFractionDigits(3);

        toggleBtn = (ToggleButton) view.findViewById(R.id.manualAuto_toggle);
        toggleBtn.setTextOff("MANUAL");
        toggleBtn.setTextOn("AUTO");
        toggleBtn.setChecked(true);
        updateBtn = (Button) view.findViewById(R.id.updateGridBtn);
        updateBtn.setEnabled(false);*/

        final View.OnTouchListener tListener = new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Fragment YY = getFragmentManager().findFragmentByTag("Arena Fragment");
                ArenaFragment tz = (ArenaFragment) YY;

                Fragment TT = getFragmentManager().findFragmentByTag("BT Fragment");
                //final BluetoothChatFragment tx = (BluetoothChatFragment) TT;

               // arenaGrid = (ArenaGrid) tz.getView().findViewById(R.id.grid);
               // startPointPos = (TextView) tz.getView().findViewById(R.id.startPointTv);
               // waypointPos = (TextView) tz.getView().findViewById(R.id.waypointTv);
                switch (setwp) {
                    case 0:
                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                            int row = (int) (motionEvent.getY() / arenaGrid.getCellHeight());
                            int column = (int) (motionEvent.getX() / arenaGrid.getCellWidth());

                            robotX = Math.min(column, 12);
                            robotY = Math.min(row, 17);
                            arenaGrid.updateRobot(robotX, robotY, arenaGrid.getRobotDirection());
                            DeviceCommands.RobotRequest request = DeviceCommands.RobotRequest.newBuilder().setReposition(DeviceCommands.DevicePositionInformation.newBuilder().setX(robotX).setY(robotY).setDirection(arenaGrid.getRobotDirection()).build()).build();
                           // tx.write(request.toByteArray());

                            coordinates = String.format("X-axis : %s \nY-axis : %s \nDirection : %s", robotX, 19 - robotY, arenaGrid.getRobotDirection());
                            startPointCoordinates = String.format("(%s, %s)", robotX, 19 - robotY);
                            robot_coordinates.setText(coordinates);
                            if (started == true)
                                arenaGrid.updateRobotStart(true);
                            startPointPos.setText(startPointCoordinates);
                            arenaGrid.invalidate();
                        }
                        break;

                    case 1:
                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                            int row = (int) (motionEvent.getY() / arenaGrid.getCellHeight());
                            int column = (int) (motionEvent.getX() / arenaGrid.getCellWidth());
                            wpX = column;
                            wpY = row;
                            arenaGrid.updateWaypoint(wpX, wpY);
                            DeviceCommands.RobotRequest request;
                            request = DeviceCommands.RobotRequest.newBuilder().setWaypoint(DeviceCommands.WaypointInformation.newBuilder().setX(wpX).setY(wpY).build()).build();
                            //tx.write(request.toByteArray());

                            if (started == true)
                                arenaGrid.updateRobotStart(true);
                            arenaGrid.updateRobot(robotX, robotY, arenaGrid.getRobotDirection());

                            request = DeviceCommands.RobotRequest.newBuilder().setReposition(DeviceCommands.DevicePositionInformation.newBuilder().setX(robotX).setY(robotY).setDirection(arenaGrid.getRobotDirection()).build()).build();
                           // tx.write(request.toByteArray());

                            wpCoordinates = String.format("(%s,%s)", wpX, 19 - wpY);
                            waypointPos.setText(wpCoordinates);
                            arenaGrid.invalidate();
                        }
                        break;

                    default:
                        break;

                }
                return true;
            }
        };

        View.OnClickListener listener = new View.OnClickListener() {
            public void onClick(View view) {
                View currentView = getView();
                if (null != currentView) {
                    Fragment TT = getFragmentManager().findFragmentByTag("BT Fragment");
                 //   final BluetoothChatFragment tx = (BluetoothChatFragment) TT;

                    Fragment YY = getFragmentManager().findFragmentByTag("Arena Fragment");
                    ArenaFragment tz = (ArenaFragment) YY;
                  //  arenaGrid = (ArenaGrid) tz.getView().findViewById(R.id.grid);
                  //  startPointPos = tz.getView().findViewById(R.id.startPointTv);
                  //  waypointPos = tz.getView().findViewById(R.id.waypointTv);

                        if (view == fastestPathBtn) {
                            fastestPathBtn.setEnabled(false);
                            exploreBtn.setEnabled(false);
                            waypointBtn.setEnabled(false);
//                            calibrateBtn.setEnabled(false);

                            // RPI
                            DeviceCommands.RobotRequest request = DeviceCommands.RobotRequest.newBuilder().setDeviceCommand(
                                    DeviceCommands.DeviceCommand.newBuilder().setCommand(DeviceCommands.DeviceCommand.Command.FASTEST_PATH).build()).build();
                           // tx.write(request.toByteArray());
                            robot_status.setText("beginFastest");

                            started = true;
                            arenaGrid.updateRobotStart(true);
                            arenaGrid.updateRobot(robotX, robotY, arenaGrid.getRobotDirection());
                            startRobotBtn.setEnabled(false);
                            fwdBtn.setEnabled(false);
                            backBtn.setEnabled(false);
                            leftBtn.setEnabled(false);
                            rightBtn.setEnabled(false);
                            setwp = 3;
                        }

                        if (view == exploreBtn) {
                            fastestPathBtn.setEnabled(true);
                            exploreBtn.setEnabled(false);
                            waypointBtn.setEnabled(false);

                            DeviceCommands.RobotRequest request = DeviceCommands.RobotRequest.newBuilder().setDeviceCommand(DeviceCommands.DeviceCommand.newBuilder().setCommand(DeviceCommands.DeviceCommand.Command.EXPLORATION).build()).build();
                            tx.write(request.toByteArray());
                            robot_status.setText("beginExplore");
                            started = true;
                            arenaGrid.updateRobotStart(true);
                            arenaGrid.updateRobot(robotX, robotY, arenaGrid.getRobotDirection());
                            startRobotBtn.setEnabled(false);
                            fwdBtn.setEnabled(false);
                            backBtn.setEnabled(false);
                            leftBtn.setEnabled(false);
                            rightBtn.setEnabled(false);
                            setwp = 3;
                        }

                        if (view == startRobotBtn) {
                            // TODO: Send Message to RPI, to start accepting command from Android
                            robot_status.setText("Robot Position Set!");
                            waypointBtn.setEnabled(true);
                            startRobotBtn.setEnabled(false);
                            setwp = 1;
                            // Send start coordinate set to RPI
                            DeviceCommands.RobotRequest request = DeviceCommands.RobotRequest.newBuilder()
                                    .setReposition(DeviceCommands.DevicePositionInformation.newBuilder().setX(robotX).setY(robotY).setDirection(DeviceCommands.Direction.UP).build())
                                    .setDeviceCommand(DeviceCommands.DeviceCommand.newBuilder().setCommand(DeviceCommands.DeviceCommand.Command.CALIBRATION_START).build())
                                    .build();
                            tx.write(request.toByteArray());
                            startPointPos.setText(startPointCoordinates);
                        }

                        if (view == waypointBtn) {
                            setwp = 3;
                            waypointBtn.setEnabled(false);

                            DeviceCommands.RobotRequest request = DeviceCommands.RobotRequest.newBuilder().setWaypoint(DeviceCommands.WaypointInformation.newBuilder().setX(wpX).setY(wpY).build()).build();
                            tx.write(request.toByteArray());
                            waypointPos.setText(wpCoordinates);
                        }

                        if (view == toggleBtn) {
                            if (toggleBtn.isChecked() == true) {
                                toggleBtn.setText("AUTO");
                                tx.manualAuto = true;
                                updateBtn.setEnabled(false);
                                Log.d("ArenaFragment....", "Auto Grid Update!!!!!!!!!!!!");
                            } else {
                                toggleBtn.setText("MANUAL");
                                tx.manualAuto = false;
                                updateBtn.setEnabled(true);
                                Log.d("ArenaFragment....", "Manual Grid Update!!!!!!!!!!!!");
                            }
                        }

                        if (view == updateBtn) {
                            arenaGrid.manualUpdateAll();
                        }

//                        if (view == calibrateBtn) {
////                            DeviceCommands.RobotRequest request = DeviceCommands.RobotRequest.newBuilder().setDeviceCommand(DeviceCommands.DeviceCommand.newBuilder().setCommand(DeviceCommands.DeviceCommand.Command.CALIBRATION).build()).build();
////                            tx.write(request.toByteArray());
//                            robot_status.setText("Calbrating...");
//                        }
                        if (view == clearGridBtn) {
                            arenaGrid.redrawArenaGrid();
                            robotX = 0;
                            robotY = 17;
                            arenaGrid.updateRobot(robotX, robotY, DeviceCommands.Direction.UP);
                           // tx.write(DeviceCommands.RobotRequest.newBuilder().setReposition(DeviceCommands.DevicePositionInformation.newBuilder().setX(robotX).setY(robotY).setDirection(DeviceCommands.Direction.UP).build()).build().toByteArray());

                            arenaGrid.updateWaypoint(99, 99); // FIXME WP 99,99???
                            arenaGrid.redrawArenaGrid();
                            fwdBtn.setEnabled(true);
                            backBtn.setEnabled(true);
                            leftBtn.setEnabled(true);
                            rightBtn.setEnabled(true);
                            arenaGrid.updateRobotStart(false);
                            arenaGrid.invalidate();
                            startRobotBtn.setEnabled(true);
                            startPointPos.setText("");
                            waypointPos.setText("");
//                            calibrateBtn.setEnabled(true);
                            started = false;
                            waypointBtn.setEnabled(false);
                            fastestPathBtn.setEnabled(true);
                            exploreBtn.setEnabled(true);
                            arrowText.setText("\n\n\n\n\n");
                            wpCoordinates = "(0,2)";
                            startPointCoordinates = "(0,2)";
                            robot_coordinates.setText("X-axis : 0\nY-axis : 2\nDirection : 0");
                            robot_status.setText("");
                            setwp = 0;

                        }

                        if (view == gyroBtn) {
                            if (gyroOnOff == false) {
                                gyroOnOff = true;
                                gyroBtn.setText("Gyroscope On");
                                Toast.makeText(getContext(), "Gyroscope turned on", Toast.LENGTH_SHORT).show();
                            } else if (gyroOnOff == true) {
                                gyroOnOff = false;
                                gyroBtn.setText("Gyroscope Off");
                                Toast.makeText(getContext(), "Gyroscope turned off", Toast.LENGTH_SHORT).show();
                            }
                        }

                        if (view == fwdBtn) {
                            DeviceCommands.RobotRequest request = DeviceCommands.RobotRequest.newBuilder().setDeviceCommand(DeviceCommands.DeviceCommand.newBuilder().setCommand(DeviceCommands.DeviceCommand.Command.FORWARD).build()).build();
                          //  tx.write(request.toByteArray());

                            robot_status.setText("Robot moving forward.");
                            arenaGrid.invalidate();
                        }

                        if (view == backBtn) {
                            DeviceCommands.RobotRequest request = DeviceCommands.RobotRequest.newBuilder().setDeviceCommand(DeviceCommands.DeviceCommand.newBuilder().setCommand(DeviceCommands.DeviceCommand.Command.BACKWARD).build()).build();
                           // tx.write(request.toByteArray());
                            robot_status.setText("Robot moving reverse.");
                            arenaGrid.invalidate();
                        }

                        if (view == leftBtn) {

                            DeviceCommands.RobotRequest request = DeviceCommands.RobotRequest.newBuilder().setDeviceCommand(DeviceCommands.DeviceCommand.newBuilder().setCommand(DeviceCommands.DeviceCommand.Command.LEFT).build()).build();
                           // tx.write(request.toByteArray());

                            robot_status.setText("Robot turing left.");
                            arenaGrid.invalidate();
                        }

                        if (view == rightBtn) {

                            DeviceCommands.RobotRequest request = DeviceCommands.RobotRequest.newBuilder().setDeviceCommand(DeviceCommands.DeviceCommand.newBuilder().setCommand(DeviceCommands.DeviceCommand.Command.RIGHT).build()).build();
                           // tx.write(request.toByteArray());

                            robot_status.setText("Robot turning right.");
                            arenaGrid.invalidate();
                        }
                    }
                }

        };

        grid.setOnTouchListener(tListener);

        clearGridBtn.setOnClickListener(listener);
        fastestPathBtn.setOnClickListener(listener);
        exploreBtn.setOnClickListener(listener);
        fwdBtn.setOnClickListener(listener);
        backBtn.setOnClickListener(listener);
        leftBtn.setOnClickListener(listener);
        rightBtn.setOnClickListener(listener);
        gyroBtn.setOnClickListener(listener);
        startRobotBtn.setOnClickListener(listener);
        toggleBtn.setOnClickListener(listener);
        updateBtn.setOnClickListener(listener);
        waypointBtn.setOnClickListener(listener);
//        calibrateBtn.setOnClickListener(listener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
       // inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Fragment TT = getFragmentManager().findFragmentByTag("BT Fragment");
        //BluetoothChatFragment tx = (BluetoothChatFragment) TT;

        switch (item.getItemId()) {
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
               // tx.insecureConnect();
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
               // tx.ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onStop() {
        super.onStop();
        // unregister sensor listeners to prevent the activity from draining the device's battery.
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // unregister sensor listeners to prevent the activity from draining the device's battery.
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // restore the sensor listeners when user resumes the application.
        initListeners();
    }

    // This function registers sensor listeners for the accelerometer, magnetometer and gyroscope.
    public void initListeners() {
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        float x = event.values[0];
        float y = event.values[1];

        DeviceCommands.DeviceCommand.Command newDirection = DeviceCommands.DeviceCommand.Command.STOP;
        if (Math.abs(x) > Math.abs(y)) {
            if (x < -2) {
                newDirection = DeviceCommands.DeviceCommand.Command.RIGHT;
            } else if (x > 2) {
                newDirection = DeviceCommands.DeviceCommand.Command.LEFT;
            }
        } else {
            if (y < -2) {
                newDirection = DeviceCommands.DeviceCommand.Command.FORWARD;
            } else if (y > 2) {
                newDirection = DeviceCommands.DeviceCommand.Command.BACKWARD;
            }
        }
        if (acceleratorDirection == newDirection) {
            // Don't send info if the direction is the same as the previous direction
            return;
        } else {
            acceleratorDirection = newDirection;

            // Only send if the new direction is not stop.
            if (acceleratorDirection == DeviceCommands.DeviceCommand.Command.STOP || !gyroOnOff) return;

            Fragment TT = getFragmentManager().findFragmentByTag("BT Fragment");
            //BluetoothChatFragment tx = (BluetoothChatFragment) TT;
           // tx.write(DeviceCommands.RobotRequest.newBuilder().setDeviceCommand(DeviceCommands.DeviceCommand.newBuilder().setCommand(acceleratorDirection).build()).build().toByteArray());

            robot_status.setText(commandStatusTextMapping.getOrDefault(acceleratorDirection, ""));
        }
    }

    // calculates orientation angles from accelerometer and magnetometer output
    public void calculateAccMagOrientation() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    // This function is borrowed from the Android reference
    // at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
    // It calculates a rotation vector from the gyroscope angular speed values.
    private void getRotationVectorFromGyro(float[] gyroValues,
                                           float[] deltaRotationVector,
                                           float timeFactor) {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float) Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if (omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix
        if (initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float) Math.sin(o[1]);
        float cosX = (float) Math.cos(o[1]);
        float sinY = (float) Math.sin(o[2]);
        float cosY = (float) Math.cos(o[2]);
        float sinZ = (float) Math.sin(o[0]);
        float cosZ = (float) Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f;
        xM[1] = 0.0f;
        xM[2] = 0.0f;
        xM[3] = 0.0f;
        xM[4] = cosX;
        xM[5] = sinX;
        xM[6] = 0.0f;
        xM[7] = -sinX;
        xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY;
        yM[1] = 0.0f;
        yM[2] = sinY;
        yM[3] = 0.0f;
        yM[4] = 1.0f;
        yM[5] = 0.0f;
        yM[6] = -sinY;
        yM[7] = 0.0f;
        yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ;
        zM[1] = sinZ;
        zM[2] = 0.0f;
        zM[3] = -sinZ;
        zM[4] = cosZ;
        zM[5] = 0.0f;
        zM[6] = 0.0f;
        zM[7] = 0.0f;
        zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    public void updateOrientationDisplay() {
        pitchValue = d.format(gyroOrientation[1] * 180 / Math.PI);
        rollValue = d.format(gyroOrientation[2] * 180 / Math.PI);

        Fragment TT = getFragmentManager().findFragmentByTag("BT Fragment");
        //BluetoothChatFragment tx = (BluetoothChatFragment) TT;

        if (gyroOnOff == true) {

            if (Float.valueOf(pitchValue) > 60.0) {
                //tx.write(DeviceCommands.RobotRequest.newBuilder().setDeviceCommand(DeviceCommands.DeviceCommand.newBuilder().setCommand(DeviceCommands.DeviceCommand.Command.FORWARD).build()).build().toByteArray());
                robot_status.setText("Robot moving forward.");
            } else if (Float.valueOf(pitchValue) < -60.0) {
                tx.write(DeviceCommands.RobotRequest.newBuilder().setDeviceCommand(DeviceCommands.DeviceCommand.newBuilder().setCommand(DeviceCommands.DeviceCommand.Command.BACKWARD).build()).build().toByteArray());
                robot_status.setText("Robot turning back.");
            }

            if (Float.valueOf(rollValue) > 60.0) {
              //  tx.write(DeviceCommands.RobotRequest.newBuilder().setDeviceCommand(DeviceCommands.DeviceCommand.newBuilder().setCommand(DeviceCommands.DeviceCommand.Command.RIGHT).build()).build().toByteArray());
                robot_status.setText("Robot turing right.");
            } else if (Float.valueOf(rollValue) < -60.0) {
              //  tx.write(DeviceCommands.RobotRequest.newBuilder().setDeviceCommand(DeviceCommands.DeviceCommand.newBuilder().setCommand(DeviceCommands.DeviceCommand.Command.LEFT).build()).build().toByteArray());
                robot_status.setText("Robot turning left.");
            }
        }
    }

    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;

            /*
             * Fix for 179° <--> -179° transition problem:
             * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
             * If so, add 360° (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360° from the result
             * if it is greater than 180°. This stabilizes the output in positive-to-negative-transition cases.
             */

            // azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
            } else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
            } else {
                fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
            }

            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            } else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            } else {
                fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
            }

            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            } else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            } else {
                fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
            }

            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);

            // update sensor output in GUI
            mHandler.post(updateOrientationDisplayTask);
        }
    }
}