import cv2
import os
import numpy as np
sift = cv2.xfeatures2d.SIFT_create()

def readImgAsBGR(filepath):
    return cv2.imread(filepath)

def readImgAsGray(filepath):
    img_BGR = readImgAsBGR(filepath)
    return cv2.cvtColor(img_BGR,cv2.COLOR_BGR2GRAY)

def trainingKpDes(directory):
    # returns a list of [keypoint, descriptor, filename]
    # generate training keypoints and descriptors
    keypoint_descriptor_list = []
    for entry in os.scandir(directory):
        #  print('Reading {} for training'.format(entry.name))
        # img_BGR = readImgAsBGR(entry.path)
        img_gray= readImgAsGray(entry.path)
        training_kp_des = sift.detectAndCompute(img_gray, None)
        keypoint_descriptor_list.append([training_kp_des[0],training_kp_des[1],entry.name])
    return keypoint_descriptor_list
keypoint_descriptor_list = trainingKpDes("Training")

def constructFlann(algorithm, trees, checks):
    # build flann matcher
    flann_params = {
        # refer to flann docs
        'algorithm': algorithm,
        'trees': trees
    }
    flann_checks = {
        # higher check value means more precise but more time
        'checks': checks
    }
    flann = cv2.FlannBasedMatcher(flann_params, flann_checks)
    return flann
flann = constructFlann(1, 5, 50)

# read query image
# queryImage_BGR = cv2.imread('Query/Blue 2.jpg')
queryImage_BGR = readImgAsBGR('Query/Blue 2.jpg')

# queryImage_gray = cv2.cvtColor(queryImage_BGR,cv2.COLOR_BGR2GRAY)
queryImage_gray = readImgAsGray('Query/Blue 2.jpg')




# generate query keypoints and descriptors
query_kp, query_des = sift.detectAndCompute(queryImage_gray,None)


###############
# build matches
for (training_kp, training_des, training_name) in keypoint_descriptor_list:
    matches = flann.knnMatch(query_des, training_des, k=2)
    good = []
    for m, n in matches:
        if m.distance < 0.7 * n.distance:
            good.append(m)
    print('num matches with {}: {}'.format(training_name, len(good)))

    # Need to draw only good matches, so create a mask
    matchesMask = [[0,0] for i in range(len(matches))]
    # ratio test as per Lowe's paper
    for i,(m,n) in enumerate(matches):
        if m.distance < 0.7*n.distance:
            matchesMask[i]=[1,0]