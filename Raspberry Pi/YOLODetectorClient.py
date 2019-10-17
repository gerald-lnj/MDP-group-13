# USAGE
# python client.py

import imagezmq
import socket
import os
import cv2
import picam

class YOLODetectorClient():
	def __init__(self):
		self.folderPath = os.path.dirname(os.path.abspath(__file__))

	def setup(self, server_address):
		# initialize the ImageSender object
		# with the socket address of the server (5555)
		self.sender = imagezmq.ImageSender(connect_to="tcp://{}:5555".format(server_address))

	def send_image(self, filename, image):
		try:
			return self.sender.send_image(filename, image)
		except KeyboardInterrupt:
			pass

	def read_image(self, filepath):
		image = cv2.imread(filepath)
		filename = (entry.name).replace('.jpg', '')

		# filename should contain coordinates, orientation
		# eg filename = '0-2-N'
		if ('-' in filename):
			[coordinates_x, coordinates_y, orientation] = filename.split('-')
		return filename, image, coordinates_x, coordinates_y, orientation

	def main(self, coordinates_x, coordinates_y, orientation):
		image = picam.takePhoto()
		# returns bytes (utf-8)
		response = client.send_image('rpi', image)
		response = response.decode('utf-8') 

		if len(response):
			print('detected {} at {}, orientation {}'.format(response, [coordinates_x, coordinates_y], orientation))
			return response, coordinates_x, coordinates_y, orientation
		

if __name__ == "__main__":
	client = YOLODetectorClient()
	client.setup('127.0.0.1')
	for entry in os.scandir(client.folderPath+'Image Recognition/Query'):
		if entry.name != '.DS_Store':
			filename, image, x, y, orientation = client.read_image(entry.path)

			# returns bytes (utf-8)
			response = client.send_image(filename, image)

			# decode to string
			if len(response):
				print('detected {} at {}, orientation {}'.format(response, [x, y], orientation))
			os.remove(entry.path)