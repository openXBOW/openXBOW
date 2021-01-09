/*F************************************************************************
 * openXBOW - the Passau Open-Source Crossmodal Bag-of-Words Toolkit
 * Copyright (C) 2016-2020, 
 *   Maximilian Schmitt & Björn Schuller: University of Passau, 
 *    University of Augsburg.
 *   Contact: maximilian.schmitt@mailbox.org
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
import openxbow.codebooks.CodebookNumericGMM;
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
    
    
    public void generateGMM(int gmmMode) {
        /* gmmMode: 0 (not allowed), 1: WITHOUT priors, 2: WITH priors */
        assert(gmmMode>0 && gmmMode<=2);
        CodebookNumericGMM bookGMM = ((CodebookNumeric)book).getGMMCodebook();
        float[]   mixtures    = bookGMM.getMixtureWeights();
        float[][] centroids   = bookGMM.getCentroids();
        float[][] covariances = bookGMM.getCovariances();
        
        int sizeCodebook = mixtures.length;
        int numFeatures  = indexFeatures.size();
        
        Map<Integer,List<Integer>> mapFrameIDs = DM.getMappingFrameIDs();
        bof = new float[DM.getNumIDs()][sizeCodebook];
        
        /* Prepare mixture weights with mode */
        float[] mixturesBook = mixtures;
        mixtures = new float[sizeCodebook];
        for (int j=0; j < sizeCodebook; j++) {
            if (gmmMode==1) {
                mixtures[j] = 1.0f / sizeCodebook;  /* uniform weight */
            } else {  /* gmmMode==1 */
                mixtures[j] = mixturesBook[j];  /* just copy */
            }
        }
        
        /* Precompute some values to make assignment faster */
        float[] prefactorsGaussianProb = new float[sizeCodebook];
        for (int j=0; j < sizeCodebook; j++) {
            prefactorsGaussianProb[j] = mixtures[j] * computePrefactorComponent(covariances[j]);
        }
        float[][] invCovariances = new float[sizeCodebook][numFeatures];
        for (int j=0; j < sizeCodebook; j++) {
            for (int m=0; m < numFeatures; m++) {
                invCovariances[j][m] = 1.0f / covariances[j][m]; 
            }
        }
        float[] fv_unbiased = new float[numFeatures];
        
        /* Get probs */
        int frameIndex = 0;
        
        for (Object[] frame : data) {
            /* Temporary variables */
            float[] features = new float[numFeatures];   /* TODO: initialise before? */
            float[] prob     = new float[sizeCodebook];  /* TODO: initialise before? */
            float   sumProb  = 0.0f;
            
            /* Convert features to float array */
            int kn = 0;
            for (int k=0; k < numFeatures; k++) {
                features[kn] = (float) frame[indexFeatures.get(k)];
                kn++;
            }
            
            /* Compute probability for each cluster (mixture component) */
            for (int j=0; j < sizeCodebook; j++) {
                //prob[j] = computeGaussianProb(features, mixtures[j], centroids[j], covariances[j]);
                prob[j] = computeGaussianProbFast(features, centroids[j], invCovariances[j], prefactorsGaussianProb[j], fv_unbiased);
                sumProb += prob[j];
            }
            /* Normalize */
            for (int j=0; j < sizeCodebook; j++) {
                prob[j] /= sumProb;
            }
            
            /* Assign values */
            for (int id=0; id < mapFrameIDs.get(frameIndex).size(); id++) {
                for (int j=0; j < sizeCodebook; j++) {
                    bof[mapFrameIDs.get(frameIndex).get(id)][j] += prob[j];
                }
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
    
    
    /* NOTE: This function should be aligned with the corresponding function in openxbow.codebooks.CodebookNumericGMM */
    private float computePrefactorComponent(float[] covariances) {
        int   numFeatures = covariances.length;
        float determinant = 1.0f;
        for (int m=0; m < numFeatures; m++) {
            determinant *= covariances[m];
        }
        determinant += Float.MIN_NORMAL;
        return (float) (1.0f / Math.sqrt( Math.pow(2.0f * Math.PI, numFeatures) * determinant ));        
    }
    
    
    /* NOTE: This function should be aligned with the corresponding function in openxbow.codebooks.CodebookNumericGMM */
    private float computeGaussianProbFast(float[] feature_vector, float[] centroid, float[] invCovariances, float prefactor, float[] fv_unbiased) {
        int numFeatures = feature_vector.length;
        
        float exponent = 0.0f;
        for (int m=0; m < numFeatures; m++) {
            // (feature_vector[m] - centroid[m]) is the unbiased feature vector; (a-b) * (a-b) is faster than Math.pow(a-b,2)
            exponent += (feature_vector[m] - centroid[m]) * (feature_vector[m] - centroid[m]) * invCovariances[m];  /* inverse of the covariance "matrix" */
        }
        
        float prob = prefactor * (float) Math.exp(-0.5f * exponent);
        return prob + Float.MIN_NORMAL;
    }
    
    
//    /* NOTE: This function should be aligned with the corresponding function in openxbow.codebooks.CodebookNumericGMM */
//    private float computeGaussianProb(float[] feature_vector, float mixture_weight, float[] centroid, float[] covariances) {
//        int numFeatures = feature_vector.length;
//        
//        float determinant = 1.0f;
//        for (int m=0; m < numFeatures; m++) {
//            determinant *= covariances[m];
//        }
//        determinant += Float.MIN_NORMAL;
//        float prefactor = (float) (1.0f / Math.sqrt( Math.pow(2.0f * Math.PI, numFeatures) * determinant ));
//        
//        float[] fv_unbiased = new float[feature_vector.length];
//        for (int m=0; m < numFeatures; m++) {
//            fv_unbiased[m] = feature_vector[m] - centroid[m];
//        }
//        
//        float exponent = 0.0f;
//        for (int m=0; m < numFeatures; m++) {
//            exponent += fv_unbiased[m] * fv_unbiased[m] / covariances[m];  /* inverse of the covariance "matrix" */
//        }
//        
//        float prob = mixture_weight * prefactor * (float) Math.exp(-0.5f * exponent);
//        return prob + Float.MIN_NORMAL;
//    }
    
    
    public Codebook getCodebook() {
        return book;
    }
}
