#!/usr/bin
from bluetooth import *
import subprocess as sp

nexus_MAC_addr = '68:B3:5E:58:96:CB'

def setup():
    btport = 3
    server_sock = BluetoothSocket(RFCOMM)
    server_sock.bind(("",btport))
    server_sock.listen(1)
    port = server_sock.getsockname()[1]
    uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

    # become visible for pairing
    advertise_service( server_sock, "MDP-Server",
        service_id = uuid,
        service_classes = [ uuid, SERIAL_PORT_CLASS ],
        profiles = [ SERIAL_PORT_PROFILE ],
    #       protocols = [ OBEX_UUID ]
        )
    #pending connection with android via bluetooth
    print("Waiting for connection on RFCOMM channel %d" % port)

    #established connection with android via bluetooth
    client_sock, client_address = server_sock.accept()
    print("Accepted connection from ", client_address)
    return client_sock, server_sock


def bt_listen_msg(client_sock):
    try:
        data = client_sock.recv(2048)
        if len(data) == 0: return None
        print("Message Received: {}".format(data))
        return(data)
    except IOError:
        print("Failed to listen from Android: {}".format(IOError))

def bt_send_msg(data, client_sock):
    try:
        client_sock.send(data)
    except IOError:
        print("Failed to send message to Android: {}".format(IOError))

def BT_checkStatus():
    stdoutdata = sp.check_output(["hcitool","con"])
    if nexus_MAC_addr in stdoutdata.split():
        return True
    else:
        return False

def BT_disconnect(client_sock, server_sock):
    #disconnect
    print("disconnected")
    client_sock.close()
    server_sock.close()
    print("all done") 

def test():
    try:
        client_sock, server_sock = setup()
        counter = 0
        while True:
            #debug
            print('check')
            counter += 1
            if counter % 100 == 0:
                if BT_checkStatus() == False:
                    setup()
            data = bt_listen_msg(client_sock)
            if data:
                bt_send_msg(data, client_sock)
    except KeyboardInterrupt:
        BT_disconnect(client_sock, server_sock)

test()

