import sys
import threading
import Queue
import time
from btclass import *
from arclass import *

class Main(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

        #self.algo_thread = algo()
        self.bt_thread = btconnection()
        self.sr_thread = ard_ser_con()
        
        #initialise connections
        self.bt_thread.setup()
        self.sr_thread.setup()
        time.sleep(1)	# wait for 1 secs before starting

    #process to read from bluetooth
    def read_from_bluetooth(self):
        while True:
            read_bt_msg = self.bt_thread.bt_listen_msg()

            # Check header and send data to algo
			# if(read_bt_msg[0:2].lower() == 'al:'):	# send to algo
			# 	self.writePC(read_bt_msg[3:])	# strip the header
			# 	print "Value received from Bluetooth: %s" % read_bt_msg[1:]

			#if(read_bt_msg[0:2].lower() == 'ar:'):	# send to SR
			#self.write_to_arduino(read_bt_msg[3:])		# strip the header
			print ("Message received from BT: {}".format(read_bt_msg[0]))
            print("Sending message to Arduino...")
            self.write_to_arduino(read_bt_msg[0])
        
			# else:
			# 	print ("incorrect header received from BT: {}".format(read_bt_msg[0])) 
			# #	time.sleep(1)

    #process to write to bluetooth
    def write_to_bluetooth(self, msg_to_bt):
        self.bt_thread.bt_send_msg(msg_to_bt)

    #process to read from arduino
    def read_from_arduino(self):
        while True:
            read_ard_msg = self.sr_thread.ard_listen_msg()
            print("Message received from Arduino: {}".format(read_ard_msg))

    #process to write to arduino
    def write_to_arduino(self, msg_to_ard):
        self.sr_thread.ard_send_msg(msg_to_ard)

    def initialize_threads(self):
        # Bluetooth (BT) read and write thread
		read_bt_thread = threading.Thread(target = self.read_from_bluetooth, name = "bt_read_thread")
		# print "created rt_bt"
		write_bt_thread = threading.Thread(target = self.write_to_bluetooth, args = ("",), name = "bt_write_thread")
		# print "created wt_bt"

		# Serial (SR) read and write thread
		read_ard_thread = threading.Thread(target = self.read_from_arduino, name = "sr_read_thread")
		# print "created rt_sr"
		write_ard_thread = threading.Thread(target = self.write_to_arduino, args = ("",), name = "sr_write_thread")
		# print "created wt_sr"

        # Set threads as daemons
		read_bt_thread.daemon = True
		write_bt_thread.daemon = True

		read_ard_thread.daemon = True
		write_ard_thread.daemon = True

        # Start Threads
		read_bt_thread.start()
		write_bt_thread.start()

		read_ard_thread.start()
		write_ard_thread.start()
	
		print("All threads initialized succesfully")

	def close_all_sockets(self):
        #close all sockets
		#pc_thread.close_all_pc_sockets()
		bt_thread.bt_disconnect()
		sr_thread.ard_disconnect()
		print ("End threads")

	def keep_main_alive(self):
		#function = Sleep for 500 ms and wake up.
		#Keep Repeating function 
		#until Ctrl+C is used to kill
		#the main thread.

		while True:
			#suspend the thread  
			time.sleep(0.5)

if __name__ == "__main__":
	mainThread = Main()
	mainThread.initialize_threads()
	mainThread.keep_main_alive()
	mainThread.close_all_sockets()