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

package openxbow.codebooks;

import java.util.Arrays;
import java.util.List;

public class CodebookNumericGMM extends CodebookNumeric {
    private float[]   mixtures    = null;
    private float[][] centroids   = null;
    private float[][] covariances = null;
    
    protected CodebookNumericGMM(CodebookConfig config) {
        super(config);
    }
    
    protected float[][] generateCodebook(List<float[]> trainingData, int sizeCodebook, float[][] centroidsInitial) {
        final int maxIterations = 500;
        
        int     trainingSize = trainingData.size();
        int[]   curCluster   = new int[trainingSize];
        float[] probCluster  = new float[trainingSize];
        int[]   lastCluster  = new int[trainingSize];
        int     numFeatures  = trainingData.get(0).length;
        
        /* Initialize mixture weights and covariances */
        centroids   = centroidsInitial;
        mixtures    = new float[sizeCodebook];
        covariances = new float[sizeCodebook][numFeatures];
        for (int k=0; k < sizeCodebook; k++) {
            mixtures[k] = 1.0f / sizeCodebook;  /* Uniform weight */
            for (int m=0; m < numFeatures; m++) {
                 covariances[k][m] = 1.0f; 
            }
        }
        
        /* Optimization */
        for (int i=0; i < trainingSize; i++) {
            lastCluster[i] = -1;
        }
        boolean clustersChanged = true;
        int     iter            = 0;
        while (clustersChanged && iter<maxIterations) {
            iter++;
            
            /* E: Get current cluster assignments with probabilities */
            /* Precompute some stats to use computeGaussianProbFast */
            float[] prefactorsGaussianProb = new float[sizeCodebook];
            for (int k=0; k < sizeCodebook; k++) {
                prefactorsGaussianProb[k] = mixtures[k] * computePrefactorComponent(covariances[k]);
            }
            float[][] invCovariances = new float[sizeCodebook][numFeatures];
            for (int k=0; k < sizeCodebook; k++) {
                for (int m=0; m < numFeatures; m++) {
                    invCovariances[k][m] = 1.0f / covariances[k][m]; 
                }
            }
            float[] fv_unbiased = new float[numFeatures];
            
            for (int i=0; i < trainingSize; i++) {
                float[] features      = trainingData.get(i);
                float   likelihood    = 0.0f;
                float   maxLikelihood = 0.0f;
                float   sumLikelihood = 0.0f;
                for (int k=0; k < sizeCodebook; k++) {
                    // likelihood = computeGaussianProb(features, mixtures[k], centroids[k], covariances[k]);
                    likelihood = computeGaussianProbFast(features, centroids[k], invCovariances[k], prefactorsGaussianProb[k], fv_unbiased);
                    sumLikelihood += likelihood;  /* Likelihood over all clusters */
                    if (likelihood > maxLikelihood) {
                        maxLikelihood = likelihood;
                        curCluster[i] = k;
                    }
                }
                probCluster[i] = maxLikelihood / sumLikelihood;  /* Probability that feature vector i belongs to current cluster k */ 
            }
            
            /* Check if the assignment has changed */
            if (Arrays.equals(curCluster, lastCluster)) {
                clustersChanged = false;
            }
            System.arraycopy(curCluster, 0, lastCluster, 0, trainingSize);
            
            if (clustersChanged) {
                /* M: Get total "responsibility" for each cluster */
                float[] response    = new float[sizeCodebook];
                int[]   responseInt = new int[sizeCodebook];
                float   sumResponse = 0.0f; 
                for (int k=0; k < sizeCodebook; k++) {
                    response[k] = 0.0f;
                    for (int i=0; i < trainingSize; i++) {
                        if (curCluster[i]==k) {
                            response[k] += probCluster[i];
                            responseInt[k] += 1;
                        }
                    }
                    sumResponse += response[k]; 
                }
                
                /* M: Update mixture weights */
                for (int k=0; k < sizeCodebook; k++) {
                    mixtures[k] = response[k] / sumResponse;  /* Relative response is the mixture weight */
                }
                /* M: Update centroids */
                for (int k=0; k < sizeCodebook; k++) {
                    if (responseInt[k]>0) {  /* Otherwise, the centroid remains unchanged */
                        for (int m=0; m < numFeatures; m++) {
                            centroids[k][m] = 0.0f;
                        }
                        for (int i=0; i < trainingSize; i++) {
                            if (curCluster[i]==k) {
                                float[] features = trainingData.get(i);
                                for (int m=0; m < numFeatures; m++) {
                                    centroids[k][m] += probCluster[i] * features[m];
                                }
                            }
                        }
                        for (int m=0; m < numFeatures; m++) {
                            centroids[k][m] /= response[k];
                        }
                    }
                }
                /* M: Update covariance matrixes */
                for (int k=0; k < sizeCodebook; k++) {
                    if (responseInt[k]>0) {  /* Otherwise, the covariances remain unchanged */
                        for (int m=0; m < numFeatures; m++) {
                            covariances[k][m] = Float.MIN_NORMAL;
                        }
                        for (int i=0; i < trainingSize; i++) {
                            if (curCluster[i]==k) {
                                float[] features = trainingData.get(i);
                                for (int m=0; m < numFeatures; m++) {  /* Subtract mean */
                                    // (features[m] - centroid[m]) is the unbiased feature vector; (a-b) * (a-b) is faster than Math.pow(a-b,2)
                                    covariances[k][m] += probCluster[i] * (features[m] - centroids[k][m]) * (features[m] - centroids[k][m]);
                                }
                            }
                        }
                        for (int m=0; m < numFeatures; m++) {
                            covariances[k][m] /= response[k];
                            if (covariances[k][m] < Float.MIN_NORMAL) {  /* Lower values might occur due to division by response[k] and will result in infinity when inverted. */
                            	covariances[k][m] = Float.MIN_NORMAL;
                            }
                        }
                    }
                }
            }
        }
        
        return centroids;
    }
    
    
    /* NOTE: This function should be aligned with the corresponding function in openxbow.main.Bag */
    private float computePrefactorComponent(float[] covariances) {
        int   numFeatures = covariances.length;
        float determinant = 1.0f;
        for (int m=0; m < numFeatures; m++) {
            determinant *= covariances[m];
        }
        determinant += Float.MIN_NORMAL;
        return (float) (1.0f / Math.sqrt( Math.pow(2.0f * Math.PI, numFeatures) * determinant ));        
    }
    
    
    /* NOTE: This function should be aligned with the corresponding function in openxbow.main.Bag */
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
    
    
//    /* NOTE: This function should be aligned with the corresponding function in openxbow.main.Bag */
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
    
    
    public float[] getMixtureWeights( ) {
        return this.mixtures;
    }
    
    public float[][] getCentroids( ) {  /* Already stored in Codebook.codewords */
        return this.centroids;
    }
    
    public float[][] getCovariances( ) {
        return this.covariances;
    }
    
    protected void setMixtureWeights(float[] mixtures) {
        this.mixtures = mixtures;
    }
    
    protected void setCentroids(float[][] centroids) {  /* Store also in Codebook.codewords */
        this.centroids = centroids;
    }
    
    protected void setCovariances(float[][] covariances) {
        this.covariances = covariances;
    }
}
