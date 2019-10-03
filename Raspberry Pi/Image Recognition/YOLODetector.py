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

boundingBoxDemo = True

folderPath = os.path.dirname(os.path.abspath(__file__))

# load the COCO class labels our YOLO model was trained on
labelsPath =os.path.join(folderPath, "rpi.names")
LABELS = open(labelsPath).read().strip().split("\n")

# initialize a list of colors to represent each possible class label
np.random.seed(42)
COLORS = np.random.randint(0, 255, size=(len(LABELS), 3),
	dtype="uint8")

# derive the paths to the YOLO weights and model configuration
weightsPath = os.path.sep.join([folderPath, "rpi_best.weights"])
configPath = os.path.sep.join([folderPath, "rpi.cfg"])

# load our YOLO object detector trained on COCO dataset (80 classes)
# and determine only the output layer names that we need from YOLO
print("[INFO] loading YOLO from disk...")
net = cv2.dnn.readNetFromDarknet(configPath, weightsPath)
ln = net.getLayerNames()
ln = [ln[i[0] - 1] for i in net.getUnconnectedOutLayers()]

# loop over frames from the video file stream
def detectImage(image_filepath):
	if platform.node() == 'raspberrypi':
		filename = image_filepath.split('/')[-1]
	else:
		filename = image_filepath.split('\\')[-1]
	# resize the frame to have a maximum width of 400 pixels, then
	# grab the frame dimensions and construct a blob
	input_img = cv2.imread(image_filepath)
	input_img = imutils.resize(input_img, width=640)
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
			if confidence > 0.5:
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
			(x, y) = (boxes[i][0], boxes[i][1])
			(w, h) = (boxes[i][2], boxes[i][3])

			if boundingBoxDemo:
				# draw a bounding box rectangle and label on the frame
				color = [int(c) for c in COLORS[classIDs[i]]]
				cv2.rectangle(input_img, (x, y), (x + w, y + h), color, 2)
				text = "{}: {:.4f}".format(LABELS[classIDs[i]],
					confidences[i])
				cv2.putText(input_img, text, (x, y - 5),
					cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)
				if platform.node() == 'raspberrypi':
					output_filepath = '{}\Output\{}'.format(folderPath, filename)
				else:
					output_filepath = '{}/Output/{}'.format(folderPath, filename)
				cv2.imwrite(output_filepath, input_img)

		# in case multiple imgs are detected,
		# get the index of img with largest bounding box area
		# i.e. closest img
		largest_image_index = np.argmax(i[2] * i[3] for i in boxes)
		detected_id = int(LABELS[classIDs[largest_image_index]])
		confidence = confidences[largest_image_index]
		
		result_str = "detected: {} (ID {}), confidence {:.4f}".format(result_list[detected_id], detected_id, confidence)
	
		cv2.putText(input_img, result_str, (10, H - 20),
			cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255,0), 2)
		if imshowDebug:
			cv2.imshow('result', input_img);cv2.waitKey(0);cv2.destroyAllWindows()
		return detected_id, confidence, result_str
	else:
		return None
	


for entry in os.scandir(folderPath + '/Query'):
	# print(entry.path)

	try:
		detected_id, confidence, result_str = detectImage(entry.path)
		print(result_str)
	except TypeError:
		print('Nothing detected')