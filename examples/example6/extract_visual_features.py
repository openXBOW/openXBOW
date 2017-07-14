#!/bin/python
# python2
# Extract visual features and store them in a csv file

import os
import skimage
from   skimage import io      # submodule
from   skimage import feature # submodule

# Image folder
folder_images  = "./images/"  # The images are stored here

# Output file
outfilename = "visual_features.csv"

# Clear LLD files
if os.path.exists(outfilename):
    os.remove(outfilename)

# Extract visual (Daisy) features
for fn in os.listdir(folder_images):
    infilename   = folder_images + fn
    instancename = os.path.splitext(fn)[0]
    
    image = skimage.io.imread(infilename)
    image = skimage.color.rgb2gray(image)
    
    descriptors = skimage.feature.daisy(image, step=8, radius=15, rings=3, histograms=4, orientations=8, normalization='l1')
    
    with open(outfilename, 'a') as fo:
        for P in descriptors:
            for Q in P:
                fo.write(instancename)
                for feat in Q:
                    fo.write(';' + str(feat))
                fo.write('\n')
