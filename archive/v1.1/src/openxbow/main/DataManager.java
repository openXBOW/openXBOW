/*F************************************************************************
 * openXBOW - the Passau Open-Source Crossmodal Bag-of-Words Toolkit
 * Copyright (C) 2016-2017, 
 *   Maximilian Schmitt & Björn Schuller: University of Passau.
 *   Contact: maximilian.schmitt@uni-passau.de
 *  
 *  This program is free software: you can redistribute it and/or modify 
 *  it under the terms of the GNU General Public License as published by 
 *  the Free Software Foundation, either version 3 of the License, or 
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License 
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ***********************************************************************E*/

package openxbow.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import openxbow.io.Reader;

public class DataManager {
    /* One ID is given for each instance (=number of frames to be put into one bag) */
    public  Reader                     reader      = null;
    private Map<Integer,String>        mapIDName   = null;  /* Stores the name for each id, without "'" */
    private Map<String,Integer>        mapNameID   = null;  /* Stores the id for each name (without "'"), in case we have different time instances for the same name, the first id with name is given */
    private Map<Integer,String[]>      mapIDLabels = null;  /* Stores the labels for each id */
    private Map<Integer,List<Integer>> mapFrameIDs = null;  /* Stores the corresponding ID(s) for each frame */
    private Map<Integer,Float>         mapIDTime   = null;  /* Stores the time stamp (center of each segment) for every id, if the input data must be segmented */
    private List<Boolean>              bIsActive   = null;  /* Specifies for each frame if activity has been detected (true) or not. This list is initialized in the Preprocessor. */
    private List<Integer>              numFrames   = null;  /* Stores the number of frames belonging to one ID */
    
    private boolean bWindowing = false;
    private float   windowSize = 0.0f;
    private float   hopSize    = 0.0f;
    
    public DataManager(Reader reader) {
        this.reader      = reader;
        this.mapIDName   = new HashMap<Integer,String>();
        this.mapNameID   = new HashMap<String,Integer>();
        this.mapIDLabels = new HashMap<Integer,String[]>();
        this.mapFrameIDs = new HashMap<Integer,List<Integer>>();
        this.numFrames   = new ArrayList<Integer>();
        this.bIsActive   = new ArrayList<Boolean>();
        for (int iFrame=0; iFrame < this.reader.inputData.size(); iFrame++) {
            bIsActive.add(true);
            mapFrameIDs.put(iFrame, new ArrayList<Integer>());
        }
    }
    
    public DataManager(Reader reader, float windowSize, float hopSize) {
        this(reader);
        if (windowSize > Float.MIN_NORMAL && hopSize > Float.MIN_NORMAL) {
            this.bWindowing  = true;
            this.windowSize  = windowSize;
            this.hopSize     = hopSize;
            this.mapIDTime   = new HashMap<Integer,Float>();
        }
    }
    
    
    public int getNumIDs() {
        return mapIDName.size();
    }
    public Map<Integer,String> getMappingIDName() {
        return mapIDName;
    }
    public Map<Integer,String[]> getMappingIDLabels() {
        return mapIDLabels;
    }
    public Map<Integer,List<Integer>> getMappingFrameIDs() {
        return mapFrameIDs;
    }
    public Map<Integer,Float> getMappingIDTime() {
        return mapIDTime;
    }
    public List<Integer> getNumFrames() {
        return numFrames;
    }
    public List<Boolean> getActivityList() {
        return bIsActive;
    }
    
    
    public void generateMappings() {
        if (bWindowing) {
            generateMappingsWindowing();
        } else {
            generateMappingsNoWindowing();
        }
    }
    
    
    public boolean readLabelsFile(String labelsFileName) {
        /* Hint: In case of windowing, the interval of the labels must match the hop size of the windows. */
        
        if (mapIDName.isEmpty()) {
            System.err.println("Error in IDOrganizer.readLabelsFile: run generateMappings(Reader reader) first!");
        }
        
        BufferedReader br = null;
        mapIDLabels.clear();
        
        try {
            String   thisLine = null;
            String[] content  = null;
            Object   name     = null;
            Object   ret      = null; 
            
            File   inputFile = new File(labelsFileName);
                   br        = new BufferedReader(new FileReader(inputFile));
            
            thisLine = br.readLine();
            
            int numLabels = 0;
            if (thisLine==null || (bWindowing && thisLine.split(";").length < 3) || (!bWindowing && thisLine.split(";").length < 2)) {
                System.err.println("Error: Labels file " + labelsFileName + " does not have the required format");
                return false;
            } else if (bWindowing) {
                numLabels = thisLine.split(";").length-2;
            } else {
                numLabels = thisLine.split(";").length-1;
            }
            
            /* Check if there is a header line */
            content = thisLine.split(";");
            name    = content[0].replace("'", "");
            if (mapNameID.get(name)==null) {  /* Header line is present */
                thisLine = br.readLine();
            }
            
            while (thisLine != null) {
                content = thisLine.split(";");
                name    = content[0].replace("'", "");
                
                if (bWindowing) {
                    /* Check if combination of name and segment is already in the list */
                    int counter = 0;
                    
                    ret = mapNameID.get(name);  /* Get first id of the correct file (name) */
                    if (ret!=null) {
                        counter = (int) ret;
                        
                        /* Search for the correct time stamp within file */
                        double dLabel = Double.parseDouble(content[1]);
                        int    id     = -1;
                        while (counter < mapIDName.size() && mapIDName.get(counter).equals(name)) {  /* NOTE: Can be improved if we know the number of IDs in one file */
                            if (areEqual(dLabel, mapIDTime.get(counter))) {
                                id = counter;
                                break;
                            }
                            counter++;
                        }
                        
                        /* Add label for the correct id */
                        if (id >= 0) {
                            String[] labels = new String[numLabels];
                            for (int m=0; m < numLabels; m++) {
                                labels[m] = content[m+2];
                            }
                            mapIDLabels.put(id, labels);
                        }
                    }
                }
                else {
                    ret = mapNameID.get(name);
                    if (ret!=null) {
                        String[] labels = new String[numLabels];
                        for (int m=0; m < numLabels; m++) {
                            labels[m] = content[m+1];
                        }
                        mapIDLabels.put((int)ret, labels);
                    }
                }
                
                thisLine = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        
        return true;
    }
    
    
    private void generateMappingsNoWindowing() {
        List<Object[]> inputData = reader.inputData;
        int            indexName = reader.getIndexName();
        
        int ind = 0; /* Overall index of the input data */
        int ID  = 0; /* Index of each instance (starting from 0) */
        
        int numLabels = reader.getIndexesLabels().size();
        
        while (ind < inputData.size()) {
            /* Check if the current name has already occurred */
            String curName = inputData.get(ind)[indexName].toString();
            int    curID   = 0;
            
            if (!mapNameID.containsKey(curName)) {
                /* Increment ID */
                curID = ID++;
                
                /* Add map entries to know which bag relates to which name */
                mapIDName.put(curID, curName);
                mapNameID.put(curName, curID);
                if (numLabels > 0) {  /* The labels for each instance must be the same */
                    String[] labels = new String[numLabels];
                    for (int m=0; m < numLabels; m++) {
                        labels[m] = inputData.get(ind)[reader.getIndexesLabels().get(m)].toString();
                    }
                    mapIDLabels.put(curID, labels);
                }
            }
            else {
                curID = mapNameID.get(curName);
            }
            
            /* Add all successive frames belonging to the same ID (name) to mapFrameIDs */
            int counter = 0;
            while (ind < inputData.size() && inputData.get(ind)[indexName].toString().equals(curName)) {
                if (bIsActive.get(ind)) {
                    mapFrameIDs.get(ind).add(curID);  /* Only one ID corresponds to each frame in case of no windowing */
                    counter++;
                }
                ind++;
            }
            
            /* Increment frame counter */
            if (curID < numFrames.size()) {
                numFrames.set(curID, numFrames.get(curID) + counter);
            } else {
                numFrames.add(counter);
            }
        }
    }
    
    
    private void generateMappingsWindowing() {
        /* Note: All frames belonging to the same file (name) must be listed coherently. 
                 All frames within each file must be in the correct order (Reason: Meaningful speed-up). */
        
        List<Object[]> inputData = reader.inputData;
        int            indexName = reader.getIndexName();
        int            indexTime = reader.getIndexTime();
        
        List<Float> listCenters = new ArrayList<Float>();
        List<Float> listLowerB  = new ArrayList<Float>();
        List<Float> listUpperB  = new ArrayList<Float>();
        
        
        /* Go through the input data in the given order. */
        int IDoffset = 0;  /* Index of each segment=window=instance (starting from 0) */
        
        int    ind      = 0;  /* Overall index of the input data */
        Object curName  = "";
        int    firstSeg = 0;  /* Meaningful speed-up */
        
        while (ind < inputData.size()) {
            if (!curName.equals(inputData.get(ind)[indexName])) {
                /* New file (name) in input -> add new IDs (one for each window center) */
                curName = inputData.get(ind)[indexName];
                
                /* Update list of block boundaries */
                updateListOfBlocks(reader, curName, ind, listCenters, listLowerB, listUpperB);
                
                firstSeg = 0;  /* Meaningful speed-up */
                IDoffset = mapIDName.size();
                for (int iSeg=0; iSeg < listCenters.size(); iSeg++) {
                    int curID = IDoffset + iSeg;
                    if (!mapNameID.containsKey(curName)) {
                        mapNameID.put(curName.toString(), curID);  /* Only the first occurrence of name - to speed up readLabelsFile() */
                    }
                    mapIDName.put(curID, curName.toString());
                    mapIDTime.put(curID, listCenters.get(iSeg));
                    numFrames.add(curID, 0);
                }
            }
            
            if (bIsActive.get(ind)) {  /* Add only frames with activity */
                float curTime = (float)inputData.get(ind)[indexTime];
                List <Integer> mapFrameIDsInd = mapFrameIDs.get(ind);
                
                int curID = IDoffset;
                for (int iSeg=firstSeg; iSeg < listLowerB.size(); iSeg++) {  /* Meaningful speed-up */
                    curID = IDoffset + iSeg;
                    if (listLowerB.get(iSeg) <= curTime && listUpperB.get(iSeg) >= curTime) {
                        mapFrameIDsInd.add(curID);
                        numFrames.set(curID, numFrames.get(curID) + 1);
                    }
                    else if (listLowerB.get(iSeg) > curTime) {  /* Meaningful speed-up */
                        break;
                    }
                    else if (listUpperB.get(iSeg) < curTime) {  /* Meaningful speed-up */
                        firstSeg = iSeg;
                    }
                }
            }
            
            ind++;
        }
    }
    
    
    private void updateListOfBlocks(Reader reader, Object curName, int startIndex, List<Float> listCenters, List<Float> listLowerB, List<Float> listUpperB) {
        listCenters.clear(); 
        listLowerB.clear();
        listUpperB.clear();
        
        List<Object[]> inputData = reader.inputData;
        int            indexName = reader.getIndexName();
        int            indexTime = reader.getIndexTime();
        
        /* Determine maximum time stamp which is taken into account */
        int   ind     = startIndex;
        float maxTime = 0.0f;
        while (ind < inputData.size() && curName.equals(inputData.get(ind)[indexName])) {
            if ((float)inputData.get(ind)[indexTime] > maxTime) {
                maxTime = (float)inputData.get(ind)[indexTime];
            }
            ind++;
        }
        
        /* Generate a list of all segment centers (windows) */
        float curCenter = 0.0f;
        int   counter   = 0;
        while (curCenter < maxTime + 1e-4) {  /* Constant 1e-4 is not a good solution but might work in most cases */
            listLowerB.add(Math.max(-Float.MIN_NORMAL,          curCenter - (windowSize/2)) - Float.MIN_NORMAL);
            listUpperB.add(Math.min(maxTime + Float.MIN_NORMAL, curCenter + (windowSize/2)) + Float.MIN_NORMAL);
            listCenters.add(curCenter);
            curCenter = hopSize * ++counter;
        }
    }
    
    
    private boolean areEqual(double f1, double f2) {
        if (f1+1E-4d > f2 && f1-1E-4d < f2) {
            return true;
        }
        else {
            return false;
        }
    }
}
