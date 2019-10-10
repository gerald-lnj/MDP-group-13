import socket
import time
import sys

class tcp_connection():

    def __init__(self):
        #self.tcp_ip = "192.168.13.13" # RPI IP address
        self.port = 10000
        self.sock = None
        self.client = None
        self.addr = None
        self.pc_is_connect = False

    def setup(self):
        try:
            # Create a TCP/IP socket
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)  #important to allow reuse of IP

            #Bind the socket to address
            self.sock.bind(('', self.port))
            self.sock.listen(1)                                          #Listen for incoming connections
            print ("Listening for incoming connections from PC...")
            self.client, self.addr = self.sock.accept()
            print ("Accepted connection from: {}".format(self.addr))
            #self.pc_is_connect = True
            #self.send_message_PC("RPI Ready\n")

        except IOError, e:   
            print ("Failed to connect to PC: {}".format(IOError))
	    print("Exception: {}".format(e))

    def pc_listen_msg(self):
        while True:
            try:
                data = self.client.recv(2048)
                return(data)
            except IOError:
                print("Failed to listen from PC: {}".format(IOError))

    def pc_send_msg(self, data):
        try:
            self.client.sendto(data, self.addr)
        except IOError:
            print("Failed to send message to PC: {}".format(IOError))

    #comment test_send function if not testing
    def test_send(self):
	msg = str.encode("EXPLORE")
	self.client.sendto(msg, self.addr)

    def send_data(self):
	msg = str.encode("COMPUTE:9-9-9-1-1-9")
	self.client.sendto(msg, self.addr)

    def pc_disconnect(self):
        self.sock.close()
        self.client.close()
        print("Connection to PC closed")

    #Hard code explore - for testing
    def test_send(self):
	msg = str.encode("EXPLORE")
	self.client.sendto(msg, self.addr)
    #Hard code sensor data - for testing
    def send_data(self):
	msg = str.encode("COMPUTE:9-9-9-1-1-9")
	self.client.sendto(msg, self.addr)

 ###Test wifi (Host) -- To test client use pc_test_socket.py

# if __name__ == "__main__":
#          print ("main")
#          pc = tcp_connection()
#          pc.setup()

#          #send_msg = ("Rpi Ready\n")
#          #print ("send_message_PC(): %s " % send_msg)
#          #pc.send_message_PC(send_msg)
#         try:
#             while True:
#                 #read from pc
#                 print ("read")
#                 read_pc_msg = pc.pc_listen_msg()
#                 print("Message Received from PC: {}".format(read_pc_msg))

#                 #send to pc
#                 print("Sending message to PC")
#                 pc.pc_send_msg("From pi: " + read_pc_msg)

#         except KeyboardInterrupt:
#             print ("closing sockets")
#             pc.pc_disconnect()
