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

package openxbow.codebooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import openxbow.main.DataManager;
import openxbow.main.HyperBag;
import openxbow.randomselection.UniqueIndexes;


public class CodebookNumericTrainingSelector {
    /* This class chooses the instances used for training of a numeric codebook                                    */
    /* Different constructors are available (supervised codebook generation, SVQ, etc.), depending on the use-case */
    
    public List<Object[]>      inputData              = null;
    public List<float[]>       trainingData           = null;
    public List<List<float[]>> trainingDataSupervised = null;
    List<Integer>              indexFeatures          = null;
    
    private DataManager DM             = null;
    private HyperBag    hyperBag       = null;  /* For creating the codebook from a Bag-of-Features (SVQ) */
    private boolean     bSupervised    = false;
    private int         numTraining    = 0;  /* =0: All input instances are chosen for training */
    
    
    public CodebookNumericTrainingSelector(DataManager DM) {
        this(DM, false);
    }
    public CodebookNumericTrainingSelector(DataManager DM, boolean bSupervised) {
        this(DM,bSupervised,0);
    }
    public CodebookNumericTrainingSelector(DataManager DM, boolean bSupervised, int numTraining) {
        this(DM,bSupervised,1,numTraining); /* 1 is the standard numeric class index */
    }
    public CodebookNumericTrainingSelector(DataManager DM, boolean bSupervised, int featureClass, int numTraining) {
        this(DM,bSupervised,DM.reader.getIndexesAttributeClass().get(featureClass),numTraining);
    }
    public CodebookNumericTrainingSelector(DataManager DM, boolean bSupervised, List<Integer> listIndexes, int numTraining) {
        this.inputData     = DM.reader.inputData;
        this.DM            = DM;
        this.bSupervised   = bSupervised;
        this.numTraining   = numTraining;
        this.indexFeatures = listIndexes;
        
        select();
    }
    public CodebookNumericTrainingSelector(HyperBag hyperBag, boolean bSupervised) {  /* SVQ top-level codebook */
        this.hyperBag      = hyperBag;
        this.bSupervised   = bSupervised;
        this.indexFeatures = new ArrayList<Integer>();
        for (int k=0; k < hyperBag.getNumSVQSubBags(); k++) {
            this.indexFeatures.add(k);
        }
        
        select();
    }
    
    
    private void select() {
        if (hyperBag==null) {
            if (bSupervised) {
                if (!isLabelNominal()) {
                    System.err.println("Error: Generating codewords per class is only available in case of one nominal label.");
                } else {
                    getTrainingFeaturesSupervised(inputData);
                }
            } else {
                getTrainingFeatures(inputData);
            }
        } else { /* SVQ top-level codebook */
            if (bSupervised) {
                if (!isLabelNominal()) {
                    System.err.println("Error: Generating codewords per class is only available in case of one nominal label.");
                } else {
                    getTrainingFeaturesSupervised(hyperBag.getListsOfBags());
                }
            } else {
                getTrainingFeatures(hyperBag.getListsOfBags());
            }
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
            if (numTraining > 0) {
                int numTrainingPerClass = (int) Math.ceil((double) numTraining / (double) numClasses);
                
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
        if (numTraining > 0) {
            UniqueIndexes   uniqueIndexes   = new UniqueIndexes(inputData.size(), numTraining);
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

