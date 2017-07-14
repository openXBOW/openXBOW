#!/bin/python
# python2
# Prepare the files for image classification with the Caltech101 dataset: http://www.vision.caltech.edu/Image_Datasets/Caltech101/
#  -Generate a labels file

import os
import shutil

folder_caltech = "./101_ObjectCategories/" # MODIFY this path to the folder with the caltech 101 files
folder_images  = "./images/"               # The images are stored here

labelsfilename = "labels.csv"

list_classes = ["anchor", "barrel", "elephant", "pizza", "saxophone"]  # Take only those image classes into account

if not os.path.exists(folder_images):
    os.mkdir(folder_images)

if os.path.exists(labelsfilename):
    os.remove(labelsfilename)

for cl in list_classes:
    for fn in os.listdir(folder_caltech + cl):
        infilename   = folder_caltech + cl + "/" + fn
        outfilename  = folder_images + cl + "_" + fn
        shutil.copyfile(infilename,outfilename)
        
        label        = cl  # the label (target) is the 5th character in the filename
        instancename = label + "_" + os.path.splitext(fn)[0]
        with open(labelsfilename, 'a') as fl:
            fl.write(instancename + ';' + label + '\n')
