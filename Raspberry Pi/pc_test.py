import socket
import string
import time
import threading


# Dummy client code

class Test(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        self.ip = "192.168.13.13"
        self.port = 10000
        self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.client_socket.connect((self.ip, self.port))

    # Send data
    def write(self):
        while True:
            msg = str.encode(input("Enter msg to send to rpi: "))
            self.client_socket.send(msg)
            print ("Sending to RPI: ",msg)
            time.sleep(0.5) #to wait for any mssg received to finish printing
        print ("quit write()")

	# Receive data
    def receive(self):
        while True:
            data = self.client_socket.recv(1024)
            if len(data) == 0:
                print ("quitting...")
                break
            print ("Message received: {} ".format(data))
        print ("quit receive()")
	
    def keep_main(self):
        while True:
            time.sleep(0.5)
    
    def close(self):
        self.client_socket.close()

if __name__ == "__main__":
    test = Test()


    rt = threading.Thread(target = test.receive)
    wt = threading.Thread(target = test.write)

    rt.daemon = True
    wt.daemon = True

    rt.start()
    wt.start()
    #print ("start rt and wt")

    test.keep_main()

    test.close
    print ("End of client program")