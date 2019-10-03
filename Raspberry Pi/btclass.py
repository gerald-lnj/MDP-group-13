from bluetooth import *

class bt_connection():
    def __init__(self):
        self.server_sock = None
        self.client_sock = None
        self.bt_is_connected = False
        
    def setup(self):
        btport = 3
        try:
            self.server_sock = BluetoothSocket(RFCOMM)
            self.server_sock.bind(("",btport))
            self.server_sock.listen(1)
            # self.server_sock = server_sock
            self.port = self.server_sock.getsockname()[1]
            uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

            # become visible for pairing
            advertise_service(self.server_sock, "MDP-Server",
            service_id = uuid,
            service_classes = [ uuid, SERIAL_PORT_CLASS ],
            profiles = [ SERIAL_PORT_PROFILE ],
        #       protocols = [ OBEX_UUID ]
            )
            #pending connection with android via bluetooth
            print("Waiting for connection on RFCOMM channel %d" % self.port)
            server_sock = self.server_sock
            #established connection with android via bluetooth
            self.client_sock, client_address = self.server_sock.accept()
            print("Accepted connection from ", client_address)
            self.bt_is_connected = True

        except IOError:
            print("Failed to connect to Android: {} ".format(IOError))
            return False

    def bt_listen_msg(self):
        while True:
            try:
                client_sock = self.client_sock
                data = client_sock.recv(2048)
                #print("Message Received: {}".format(data))
                return(data)
            except IOError:
                print("Failed to listen from Android: {}".format(IOError))

    def bt_send_msg(self, data):
        try:
            self.client_sock.send(data)
        except IOError:
            print("Failed to send message to Android: {}".format(IOError))

    def bt_checkStatus():
        nexus_MAC_addr = '68:B3:5E:58:96:CB'
        stdoutdata = sp.check_output(["hcitool","con"])
        if nexus_MAC_addr in stdoutdata.split():
            return True
        else:
            return False

    def bt_disconnect(self):
        #disconnect
        print("disconnected")
        self.client_sock.close()
        self.server_sock.close()
        self.bt_is_connected = False
        print("all done")