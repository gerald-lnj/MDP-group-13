import cv2
import numpy as np
import os
import platform

imshowDebug = True # mostly going to be used for cv2.imshow debugging perposes
if platform.node() == 'raspberrypi': # can't display images on rpi
    imshowDebug = False

# define the list of boundaries
boundaries_HSV = [
    ([85, 140, 140], [120, 255, 255], "B"), # blue, tested
    ([0, 140, 140], [10, 255, 255], "R"), # red
    ([20, 140, 140], [35, 255, 255], "Y"), # yellow
    # ([0, 0, 0], [0, 0, 255], "W"), # white, dummy
    ([55, 50, 50], [75, 255, 255], "G"), # green, dummy
    ]
black_boundaries_HSV = [np.array([0, 0, 0], dtype = "uint8"), np.array([120, 127, 100], dtype = "uint8")]

result_dict = {
    'W Arrow': '1',
    'R Arrow': '2',
    'G Arrow': '3',
    'B Arrow': '4',
    'Y Circle': '5',
    'B 1': '6',
    'G 2': '7',
    'R 3': '8',
    'W 4': '9',
    'Y 5': '10',
    'R A': '11',
    'G B': '12',
    'W C': '13',
    'B D': '14',
    'Y E': '15',
}

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
    # list for saving non-zero counts of colour, and their masked imgs. init to 0.
    colour_segmenting_results = {
        'colour': 0,
        'nonZeroCount': 0,
        'img_BGR': 0,
        'masked_image': 0,
        'bounding': 0
    }
    try:
        filename = query_image_filepath.split('/')[-1]
        query_img_BGR = cv2.imread(query_image_filepath)
        query_img_BGR = cv2.GaussianBlur(query_img_BGR, (5, 5), 0)

        query_img_HSV = cv2.cvtColor(query_img_BGR, cv2.COLOR_BGR2HSV) # raw img in hsv


        # get mask of black areas (playing field only)
        black_mask = mask = cv2.inRange(query_img_HSV, black_boundaries_HSV[0], black_boundaries_HSV[1]) # black HSV range
        ret, thresh = cv2.threshold(black_mask, 127, 255, 0)

        # get countours of playing field
        contours, hierarchy = cv2.findContours(thresh, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

        if len(contours) > 0:
            largestContour = contours[0]
            for countour in contours:
                if (cv2.contourArea(countour) > cv2.contourArea(largestContour)):
                    largestContour = countour

            # use the largest countour as selection (exclude windows, etc)
            filled_black_mask = cv2.drawContours(mask, [largestContour], -1, (255, 255, 255), cv2.FILLED)
        else:
            filled_black_mask = query_img_HSV
        # original img, with only the playing field area
        playing_field_BGR = cv2.bitwise_and(query_img_BGR, query_img_BGR, mask = mask)
        playing_field_HSV = cv2.cvtColor(playing_field_BGR, cv2.COLOR_BGR2HSV) # raw img in hsv

        # if imshowDebug:
        #     cv2.imshow('playing_field', playing_field)
        #     cv2.waitKey(0)
        
        # loop over the boundaries
        for (lower, upper, colour) in boundaries_HSV:
            # create NumPy arrays from the boundaries
            lower = np.array(lower, dtype = "uint8")
            upper = np.array(upper, dtype = "uint8")
            
            # get mask from specified boundaries
            mask = cv2.inRange(playing_field_HSV, lower, upper)
            
            

            # count nonzero pixels in mask
            nonZeroCount = cv2.countNonZero(mask)

            # apply mask
            output = cv2.bitwise_and(query_img_BGR, query_img_BGR, mask = mask)

            # save results and output img to colour_nonzero_count
            if nonZeroCount > colour_segmenting_results['nonZeroCount']:
                colour_segmenting_results = {
                    'colour': colour,
                    'nonZeroCount': nonZeroCount,
                    'img_BGR': query_img_BGR,
                    'masked_image': output,
                    'mask': mask
                }

        # create bounding rect
        contours, hierarchy = cv2.findContours(colour_segmenting_results['mask'], cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
        cont_sorted = sorted(contours, key=cv2.contourArea, reverse=True)[:5]
        bounding_contour = cont_sorted[0]

        # x,y,w,h = cv2.boundingRect(cont_sorted[0])
        # bounding_box_img = cv2.rectangle(query_img_BGR,(x,y),(x+w,y+h),(0,0,255),2)
        colour_segmenting_results['bounding'] = bounding_contour

        if imshowDebug:
            cv2.imshow(
                '{}: Original BGR, Playing area Mask, {} mask'.format(filename, colour), 
                np.concatenate(
                    (query_img_BGR,
                    playing_field_BGR,
                    cv2.cvtColor(
                        mask,
                        cv2.COLOR_GRAY2BGR),
                    ),
                    axis=1
                )
            )
            cv2.waitKey(0)
            cv2.destroyAllWindows()


        print('{} nonZero count: {}'.format(colour_segmenting_results['colour'], colour_segmenting_results['nonZeroCount']))
        return colour_segmenting_results
    except cv2.error as e:
        print('cv2 error in detectColourAndMask(): \n{}'.format(e))

def matchShapes(colour_segmenting_results, reference_img_directory_path):
    try:
        # colour_segmenting_results format:
        # colour_segmenting_results = {
        #     'colour': string, refer to boundaries for the exact text
        #     'nonZeroCount': int, counts number of detected pixels of colour
        #     'image': img object returned from cv2.imread()
        # }
        query_BGR = colour_segmenting_results['masked_image']
        query_gray = cv2.cvtColor(query_BGR,cv2.COLOR_BGR2GRAY)
        _,query_gray_binary = cv2.threshold(query_gray, 1, 255, cv2.THRESH_BINARY) # to check if this is accurate binarisation
        results = []

        for reference_entry in os.scandir(reference_img_directory_path):
            if ((reference_entry.name.split('.')[-1] == 'jpg') # ignore .DS_Store for macs
            and (reference_entry.name[0] == colour_segmenting_results['colour'])): # filter by colour. eg if the colour is blue, only pick those starting with 'B'
                reference_BGR = cv2.imread(reference_entry.path)
                reference_gray = cv2.cvtColor(reference_BGR,cv2.COLOR_BGR2GRAY)
                _,reference_gray_binary = cv2.threshold(reference_gray, 128, 255, cv2.THRESH_BINARY)
                results.append([reference_entry.name.replace('.jpg', ''), cv2.matchShapes(query_gray_binary,reference_gray_binary,cv2.CONTOURS_MATCH_I2,0)])
        results = sortAscending(results)

        x,y,w,h = cv2.boundingRect(colour_segmenting_results['bounding'])
        bounding_box_img = cv2.rectangle(colour_segmenting_results['img_BGR'],(x,y),(x+w,y+h),(0,0,255),2)
        bounding_box_img = cv2.putText(bounding_box_img, result_dict[results[0][0]], (x, y-10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (100,255,100), 2)
        if (imshowDebug):
            cv2.imshow('results ({})'.format(colour_segmenting_results['colour']), bounding_box_img)
            cv2.waitKey(0)
            cv2.destroyAllWindows()


        


        for result in results:
            print(result)
        return result, bounding_box_img
    except cv2.error as e:
        print('cv2 error: {}'.format(e))

def main(query_image_filepath, reference_img_directory_path):
    name = query_image_filepath.split('/')[-1].split('.')[0]
    colour_segmenting_results = detectColourAndMask(query_image_filepath)

    result, bounding_box_img = matchShapes(colour_segmenting_results, reference_img_directory_path)
    print(result)
    result_name = result[0]
    result_id = result_dict[result_name]
    result_distance = result[1]
    cv2.imwrite('Raspberry Pi/Image Recognition/Output/{}.jpeg'.format(name), bounding_box_img)    


reference_img_directory_path = 'Raspberry Pi/Image Recognition/Training'

# main('/Users/gerald/Documents/MDP/MDP-group-13/Raspberry Pi/Image Recognition/Query/Blue/b 1b.jpeg', reference_img_directory_path)
# temp = detectColourAndMask('Query/Green/green 2.jpeg')

# for root, dirs, files in os.walk('/Users/gerald/Documents/MDP/MDP-group-13/Raspberry Pi/Image Recognition/Query/Green'):
#     for name in files:
#         print(os.path.join(root, name))
#         temp = detectColourAndMask(os.path.join(root, name))

for root, dirs, files in os.walk('Raspberry Pi/Image Recognition/Query'):
    for name in files:
        if name != '.DS_Store':
            print(os.path.join(root, name))
            temp = main(os.path.join(root, name), reference_img_directory_path)
            print('\n')
