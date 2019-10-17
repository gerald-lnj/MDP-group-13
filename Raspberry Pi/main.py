import sys
import threading
import os
import subprocess as sp
import time
from btclass import *
from arclass import *
from tcpclass import *

from YOLODetectorClient import *

class Main(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        # make rpi discoverable
        self.is_py2 = sys.version[0] == '2'
        if self.is_py2:
            os.system('sudo hciconfig hci0 piscan')
        else:
            sp.run(['sudo', 'hciconfig', 'hci0' ,'piscan'])
        
        self.bt_thread = bt_connection()
        self.sr_thread = ard_connection()
        self.pc_thread = tcp_connection()
        self.image_thread = YOLODetectorClient()
        
        #initialise connections
        self.bt_thread.setup()
        self.sr_thread.setup()
        self.pc_thread.setup()
        # TODO: need to find ip of YOLODetectorServer computer
        self.image_thread.setup('192.168.13.12') # ip of gerald's com

        # init coordinates, orientation for image recognition
        self.row = 0
        self.col = 0
        self.orientation = 0

        time.sleep(1)	# wait for 1 secs before starting

        # #TEST: send waypoint to pc
        # self.pc_thread.send_data()
        # #TEST: send explore to pc
        # self.pc_thread.test_explore()

    #process to read from bluetooth
    def read_from_bluetooth(self):
        while True:
            #check if android is connected via bt
            if self.bt_thread.bt_checkStatus() == False:
                self.bt_thread.setup()

            #Get message from bluetooth
            read_bt_msg = self.bt_thread.bt_listen_msg()

            #Check header and send to arduino
            if(read_bt_msg[0:3].lower() == 'ar:'):
                print("Message Received from BT: {}".format(read_bt_msg))
                print("Sending message to Arduino: {}".format(read_bt_msg[3:]))
                self.write_to_arduino(read_bt_msg[3:])

            # Check header and send to pc
            elif(read_bt_msg[0:3].lower() == 'al:'):               
                if(read_bt_msg[3:].lower() == 'explore'):
                    print("Message Received from BT: {}".format(read_bt_msg))
                    print("Sending message to PC: {}".format(read_bt_msg[3:]))
                    self.write_to_pc(read_bt_msg[3:])
                    msg = "I"
                    print("Sending message to Arduino: {}".format(msg))
                    self.write_to_arduino(msg)

                elif(read_bt_msg[3:].lower() == 'fastest'):
                    print("Message Received from BT: {}".format(read_bt_msg))
                    print("Sending message to PC: {}".format(read_bt_msg[3:]))
                    self.write_to_pc(read_bt_msg[3:])

                else:	
                    print("Message Received from BT: {}".format(read_bt_msg))
                    print("Sending message to PC: {}".format(read_bt_msg[3:]))
                    self.write_to_pc(read_bt_msg[3:])
        
            else:
                print ("Incorrect header received from BT: {}".format(read_bt_msg[0:2])) 
                time.sleep(1)

    #process to write to bluetooth
    def write_to_bluetooth(self, msg_to_bt):
        #check if android is connected via bt
        #if self.bt_thread.bt_checkStatus() == False:
        #       self.bt_thread.setup()
        self.bt_thread.bt_send_msg(msg_to_bt)

    #process to read from arduino
    def read_from_arduino(self):
        while True:

            #Get message from arduino
            read_ard_msg = self.sr_thread.ard_listen_msg()

            #Check header and send to android
            if(read_ard_msg[0:3].lower() == 'an:'):
                print("Message Received from Arduino: {}".format(read_ard_msg))
                print("Sending message to Android: {}".format(read_ard_msg[3:]))
                self.write_to_bluetooth(read_ard_msg[3:])

            #Check header and send to pc
            elif(read_ard_msg[0:3].lower() == 'al:'):
                print("Message Received from Arduino: {}".format(read_ard_msg))
                print("Sending message to PC: {}".format(read_ard_msg[3:]))
                self.write_to_pc(read_ard_msg[3:])
                # TODO: uncomment after image recogintion is ready
                # # on sensor data from ar to al, take pic and send to server
                # response, coordinates_x, coordinates_y, orientation = self.image_thread.main(self.row, self.col, self.orientation)
                # if response:
                #     self.write_to_bluetooth('IMAGE:{}-{}-{}-{}'.format(response, coordinates_x, coordinates_y, orientation))

    #process to write to arduino
    def write_to_arduino(self, msg_to_ard):
        self.sr_thread.ard_send_msg(msg_to_ard)

    #process to read from pc
    def read_from_pc(self):
        while True:
            #Get message from pc
            read_pc_msg = self.pc_thread.pc_listen_msg()

            #Check header and send to android
            if(read_pc_msg[0:3].lower() == 'an:'):
                print("Message Received from PC: {}".format(read_pc_msg))
                print("Sending message to Android: {}".format(read_pc_msg[3:]))
                self.write_to_bluetooth(read_pc_msg[3:])

            #Check header and send to arduino
            elif(read_pc_msg[0:3].lower() == 'ar:'):
                print("Message Received from PC: {}".format(read_pc_msg))
                print("Sending message to Arduino: {}".format(read_pc_msg[3:]))
                self.write_to_arduino(read_pc_msg[3:])

            #Check header and send to arduino and android
            elif(read_pc_msg[0:8].lower() == 'movement'):
                print("Message Received from PC: {}".format(read_pc_msg))
                # read_pc_msg = MOVEMENT|MDF1|MDF2|(any combi of w, c, a, d, b, 1-9)|S|[5 6]|orientation

                # update coords, orientation in preperation for img when sensor data received
                msg = read_pc_msg.split("|")
                coordinates = msg[-2] #'[5 6]'
                coordinates = coordinates[1:-1] # '5 6'
                coordinates = coordinates.split(' ')
                [self.row, self.col] = [i for i in coordinates if len(i)>0] # ['5', '6']
                self.orientation = msg[-1]

                android_msg = '|'.join([msg[0], msg[1], msg[2], msg[-2], msg[-1]])
                if (msg[-3].lower() == 'stop'):
                    arduino_msg = msg[3:-3]
                else:
                    arduino_msg = msg[3:-2]

                android_msg =  '|'.join([msg[0], msg[1], msg[2], msg[-2], msg[-1]])
               
                #send all movement char to arduino
                for i in arduino_msg:
                    print("Sending message to Arduino: {}".format(i))
                    self.write_to_arduino(i)
                
                print("Sending message to Android: {}".format(android_msg))
                self.write_to_bluetooth(android_msg)
                if(msg[-3].lower() == 'stop'):
                    print("Sending message to Arduino: Z")
                    stop_msg = str.encode("Z")
                    self.write_to_arduino(stop_msg)
                    print("Sending message to Android: {}".format(msg[-3]))
                    self.write_to_bluetooth(msg[-3])
                    # self.pc_thread.pc_disconnect()

            elif(read_pc_msg[0:4].lower() == 'done'):
                print("Message Received from PC: {}".format(read_pc_msg))
                # read_pc_msg = DONE|MDF1|MDF2|(any combi of w, c, a, d, b, 1-9)
                msg = read_pc_msg.split("|")
                 
                arduino_msg = msg[3:]
                for i in arduino_msg:
                    print("Sending message to Arduino: {}".format(i))
                    self.write_to_arduino(i)

                #Wait for callibration to be done
                time.sleep(10)

                #TEST: start fastest path
                #self.pc_thread.test_fastest()

            elif(read_pc_msg[0:7].lower() == 'fastest'):
                print("Message Received from PC: {}".format(read_pc_msg))
                # read_pc_msg = FASTEST|(any combi of w, c, a, d, b, 1-9)
                msg = read_pc_msg.split("|")
                arduino_msg = msg[1:]
                for i in arduino_msg:
                    print("Sending message to Arduino: {}".format(i))
                    self.write_to_arduino(i)
                    
            else:
                print ("Incorrect header received from PC: {}".format(read_pc_msg[0:2])) 
                time.sleep(1)

    #process to write to pc
    def write_to_pc(self, msg_to_pc):
        self.pc_thread.pc_send_msg(msg_to_pc)

    def initialize_threads(self):
        # Bluetooth (BT) read and write thread
        read_bt_thread = threading.Thread(target = self.read_from_bluetooth, name = "bt_read_thread")
        write_bt_thread = threading.Thread(target = self.write_to_bluetooth, args = ("",), name = "bt_write_thread")

        # Serial (SR) read and write thread
        read_ard_thread = threading.Thread(target = self.read_from_arduino, name = "sr_read_thread")
        write_ard_thread = threading.Thread(target = self.write_to_arduino, args = ("",), name = "sr_write_thread")

        #TCP (PC) read and write thread
        read_pc_thread = threading.Thread(target = self.read_from_pc, name = "pc_read_thread")
        write_pc_thread = threading.Thread(target = self.write_to_pc, args = ("",), name = "pc_write_thread")

        # Set threads as daemons
        read_bt_thread.daemon = True
        write_bt_thread.daemon = True

        read_ard_thread.daemon = True
        write_ard_thread.daemon = True

        read_pc_thread.daemon = True
        write_pc_thread.daemon = True

        # Start Threads
        read_bt_thread.start()
        write_bt_thread.start()

        read_ard_thread.start()
        write_ard_thread.start()

        read_pc_thread.start()
        write_pc_thread.start()
        
        print("All threads initialized succesfully")

    def close_all_sockets(self):
        #close all sockets
        #pc_thread.close_all_pc_sockets()
        bt_thread.bt_disconnect()
        sr_thread.ard_disconnect()
        pc_thread.pc_disconnect()
        print("End threads")
        
    def keep_main_alive(self):
        while True:
            #suspend the thread  
            time.sleep(0.5)

if __name__ == "__main__":
    try:	
        mainThread = Main()
        mainThread.initialize_threads()
        mainThread.keep_main_alive()
    except KeyboardInterrupt:	
	    mainThread.close_all_sockets()
