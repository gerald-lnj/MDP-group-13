#!/user/bin/env python
import serial

class ard_connection():
    def __init__(self):
        self.port = "/dev/ttyACM0"
        self.baud_rate = 115200
        self.s1 = None

    def setup(self):
        try:
            #establish connection with arduino via serial port
            self.s1 = serial.Serial(self.port, self.baud_rate)
            self.s1.flushInput()
            print("Established connection to Arduino serial port")
        except IOError:
            print("Fail to establish connection with Arduino: {}".format(IOError))
            self.s1.close()

    def ard_disconnect(self):
        self.s1.close()
        print("Connection to Arduino serial port closed")

    def ard_listen_msg(self):
        try:
            data = self.s1.readline()
            if len(data) == 0: 
                print("No message received.")
                return None
            #print("Message Received: {}".format(data))
            return data
        except IOError:
            print("Failed to listen from Arduino: {}".format(IOError))

    def ard_send_msg(self, data):
        try:
            #TODO: ensure that arduino can also read a byte array
            # data_bytes = bytes(data, 'utf-8')
            data_bytes = (data.encode('utf-8'))
            self.s1.write(data_bytes)
        except IOError:
            print("Failed to send message to Arduino: {}".format(IOError))
