

import socket
import json
import numpy as np
import os
import time
import tornado.web as web
import tornado.websocket as websocket
import tornado.ioloop as ioloop
import threading
from threading import Thread

from tornado.options import define, options
from Algo.Exploration import Exploration
from Algo.FastestPath import FastestPath
from Algo.Constants import START, GOAL, NORTH, WEST

# Global Variables
define("port", default=8888, help="run on the given port", type=int)
clients = dict()
currentMap = np.zeros([20, 15])
mdfCounter = 3




log_file = open('log.txt', 'w')

area = 0
exp = ''
fsp = ''
visited = dict()
waypoint = None
steps = 0
numCycle = 1
t_s = 0
direction = 1

map_name = 'map.txt'

step = 0.1


class FuncThread(threading.Thread):



    def __init__(self, target, *args):

        self._target = target
        self._args = args
        threading.Thread.__init__(self)

    def run(self):

        self._target(*self._args)


class IndexHandler(web.RequestHandler):



    @web.asynchronous
    def get(self):
        self.render("index.html")


class WebSocketHandler(websocket.WebSocketHandler):



    def open(self):

        self.id = self.get_argument("Id")
        self.stream.set_nodelay(True)
        clients[self.id] = {"id": self.id, "object": self}
        print("WebSocket opened")

    def on_message(self, message):

        print("Client " + str(self.id) + " received a message : " + str(message))

    def on_close(self):

        print("WebSocket closed")
        if self.id in clients:
            del clients[self.id]


class StartHandler(web.RequestHandler):


    @web.asynchronous
    def get(self):
        self.write("Starting...")
        self.step = self.get_argument("step")
        self.limit = self.get_argument("limit")
        self.coverage = self.get_argument("coverage")
        global step
        step = float(self.step)
        startExploration(self.limit, self.coverage)
        self.flush()


class ResetHandler(web.RequestHandler):



    @web.asynchronous
    def get(self):
        self.write("Reset...")
        global exp
        exp = Exploration(map_name, 5)
        update(np.zeros([20, 15]), exp.exploredArea, exp.robot.center, exp.robot.head,
               START, GOAL, 0)


class FSPHandler(web.RequestHandler):


    @web.asynchronous
    def get(self):
        self.x = self.get_argument("x")
        self.y = self.get_argument("y")
        self.write("Starting...")
        startFastestPath([self.x, self.y])
        self.flush()


class LoadMapHandler(web.RequestHandler):


    @web.asynchronous
    def get(self):
        global map_name
        self.name = self.get_argument("name")
        map_name = self.name


def startExploration(limit, coverage):

    global exp, t_s
    exp = Exploration(map_name, 5)
    t_s = time.time()
    t2 = FuncThread(exploration, exp, limit, coverage)
    t2.start()



def exploration(exp, limit, coverage):

    global currentMap, area
    limit = map(int, str(limit).strip().split(':'))
    time_limit = limit[0]*60*60 + limit[1]*60 + limit[2]
    elapsedTime = 0
    update(exp.currentMap, exp.exploredArea, exp.robot.center, exp.robot.head, START, GOAL, 0)
    logger('Exploration Started !')
    current = exp.moveStep()
    currentMap = exp.currentMap
    area = exp.exploredArea
    visited = dict()
    steps = 0
    numCycle = 1
    while (not current[1] and elapsedTime <= time_limit and exp.exploredArea < int(coverage)):
        elapsedTime = round(time.time()-t_s, 2)
        update(exp.currentMap, exp.exploredArea, exp.robot.center, exp.robot.head, START, GOAL,
               elapsedTime)
        current = exp.moveStep()
        currentMap = exp.currentMap
        area = exp.exploredArea
        steps += 1
        currentPos = tuple(exp.robot.center)
        if (currentPos in visited):
            visited[currentPos] += 1
            if (visited[currentPos] > 3):
                neighbour = exp.getExploredNeighbour()
                if (neighbour):
                    neighbour = np.asarray(neighbour)
                    fsp = FastestPath(currentMap, exp.robot.center, neighbour,
                                      exp.robot.direction, None)
                    fastestPath(fsp, neighbour, exp.exploredArea, None)
                    exp.robot.center = neighbour
                else:
                    break
        else:
            visited[currentPos] = 1
        if (np.array_equal(exp.robot.center, START)):
            numCycle += 1
            if (numCycle > 1 and steps > 4):
                neighbour = exp.getExploredNeighbour()
                if (neighbour):
                    neighbour = np.asarray(neighbour)
                    fsp = FastestPath(currentMap, exp.robot.center, neighbour,
                                      exp.robot.direction, None)
                    fastestPath(fsp, neighbour, exp.exploredArea, None)
                    exp.robot.center = neighbour
                else:
                    break
        time.sleep(float(step))
    update(exp.currentMap, exp.exploredArea, exp.robot.center, exp.robot.head, START, GOAL,
           elapsedTime)
    logger('Exploration Done !')
    logger("Map Descriptor 1  -->  "+str(exp.robot.descriptor_1()))
    logger("Map Descriptor 2  -->  "+str(exp.robot.descriptor_2()))
    print currentMap
    fsp = FastestPath(currentMap, exp.robot.center, START, exp.robot.direction, None)
    logger('Fastest Path Started !')
    fastestPath(fsp, START, exp.exploredArea, None)


def startFastestPath(waypoint):
    """To start the fastest path of the maze
    """
    global fsp
    global t_s
    waypoint = map(int, waypoint)
    fsp = FastestPath(currentMap, START, GOAL, NORTH, waypoint)
    t_s = time.time()
    logger('Fastest Path Started !')
    t3 = FuncThread(fastestPath, fsp, GOAL, area, waypoint)
    t3.start()


def markMap(curMap, waypoint):
    if waypoint:
        curMap[tuple(waypoint)] = 7
    return curMap


def combineMovement(movement):
    counter = 0
    shortMove = []
    while (counter < len(movement)):
        if (counter < len(movement)-7) and all(x == 'W' for x in movement[counter:counter+7]):
            shortMove.append('7')
            counter += 7
        elif (counter < len(movement)-5) and all(x == 'W' for x in movement[counter:counter+5]):
            shortMove.append('5')
            counter += 5
        elif (counter < len(movement)-3) and all(x == 'W' for x in movement[counter:counter+3]):
            shortMove.append('3')
            counter += 3
        elif (counter < len(movement)-2) and all(x == 'W' for x in movement[counter:counter+2]):
            shortMove.append('2')
            counter += 2
        else:
            shortMove.append(movement[counter])
            counter += 1
    shortMove += movement[counter:]
    return shortMove


def fastestPath(fsp, goal, area, waypoint):
    fsp.getFastestPath()
    logger(json.dumps(fsp.path))
    while (fsp.robot.center.tolist() != goal.tolist()):
        fsp.moveStep()
        update(markMap(np.copy(fsp.exploredMap), waypoint), area, fsp.robot.center, fsp.robot.head,
               START, GOAL, 0)
    logger('Fastest Path Done !')


def update(current_map, exploredArea, center, head, start, goal, elapsedTime):

    for key in clients:
        message = dict()
        message['area'] = '%.2f' % (exploredArea)
        tempMap = current_map.copy()
        tempMap[start[0]-1: start[0]+2, start[1]-1: start[1]+2] = 3
        tempMap[goal[0]-1: goal[0]+2, goal[1]-1: goal[1]+2] = 4
        message['map'] = json.dumps(tempMap.astype(int).tolist())
        message['center'] = json.dumps(center.astype(int).tolist())
        message['head'] = json.dumps(head.astype(int).tolist())
        message['time'] = '%.2f' % (elapsedTime)
        clients[key]['object'].write_message(json.dumps(message))


def logger(message):
    for key in clients:
        log = {'log': message}
        clients[key]['object'].write_message(json.dumps(log))

#need to modify 
def output_formatter(msg, movement):
    if not isinstance(movement, list):
        movement = movement.tolist()
    movement = map(str, movement)
    return msg+'|'+'|'.join(movement)


class RPi(threading.Thread):
    def __init__(self):
        print "starting rpi communication"
        threading.Thread.__init__(self)

        self.ip = "192.168.13.13"  # Connecting to IP address of MDPGrp13
        self.port = 10000

        # Create a TCP/IP socket
        self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.client_socket.connect((self.ip, self.port))
        print "sent connection request"

        # Receive and send data to RPi data
    def receive_send(self):
        time_t = time.time()
        a=0
        while True:
            current_pos = None
            data = self.client_socket.recv(2048)
            log_file.write(data+'\n')
            log_file.flush()
            if (data):
                print ('Received %s from RPi' % (data))
                split_data = data.split(":")
                global exp, t_s, area, steps, numCycle, currentMap, exp, fsp
                if (split_data[0] == 'EXPLORE'):
                    t_s = time.time()
                    exp = Exploration(sim=False)
                    current_pos = exp.robot.center
                    visited[tuple(current_pos)] = 1
                    update(exp.currentMap, exp.exploredArea, exp.robot.center, exp.robot.head,
                           START, GOAL, 0)
                elif (split_data[0] == 'WAYPOINT'):
                    global waypoint
                    waypoint = map(int, split_data[1:])
                    waypoint[0] = 19 - waypoint[0]
                elif (split_data[0] == 'COMPUTE'):

                    print 'Time 0: %s s' % (time.time() - time_t)
                    print(split_data)
                    split_data=split_data[1].split("-")
                    sensors=map(int,split_data)
                    #
                    # sensors = map(float, split_data)
                    current_pos = exp.robot.center
                    current = exp.moveStep(sensors)
                    currentMap = exp.currentMap
                    # currentMap[16][0]=1
                    # currentMap[16][1]=1
                    # currentMap[16][2]=1
                    ### add 
                    # visited[tuple(current_pos)] = 1
                    ###
                    if (not current[1]):
                        time_t = time.time()
                        move = combineMovement(current[0])
                        elapsedTime = round(time.time()-t_s, 2)
                        update(exp.currentMap, exp.exploredArea, exp.robot.center, exp.robot.head,
                               START, GOAL, elapsedTime)
                        steps += 1
                        current_pos = tuple(exp.robot.center)
                        if (current_pos in visited):
                            ####
                            print(visited)
                            print("check !!!!!!!!!!!!!!!!!!!!!!!")
                            ####
                            visited[current_pos] += 1
                            if (visited[current_pos] > 3):
                                print("redirct")
                                neighbour = exp.getExploredNeighbour()
                                if (neighbour):
                                    neighbour = np.asarray(neighbour)
                                    fsp = FastestPath(currentMap, exp.robot.center, neighbour,
                                                      exp.robot.direction, None, sim=False)
                                    fastestPath(fsp, neighbour, exp.exploredArea, None)
                                    move.extend(combineMovement(fsp.movement))
                                    exp.robot.phase = 2
                                    exp.robot.center = neighbour
                                    exp.robot.head = fsp.robot.head
                                    exp.robot.direction = fsp.robot.direction
                                    if exp.robot.direction!=3:
                                        if exp.robot.direction==1:
                                            move.append('B')
                                            print("adjust B")
                                        elif exp.robot.direction==2:
                                            move.append('D')
                                            print("adjust D")
                                        elif exp.robot.direction==4:
                                            move.append('A')
                                            print("adjust A")
                                    exp.robot.direction=3    
                                    currentMap = exp.currentMap
                                # else:
                                #     #####
                                #     print("else1")
                                #     fsp = FastestPath(currentMap, exp.robot.center, START, exp.robot.direction,
                                #           None, sim=False)
                                #     fastestPath(fsp, START, exp.exploredArea, None)
                                #     move.extend(combineMovement(fsp.movement))
                                #     currentMap = exp.currentMap
                                #     get_msg = output_formatter('DONE', [str(exp.robot.descriptor_1()),
                                #                    str(exp.robot.descriptor_2())] + move + ['STOP'] + [str(exp.robot.center), str(exp.robot.direction)])
                                #     # self.client_socket.send(get_msg)                                   





                            if (np.array_equal(exp.robot.center, START) and exp.exploredArea > 50):
                                numCycle += 1
                                if (numCycle > 1 and steps > 4):
                                    print("cycle")
                                    neighbour = exp.getExploredNeighbour()
                                    if (neighbour):
                                        neighbour = np.asarray(neighbour)
                                        fsp = FastestPath(currentMap, exp.robot.center, neighbour,
                                                          exp.robot.direction, None, sim=False)
                                        fastestPath(fsp, neighbour, exp.exploredArea, None)
                                        move.extend(combineMovement(fsp.movement))
                                        exp.robot.phase = 2
                                        exp.robot.center = neighbour
                                        exp.robot.head = fsp.robot.head
                                        exp.robot.direction = fsp.robot.direction
                                        currentMap = exp.currentMap

                                    # else:
                                    #     #####
                                    #     print("else2")
                                    #     fsp = FastestPath(currentMap, exp.robot.center, START, exp.robot.direction,
                                    #           None, sim=False)
                                    #     fastestPath(fsp, START, exp.exploredArea, None)
                                    #     move.extend(combineMovement(fsp.movement))
                                    #     currentMap = exp.currentMap
                                    #     get_msg = output_formatter('DONE', [str(exp.robot.descriptor_1()),
                                    #                    str(exp.robot.descriptor_2())] + move + ['STOP'] + [str(exp.robot.center), str(exp.robot.direction)])
                                    #     # self.client_socket.send(get_msg)  

                        else:
                            visited[tuple(current_pos)] = 1
                        print 'Time 1: %s s' % (time.time() - time_t)
                        time_t = time.time()
                        global mdfCounter
                        if mdfCounter == 3:
                            get_msg = output_formatter('MOVEMENT', [str(exp.robot.descriptor_1()),
                                                       str(exp.robot.descriptor_2())] + move + ['S'] + [str(exp.robot.center), str(exp.robot.direction)])
                            # mdfCounter = 0
                            print(move)
                            print(current_pos)


                        # else:
                        #     get_msg = output_formatter('MOVEMENT', [str(0), str(0)] + move + ['S'])
                        #     mdfCounter += 1
                        print 'Time 2: %s s' % (time.time() - time_t)
                    else:
                        move = combineMovement(current[0])
                        get_msg = output_formatter('MOVEMENT', [str(exp.robot.descriptor_1()),
                                                   str(exp.robot.descriptor_2())] + move + ['STOP'] + [str(exp.robot.center), str(exp.robot.direction)])
                        self.client_socket.send(get_msg)
                        print ('Sent %s to RPi' % (get_msg))
                        log_file.write('Robot Center: %s\n' % (str(exp.robot.center)))
                        log_file.write('Sent %s to RPi\n\n' % (get_msg))
                        log_file.flush()
                        time.sleep(2)
                        update(exp.currentMap, exp.exploredArea, exp.robot.center, exp.robot.head,
                               START, GOAL, elapsedTime)
                        logger('Exploration Done !')
                        logger("Map Descriptor 1  -->  "+str(exp.robot.descriptor_1()))
                        logger("Map Descriptor 2  -->  "+str(exp.robot.descriptor_2()))
                        fsp = FastestPath(currentMap, exp.robot.center, START, exp.robot.direction,
                                          None, sim=False)
                        logger('Fastest Path Started !')
                        fastestPath(fsp, START, exp.exploredArea, None)
                        move = combineMovement(fsp.movement)
                        currentMap = exp.currentMap
                        global direction
                        if (fsp.robot.direction == WEST):
                            calibrate_move = ['A', 'L', 'B']
                        else:
                            calibrate_move = ['L', 'B']
                        direction = NORTH
                        get_msg = output_formatter('DONE', [str(exp.robot.descriptor_1()),
                                                   str(exp.robot.descriptor_2())] + move +
                                                   calibrate_move + [str(exp.robot.center), str(exp.robot.direction)])
                        # self.client_socket.send(get_msg)
                        time.sleep(1)
                        # get_msg = output_formatter('MOVEMENT', [str(exp.robot.descriptor_1()),
                        #                            str(exp.robot.descriptor_2())] + move +
                        #                            calibrate_move)
                    self.client_socket.send(get_msg)
                    print ('Sent %s to RPi' % (get_msg))
                    time_t = time.time()
                    log_file.write('Robot Center: %s\n' % (str(exp.robot.center)))
                    log_file.write('Sent %s to RPi\n\n' % (get_msg))
                    log_file.flush()
                elif (split_data[0] == 'FASTEST'):
                    print (currentMap)
                    fsp = FastestPath(currentMap, START, GOAL, direction, waypoint, sim=False)
                    current_pos = fsp.robot.center
                    fastestPath(fsp, GOAL, 300, waypoint)
                    # move = fsp.movement
                    move = combineMovement(fsp.movement)
                    get_msg = output_formatter('FASTEST',move)
                    self.client_socket.send(get_msg)
                    print ('Sent %s to RPi' % (get_msg))
                elif (split_data[0] == 'MANUAL'):
                    manual_movement = split_data[1:]
                    for move in manual_movement:
                        exp.robot.moveBot(move)
                else:
                    print("error")

    def keep_main(self):
        while True:
            time.sleep(0.5)


settings = dict(
    template_path=os.path.join(os.path.dirname(__file__), "GUI", "templates"),
    debug=True
)

app = web.Application([
    (r'/', IndexHandler),
    (r'/websocket', WebSocketHandler),
    (r'/start', StartHandler),
    (r'/reset', ResetHandler),
    (r'/fsp', FSPHandler),
    (r'/lm', LoadMapHandler),
    (r'/(.*)', web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), "GUI")})
], **settings)


def func1():
    print "Starting communication with RPi"
    client_rpi = RPi()
    rt = threading.Thread(target=client_rpi.receive_send)
    rt.daemon = True
    rt.start()
    client_rpi.keep_main()


def func2():
    print "Starting communication with front-end"
    app.listen(options.port)
    t1 = FuncThread(ioloop.IOLoop.instance().start)
    t1.start()
    t1.join()

if __name__ == '__main__':
    Thread(target=func1).start()
    Thread(target=func2).start()
