
import copy
import numpy as np

from Constants import MAX_ROWS, MAX_COLS, NORTH, SOUTH, EAST, WEST, FORWARD, LEFT, RIGHT


class Node:


    def __init__(self, value, coord, H):

        self.value = value
        self.coord = coord
        self.parent = None
        self.H = H
        self.G = float('inf')


class FastestPath:



    def __init__(self, exploredMap, start, goal, direction, waypoint=None, calibrateLim=5, sim=True):

        self.exploredMap = exploredMap
        self.graph = []
        self.start = start
        self.goal = goal
        self.waypoint = waypoint
        self.index = 1
        self.direction = direction
        self.path = []
        self.movement = []
        self.calibrateLim = calibrateLim
        self.sim = sim
        if sim:
            from Simulator import Robot
            self.robot = Robot(self.exploredMap, direction, start, None)
        else:
            from Real import Robot
            self.robot = Robot(self.exploredMap, direction, start)

    def __getHeuristicCosts(self, goal):

        cols, rows = np.meshgrid(range(0, 15), range(0, 20))
        cost = np.zeros([20, 15])
        cost = np.sqrt(np.square(rows - goal[0]) + np.square(cols - goal[1]))
        cost /= np.max(cost)
        return cost

    def __validInds(self, inds):

        valid = []
        for i in inds:
            r, c = i
            x, y = np.meshgrid([-1, 0, 1], [-1, 0, 1])
            x, y = x+r, y+c
            if (np.any(x < 0) or np.any(y < 0) or np.any(x >= MAX_ROWS) or np.any(y >= MAX_COLS)):
                valid.append(False)
            elif (np.any(self.exploredMap[x[0, 0]:x[0, 2]+1, y[0, 0]:y[2, 0]+1] != 1)):
                valid.append(False)
            else:
                valid.append(True)
        return [inds[i] for i in range(len(inds)) if valid[i]]

    def __getNeighbours(self, loc):

        r, c = loc.coord
        inds = [(r-1, c), (r+1, c), (r, c-1), (r, c+1)]
        inds = self.__validInds(inds)
        neighbours = [self.graph[n[0]][n[1]] for n in inds]

        return [n for n in neighbours if n.value == 1]


    def __getCost(self, current_pos, next_pos):
        if self.direction in [NORTH, SOUTH]:
            if current_pos[1] == next_pos[1]:
                return 0
            else:
                return 1
        else:
            if current_pos[0] == next_pos[0]:
                return 0
            else:
                return 1
        return 1


    def __setDirection(self, prev_pos, current_pos):
        if prev_pos[0] < current_pos[0]:
            self.direction = SOUTH
        elif prev_pos[1] < current_pos[1]:
            self.direction = EAST
        elif prev_pos[1] > current_pos[1]:
            self.direction = WEST
        else:
            self.direction = NORTH


    def __astar(self, start, goal):

        goal = self.graph[goal[0]][goal[1]]

        closedSet = set()

        openSet = set()
        current = self.graph[start[0]][start[1]]
        current.G = 0

        openSet.add(current)
        prev = None
        while (openSet):
            current = min(openSet, key=lambda o: o.G + o.H)
            if prev:
                self.__setDirection(prev.coord, current.coord)

            if (current == goal):
                path = []
                while (current.parent):
                    path.append(current.coord)
                    current = current.parent
                path.append(current.coord)
                return path[::-1]
            else:
                openSet.remove(current)
                closedSet.add(current)
                for node in self.__getNeighbours(current):
                    if node in closedSet:
                        continue
                    if node in openSet:

                        new_g = current.G + self.__getCost(current.coord, node.coord)
                        if node.G > new_g:
                            node.G = new_g
                            node.parent = current
                    else:

                        node.G = current.G + self.__getCost(current.coord, node.coord)
                        node.parent = current
                        openSet.add(node)
            prev = copy.deepcopy(current)

        raise ValueError('No Path Found')

    def __initGraph(self, h_n):

        self.graph = []
        for row in xrange(MAX_ROWS):
            self.graph.append([])
            for col in xrange(MAX_COLS):
                self.graph[row].append(Node(self.exploredMap[row][col], (row, col), h_n[row][col]))

    def getFastestPath(self):

        path = []
        start = copy.copy(self.start)
        if (self.waypoint):
            h_n = self.__getHeuristicCosts(self.waypoint)
            self.__initGraph(h_n)
            fsp = self.__astar(start, self.waypoint)
            start = copy.copy(self.waypoint)

            path.extend(fsp[:-1])
        h_n = self.__getHeuristicCosts(self.goal)
        self.__initGraph(h_n)
        fsp = self.__astar(start, self.goal)
        path.extend(fsp)
        self.path = path
        self.markMap()

    def markMap(self):

        for ind in self.path:
            self.exploredMap[tuple(ind)] = 6

    def moveStep(self):

        movement = []
        if (self.robot.center.tolist() != self.path[self.index]):
            diff = self.robot.center - np.asarray(self.path[self.index])
            if (diff[0] == -1 and diff[1] == 0):  # Going south
                if self.robot.direction == NORTH:
                    movement.extend((RIGHT, RIGHT, FORWARD))
                elif self.robot.direction == EAST:
                    movement.extend((RIGHT, FORWARD))
                elif self.robot.direction == SOUTH:
                    movement.append(FORWARD)
                else:
                    movement.extend((LEFT, FORWARD))
            elif (diff[0] == 0 and diff[1] == 1):  # Going west
                if self.robot.direction == NORTH:
                    movement.extend((LEFT, FORWARD))
                elif self.robot.direction == EAST:
                    movement.extend((RIGHT, RIGHT, FORWARD))
                elif self.robot.direction == SOUTH:
                    movement.extend((RIGHT, FORWARD))
                else:
                    movement.append(FORWARD)
            elif (diff[0] == 0 and diff[1] == -1):  # Going east
                if self.robot.direction == NORTH:
                    movement.extend((RIGHT, FORWARD))
                elif self.robot.direction == EAST:
                    movement.append(FORWARD)
                elif self.robot.direction == SOUTH:
                    movement.extend((LEFT, FORWARD))
                else:
                    movement.extend((RIGHT, RIGHT, FORWARD))
            else:  # Going north
                if self.robot.direction == NORTH:
                    movement.append(FORWARD)
                elif self.robot.direction == EAST:
                    movement.extend((LEFT, FORWARD))
                elif self.robot.direction == SOUTH:
                    movement.extend((RIGHT, RIGHT, FORWARD))
                else:
                    movement.extend((RIGHT, FORWARD))
            for move in movement:
                self.robot.moveBot(move)
        self.movement.extend(movement)

        self.index += 1
