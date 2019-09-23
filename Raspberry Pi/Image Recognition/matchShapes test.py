import cv2
import numpy as np

# define the list of boundaries
boundaries_HSV = {
#   "red": ([17, 15, 100], [50, 56, 200], "red"),
  "blue": ([90, 150, 140], [110, 170, 185], "blue"),
#   "yellow": ([25, 146, 190], [62, 174, 250]),
#   "gray": ([103, 86, 65], [145, 133, 128]),
}


img_BGR = cv2.imread('Test/Blue 2.jpg')
img_HSV = cv2.cvtColor(img_BGR, cv2.COLOR_BGR2HSV)
img_gray= cv2.cvtColor(img_BGR,cv2.COLOR_BGR2GRAY)


# loop over the boundaries
for (lower, upper, colour) in boundaries_HSV.values():
    # create NumPy arrays from the boundaries
    lower = np.array(lower, dtype = "uint8")
    upper = np.array(upper, dtype = "uint8")
    
    # find the colors within the specified boundaries and apply the mask
    mask = cv2.inRange(img_HSV, lower, upper)
    print('nonZero count: {}'.format(cv2.countNonZero(mask)))
    if cv2.countNonZero(mask):
        print('{} detected'.format(colour))
        output = cv2.bitwise_and(img_BGR, img_BGR, mask = mask)
 
        # show the images
        cv2.imshow("images", np.hstack([img_BGR, output]))
        cv2.waitKey(0)
    else:
        print('{} not detected'.format(colour))