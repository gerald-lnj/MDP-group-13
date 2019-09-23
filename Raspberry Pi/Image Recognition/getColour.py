import cv2

img_BGR = cv2.imread('Training/G Arrow.jpg')
img_HSV = cv2.cvtColor(img_BGR, cv2.COLOR_BGR2HSV)
colour = img_HSV[500,500]
print(colour)