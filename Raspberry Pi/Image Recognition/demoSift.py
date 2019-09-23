import numpy as np
import os
import cv2
from matplotlib import pyplot as plt

def readImgAsBGR(filepath):
    return cv2.imread(filepath)

def readImgAsGray(filepath):
    img_BGR = readImgAsBGR(filepath)
    return cv2.cvtColor(img_BGR,cv2.COLOR_BGR2GRAY)

# Initiate SIFT detector
sift = cv2.xfeatures2d.SIFT_create()

queryImg = cv2.imread('Query/Blue 2.jpg',0)

# generate training keypoints and descriptors
keypoint_descriptor_list = []
for entry in os.scandir("Training2"):
    #  print('Reading {} for training'.format(entry.name))
    # img_BGR = readImgAsBGR(entry.path)
    img_gray= readImgAsGray(entry.path)
    training_kp_des = sift.detectAndCompute(img_gray, None)
    keypoint_descriptor_list.append([entry.name, img_gray, training_kp_des[0],training_kp_des[1]])


# find the keypoints and descriptors with SIFT
queryKp, queryDes = sift.detectAndCompute(queryImg,None)

# FLANN parameters
FLANN_INDEX_KDTREE = 0
index_params = dict(algorithm = FLANN_INDEX_KDTREE, trees = 5)
search_params = dict(checks=50)   # or pass empty dictionary

flann = cv2.FlannBasedMatcher(index_params,search_params)

for (training_name, trainingImg, trainingKp, trainingDes) in keypoint_descriptor_list:
    matches = flann.knnMatch(trainingDes,queryDes,k=2)

    # Need to draw only good matches, so create a mask
    matchesMask = [[0,0] for i in range(len(matches))]

    # ratio test as per Lowe's paper
    for i,(m,n) in enumerate(matches):
        if m.distance < 0.7*n.distance:
            matchesMask[i]=[1,0]

    draw_params = dict(matchColor = (0,255,0),
                    singlePointColor = (255,0,0),
                    matchesMask = matchesMask,
                    flags = 0)

    img3 = cv2.drawMatchesKnn(trainingImg,trainingKp,queryImg,queryKp,matches,None,**draw_params)

    plt.imshow(img3,),plt.show()