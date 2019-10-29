from YOLODetectorClient import *
import sys
import threading
import os

image_thread = YOLODetectorClient()
image_thread.setup('192.168.13.4')

y_coords, x_coords, orientation = 1, 1, 1

def image_request():
    thread = threading.Thread(target=image_thread.main(y_coords, x_coords, orientation, None))
    thread2 =threading.Thread(target=foo())
    thread.start()
    thread2.start()
    print('thread done')

def foo():
    print('foo')

image_request()
