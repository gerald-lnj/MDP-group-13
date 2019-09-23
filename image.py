from time import sleep
from datetime import datetime
from picamera import PiCamera

camera = PiCamera()
camera.resolution = (1024, 768)
camera.start_preview()
# Camera warm-up time
sleep(2)
camera.capture('foo.jpg')


while(True):
    camera.capture('images/'+datetime.now().isoformat()[:19].replace('-','').replace(':', '')+'.jpg')
    print('took a photo!')
    sleep(5)