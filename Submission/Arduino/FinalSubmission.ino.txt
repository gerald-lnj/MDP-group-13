#include <MsTimer2.h>
#include <PID_v1.h>
#include <EnableInterrupt.h>
#include <DualVNH5019MotorShield.h>
#include <SharpIR.h>

#define SRmodel GP2Y0A21YK0F
#define LRmodel GP2Y0A02YK0F

double getDistance;

SharpIR sr1(SharpIR::SRmodel, A0); //Front Left
SharpIR sr2(SharpIR::SRmodel, A1); //Front Centre
SharpIR sr3(SharpIR::SRmodel, A2); //Front Right
SharpIR sr4(SharpIR::SRmodel, A3); //Right SR
SharpIR sr5(SharpIR::LRmodel, A4); //Right LR
SharpIR sr0(SharpIR::LRmodel, A5); //Left LR

const int LEFT_PULSE = 11; // LEFT M1 Pulse
const int RIGHT_PULSE = 3; // RIGHT M2 Pulse
int MOVE_MAX_SPEED = 300; //Change to 350 for FSP
const int TURN_MAX_SPEED = 250;
int DELAY_CONSTANT = 1000; //Change to 100 for FSP
int TURN_TICKS_R = 775; //ideal 780
int TURN_TICKS_L = 790; //ideal 780
const int TICKS[8] = {0, 570, 1160, 1760, 2370, 2970, 3570, 4180};
const double kp = 7.25, ki = 1.25, kd = 0;

double tick_R = 0;
double tick_L = 0;
double speed_O = 0;
double previous_tick_R = 0;
double previous_error = 0;

boolean caliFrontFlag = true;
boolean caliRightFlag = true;
boolean printFlag = false;

DualVNH5019MotorShield md;
PID myPID(&tick_R, &speed_O, &tick_L, kp, ki, kd, REVERSE);

void setup() {
  // put your setup code here, to run once:
  delay(500);
  Serial.begin(115200);
  setupMotorEncoder();
  setupPID();
  delay(500);
}

void loop() {
  // put your main code here, to run repeatedly:
  char cc;
  if (printFlag == true)
  {
    printSensors();
  }
  if (Serial.available() > 0)
  {
    cc = char(Serial.read());
    readRobotCommands(cc);
  }
}

//-------------------------Run Codes-------------------------

void printSensors(){
  String sendData = "al:COMPUTE:"+getBlock(1)+"-"+getBlock(2)+"-"+getBlock(3)+"-"+getBlock(4)+"-"+getBlock(5)+"-"+getBlock(0);
  while(printFlag == true){
    Serial.println(sendData);
    printFlag = false;
  }
}

void readRobotCommands(char command){
  switch(command){
    case 'S': //Return sensor readings
      printFlag = true;
      break;
      
    case 'W': //Move forward by 1 grid
      moveForwardByGrid(1);
      //printFlag = true;
      break;

    case '2': //Move forward by 2 grids
      moveForwardByGrid(2);
      //printFlag = true;
      break;

    case '3': //Move forward by 3 grids
      moveForwardByGrid(3);
      //printFlag = true;
      break;

    case '4': //Move forward by 4 grids
      moveForwardByGrid(4);
      //printFlag = true;
      break;

    case '5': //Move forward by 5 grids
      moveForwardByGrid(5);
      //printFlag = true;
      break;

    case '6': //Move forward by 6 grids
      moveForwardByGrid(6);
      //printFlag = true;
      break;

    case '7': //Move forward by 7 grids
      moveForwardByGrid(7);
      //printFlag = true;
      break;
      
    case 'A': //Turn left
      turnLeft();
      //printFlag = true;
      break;
      
    case 'D': //Turn right
      turnRight();
      //printFlag = true;
      break;

    case 'B': //Turn right
      turnRight();
      turnRight();
      //printFlag = true;
      break;

    case 'C': //Calibrate front
      caliFront();
      //caliFront();
      break;

    case 'R': //Calibrate right
      caliRight();
      //caliRight();
      break;

    case 'L': //Calibrate front & right
      caliRight();
      caliFront();
      break;

    case 'I': //Initial set-up 
      caliFront();
      turnLeft();
      caliFront();
      turnLeft();
      md.setSpeeds(-150,150);
      delay(100);
      initializeMotor_End();
      delay(100);
      printFlag = true;
      break;

    case 'Z': //Final stop
      printFlag = false;
      MOVE_MAX_SPEED = 375;
      DELAY_CONSTANT = 100;
      TURN_TICKS_R = TURN_TICKS_R - 5;
      TURN_TICKS_L = TURN_TICKS_L + 5;
      delay(200);
      break;

    default:
      break;
  }
}

//-------------------------Motor Codes-------------------------
void setupMotorEncoder() {
  md.init();
  pinMode(LEFT_PULSE, INPUT);
  pinMode(RIGHT_PULSE, INPUT);
  enableInterrupt(LEFT_PULSE, leftMotorTime, CHANGE);
  enableInterrupt(RIGHT_PULSE, rightMotorTime, CHANGE);
}

void stopMotorEncoder() {
  disableInterrupt(LEFT_PULSE);
  disableInterrupt(RIGHT_PULSE);
}

void setupPID() {
  myPID.SetMode(AUTOMATIC);
  myPID.SetOutputLimits(-370, 370);
  myPID.SetSampleTime(5);
}

void moveForwardByGrid(int grid){
  initializeTick();
  initializeMotor_Start();

  int target_count = TICKS[grid];  
  double currentSpeed = MOVE_MAX_SPEED;
  
  double offset = 0;
  int last_tick_R = 0;
  while (tick_R <= target_count || tick_L <= target_count) {
    if ((tick_R - last_tick_R) >= 10 || (tick_R == 0) || (tick_R == last_tick_R)){
      last_tick_R = tick_R;
      offset += 0.1;
    }
    if (myPID.Compute() || (tick_R == last_tick_R)){
      if (offset >= 1)
        md.setSpeeds((currentSpeed + speed_O), -(currentSpeed - speed_O));
      else
        md.setSpeeds(offset * (currentSpeed + speed_O), -(offset * (currentSpeed - speed_O)));
    }
  }
  initializeMotor_End();
}

void turnRight() {
  initializeTick();
  initializeMotor_Start();
  double currentSpeed = TURN_MAX_SPEED;
  double offset = 0;

  while (tick_R < TURN_TICKS_R || tick_L < TURN_TICKS_R) {
    //    offset = computePID();
    if (myPID.Compute())
      md.setSpeeds(-(currentSpeed + speed_O), -(currentSpeed - speed_O));
  }
  initializeMotor_End();
}

void turnLeft() {
  initializeTick();
  initializeMotor_Start();
  double currentSpeed = TURN_MAX_SPEED;
  double offset = 0;

  while (tick_R < TURN_TICKS_L || tick_L < TURN_TICKS_L) {
    //    offset = computePID();
    if (myPID.Compute())
      md.setSpeeds((currentSpeed + speed_O), (currentSpeed - speed_O));
  }
  initializeMotor_End();
}

void leftMotorTime() {
  tick_L++;
}

void rightMotorTime() {
  tick_R++;
}

void initializeTick() {
  tick_R = 0;
  tick_L = 0;
  speed_O = 0;
  previous_tick_R = 0;
}

void initializeMotor_Start() {
  md.setSpeeds(0, 0);
  md.setBrakes(0, 0);
}

void initializeMotor_End() {
  md.setSpeeds(0, 0);
  md.setBrakes(400, 400);
  delay(DELAY_CONSTANT);
}

//-------------------------Calibrate Codes-------------------------

void calibrate() {
  int SPEEDL = 100;
  int SPEEDR = 100;
  int count = 0;
  
  while(count != 100)
{
    if((getAverageDistance(1) >= 10.7 && getAverageDistance(1) < 10.9)||(getAverageDistance(3) >= 10.7 && getAverageDistance(3) < 10.9)){
      md.setBrakes(150, 150);
      break;
    }
    else if(getAverageDistance(1) <= 10.7 || getAverageDistance(3) <= 10.7)
    {
      md.setSpeeds(-SPEEDR, SPEEDL);
      count++;
    }
    else {
      md.setSpeeds(SPEEDR, -SPEEDL);
      count++;
    }
  }
  md.setBrakes(150, 150);
}

void stepBack(){
  int SPEEDL = 100;
  int SPEEDR = 100;
  while(getAverageDistance(1)<12 || getAverageDistance(3)<12){
    md.setSpeeds(-SPEEDR,SPEEDL);
  }
  md.setBrakes(150,150);
}

void caliFront()
{
  stepBack();
  delay(100);
  double targetDist = 10.8;
  int SPEEDL = 100;
  int SPEEDR = 100;
  
  double distDiff = getAverageDistance(1) - getAverageDistance(3);
  while (abs(distDiff)>0.1)
  {
    double distDiff = getAverageDistance(1) - getAverageDistance(3);
    
    if(abs(distDiff)>=0.1){
      if ((distDiff >= 0.1) && (getAverageDistance(1) >= targetDist)) {
        md.setSpeeds(0, -SPEEDL); //left forward
      }
      else if ((distDiff <= -0.1) && (getAverageDistance(1) < targetDist)) {
        md.setSpeeds(0, SPEEDL); //left backward
      }
      else if ((distDiff >= 0.1) && (getAverageDistance(1) < targetDist)) {
        md.setSpeeds(-SPEEDR, 0); //right backward
      }
      else if ((distDiff <= -0.1) && (getAverageDistance(1) >= targetDist)) {
        md.setSpeeds(SPEEDR, 0); //right forward
      }
    }
    else{
      break;
    }
  }
  
  md.setBrakes(150, 150);
  caliFrontFlag = false;
  delay(100);
  calibrate();
  delay(100); 
}

void caliRight(){
  turnRight();
  caliFront();
  turnLeft();
}

//-------------------------Sensor Codes-------------------------

double getAverageDistance(int sensor) {
  double sum = 0;
  double average = 0;
  
  //Front Left SR
  if (sensor == 1) {
    //Get sum of 20 values
    for (int i = 0; i < 20; i++) {
      sum = sum + sr1.getDistance();
    }
    average = sum / 20; 
    
    //Serial.print("Front Left SR Distance: ");
    //Serial.println(average);

    return (average);
  } 

  //Front Center SR
  if (sensor == 2) {
    //Get sum of 20 values
    for (int i = 0; i < 20; i++) {
      sum = sum + sr2.getDistance();
    }
    average = sum / 20;

    //Serial.print("Front Center SR Distance: ");
    //Serial.println(average);

    return average;
  }
  
  //Front Right SR
  if (sensor == 3) {
    //Get sum of 20 values
    for (int i = 0; i < 20; i++) {
      sum = sum + sr3.getDistance();
    }
    average = sum / 20; 

    //Serial.print("Front Right SR Distance: ");
    //Serial.println(average);

    return average;
  }
  
  //Right SR
  if (sensor == 4) {
    //Get sum of 20 values
    for (int i = 0; i < 20; i++) {
      sum = sum + sr4.getDistance();
    }
    average = sum / 20;

    //Serial.print("Right SR Distance: ");
    //Serial.println(average);

    return average;
  }
  
  // Right LR
  if (sensor == 5) {
    //Get sum of 20 values
    for (int i = 0; i < 20; i++) {
      sum = sum + sr5.getDistance();
    }
    average = sum / 20;
    
    //Serial.print("Right LR Distance: ");
    //Serial.println(average);
    return average;
  }

  // Left LR (not complete & calibrated)
  if (sensor == 0) {
    //Get sum of 20 values
    for (int i = 0; i < 20; i++) {
      sum = sum + sr0.getDistance();
    }
    average = sum / 20;
    
    //Serial.print("Left LR Distance: ");
    //Serial.println(average);
    return average;
  }
}

String getBlock(int sensor){
  double dist = 0;

  //Front Left SR
  if(sensor == 1){
    dist = getAverageDistance(1);
    
    if(9 <= dist && dist <= 14){
      //Serial.println("Front Left SR Block: 1");
      return "1";
    }
    else if(dist > 14 && dist <= 24){
      //Serial.println("Front Left SR Block: 2");
      return "2";
    }
    else if(dist > 24 && dist <= 34){
      //Serial.println("Front Left SR Block: 3");
      return "3";
    }
    else{
      //Serial.println("Front Left SR Block: 9");
      return "9";
    }
  } 

  //Front Centre SR
  if(sensor == 2){
    dist = getAverageDistance(2);

    if(9 <= dist && dist <= 14.5){
      //Serial.println("Front Center SR Block: 1");
      return "1";
    }
    else if(dist > 14.5 && dist <=24){
      //Serial.println("Front Center SR Block: 2");
      return "2";
    }
    else if(dist > 24 && dist <=34.5){
      //Serial.println("Front Center SR Block: 3");
      return "3";
    }
    else{
      //Serial.println("Front Center SR Block: 9");
      return "9";
    }
  }

  //Front Right SR
  if(sensor == 3){
    dist = getAverageDistance(3);
    
    if(9 <= dist && dist <= 15.5){
      //Serial.println("Front Right SR Block: 1");
      return "1";
    }
    else if(dist > 15.5 && dist <=25){
      //Serial.println("Front Right SR Block: 2");
      return "2";
    }
    else if(dist > 25 && dist <=30.5){
      //Serial.println("Front Right SR Block: 3");
      return "3";
    }
    else{
      //Serial.println("Front Left SR Block: 9");
      return "9";
    }
  }

  //Right SR
  if(sensor == 4){
    dist = getAverageDistance(4);

    if(9 <= dist && dist <= 15){
      //Serial.println("Right SR Block: 1");
      return "1";
    }
    else if(dist > 15 && dist <= 24){
      //Serial.println("Right SR Block: 2");
      return "2";
    }
    else if(dist > 24 && dist <= 34){
      //Serial.println("Right SR Block: 3");
      return "3";
    }
    else{
      //Serial.println("Right SR Block: 9");
      return "9";
    }
  }

  //Right LR
  if(sensor == 5){
    dist = getAverageDistance(5);

    if (dist >= 19 && dist <= 20) {
      //Serial.println("Left LR Distance: 1");
      return "1";
    }
    else if (dist > 20 && dist <= 25) {
      //Serial.println("Left LR Distance: 2");
      return "2";
    }
    else if (dist > 25 && dist <= 33) {
      //Serial.println("Left LR Distance: 3");
      return "3";
    }
    else if (dist > 33 && dist <= 39) {
      //Serial.println("Left LR Distance: 4");
      return "4";
    }
    else if (dist > 39 && dist <= 42) {
      //Serial.println("Left LR Distance: 5");
      return "5";
    }
    else {
      //Serial.println("Left LR Distance: 9");
      return "9";
    }
  }

  //Left LR
  if(sensor == 0){
    dist = getAverageDistance(0);

    if (dist >= 19 && dist <= 19.9 ) {
      //Serial.println("Left LR Distance: 1");
      return "1";
    }
    else if (dist > 19.9 && dist <= 25) {
      //Serial.println("Left LR Distance: 2");
      return "2";
    }
    else if (dist > 25 && dist <= 33) {
      //Serial.println("Left LR Distance: 3");
      return "3";
    }
    else if (dist > 33 && dist <= 39.5) {
      //Serial.println("Left LR Distance: 4");
      return "4";
    }
    else if (dist > 39.5 && dist <= 46.5) {
      //Serial.println("Left LR Distance: 5");
      return "5";
    }
    else {
      //Serial.println("Left LR Distance: 9");
      return "9";
    }
  } 
}
