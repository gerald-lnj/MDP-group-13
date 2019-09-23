import cv2
import os
import numpy as np
from matplotlib import pyplot as plt

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
        keypoint_descriptor_list.append([entry.name, img_gray, training_kp_des[0],training_kp_des[1]])
    return keypoint_descriptor_list

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

def main(training_directory_path, query_image_path):
    # generate training keypoints and descriptors
    keypoint_descriptor_list = trainingKpDes(training_directory_path)

    #create flann matcher
    flann = constructFlann(1, 5, 100)

    # read query image, and generate keypoints and descriptors
    queryImage_gray = readImgAsGray(query_image_path)
    query_kp, query_des = sift.detectAndCompute(queryImage_gray,None)

    # build matches
    for (training_name, training_img, training_kp, training_des) in keypoint_descriptor_list:
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
            if m.distance < 0.75*n.distance:
                matchesMask[i]=[1,0]
        draw_params = dict(matchColor = (0,255,0),
                   singlePointColor = (255,0,0),
                   matchesMask = matchesMask,
                   flags = 0)
        img3 = cv2.drawMatchesKnn(training_img,training_kp,queryImage_gray,query_kp,matches,None,**draw_params)
        # show the images
        cv2.imshow(training_name, img3)
        cv2.waitKey(0)



# main('Training2', 'Query/Blue 2.jpg')
main('Training2', 'Query/box_in_scene.png')