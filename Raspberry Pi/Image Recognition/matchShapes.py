import cv2
import numpy as np
import os
import platform

imshowDebug = True # mostly going to be used for cv2.imshow debugging perposes
if platform.node() == 'raspberrypi': # can't display images on rpi
    imshowDebug = False

# define the list of boundaries
boundaries_HSV = [
    # ([90, 140, 140], [110, 255, 255], "B"), # blue, tested
    # ([17, 15, 100], [50, 56, 200], "R"), # red
    # ([25, 146, 190], [62, 174, 250], "Y"), # yellow
    # ([103, 86, 65], [145, 133, 128], "W"), # white, dummy
    # ([55, 35, 50], [75, 255, 255], "G"), # green, dummy
    ([0, 0, 0], [120, 60, 100], "B"), # black, dummy
    ]

def sortAscending(list): 
    l = len(list) 
    for i in range(0, l): 
        for j in range(0, l-i-1): 
            if (list[j][1] > list[j + 1][1]): 
                tempo = list[j] 
                list[j]= list[j + 1] 
                list[j + 1]= tempo 
    return list 

def sortDescending(list): 
    l = len(list) 
    for i in range(0, l): 
        for j in range(0, l-i-1): 
            if (list[j][1] < list[j + 1][1]): 
                tempo = list[j] 
                list[j]= list[j + 1] 
                list[j + 1]= tempo 
    return list 

# returns colour, nonZerocount and masked image
def detectColourAndMask(query_image_filepath):
    # read query img as BGR, HSV, grayscale
    query_img_BGR = cv2.imread(query_image_filepath)
    query_img_HSV = cv2.cvtColor(query_img_BGR, cv2.COLOR_BGR2HSV)

    # list for saving non-zero counts of colour, and their masked imgs. init to 0.
    colour_segmenting_results = {
        'colour': 0,
        'nonZeroCount': 0,
        'image': 0
    }

    # loop over the boundaries
    for (lower, upper, colour) in boundaries_HSV:
        # create NumPy arrays from the boundaries
        lower = np.array(lower, dtype = "uint8")
        upper = np.array(upper, dtype = "uint8")
        
        # get mask from specified boundaries
        mask = cv2.inRange(query_img_HSV, lower, upper)

        # count nonzero pixels in mask
        nonZeroCount = cv2.countNonZero(mask)

        # apply mask
        output = cv2.bitwise_and(query_img_BGR, query_img_BGR, mask = mask)

        # save results and output img to colour_nonzero_count
        if nonZeroCount > colour_segmenting_results['nonZeroCount']:
            colour_segmenting_results = {
                'colour': colour,
                'nonZeroCount': nonZeroCount,
                'image': output
            }

    print('{} nonZero count: {}'.format(colour_segmenting_results['colour'], colour_segmenting_results['nonZeroCount']))
    return colour_segmenting_results

def matchShapes(colour_segmenting_results, reference_img_filepath):
    im1 = colour_segmenting_results['image']
    im1_gray = cv2.cvtColor(im1,cv2.COLOR_BGR2GRAY)
    _,im1_binary = cv2.threshold(im1_gray, 1, 255, cv2.THRESH_BINARY) # to check if this is accurate binarisation
    results = []

    for entry in os.scandir('Training'):
        if (entry.name.split('.')[-1] == 'jpg') and (entry.name[0] == colour_segmenting_results['colour']):
            im2 = cv2.imread(entry.path)
            im2_gray = cv2.cvtColor(im2,cv2.COLOR_BGR2GRAY)
            _,im2_binary = cv2.threshold(im2_gray, 128, 255, cv2.THRESH_BINARY)
            results.append([entry.name.replace('.jpg', ''), cv2.matchShapes(im1_binary,im2_binary,cv2.CONTOURS_MATCH_I2,0)])
    results = sortAscending(results)

    for result in results:
        print(result)

def main(query_image_filepath):
    colour_segmenting_results = detectColourAndMask(query_image_filepath)

    matchShapes(colour_segmenting_results, query_image_filepath)
    return

main('Query/Blue/bluecrop.jpg')