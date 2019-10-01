from time import sleep
from datetime import datetime
from picamera import PiCamera

camera = PiCamera()
camera.resolution = (600, 600)
camera.start_preview()
# Camera warm-up time

camera.capture('Raspberry Pi/Image Recognition/Query/'+datetime.now().isoformat()[:19].replace('-','').replace(':', '')+'.jpg')
print('took a photo!')
