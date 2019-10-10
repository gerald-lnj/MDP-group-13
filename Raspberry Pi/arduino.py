#!/user/bin/env python
import serial

def setup():
    port = "/dev/ttyACM0"
    try:
        #establish connection with arduino via serial port
        s1 = serial.Serial(port, 115200)
        s1.flushInput()
        print("Established connection to Arduino serial port")
        return s1
    except IOError:
        print("Fail to establish connection with Arduino: {}".format(IOError))
        s1.close()

def ard_disconnect(s1):
    s1.close()
    print("Connection to Arduino serial port closed")

def ard_listen_msg(s1):
    try:
        data = s1.readline()
        if len(data) == 0: 
            print("No message received.")
            return None
        print("Message Received: {}".format(data))
        return data
    except IOError:
        print("Failed to listen from Arduino: {}".format(IOError))

def ard_send_msg(s1, data):
    try:
        s1.write(data)
    except IOError:
        print("Failed to send message to Arduino: {}".format(IOError))

def test():
    # try:
    #     s1 = setup()
    #     while True:

    #         #listen from arduino
    #         if s1.inWaiting()>0:
    #             data = ard_listen_msg
    #             if data:
    #                 ard_send_msg(s1,"From pi: "+ data)
    #                 print("message sent to arduino")

    # except KeyboardInterrupt:
    #     ard_disconnect(s1)

    try:
        s1 = setup()

        while True:
            #send to arduino
            data_out = raw_input("Enter an alphabet: ")
            print("Sending Message to Arduino: {}".format(data_out))
            ard_send_msg(s1,data_out)

            #receive from arduino
            data_in = ard_listen_msg(s1)
            #print("Message Received from Arduino: {}".format(data_in))
            #else: print("No data received from arduino")
                
    except KeyboardInterrupt:
        ard_disconnect(s1)
    
test()
