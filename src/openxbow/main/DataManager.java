/*F********************************************************************************
 * openXBOW - the Passau Open-Source Crossmodal Bag-of-Words Toolkit
 * 
 * (c) 2016, Maximilian Schmitt, Björn Schuller: University of Passau. 
 *     All rights reserved.
 * 
 * Any form of commercial use and redistribution is prohibited, unless another
 * agreement between you and the copyright holder exists.
 * 
 * Contact: maximilian.schmitt@uni-passau.de
 * 
 * If you use openXBOW or any code from openXBOW in your research work,
 * you are kindly asked to acknowledge the use of openXBOW in your publications.
 * See the file CITING.txt for details.
 *******************************************************************************E*/

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
            String thisLine;
            File   inputFile = new File(labelsFileName);
                   br        = new BufferedReader(new FileReader(inputFile));
            
            /* Skip (and check) header line (must always appear in label file) */
            int numLabels = 0;
            thisLine = br.readLine();
            
            if (thisLine==null || (bWindowing && thisLine.split(";").length < 3) || (!bWindowing && thisLine.split(";").length < 2)) {
                System.err.println("Error: Labels file " + labelsFileName + " does not have the required format");
                return false;
            } else if (bWindowing) {
                numLabels = thisLine.split(";").length-2;
            } else {
                numLabels = thisLine.split(";").length-1;
            }
            
            while ((thisLine = br.readLine()) != null) {
                String[] content = thisLine.split(";");
                String   name    = content[0].replace("'", "");
                
                if (bWindowing) {
                    /* Check if combination of name and segment is already in the list */
                    int counter = 0;
                    
                    Object ret = mapNameID.get(name);  /* Get first id of the correct file (name) */
                    if (ret==null) {
                        System.err.println("Warning: Instance " + content[0] + " " + content[1] + " in labels file not found in the input data.");
                    } else {
                        counter = (int) ret;
                    }
                    
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
                    if (id < 0) {
                        System.err.println("Warning: Instance " + content[0] + " " + content[1] + " in labels file not found in the input data.");
                    } else {
                        if (!mapIDLabels.containsKey(id)) {
                            String[] labels = new String[numLabels];
                            for (int m=0; m < numLabels; m++) {
                                labels[m] = content[m+2];
                            }
                            mapIDLabels.put(id, labels);
                        }
                    }
                }
                else {
                    Object ret = mapNameID.get(name);
                    if (ret==null) {
                        System.err.println("Error: Instance " + content[0] + " in labels file not found in the input data.");
                    } else {
                        String[] labels = new String[numLabels];
                        for (int m=0; m < numLabels; m++) {
                            labels[m] = content[m+1];
                        }
                        mapIDLabels.put((int)ret, labels);
                    }
                }
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
            
            int counter = 0;
            while (ind < inputData.size() && inputData.get(ind)[indexName].toString().equals(curName)) {
                if (bIsActive.get(ind)) {
                    mapFrameIDs.get(ind).add(curID);  /* Only one ID correspongs to each frame in case of no windowing */
                    counter++;
                }
                ind++;
            }
            
            if (curID < numFrames.size()) {
                int tmp = numFrames.get(curID);
                numFrames.set(curID,tmp++);
            } else {
                numFrames.add(counter);
            }
        }
    }
    
    
    private void generateMappingsWindowing() {
        /* Hint: The specified hop size of the windows must be a multiple of the hop size of the feature vectors. 
         * Successive frames must be in successive order in the input data.                                       */
        
        List<Object[]> inputData = reader.inputData;
        int            indexName = reader.getIndexName();
        int            indexTime = reader.getIndexTime();
        
        int numInstancesSemi = (int) Math.round(windowSize/2/getHopSizeInput()); /* number of input feature vectors in one direction from the center of each segment=window=instance */
        int numLabels        = reader.getIndexesLabels().size();
        
        double nextCenter = 0.0d;  /* Center of the next window */
        int    ind        = 0;     /* Overall index of the input data */
        int    ID         = 0;     /* Index of each segment=window=instance (starting from 0) */
        
        while (ind < inputData.size()) {
            String  curName   = inputData.get(ind)[indexName].toString();
            int     curID     = 0;
            boolean bIndFound = false;
            
            /* Look for the next window center */
            while (!bIndFound && ind < inputData.size() && curName.equals(inputData.get(ind)[indexName].toString())) {
                if (areEqual(Double.parseDouble(inputData.get(ind)[indexTime].toString()), nextCenter)) {
                    bIndFound = true;
                }
                else {
                    ind++;
                }
            }
            
            if (bIndFound) {
                /* Find the boundaries of the current window. Make sure that we are in the specified name space */
                int iMin = ind - numInstancesSemi;
                while (iMin < 0 || !inputData.get(iMin)[indexName].toString().equals(curName)) { iMin++; }
                int iMax = ind + numInstancesSemi;
                while (iMax >= inputData.size() || !inputData.get(iMax)[indexName].toString().equals(curName)) { iMax--; }
                
                /* Increment ID */
                curID = ID++;
                
                /* Add the new ID (curID) to the mapFrameIDs for all frames within this window */
                int counter = 0; 
                if (bIsActive.get(ind)) {  /* Add only if activity is present at the center of the window */
                    for (int iFrame=iMin; iFrame<=iMax; iFrame++) {
                        if (bIsActive.get(iFrame)) {  /* Add only frames with activity */
                            mapFrameIDs.get(iFrame).add(curID);
                            counter++;
                        }
                    }
                }
                numFrames.add(counter);
                
                /* Add map entries to know which bag relates to which name and time stamp */
                if (!mapNameID.containsKey(curName)) {
                    mapNameID.put(curName, curID); /* First occurrence of name */
                }
                mapIDName.put(curID, curName);
                mapIDTime.put(curID, Float.parseFloat(inputData.get(ind)[indexTime].toString()));  /* ind is the center of the window */
                
                if (numLabels > 0) {
                    String[] labels = new String[numLabels];
                    for (int m=0; m < numLabels; m++) {
                        labels[m] = inputData.get(ind)[reader.getIndexesLabels().get(m)].toString();
                    }
                    mapIDLabels.put(curID, labels);
                }
                
                /* Increment window */
                nextCenter += hopSize;
            }
            else {
                nextCenter = 0.0d;
            }
        }
    }
    
    
    private float getHopSizeInput() {
        /* Note: Only a fast way to get the hop size of the input features, assuming that the hopsize is always the same and the first two frames are successive data frames */
        float timeStamp0 = (float) reader.inputData.get(0)[reader.getIndexTime()];
        float timeStamp1 = (float) reader.inputData.get(1)[reader.getIndexTime()];
        
        return timeStamp1 - timeStamp0;
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
