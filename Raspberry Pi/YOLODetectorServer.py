# USAGE
# python yolo_video.py --input videos/airport.mp4 --output output/airport_output.avi --yolo yolo-coco


# import the necessary packages
from imutils import build_montages
import numpy as np
import argparse
import imutils
import time
import cv2
import os
import shlex
import imagezmq
from datetime import datetime

result_list = [
	'', # since official IDs start from 1, not 0
	'White Arrow',
	'Red Arrow',
	'Green Arrow',
	'Blue Arrow',
	'Yellow Circle',
	'Blue 1',
	'Green 2',
	'Red 3',
	'White 4',
	'Yellow 5',
	'Red A',
	'Green B',
	'White C',
	'Blue D',
	'Yellow E',
]

# initialize the ImageHub object
imageHub = imagezmq.ImageHub();
folderPath = os.path.dirname(os.path.abspath(__file__))

# load the COCO class labels our YOLO model was trained on
labelsPath =os.path.join(folderPath, "Image Recognition/rpi.names")
LABELS = open(labelsPath).read().strip().split("\n")

# initialize a list of colors to represent each possible class label
np.random.seed(42)
COLORS = np.random.randint(0, 255, size=(len(LABELS), 3),
	dtype="uint8")

# derive the paths to the YOLO weights and model configuration
weightsPath = os.path.sep.join([folderPath, "Image Recognition/rpi_best.weights"])
configPath = os.path.sep.join([folderPath, "Image Recognition/rpi.cfg"])

# load our YOLO object detector trained on COCO dataset (80 classes)
# and determine only the output layer names that we need from YOLO
print("[INFO] loading YOLO from disk...")
net = cv2.dnn.readNetFromDarknet(configPath, weightsPath)
ln = net.getLayerNames()
ln = [ln[i[0] - 1] for i in net.getUnconnectedOutLayers()]

# loop over frames from the video file stream
while True:
	# receive RPi name and frame from the RPi and acknowledge
	# the receipt
	(rpiName, frame) = imageHub.recv_image()

	# resize the frame to have a maximum width of 400 pixels, then
	# grab the frame dimensions and construct a blob
	frame = imutils.resize(frame, width=640)
	(H, W) = frame.shape[:2]
	# construct a blob from the input frame and then perform a forward
	# pass of the YOLO object detector, giving us our bounding boxes
	# and associated probabilities
	blob = cv2.dnn.blobFromImage(frame, 1 / 255.0, (480, 480),
		swapRB=True, crop=False)
	net.setInput(blob)
	start = time.time()
	layerOutputs = net.forward(ln)
	end = time.time()

	# initialize our lists of detected bounding boxes, confidences,
	# and class IDs, respectively
	boxes = []
	confidences = []
	classIDs = []

	# loop over each of the layer outputs
	for output in layerOutputs:
		# loop over each of the detections
		for detection in output:
			# extract the class ID and confidence (i.e., probability)
			# of the current object detection
			scores = detection[5:]
			classID = np.argmax(scores)
			confidence = scores[classID]

			# filter out weak predictions by ensuring the detected
			# probability is greater than the minimum probability
			if confidence > 0.8:
				# scale the bounding box coordinates back relative to
				# the size of the image, keeping in mind that YOLO
				# actually returns the center (x, y)-coordinates of
				# the bounding box followed by the boxes' width and
				# height
				box = detection[0:4] * np.array([W, H, W, H])
				(centerX, centerY, width, height) = box.astype("int")

				# use the center (x, y)-coordinates to derive the top
				# and and left corner of the bounding box
				x = int(centerX - (width / 2))
				y = int(centerY - (height / 2))

				# update our list of bounding box coordinates,
				# confidences, and class IDs
				boxes.append([x, y, int(width), int(height)])
				confidences.append(float(confidence))
				classIDs.append(classID)

	# apply non-maxima suppression to suppress weak, overlapping
	# bounding boxes
	idxs = cv2.dnn.NMSBoxes(boxes, confidences, 0.5, 0.3)

	# ensure at least one detection exists
	if len(idxs) > 0:
		# loop over the indexes we are keeping
		for i in idxs.flatten():
			# extract the bounding box coordinates
			imgID = LABELS[classIDs[i]]
			imgName = result_list[int(imgID)]
			confidence = confidences[i]
			text = 'detected ID {} ({}), confidence {}'.format(imgID, imgName, confidence)
			print(text)
			reply = bytes(imgID, 'utf-8')
			imageHub.send_reply(reply)
	else:
		print('Nothing detected')
		reply = bytes('', 'utf-8')
		imageHub.send_reply(reply)

			