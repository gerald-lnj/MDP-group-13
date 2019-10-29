# USAGE
# cd to MDP-group-13
# python YOLODetector

# import the necessary packages
import numpy as np
import argparse
import imutils
import cv2
import os
import platform
import imagezmq

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

imshowDebug = False # mostly going to be used for cv2.imshow debugging perposes
if platform.node() == 'raspberrypi': # can't display images on rpi
    imshowDebug = False

imwriteDebug = False

boundingBoxDemo = False

dir_path = os.path.dirname(os.path.realpath(__file__))

# initialize the ImageHub object
imageHub = imagezmq.ImageHub()

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
def detectImage():
	try:
		(filename, input_img) = imageHub.recv_image()
		# filename = x_cor, y_cor, orientation
		
		# resize the frame to have a maximum width of 400 pixels, then
		# grab the frame dimensions and construct a blob
		input_img = imutils.resize(input_img, width=640)

		# if false positive,
		# can try using this to mask out non black areas
		# input_img = blackMask(input_img)

		(H, W) = input_img.shape[:2]
		# construct a blob from the input frame and then perform a forward
		# pass of the YOLO object detector, giving us our bounding boxes
		# and associated probabilities
		blob = cv2.dnn.blobFromImage(input_img, 1 / 255.0, (480, 480),
			swapRB=True, crop=False)
		net.setInput(blob)
		layerOutputs = net.forward(ln)

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
			# keep track of the image closest to midpoint
			[input_img_h, input_img_w, _] = input_img.shape
			boxMidEuclid = []

			# loop over the indexes we are keeping
			for i in idxs.flatten():
				# extract the bounding box coordinates
				(x, y) = (boxes[i][0], boxes[i][1])
				(w, h) = (boxes[i][2], boxes[i][3])

				# draw a bounding box rectangle and label on the frame
				color = [int(c) for c in COLORS[classIDs[i]]]
				cv2.rectangle(input_img, (x, y), (x + w, y + h), color, 2)
				text = "{}: {:.4f}".format(LABELS[classIDs[i]],
					confidences[i])

				box_centre_x = boxes[i][0] + boxes[i][2]/2
				box_centre_y = boxes[i][1] + boxes[i][3]/2
				euclid = (box_centre_x-input_img_w)**2 + (box_centre_y-input_img_h)**2
				boxMidEuclid.append([euclid, i])

				cv2.putText(input_img, text, (x, y - 5),
					cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)
		

			# in case multiple imgs are detected,
			# get the index of closest img to centre
			closestImg = np.argmin(i[0] for i in boxMidEuclid)
			centered_image_index = boxMidEuclid[closestImg][1]
			detected_id = int(LABELS[classIDs[centered_image_index]])
			confidence = confidences[centered_image_index]
			if boundingBoxDemo:
				filename = '{}-{}.jpg'.format(detected_id, 'sample:coords')
				output_filepath = '{}\Output\{}'.format(folderPath, filename)
				cv2.imwrite(output_filepath, input_img)
			
			result_str = "detected: {} (ID {}), confidence {:.4f}".format(result_list[detected_id], detected_id, confidence)
		
			cv2.putText(input_img, result_str, (10, H - 20),
				cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255,0), 2)
			if imshowDebug:
				cv2.imshow('result', input_img);cv2.waitKey(0);cv2.destroyAllWindows()
			reply = bytes('{}'.format(detected_id), 'utf-8')

			if imwriteDebug:
				filename = '{}-{}'.format(result_list[detected_id], filename)
				cv2.imwrite('{}/debug/{}.jpg'.format(dir_path, filename))


			# imageHub.send_reply(reply)
			imageHub.send_reply(reply)

			return detected_id, confidence, result_str
		else:
			reply = bytes('', 'utf-8')
			imageHub.send_reply(reply)
			return None
	except KeyboardInterrupt:
		return KeyboardInterrupt

def blackMask(input_img):
	input_img_HSV = cv2.cvtColor(input_img, cv2.COLOR_BGR2HSV)
	black_boundaries_HSV = [
		np.array([0, 0, 0], dtype = "uint8"),
		np.array([120, 127, 100], dtype = "uint8")
	]

	black_mask = cv2.inRange(input_img_HSV, black_boundaries_HSV[0], black_boundaries_HSV[1])
	_, thresh = cv2.threshold(black_mask, 127, 255, 0)

	# get countours of playing field
	contours, _ = cv2.findContours(thresh, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

	if len(contours) ==  0:
		return input_img
	else:
		largestContour = contours[0]
		for countour in contours:
			if (cv2.contourArea(countour) > cv2.contourArea(largestContour)):
				largestContour = countour

		# use the largest countour as selection (exclude windows, etc)
		filled_black_mask = cv2.drawContours(black_mask, [largestContour], -1, (255, 255, 255), cv2.FILLED)
		black_masked_img = cv2.bitwise_and(input_img, input_img, mask = filled_black_mask)
		return black_masked_img





while True:
	try:
		detected_id, confidence, result_str = detectImage()
		print(result_str)
	except TypeError: # return None
		print('Nothing detected')
	except KeyboardInterrupt:
		print('exiting...')
		break