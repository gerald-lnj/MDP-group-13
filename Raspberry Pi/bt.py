from bluetooth import *
class bluetooth():
	def __init__(self):
		server_socket = BluetoothSocket(RFCOMM)
		server_socket.bind(("",PORT_ANY)) 
		server_socket.listen(1) 
		self.server_socket = server_socket
		self.port = server_socket.getsockname()[1] 
		uuid = "99999999-0000-0000-1111-eeeeeeeeeeee" 
		advertise_service( server_socket, "Group13-BluetoothService",           
		service_id = uuid,     
		service_classes = [ uuid, SERIAL_PORT_CLASS ],                    
		profiles = [ SERIAL_PORT_PROFILE ],  
			protocols = [ OBEX_UUID ])

	def establish_con(self):
		print("Waiting for connection on RFCOMM channel %d" % self.port)
		server_socket = self.server_socket
		try:
			client_socket, address = server_socket.accept()
			self.client_socket = client_socket
			return True
		except:
			print ("Error connecting to Bluetooth")
		print("Accepted connection from ", address)
		return False
	
	
	def send_msg(self,message):
		client_socket = self.client_socket
		client_socket.send(message)
	
	def listen_msg(self):
		while True:
			client_socket = self.client_socket
			data = client_socket.recv(2048)
			return data
			
	def disconnect(self):
		print ("Disconnected")
		self.client_socket.close()
		self.server_socket.close()