#!/user/bin/env python
import serial
port = "/dev/ttyACM0"

s1 = serial.Serial(port, 115200)
s1.flushInput()

while True:
    if s1.inWaiting()>0:
        inputValue = s1.readline()
        print(inputValue)