#include <MsTimer2.h>
#include <PID_v1.h> //#include <ArduinoPIDLibrary.h>
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
SharpIR sr5(SharpIR::SRmodel, A4); //Right LR
SharpIR sr0(SharpIR::LRmodel, A5); //Left LR

const int LEFT_PULSE = 11; // LEFT M1 Pulse
const int RIGHT_PULSE = 3; // RIGHT M2 Pulse
const int MOVE_FAST_SPEED = 375;
const int MOVE_MAX_SPEED = 300;
const int MOVE_MIN_SPEED = 250;
const int TURN_MAX_SPEED = 250;
const int ROTATE_MAX_SPEED = 150;
const int TURN_TICKS_R = 790;
const int TURN_TICKS_L = 785;
const int TICKS[9] = {0, 570, 1165, 1780, 2380, 2985, 3600, 4195, 4785};
const double DIST_WALL_CENTER_BOX = 1.58;
const double kp = 7.25, ki = 1.25, kd = 0; // 7.35, 1.25, 0

int TENCM_TICKS_OFFSET = 0;

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
  delay(1000);
  Serial.begin(115200);
  setupMotorEncoder();
  setupPID();
  delay(20);
  /*for(int i=0; i<12; i++){
    //turnLeft();
    turnRight();
    delay(1000);
  }*/
  /*for(int i=0; i<2; i++){
    moveForwardByGrid(4
    );
    delay(500);
  }*/
  //moveForwardByGrid(8);
  //moveForward(80);
  //moveForward(120);
  //delay(1000);
  //kcaliFront();
  
}

void loop() {
  // put your main code here, to run repeatedly:
  /*char cc;
  if (printFlag == true)
  {
    printSensors();
  }
  if (Serial.available() > 0)
  {
    cc = char(Serial.read());
    readRobotCommands(cc);
  }*/
}

//-------------------------Communication Codes-------------------------
void printSensors(){
  String sendData = "0"+getBlock(1)+"-1"+getBlock(2)+"-2"+getBlock(3)+"-3"+getBlock(4)+"-4"+getBlock(5)+"-5"+getBlock(0);
  while(printFlag == true){
    Serial.println(sendData);
    printFlag = false;
  }
}

void readRobotCommands(char command){
  switch(command){
    case '1': //Move forward by 1 grid
      moveForwardByGrid(1);
      printFlag = true;
      break;

    case '2': //Move forward by 2 grids
      moveForwardByGrid(2);
      printFlag = true;
      break;

    case '3': //Move forward by 3 grids
      moveForwardByGrid(3);
      printFlag = true;
      break;

    case '4': //Move forward by 4 grids
      moveForwardByGrid(4);
      printFlag = true;
      break;

    case '5': //Move forward by 5 grids
      moveForwardByGrid(5);
      printFlag = true;
      break;

    case '6': //Move forward by 6 grids
      moveForwardByGrid(6);
      printFlag = true;
      break;

    case '7': //Move forward by 7 grids
      moveForwardByGrid(7);
      printFlag = true;
      break;
      
    case 'L': //Turn left
      turnLeft();
      printFlag = true;
      break;
      
    case 'R': //Turn right
      turnRight();
      printFlag = true;
      break;

    case 'C': //Calibrate front
      caliFront();
      caliFront();
      delay(200);
      break;

    case 'c': //Calibrate right
      caliRight();
      caliRight();
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

  //float num_rev = (distance * 10) / (PI * 60);  // Convert to mm
  //target_count = num_rev * 562 * 2;
  
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

    /*if(myPID.Compute()){
      md.setSpeeds(currentSpeed + speed_O, -(currentSpeed - speed_O));
    }*/
    
    //Serial.print("R: ");
    //Serial.print(tick_R);
    //Serial.print(" L: ");
    //Serial.print(tick_L);
    //Serial.print(" S: ");
    //Serial.println(speed_O);
    
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
  initializeRightTurnEnd();
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
  initializeLeftTurnEnd();
}

void rotateRightLess90(double degree){
  initializeTick();
  initializeMotor_Start();
  double currentSpeed = ROTATE_MAX_SPEED;
  double offset = 0;
  double rotate_ticks_r = (degree / 90 * TURN_TICKS_R);
  while (tick_R < rotate_ticks_r || tick_L < rotate_ticks_r) {
    //    offset = computePID();
    if (myPID.Compute())
      md.setSpeeds(-(currentSpeed + speed_O), -(currentSpeed - speed_O));
  }
  initializeMotor_End();
  initializeLeftTurnEnd();
}

void rotateLeftLess90(double degree){
  initializeTick();
  initializeMotor_Start();
  double currentSpeed = ROTATE_MAX_SPEED;
  double offset = 0;
  double rotate_ticks_l = (degree / 90 * TURN_TICKS_L);
  while (tick_R < rotate_ticks_l || tick_L < rotate_ticks_l) {
    //    offset = computePID();
    if (myPID.Compute())
      md.setSpeeds((currentSpeed + speed_O), (currentSpeed - speed_O));
  }
  initializeMotor_End();
  initializeLeftTurnEnd();
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
  delay(5);
}

void initializeRightTurnEnd() {
  //  rotateRight(8);
  //  rotateLeft(6);
}


void initializeLeftTurnEnd() {
  //  rotateLeft(8);
  //  rotateRight(6);
}

//-------------------------Calibrate Codes-------------------------

void calibrate() {
  int SPEEDL = 100;
  int SPEEDR = 100;
  int count = 0;
  
  while(count != 50)
  {
    if((getAverageDistance(1) >= 10.85 && getAverageDistance(1) < 11.15)||(getAverageDistance(3) >= 10.85 && getAverageDistance(3) < 11.15)){
      md.setBrakes(100, 100);
      break;
    }
    else if(getAverageDistance(1) <= 10.8 || getAverageDistance(3) <= 10.8)
    {
      md.setSpeeds(-SPEEDR, SPEEDL);
      count++;
    }
    else {
      md.setSpeeds(SPEEDR, -SPEEDL);
      count++;
    }
  }
  md.setBrakes(100, 100);
}

void stepBack(){
  int SPEEDL = 100;
  int SPEEDR = 100;
  while(getAverageDistance(1)<12 || getAverageDistance(3)<12){
    md.setSpeeds(-SPEEDR,SPEEDL);
  }
  md.setBrakes(100,100);
}

void caliFront()
{
  stepBack();
  //Serial.println("caliFront() called");
  double targetDist = 11.0;
  int SPEEDL = 100;
  int SPEEDR = 100;
  
  double distDiff = getAverageDistance(1) - getAverageDistance(3);
  while (abs(distDiff)>0.1)
  {
    double distDiff = getAverageDistance(1) - getAverageDistance(3);
    Serial.print("distDiff: ");
    Serial.println(distDiff);
    
    if(abs(distDiff)>=0.1){
      if ((distDiff >= 0.1) && (getAverageDistance(1) >= targetDist)) {
        md.setSpeeds(0, -SPEEDL); //left forward
        Serial.println("left forward executed");
      }
      else if ((distDiff <= -0.1) && (getAverageDistance(1) < targetDist)) {
        md.setSpeeds(0, SPEEDL); //left backward
        Serial.println("left backward executed");
      }
      else if ((distDiff >= 0.1) && (getAverageDistance(1) < targetDist)) {
        md.setSpeeds(-SPEEDR, 0); //right backward
        Serial.println("right backward executed");
      }
      else if ((distDiff <= -0.1) && (getAverageDistance(1) >= targetDist)) {
        md.setSpeeds(SPEEDR, 0); //right forward
        Serial.println("right forward executed");
      }
    }
    else{
      break;
    }
  }
  
  md.setBrakes(100, 100);
  caliFrontFlag = false;
  calibrate(); 
}

void caliRight(){
  //Serial.println("caliRight() called");
  double targetDist = 9.0;
  int SPEEDL = 70;
  int SPEEDR = 70;
  int count = 0;
  double diff = getAverageDistance(4) - getAverageDistance(5);
  while((abs(diff)>0.2) && (getBlock(4) == getBlock(5)) && count < 10){
    count++;
    if(diff > 0.2){
      rotateRightLess90(abs(diff)*5);
      diff = getAverageDistance(4) - getAverageDistance(5);
      if(getBlock(4)!=getBlock(5)){
        rotateLeftLess90(abs(diff)*4);
      }
    }
    else{
      rotateLeftLess90(abs(diff)*5);
      diff = getAverageDistance(4) - getAverageDistance(5);
      if(getBlock(4)!=getBlock(5)){
        rotateRightLess90(abs(diff)*4);
      }
    }
    
    diff = getAverageDistance(4) - getAverageDistance(5);
  }
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
    
    Serial.print("Front Left SR Distance: ");
    Serial.println(average);

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

    Serial.print("Front Right SR Distance: ");
    Serial.println(average);

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
    
    if(9 <= dist && dist <= 19){ //11 is first point in first grid
      //Serial.println("Front Left SR Block: 1");
      return "1";
    }
    else if(dist > 19 && dist <= 29){
      //Serial.println("Front Left SR Block: 2");
      return "2";
    }
    else if(dist > 29 && dist <= 39){
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

    if(9 <= dist && dist <= 17){ //9 is first point in first grid
      //Serial.println("Front Center SR Block: 1");
      return "1";
    }
    else if(dist > 17 && dist <=26){
      //Serial.println("Front Center SR Block: 2");
      return "2";
    }
    else if(dist > 26 && dist <=35){
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
    
    if(9 <= dist && dist <= 20){ //11 is first point in first grid
      //Serial.println("Front Right SR Block: 1");
      return "1";
    }
    else if(dist > 20 && dist <=28){
      //Serial.println("Front Right SR Block: 2");
      return "2";
    }
    else if(dist > 28 && dist <=33){
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

    if(9 <= dist && dist <= 18){
      //Serial.println("Right SR Block: 1");
      return "1";
    }
    else if(dist > 18 && dist <= 29){
      //Serial.println("Right SR Block: 2");
      return "2";
    }
    else if(dist > 29 && dist <= 50){
      //Serial.println("Right SR Block: 3");
      return "3";
    }
    else{
      //Serial.println("Right SR Block: 9");
      return "9";
    }
  }

  if(sensor == 5){
    dist = getAverageDistance(5);

    if (dist >= 9 && dist <= 11) {
      //Serial.println("Right LR Distance: 1");
      return "1";
    }
    else if (dist > 11 && dist <= 14.5) {
      //Serial.println("Right LR Distance: 2");
      return "2";
    }
    else if (dist > 14.5 && dist <= 18) {
      //Serial.println("Right LR Distance: 3");
      return "3";
    }
    else if (dist > 18 && dist <= 25) {
      //Serial.println("Right LR Distance: 4");
      return "4";
    }
    else if (dist > 25 && dist <= 29) {
      //Serial.println("Right LR Distance: 5");
      return "5";
    }
    else {
      //Serial.println("Right LR Distance: 9");
      return "9";
    }
  }

  if(sensor == 0){
    dist = getAverageDistance(0);

    if (dist >= 19 && dist <= 22 ) {
      //Serial.println("Left LR Distance: 1");
      return "1";
    }
    else if (dist > 22 && dist <= 28) {
      //Serial.println("Left LR Distance: 2");
      return "2";
    }
    else if (dist > 28 && dist <= 35) {
      //Serial.println("Left LR Distance: 3");
      return "3";
    }
    else if (dist > 35 && dist <= 43) {
      //Serial.println("Left LR Distance: 4");
      return "4";
    }
    else if (dist > 43 && dist <= 54) {
      //Serial.println("Left LR Distance: 5");
      return "5";
    }
    else {
      //Serial.println("Left LR Distance: 9");
      return "9";
    }
  } 
}
