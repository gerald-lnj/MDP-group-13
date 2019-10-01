import time
import Picamera
import datetime

cam = picamera.Picamera()

filename = datetime.datetime.now()
print(filename)
cam.capture(filename)
print("CAPTURED {}".format(filename))