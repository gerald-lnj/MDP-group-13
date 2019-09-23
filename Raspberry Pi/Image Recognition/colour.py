import cv2

img_BGR = cv2.imread('blue.jpg')
img_HSV = cv2.cvtColor(img_BGR, cv2.COLOR_BGR2HSV)
colour = img_HSV[0,0]
print(colour)