
import numpy as np
import os

from Constants import MAX_ROWS, MAX_COLS, NORTH, SOUTH, EAST, WEST, RIGHT, LEFT

class Robot:


    def __init__(self, exploredMap, direction, start, realMap):
 (string): File name of the real map

        self.exploredMap = exploredMap
        self.direction = direction
        self.center = np.asarray(start)
        self.head = None
        self.setHead()
        if realMap:
            self.realMap = realMap
            self.map = self.loadMap()
        self.movement = []
        self.markArea(start, 1)

    def markArea(self, center, value):

        self.exploredMap[center[0]-1:center[0]+2, center[1]-1:center[1]+2] = value

    def setHead(self):
        if (self.direction == NORTH):
            self.head = self.center + [-1, 0]
        elif (self.direction == EAST):
            self.head = self.center + [0, 1]
        elif (self.direction == SOUTH):
            self.head = self.center + [1, 0]
        else:
            self.head = self.center + [0, -1]

    def loadMap(self):


        with open(os.path.join('Maps', self.realMap)) as f:
            return np.genfromtxt(f, dtype=int, delimiter=1)

    def getValue(self, inds):

        vals = []
        for (r, c) in inds:
            if (0 <= r < MAX_ROWS) and (0 <= c < MAX_COLS):
                if self.map[r][c] == 2:
                    if self.exploredMap[r][c] == 0:
                        self.exploredMap[r][c] = 2
                    vals.append(2)
                    break
                else:
                    if self.exploredMap[r][c] == 0:
                        self.exploredMap[r][c] = 1
                    vals.append(1)
        if len(vals) < len(inds):
            vals = vals + [None]*(len(inds)-len(vals))
        return vals

    def getSensors(self):

        distanceShort = 3
        distanceLong = 5
        r, c = self.center

        # Front Left
        if self.direction == NORTH:
            self.getValue(zip(range(r-distanceShort-1, r-1), [c-1]*distanceShort)[::-1])
        elif self.direction == EAST:
            self.getValue(zip([r-1]*distanceShort, range(c+2, c+distanceShort+2)))
        elif self.direction == WEST:
            self.getValue(zip([r+1]*distanceShort, range(c-distanceShort-1, c-1))[::-1])
        else:
            self.getValue(zip(range(r+2, r+distanceShort+2), [c+1]*distanceShort))

        # Front Center
        if self.direction == NORTH:
            self.getValue(zip(range(r-distanceShort-1, r-1), [c]*distanceShort)[::-1])
        elif self.direction == EAST:
            self.getValue(zip([r]*distanceShort, range(c+2, c+distanceShort+2)))
        elif self.direction == WEST:
            self.getValue(zip([r]*distanceShort, range(c-distanceShort-1, c-1))[::-1])
        else:
            self.getValue(zip(range(r+2, r+distanceShort+2), [c]*distanceShort))

        # Front Right
        if self.direction == NORTH:
            self.getValue(zip(range(r-distanceShort-1, r-1), [c+1]*distanceShort)[::-1])
        elif self.direction == EAST:
            self.getValue(zip([r+1]*distanceShort, range(c+2, c+distanceShort+2)))
        elif self.direction == WEST:
            self.getValue(zip([r-1]*distanceShort, range(c-distanceShort-1, c-1))[::-1])
        else:
            self.getValue(zip(range(r+2, r+distanceShort+2), [c-1]*distanceShort))

        # Right Top
        if self.direction == NORTH:
            self.getValue(zip([r-1]*distanceShort, range(c+2, c+distanceShort+2)))
        elif self.direction == EAST:
            self.getValue(zip(range(r+2, r+distanceShort+2), [c+1]*distanceShort))
        elif self.direction == WEST:
            self.getValue(zip(range(r-distanceShort-1, r-1), [c-1]*distanceShort)[::-1])
        else:
            self.getValue(zip([r+1]*distanceShort, range(c-distanceShort-1, c-1))[::-1])

        # Right Bottom
        if self.direction == NORTH:
            self.getValue(zip([r+1]*distanceLong, range(c+2, c+distanceLong+2)))
        elif self.direction == EAST:
            self.getValue(zip(range(r+2, r+distanceLong+2), [c-1]*distanceLong))
        elif self.direction == WEST:
            self.getValue(zip(range(r-distanceLong-1, r-1), [c+1]*distanceLong)[::-1])
        else:
            self.getValue(zip([r-1]*distanceLong, range(c-distanceLong-1, c-1))[::-1])

        # Left Top
        if self.direction == NORTH:
            self.getValue(zip([r-1]*distanceLong, range(c-distanceLong-1, c-1))[::-1])
        elif self.direction == EAST:
            self.getValue(zip(range(r-distanceLong-1, r-1), [c+1]*distanceLong)[::-1])
        elif self.direction == WEST:
            self.getValue(zip(range(r+2, r+distanceLong+2), [c-1]*distanceLong))
        else:
            self.getValue(zip([r+1]*distanceLong, range(c+2, c+distanceLong+2)))

    def moveBot(self, movement):
        """Simulates the bot movement based on current location, direction and received action

        Args:
            movement (string): Next movement received (see Constants)
        """
        self.movement.append(movement)
        if self.direction == NORTH:
            if movement == RIGHT:
                self.direction = EAST
            elif movement == LEFT:
                self.direction = WEST
            else:
                self.center = self.center + [-1, 0]
                self.markArea(self.center, 1)
            self.setHead()
        elif self.direction == EAST:
            if movement == RIGHT:
                self.direction = SOUTH
            elif movement == LEFT:
                self.direction = NORTH
            else:
                self.center = self.center + [0, 1]
                self.markArea(self.center, 1)
            self.setHead()
        elif self.direction == SOUTH:
            if movement == RIGHT:
                self.direction = WEST
            elif movement == LEFT:
                self.direction = EAST
            else:
                self.center = self.center + [1, 0]
                self.markArea(self.center, 1)
            self.setHead()
        else:
            if movement == RIGHT:
                self.direction = NORTH
            elif movement == LEFT:
                self.direction = SOUTH
            else:
                self.center = self.center + [0, -1]
                self.markArea(self.center, 1)
            self.setHead()

    def descriptor_1(self):
        descriptor = np.zeros([20, 15]).astype(int)
        descriptor[self.exploredMap[::-1, :] != 0] = 1
        bits = '11'
        for row in descriptor:
            bits += ''.join(map(str, row.tolist()))
        bits += '11'
        hex_str = ['%X' % int(bits[i:i+4], 2) for i in range(0, len(bits)-3, 4)]
        return ''.join(hex_str)

    def descriptor_2(self):
        bits = ''
        for row in self.exploredMap[::-1, :]:
            for bit in row:
                if bit == 2:
                    bits += '1'
                elif bit != 0:
                    bits += '0'
        bits += '0'*(4 - len(bits) % 4)
        hex_str = ['%X' % int(bits[i:i+4], 2) for i in range(0, len(bits)-3, 4)]
        return ''.join(hex_str)
