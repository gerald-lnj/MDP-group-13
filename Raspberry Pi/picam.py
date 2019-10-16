from time import sleep
from datetime import datetime
from picamera import PiCamera
from picamera.array import PiRGBArray

camera = PiCamera()
camera.resolution = (640, 640)
camera.rotation = 180
rawCapture = PiRGBArray(camera)

def savePhoto(coordinates_x, coordinates_y, orientation):
    # Camera warm-up time
    camera.start_preview()
    filename = '{}-{}-{}.jpg'.format(coordinates_x, coordinates_y, orientation)
    camera.capture(filename)
    print('took a photo!')

def takePhoto():
    # initialize the camera and grab a reference to the raw camera capture
    # allow the camera to warmup
    camera.start_preview()
    # grab an image from the camera
    camera.capture(rawCapture, format="bgr")
    rawCapture.truncate(0)
    image = rawCapture.array
    return image
