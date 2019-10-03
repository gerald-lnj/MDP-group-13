# USAGE
# python client.py --server-ip SERVER_IP
import sys
# import the necessary packages
from imutils.video import VideoStream
import imagezmq
import argparse
import socket
import time
import os
import cv2

os.system('sudo modprobe bcm2835-v4l2') 

folderPath = os.path.dirname(os.path.abspath(__file__))

# construct the argument parser and parse the arguments
ap = argparse.ArgumentParser()
ap.add_argument("-s", "--server-ip", required=True,
	help="ip address of the server to which the client will connect")
args = vars(ap.parse_args())

# initialize the ImageSender object
# with the socket address of the server (5555)
sender = imagezmq.ImageSender(connect_to="tcp://{}:5555".format(
	args["server_ip"]))

# get the host name, initialize the video stream,
# and allow the camera sensor to warmup
rpiName = socket.gethostname()
vs = VideoStream(src=-1,usePiCamera=False).start()
#vs = VideoStream(src=0).start()
time.sleep(2.0)
 

def send_image(image):
	sender.send_image(rpiName, image)

for entry in os.scandir('{}/Query'.format(folderPath)):
	# read image from query folder
	image = cv2.imread(entry.path)
	print(send_image(image))
