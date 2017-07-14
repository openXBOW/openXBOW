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
    
    public Preprocessor(DataManager DM, HyperCodebook book, Options options) {
        this.DM               = DM;
        this.book             = book;
        this.bRemoveLowEnergy = options.bRemoveLowEnergy;
        this.energyIndex      = options.energyIndex;
        this.energyThreshold  = options.energyThreshold;
        this.bStandardize     = options.bStandardizeInput;
        this.bNormalize       = options.bNormalizeInput;
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
            if (book.getMeans()==null) {  /* NOTE: It is decided whether or not a codebook is given! */
                float[] means        = getMeans(DM.reader);
                float[] standardDevs = getStandardDevs(DM.reader, means);
                book.setStandardize();
                book.setMeans(means);
                book.setStandardDevs(standardDevs);
                standardizeFeatureVectors(DM.reader, means, standardDevs);
            }
            else {
                standardizeFeatureVectors(DM.reader, book.getMeans(), book.getStandardDevs());
            }
        }
        
        if (bNormalize) {
            if (book.getMIN()==null) {  /* NOTE: It is decided whether or not a codebook is given! */
                List<float[]> MINandWIDTH = getMINandWIDTH(DM.reader);
                float[] MIN   = MINandWIDTH.get(0);
                float[] WIDTH = MINandWIDTH.get(1);
                book.setNormalize();
                book.setMIN(MIN);
                book.setWIDTH(WIDTH);
                normalizeFeatureVectors(DM.reader, MIN, WIDTH);
            }
            else {
                normalizeFeatureVectors(DM.reader, book.getMIN(), book.getWIDTH());
            }
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
        System.out.println("Standardization of the input ...");
        
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
        System.out.println("Normalization of the input ...");
        
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
