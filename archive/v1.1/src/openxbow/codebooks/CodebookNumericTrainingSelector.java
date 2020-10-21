/*F************************************************************************
 * openXBOW - the Passau Open-Source Crossmodal Bag-of-Words Toolkit
 * Copyright (C) 2016-2017, 
 *   Maximilian Schmitt & Bj�rn Schuller: University of Passau.
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

package openxbow.codebooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import openxbow.main.DataManager;
import openxbow.randomselection.UniqueIndexes;


public class CodebookNumericTrainingSelector {
    /* This class chooses the instances used for training of a numeric codebook                               */
    /* Different constructors are available (supervised codebook generation, etc.), depending on the use-case */
    
    public List<Object[]>      inputData              = null;
    public List<float[]>       trainingData           = null;
    public List<List<float[]>> trainingDataSupervised = null;
    
    private List<Integer>  indexFeatures = null;
    private DataManager    DM            = null;
    private CodebookConfig config        = null;
    
    
    public CodebookNumericTrainingSelector(DataManager DM, CodebookConfig config) {
        this(DM,config,1); /* 1 is the standard numeric class index */
    }
    public CodebookNumericTrainingSelector(DataManager DM, CodebookConfig config, int featureClass) {
        this(DM,config,DM.reader.getIndexesAttributeClass().get(featureClass));
    }
    public CodebookNumericTrainingSelector(DataManager DM, CodebookConfig config, List<Integer> listIndexes) {
        this.inputData     = DM.reader.inputData;
        this.DM            = DM;
        this.config        = config;
        this.indexFeatures = listIndexes;
        
        select();
    }
    
    
    private void select() {
        if (config.bSupervised) {
            if (!isLabelNominal()) {
                System.err.println("Error: Generating codewords per class is only available in case of one nominal label.");
            } else {
                getTrainingFeaturesSupervised(inputData);
            }
        } else {
            getTrainingFeatures(inputData);
        }
    }
    
    
    private void getTrainingFeaturesSupervised(List<Object[]> data) {
        trainingDataSupervised = new ArrayList<List<float[]>>();
        
        /* Get number of classes */
        Vector<String> vecLabels = new Vector<String>();  
        for (int id=0; id < DM.getMappingIDLabels().size(); id++) {
            if (!vecLabels.contains(DM.getMappingIDLabels().get(id)[0])) {
                vecLabels.add(DM.getMappingIDLabels().get(id)[0]);
            }
        }
        
        int numClasses = vecLabels.size();
        
        /* Generate a map which tells us the corresponding frames for each ID */
        Map<Integer,List<Integer>> mapFrameIDs = DM.getMappingFrameIDs();
        Map<Integer,List<Integer>> mapIDFrames = new HashMap<Integer,List<Integer>>();
        for (int id=0; id < DM.getMappingIDLabels().size(); id++) {
            mapIDFrames.put(id, new ArrayList<Integer>());
            for (int frameIndex=0; frameIndex < mapFrameIDs.size(); frameIndex++) {
                if (mapFrameIDs.get(frameIndex).contains(id)) {
                    mapIDFrames.get(id).add(frameIndex);
                }
            }
        }
        
        for (int c=0; c < numClasses; c++) {
            List<Object[]> classData         = new ArrayList<Object[]>();
            List<float[]>  trainingDataClass = new ArrayList<float[]>();
            
            /* Select all instances of class c */
            for (int id=0; id < DM.getMappingIDLabels().size(); id++) {
                if (DM.getMappingIDLabels().get(id)[0].equals(vecLabels.get(c))) {
                    for (int frameIndex : mapIDFrames.get(id)) {
                        classData.add(data.get(frameIndex));
                    }
                }
            }
            
            /* Choose instances for training */
            if (config.numTraining > 0) {
                int numTrainingPerClass = (int) Math.ceil((double) config.numTraining / (double) numClasses);
                
                UniqueIndexes   uniqueIndexes   = new UniqueIndexes(classData.size(), numTrainingPerClass);
                Vector<Integer> indexesTraining = uniqueIndexes.getIndexes();
                
                for (int i=0; i < indexesTraining.size(); i++) {
                    float[] features = new float[indexFeatures.size()];
                    for (int k=0; k < indexFeatures.size(); k++) {
                        features[k] = (float) classData.get(indexesTraining.get(i))[indexFeatures.get(k)]; 
                    }
                    trainingDataClass.add(features);
                }
            }
            else {
                for (int i=0; i < classData.size(); i++) {
                    float[] features = new float[indexFeatures.size()];
                    for (int k=0; k < indexFeatures.size(); k++) {
                        features[k] = (float) classData.get(i)[indexFeatures.get(k)]; 
                    }
                    trainingDataClass.add(features);
                }
            }
            
            trainingDataSupervised.add(trainingDataClass);
        }
    }
    
    
    private void getTrainingFeatures(List<Object[]> inputData) {
        trainingData = new ArrayList<float[]>();
        
        /* Choose instances for training */
        if (config.numTraining > 0) {
            UniqueIndexes   uniqueIndexes   = new UniqueIndexes(inputData.size(), config.numTraining);
            Vector<Integer> indexesTraining = uniqueIndexes.getIndexes();
            for (int i=0; i < indexesTraining.size(); i++) {
                float[] features = new float[indexFeatures.size()];
                for (int k=0; k < indexFeatures.size(); k++) {
                    features[k] = (float) inputData.get(indexesTraining.get(i))[indexFeatures.get(k)]; 
                }
                trainingData.add(features);
            }
        }
        else {
            for (int i=0; i < inputData.size(); i++) {
                float[] features = new float[indexFeatures.size()];
                for (int k=0; k < indexFeatures.size(); k++) {
                    features[k] = (float) inputData.get(i)[indexFeatures.get(k)]; 
                }
                trainingData.add(features);
            }
        }
    }
    
    
    private boolean isLabelNominal() {
        if (DM.getMappingIDLabels().isEmpty() || DM.getMappingIDLabels().get(0).length!=1) {
              return false;  /* Not one label */
        }
        else {
            for (int id=0; id < DM.getMappingIDLabels().size(); id++) {
                if (!isNominal(DM.getMappingIDLabels().get(id)[0])) {
                    return false;  /* If there is at least one non-nominal label */
                }
            }
        }
        
        return true;
    }
  
    private boolean isNominal(String str) {
        try {
            Double.parseDouble(str);
        } catch(NumberFormatException e) {
            return true;  /* No number */
        }
        
        double dTest = Double.parseDouble(str);
        int    iTest = (int) Math.round(dTest);
        if (areEqual(dTest,iTest)) {
            return true;  /* Integer */
        }
        
        return false;  /* Float / Double */
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

