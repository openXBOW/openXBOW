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

package openxbow.main;

import java.util.List;
import java.util.Map;

import openxbow.codebooks.Codebook;
import openxbow.codebooks.CodebookNumeric;
import openxbow.codebooks.CodebookText;
import openxbow.nlp.String2WordVector;


public class Bag {
    private List<Object[]> data;  /* Input data or assigned indexes of subcodebooks */
    private Codebook       book;
    private DataManager    DM;
    private List<Integer>  indexFeatures;
    
    public  float[][] bof         = null;
    public  int[][]   assignments = null;  /* Dim 1: frameIndex, Dim 2: assignment index (multi assignment) */
    
    public Bag (List<Object[]> data, Codebook book, DataManager DM, List<Integer> indexFeatures) {
        this.data          = data;
        this.book          = book;
        this.DM            = DM;
        this.indexFeatures = indexFeatures;
    }
    
    public Bag() {
        /* Only to concatenate sub-bags */
    }
    
    
    public void generateBoW () {
        CodebookText bookText = (CodebookText)book;
        
        int sizeCodebook = bookText.getCodebook().length;
        
        String2WordVector s2wv = new String2WordVector();
        
        Map<Integer,List<Integer>> mapFrameIDs = DM.getMappingFrameIDs();
        
        bof = new float[DM.getNumIDs()][sizeCodebook];
        
        /* Assign */
        int frameIndex = 0;
        
        for (Object[] frame : data) {  /* Put all text features into one bag */
            String text  = "";
            
            for (int k=0; k < indexFeatures.size(); k++) {
                text = text.concat(frame[indexFeatures.get(k)].toString());
                text = text.concat(" ");
            }
            
            String[] wordVector = s2wv.string2WordVector(text, bookText.getStopCharacters(), bookText.getNGram(), bookText.getNCharGram());
            
            for (int k=0; k < wordVector.length; k++) {
                for (int j=0; j < sizeCodebook; j++) {
                    if (bookText.getCodebook()[j].equals(wordVector[k])) {
                        /* Increase the counter for all corresponding instances (IDs) */
                        for (int id=0; id < mapFrameIDs.get(frameIndex).size(); id++) {
                            bof[mapFrameIDs.get(frameIndex).get(id)][j]++;
                        }
                        
                        break;
                    }
                }
            }
            
            frameIndex++;
        }
    }
    
    
    public void generateBoF (int numAssignments, float gaussianEncoding, float offCodewords, boolean bGetAssignments) {
        CodebookNumeric bookNumeric = (CodebookNumeric)book;
        float[][]       codebook    = bookNumeric.getCodebook();
        
        int sizeCodebook          = codebook.length;
        int numFeatures           = indexFeatures.size();
        boolean bGaussianEncoding = false;
        boolean bOffCodewords     = false;
        if (gaussianEncoding > Float.MIN_VALUE) {
            bGaussianEncoding = true;
        }
        if (offCodewords > Float.MIN_VALUE) {
            bOffCodewords = true;
        }
        
        Map<Integer,List<Integer>> mapFrameIDs = DM.getMappingFrameIDs();
        bof = new float[DM.getNumIDs()][sizeCodebook];
        
        if (bGetAssignments) {  /* Required in case of 1) Output word indexes or 2) numeric n-grams */
            assignments = new int[data.size()][numAssignments];
        }
        
        /* Assign */
        int frameIndex = 0;
        
        for (Object[] frame : data) {
            /* Temporary variables */
            float[] distance = new float[sizeCodebook];
            float[] features = new float[numFeatures];
            float   diff     = 0;
            
            /* Convert features to float array */
            int kn = 0;
            for (int k=0; k < numFeatures; k++) {
                features[kn] = (float) frame[indexFeatures.get(k)];
                kn++;
            }
            
            /* Compute distances to codewords */
            for (int j=0; j < sizeCodebook; j++) {
                for (int k=0; k < numFeatures; k++) {
                    diff = features[k] - codebook[j][k];
                    distance[j] += diff * diff;
                }
            }
            
            /* Find assignments */
            for (int a=0; a < numAssignments; a++) {
                float minDistance = Float.MAX_VALUE;
                int   minIndex    = 0;
                
                for (int j=0; j < sizeCodebook; j++) {
                    if (distance[j] < minDistance) {
                        minDistance = distance[j];
                        minIndex    = j;
                    }
                }
                
                if (assignments!=null) {
                    assignments[frameIndex][a] = minIndex;
                }
                
                float increment = 1.0f;
                
                if (bGaussianEncoding) {
                    float frac = minDistance / (2 * gaussianEncoding * gaussianEncoding);  /* minDistance is squared distance */
                    increment = (float) Math.exp(-frac);
                }
                
                if (!bOffCodewords || Math.sqrt(minDistance) <= offCodewords) {
                    /* Increase the counter for all corresponding instances (IDs) */
                    for (int id=0; id < mapFrameIDs.get(frameIndex).size(); id++) {
                        bof[mapFrameIDs.get(frameIndex).get(id)][minIndex] += increment;
                    }
                }
                
                distance[minIndex] = Float.MAX_VALUE;
            }
            
            frameIndex++;
        }
        
        /* Make sure that we do not have a bag of only zeros */
        for (int id=0; id < bof.length; id++) {
            if (DM.getNumFrames().get(id)==0) {
                for (int w=0; w < bof[0].length; w++) {
                    bof[id][w] = 0.001f;
                }
            }
        }
    }
    
    
    public Codebook getCodebook() {
        return book;
    }
}
