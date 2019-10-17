import cv2

img_BGR = cv2.imread('Query/green/green 2.png', cv2.IMREAD_UNCHANGED)
img_BGR = cv2.GaussianBlur(img_BGR, (5, 5), 0)
img_HSV = cv2.cvtColor(img_BGR, cv2.COLOR_BGR2HSV)
height, width, _ = img_HSV.shape

lower_boundary = [180,255,255]
upper_boundary = [0,0,0]

for i in range(height):
    for j in range(width):
        colour = img_HSV[i, j]
        if img_BGR[i,j][3] > 250:
            for k in range(len(lower_boundary)):
                lower_boundary[k] =  colour[k]  if colour[k] < lower_boundary[k] else lower_boundary[k]
            for k in range(len(lower_boundary)):
                upper_boundary[k] =  colour[k]  if colour[k] > upper_boundary[k] else upper_boundary[k]

print('lower boundary: {}'.format(lower_boundary))
print('upper boundary: {}'.format(upper_boundary))
