import numpy as np
import time
 
from Constants import NORTH, SOUTH, WEST, EAST, FORWARD, LEFT, RIGHT, START, MAX_ROWS, MAX_COLS, BACK

cal_count=0

class Exploration:



    def __init__(self, realMap=None, timeLimit=None, calibrateLim=6, sim=True):

        self.timeLimit = timeLimit
        self.exploredArea = 0
        self.currentMap = np.zeros([20, 15])
        if sim:
            from Simulator import Robot
            self.robot = Robot(self.currentMap, EAST, START, realMap)
            self.sensors = self.robot.getSensors()
        else:
            from Real import Robot
            self.robot = Robot(self.currentMap, EAST, START)
        self.exploredNeighbours = dict()
        self.sim = sim
        self.calibrateLim = calibrateLim
        self.virtualWall = [0, 0, MAX_ROWS, MAX_COLS]

    def __validInds(self, inds):

        front = self.frontFree()
        print(front)
        if (self.checkFree([1, 2, 3, 0], self.robot.center)):
            self.robot.moveBot(RIGHT)
            move.append(RIGHT)
            front = self.frontFree()
            for i in range(front):
                self.robot.moveBot(FORWARD)
            move.extend([FORWARD]*front)
            print("try right")
        elif (front):
            for i in range(front):
                self.robot.moveBot(FORWARD)
            move.extend([FORWARD]*front)
            print("try front")
        elif (self.checkFree([3, 0, 1, 2], self.robot.center)):
            self.robot.moveBot(LEFT)
            move.append(LEFT)
            front = self.frontFree()
            for i in range(front):
                self.robot.moveBot(FORWARD)
            move.extend([FORWARD]*front)
            print("try Left")
        else:
            self.robot.moveBot(RIGHT)
            self.robot.moveBot(RIGHT)
            move.append(BACK)




        
        if not (self.sim):
            calibrate_front = self.robot.can_calibrate_front()
            calibrate_right = self.robot.can_calibrate_right()
            if self.robot.is_corner():
                move.append('L')
            elif (calibrate_right[0]):
                global cal_count
                cal_count=cal_count+1
                if (cal_count%2==0):
                    move.append(calibrate_right[1])
            elif (calibrate_front[0]):
                move.append(calibrate_front[1])
        return move

    def checkFree(self, order, center):

        directionFree = np.asarray([self.northFree(center), self.eastFree(center),
                                    self.southFree(center), self.westFree(center)])
        directionFree = directionFree[order]
        if self.robot.direction == NORTH:
            return directionFree[0]
        elif self.robot.direction == EAST:
            return directionFree[1]
        elif self.robot.direction == SOUTH:
            return directionFree[2]
        else:
            return directionFree[3]

    def validMove(self, inds):

        for (r, c) in inds:
            if not ((self.virtualWall[0] <= r < self.virtualWall[2]) and (
                     self.virtualWall[1] <= c < self.virtualWall[3])):
                return False
        return (self.currentMap[inds[0][0], inds[0][1]] == 1 and
                self.currentMap[inds[1][0], inds[1][1]] == 1 and
                self.currentMap[inds[2][0], inds[2][1]] == 1)

    def northFree(self, center):

        r, c = center
        inds = [[r-2, c], [r-2, c-1], [r-2, c+1]]
        return self.validMove(inds)

    def eastFree(self, center):

        r, c = center
        inds = [[r, c+2], [r-1, c+2], [r+1, c+2]]
        return self.validMove(inds)

    def southFree(self, center):

        r, c = center
        inds = [[r+2, c], [r+2, c-1], [r+2, c+1]]
        return self.validMove(inds)

    def westFree(self, center):

        r, c = center
        inds = [[r, c-2], [r-1, c-2], [r+1, c-2]]
        return self.validMove(inds)

    def frontFree(self):
        r, c = self.robot.center
        counter = 0
        if self.robot.direction == NORTH and self.validMove([[r-2, c], [r-2, c-1], [r-2, c+1]]):
            counter = 1
            while(True):
                if (self.validMove([[r-2-counter, c], [r-2-counter, c-1], [r-2-counter, c+1]])) and\
                        not self.checkFree([1, 2, 3, 0], [r-(counter), c]) and\
                        self.checkExplored([r-(counter), c]):
                    counter += 1
                else:
                    break
        elif self.robot.direction == EAST and self.validMove([[r, c+2], [r-1, c+2], [r+1, c+2]]):
            counter = 1
            while(True):
                if (self.validMove([[r, c+2+counter], [r-1, c+2+counter], [r+1, c+2+counter]])) and\
                        not self.checkFree([1, 2, 3, 0], [r, c+(counter)]) and\
                        self.checkExplored([r, c+(counter)]):
                    counter += 1
                else:
                    break
        elif self.robot.direction == WEST and self.validMove([[r, c-2], [r-1, c-2], [r+1, c-2]]):
            counter = 1
            while(True):
                if (self.validMove([[r, c-2-counter], [r-1, c-2-counter], [r+1, c-2-counter]])) and\
                        not self.checkFree([1, 2, 3, 0], [r, c-(counter)]) and\
                        self.checkExplored([r, c-(counter)]):
                    counter += 1
                else:
                    break
        elif self.robot.direction == SOUTH and self.validMove([[r+2, c], [r+2, c-1], [r+2, c+1]]):
            counter = 1
            while(True):
                if (self.validMove([[r+2+counter, c], [r+2+counter, c-1], [r+2+counter, c+1]])) and\
                        not self.checkFree([1, 2, 3, 0], [r+(counter), c]) and\
                        self.checkExplored([r+(counter), c]):
                    counter += 1
                else:
                    break
        return counter


    def checkExplored(self, center):
        r, c = center
        flag = True
        inds = []
        distanceShort = 3
        distanceLong = 5
        if self.robot.direction == NORTH:
            inds.append(zip([r-1]*distanceShort, range(c+2, c+distanceShort+2)))
            inds.append(zip([r+1]*distanceLong, range(c+2, c+distanceLong+2)))
            inds.append(zip([r-1]*distanceLong, range(c-distanceLong-1, c-1))[::-1])
        elif self.robot.direction == EAST:
            inds.append(zip(range(r+2, r+distanceShort+2), [c+1]*distanceShort))
            inds.append(zip(range(r+2, r+distanceLong+2), [c-1]*distanceLong))
            inds.append(zip(range(r-distanceLong-1, r-1), [c+1]*distanceLong)[::-1])
        elif self.robot.direction == WEST:
            inds.append(zip(range(r-distanceShort-1, r-1), [c-1]*distanceShort)[::-1])
            inds.append(zip(range(r-distanceLong-1, r-1), [c+1]*distanceLong)[::-1])
            inds.append(zip(range(r+2, r+distanceLong+2), [c-1]*distanceLong))
        else:
            inds.append(zip([r+1]*distanceShort, range(c-distanceShort-1, c-1))[::-1])
            inds.append(zip([r-1]*distanceLong, range(c-distanceLong-1, c-1))[::-1])
            inds.append(zip([r+1]*distanceLong, range(c+2, c+distanceLong+2)))
        for sensor in inds:
            if flag:
                for (x, y) in sensor:
                    if (x < self.virtualWall[0] or x == self.virtualWall[2] or
                            y < self.virtualWall[1] or y == self.virtualWall[3] or
                            self.currentMap[x, y] == 2):
                        break
                    elif (self.currentMap[x, y] == 0):
                        flag = False
                        break
        return flag


    def moveStep(self, sensor_vals=None):

        if (sensor_vals):
            self.robot.getSensors(sensor_vals)
        else:
            self.robot.getSensors()
        move = self.nextMove()
        self.getExploredArea()
        if (self.exploredArea == 100):
            return move, True
        else:
            return move, False

    def explore(self):

        print "Starting exploration ..."
        startTime = time.time()
        endTime = startTime + self.timeLimit

        while(time.time() <= endTime):
            if (self.moveStep()):
                print "Exploration completed !"
                return

        print "Time over !"
        return

    def getExploredNeighbour(self):
        locs = np.where(self.currentMap == 0)
        self.virtualWall = [np.min(locs[0]), np.min(locs[1]), np.max(locs[0])+4, np.max(locs[1])+4]
        if ((self.virtualWall[2]-self.virtualWall[0] < 3) and self.virtualWall[2] < MAX_ROWS-3):
            self.virtualWall[2] += 3
        locs = np.asarray(zip(locs[0], locs[1]))
        cost = np.abs(locs[:, 0] - self.robot.center[0]) + np.abs(locs[:, 1] - self.robot.center[1])
        cost = cost.tolist()
        locs = locs.tolist()
        while (cost):
            position = np.argmin(cost)
            coord = locs.pop(position)
            cost.pop(position)
            neighbours = np.asarray([[-2, 0], [2, 0], [0, -2], [0, 2]]) + coord
            neighbours = self.__validInds(neighbours)
            for neighbour in neighbours:
                if (neighbour not in self.exploredNeighbours):
                    self.exploredNeighbours[neighbour] = True
                    return neighbour
        return None
