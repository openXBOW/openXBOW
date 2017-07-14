#!/bin/python
# python2
# Prepare the files for emotion recognition with EmoDB
#  -Convert wav to 16 bits (openSMILE cannot read 32bit wave files)
#  -Split the data into a speaker-independent training (6 subjects: 11,12,13,14,15,16) and test partition (4 subjects: 03,08,09,10)
#  -Generate a labels file for both partitions

import os

folder_audio_emodb = "./emodb/wav/"  # MODIFY this path to the folder with the emodb audio files
folder_audio_train = "./wav_train/"  # The audio files in the training partition converted to 16 bits are stored here
folder_audio_test  = "./wav_test/"   # The audio files in the test partition converted to 16 bits are stored here

labelsfilename_train = "labels_train.csv"
labelsfilename_test  = "labels_test.csv"

if not os.path.exists(folder_audio_train):
    os.mkdir(folder_audio_train)
if not os.path.exists(folder_audio_test):
    os.mkdir(folder_audio_test)

if os.path.exists(labelsfilename_train):
    os.remove(labelsfilename_train)
if os.path.exists(labelsfilename_test):
    os.remove(labelsfilename_test)

for fn in os.listdir(folder_audio_emodb):
    infilename   = folder_audio_emodb + fn
    instancename = os.path.splitext(fn)[0]
    
    label = fn[5]  # the label (target) is the 5th character in the filename
    
    if int(fn[:2]) > 10:  # training partition
        outfilename = folder_audio_train + fn
        with open(labelsfilename_train, 'a') as fl:
            fl.write(instancename + ';' + label + '\n')
    else:  # test partition
        outfilename = folder_audio_test + fn
        with open(labelsfilename_test, 'a') as fl:
            fl.write(instancename + ';' + label + '\n')
    
    sox_call = "sox " + infilename + " -b 16 " + outfilename
    os.system(sox_call)
