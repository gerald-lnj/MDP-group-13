from time import sleep
from datetime import datetime
from picamera import PiCamera
from picamera.array import PiRGBArray

def savePhoto(coordinates_x, coordinates_y, orientation):
    camera = PiCamera()
    camera.resolution = (600, 600)
    camera.start_preview()
    # Camera warm-up time
    filename = '{}-{}-{}.jpg'.format(coordinates_x, coordinates_y, orientation)
    camera.capture('Raspberry Pi/Image Recognition/Query/'+filename)
    print('took a photo!')

def takePhoto():
    # initialize the camera and grab a reference to the raw camera capture
    camera = PiCamera()
    camera.resolution = (600, 600)
    rawCapture = PiRGBArray(camera)
    
    # allow the camera to warmup
    sleep(0.1)
    
    # grab an image from the camera
    camera.capture(rawCapture, format="bgr")
    image = rawCapture.array
    return image