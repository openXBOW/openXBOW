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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import openxbow.codebooks.HyperCodebook;
import openxbow.io.Reader;

public class Preprocessor {
    private DataManager   DM;
    private HyperCodebook book;
    private boolean       bRemoveLowEnergy;
    private int           energyIndex;
    private float         energyThreshold;
    private boolean       bStandardize;
    private boolean       bNormalize;
    
    public Preprocessor(DataManager DM, HyperCodebook book, boolean bRemoveLowEnergy, int energyIndex, float energyThreshold, boolean bStandardize, boolean bNormalize) {
        this.DM               = DM;
        this.book             = book;
        this.bRemoveLowEnergy = bRemoveLowEnergy;
        this.energyIndex      = energyIndex;
        this.energyThreshold  = energyThreshold;
        this.bStandardize     = bStandardize;
        this.bNormalize       = bNormalize;
    }
    
    public void preprocessInput() {
        /* Remove low-energy features */
        if (bRemoveLowEnergy) {
            book.setRemoveLowEnergy();
            book.setEnergyIndex(energyIndex);
            book.setEnergyThreshold(energyThreshold);
            setActivityList(DM, energyIndex, energyThreshold);
        }
        else if (book.isLowEnergyRemoved()) {
            setActivityList(DM, book.getEnergyIndex(), book.getEnergyThreshold());
        }
        
        /* Standardize/normalize input */
        if (bStandardize && bNormalize) {
            System.err.println("Error: Both standardization and normalization of the input is not possible!");
            return;
        }
        
        if (bStandardize) {
            float[] means        = getMeans(DM.reader);
            float[] standardDevs = getStandardDevs(DM.reader, means);
            book.setStandardize();
            book.setMeans(means);
            book.setStandardDevs(standardDevs);
            standardizeFeatureVectors(DM.reader, means, standardDevs);
        }
        else if (book.isStandardized()) {
            standardizeFeatureVectors(DM.reader, book.getMeans(), book.getStandardDevs());
        }
        
        if (bNormalize) {
            List<float[]> MINandWIDTH = getMINandWIDTH(DM.reader);
            float[] MIN   = MINandWIDTH.get(0);
            float[] WIDTH = MINandWIDTH.get(1);
            book.setNormalize();
            book.setMIN(MIN);
            book.setWIDTH(WIDTH);
            normalizeFeatureVectors(DM.reader, MIN, WIDTH);
        }
        else if (book.isNormalized()) {
            normalizeFeatureVectors(DM.reader, book.getMIN(), book.getWIDTH());
        }
    }
    
    
    private void setActivityList(DataManager DM, int energyIndex, float energyThreshold) {
        System.out.println("Recognizing activity ...");
        List<Object[]> inputData = DM.reader.inputData;
        List<Boolean>  bIsActive = DM.getActivityList();
        
        for (int i=0; i < inputData.size(); i++) {
            if (((float)inputData.get(i)[energyIndex-1]) < energyThreshold) {
                bIsActive.set(i, false);  /* All elements have been initialized with 'true' */
            }
        }
    }
    
    
    private void standardizeFeatureVectors(Reader reader, float[] mean, float[] std) {
        System.out.println("Standardization ...");
        
        List<Object[]> inputData = reader.inputData;
        
        /* Standardization */
        for (Entry<Integer,List<Integer>> e : reader.getIndexesAttributeClass().entrySet()) {
            if (e.getKey() > 0) {  /* Numeric */
                for (int j=0; j < inputData.size(); j++) {
                    for (int f : e.getValue()) {
                        inputData.get(j)[f] = (((float)inputData.get(j)[f]) - mean[f]) / std[f];
                    }
                }
            }
        }
    }
    
    
    private float[] getMeans(Reader reader) {
        List<Object[]> inputData = reader.inputData;
        
        float[] mean    = new float[reader.getNumAttributes()];
        int     counter = 0;  /* Counts the samples */
        
        /* Arithmetic mean */
        for (Entry<Integer,List<Integer>> e : reader.getIndexesAttributeClass().entrySet()) {
            if (e.getKey() > 0) {  /* Numeric */
                for (int j=0; j < inputData.size(); j++) {
                    for (int f : e.getValue()) {
                        mean[f] += (float)inputData.get(j)[f];
                    }
                    counter++;
                }
            }
        }
        for (int f=0; f < mean.length; f++) {
            mean[f] = mean[f] / counter;
        }
        
        return mean;
    }
    
    private float[] getStandardDevs(Reader reader, float[] mean) {
        List<Object[]> inputData = reader.inputData;
        
        float[] std = new float[reader.getNumAttributes()];
        int   counter = 0;  /* Counts the samples */
        float diff    = 0;
        
        /* Standard deviation */
        for (Entry<Integer,List<Integer>> e : reader.getIndexesAttributeClass().entrySet()) {
            if (e.getKey() > 0) {  /* Numeric */
                for (int j=0; j < inputData.size(); j++) {
                    for (int f : e.getValue()) {
                        diff = ((float)inputData.get(j)[f]) - mean[f];
                        std[f] += diff * diff;
                    }
                    counter++;
                }
            }
        }
        for (int f=0; f < std.length; f++) {
            std[f] = std[f] / (counter - 1);
            std[f] = (float) Math.sqrt(std[f]);
        }
        
        return std;
    }
    
    
    private void normalizeFeatureVectors(Reader reader, float[] MIN, float[] WIDTH) {
        System.out.println("Normalization ...");
        
        List<Object[]> inputData = reader.inputData;
        
        /* Normalization */
        for (Entry<Integer,List<Integer>> e : reader.getIndexesAttributeClass().entrySet()) {
            if (e.getKey() > 0) {  /* Numeric */
                for (int j=0; j < inputData.size(); j++) {
                    for (int f : e.getValue()) {
                        inputData.get(j)[f] = (((float)inputData.get(j)[f]) - MIN[f]) / WIDTH[f];
                    }
                }
            }
        }
    }
    
    private List<float[]> getMINandWIDTH(Reader reader) {
        List<Object[]> inputData = reader.inputData;
        
        float[] MIN   = new float[reader.getNumAttributes()];
        float[] MAX   = new float[reader.getNumAttributes()];
        float[] WIDTH = new float[reader.getNumAttributes()];
        
        for (int f=0; f < MIN.length; f++) {
            MIN[f] = Float.POSITIVE_INFINITY;
            MAX[f] = Float.NEGATIVE_INFINITY;
        }
        
        
        /* Arithmetic mean */
        for (Entry<Integer,List<Integer>> e : reader.getIndexesAttributeClass().entrySet()) {
            if (e.getKey() > 0) {  /* Numeric */
                for (int j=0; j < inputData.size(); j++) {
                    for (int f : e.getValue()) {
                        if (MIN[f] > (float)inputData.get(j)[f]) {
                            MIN[f] = (float)inputData.get(j)[f];
                        }
                        if (MAX[f] < (float)inputData.get(j)[f]) {
                            MAX[f] = (float)inputData.get(j)[f];
                        }
                    }
                }
            }
        }
        for (int f=0; f < MIN.length; f++) {
            WIDTH[f] = MAX[f] - MIN[f];
            if (WIDTH[f] < Float.MIN_NORMAL) {
                WIDTH[f] = 1;
                //System.err.println("Warning: Feature " + String.valueOf(f) + " seems to be constant.");
                //Includes also non-numeric attributes
            }
        }
        
        /* Put MIN and WIDTH in a list */
        List<float[]> result = new ArrayList<float[]>();
        result.add(MIN);
        result.add(WIDTH);
        
        return result;
    }
}
