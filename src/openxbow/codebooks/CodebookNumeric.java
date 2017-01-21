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

package openxbow.codebooks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import openxbow.codebooks.CodebookConfig.cbgenmethod;


public class CodebookNumeric extends Codebook {
    private float[][] codewords = null;
    
    public CodebookNumeric(CodebookConfig config) {
        super(config);
    }
    
    public void generateCodebook(CodebookNumericTrainingSelector train) {
        if (train.trainingDataSupervised != null) {
            int       numClasses          = train.trainingDataSupervised.size();
            int       numClustersPerClass = (int) Math.ceil((double) config.sizeCodebookInitial / (double) numClasses);
            float[][] centroidsClass      = null;
            
            codewords = new float[numClustersPerClass*numClasses][train.trainingDataSupervised.get(0).get(0).length];
            
            for (int c=0; c < numClasses; c++) {
                if (train.trainingDataSupervised.get(c).size() < numClustersPerClass) {
                    System.err.println("Error in CodebookNumeric.generateCodebook(...): sizeCodebook is larger than number of class-specific training data.");
                    return;
                }
                
                if (config.generationMethod==cbgenmethod.kmeans) {
                    centroidsClass = kMeans(train.trainingDataSupervised.get(c),numClustersPerClass,false,config.randomSeed,false);
                }
                else if (config.generationMethod==cbgenmethod.kmeanspp) {
                    centroidsClass = kMeans(train.trainingDataSupervised.get(c),numClustersPerClass,true,config.randomSeed,false);
                }
                else if (config.generationMethod==cbgenmethod.kmeansnorm) {
                    centroidsClass = kMeans(train.trainingDataSupervised.get(c),numClustersPerClass,false,config.randomSeed,true);
                }
                else if (config.generationMethod==cbgenmethod.kmeansppnorm) {
                    centroidsClass = kMeans(train.trainingDataSupervised.get(c),numClustersPerClass,true,config.randomSeed,true);
                }
                else if (config.generationMethod==cbgenmethod.random) {
                    centroidsClass = randomSampling(train.trainingDataSupervised.get(c),numClustersPerClass,config.randomSeed);
                }
                else if (config.generationMethod==cbgenmethod.randompp) {
                    centroidsClass = randomSamplingPlusPlus(train.trainingDataSupervised.get(c),numClustersPerClass,config.randomSeed);
                }
                else {
                    System.err.println("Error: Codebook generation method unknown.");
                }
                
                for (int m=0; m < numClustersPerClass; m++) {
                    int offset = c*numClustersPerClass;
                    for (int n=0; n < centroidsClass[0].length; n++) {
                        codewords[offset+m][n] = centroidsClass[m][n];
                    }
                }
            }
        }
        else {
            if (train.trainingData.size() < config.sizeCodebookInitial) {
                System.err.println("Error in CodebookNumeric.generateCodebook(...): sizeCodebook is larger than number of training data.");
                return;
            }
            
            /* Codebook generation */
            if (config.generationMethod==cbgenmethod.kmeans) {
                codewords = kMeans(train.trainingData,config.sizeCodebookInitial,false,config.randomSeed,false);
            }
            else if (config.generationMethod==cbgenmethod.kmeanspp) {
                codewords = kMeans(train.trainingData,config.sizeCodebookInitial,true,config.randomSeed,false);
            }
            else if (config.generationMethod==cbgenmethod.kmeansnorm) {
                codewords = kMeans(train.trainingData,config.sizeCodebookInitial,false,config.randomSeed,true);
            }
            else if (config.generationMethod==cbgenmethod.kmeansppnorm) {
                codewords = kMeans(train.trainingData,config.sizeCodebookInitial,true,config.randomSeed,true);
            }
            else if (config.generationMethod==cbgenmethod.random) {
                codewords = randomSampling(train.trainingData,config.sizeCodebookInitial,config.randomSeed);
            }
            else if (config.generationMethod==cbgenmethod.randompp) {
                codewords = randomSamplingPlusPlus(train.trainingData,config.sizeCodebookInitial,config.randomSeed);
            }
            else {
                System.err.println("Error: Codebook generation method unknown.");
            }
        }
        
        /* Reduce the size of the codebook and increase the robustness by removing similar codewords */
        if (config.bReduceCodebook) {
            boolean[] bRemove = new boolean[codewords.length];
            for (int k=0; k < bRemove.length; k++) {
                bRemove[k] = false;
            }
            
            int countRemoves = 0;
            for (int k1=0; k1 < codewords.length-1; k1++) {
                List<Integer> indexesMerge = new ArrayList<Integer>();
                for (int k2=k1+1; k2 < codewords.length; k2++) {
                    if (!bRemove[k2]) {
                        if (computePCC(codewords[k1],codewords[k2]) > config.thReduceCodebook) {
                        //if (computeCCC(codewords[k1],codewords[k2]) > thReduceCodebook) {
                        //if (computeEucDist(codewords[k1],codewords[k2]) < thReduceCodebook) {
                            indexesMerge.add(k2);
                            bRemove[k2] = true;
                            countRemoves++;
                        }
                    }
                }
                for (int m=0; m < codewords[k1].length; m++) {
                    for (int k2 : indexesMerge) {
                        codewords[k1][m] += codewords[k2][m];
                    }
                    codewords[k1][m] /= (indexesMerge.size() + 1);
                }
            }
            
            float[][] codewordsReduced = new float[codewords.length-countRemoves][codewords[0].length];
            int c=0;
            for (int k=0; k < codewords.length; k++) {
                if (!bRemove[k]) {
                    codewordsReduced[c++] = codewords[k];
                }
            }
            codewords = codewordsReduced;
        }
    }
    
    
    private float[][] randomSampling(List<float[]> trainingData, int sizeCodebook, int randomSeed) {
        float[][] centroids     = new float[sizeCodebook][trainingData.get(0).length];
        Random    randGenerator = new Random(randomSeed);  /* Seed 10 to keep it consistent with simpleKMeans in Weka */
        
        /* Copy training data */
        List<float[]> initData = new ArrayList<float[]>();
        initData.addAll(trainingData);
        
        int k = 0;
        int counter = 0;
        do {
            /* Get random index (chosen instances are moved to the back below) */
            int index = randGenerator.nextInt(initData.size() - counter++);  /* Consistency with Weka */
            
            /* Check if equal centroid has already been chosen */
            float[] centroid = new float[initData.get(0).length];
            for (int m=0; m < initData.get(0).length; m++) {
                centroid[m] = initData.get(index)[m];
            }
            boolean bNewCentroid = true;
            boolean bDifferent = false;
            for (int k2=0; k2 < k; k2++) {
                bDifferent = false;
                for (int m=0; m < initData.get(0).length; m++) {
                    if (!areEqual(centroid[m],centroids[k2][m])) {
                        bDifferent = true;
                        break;
                    }
                }
                if (!bDifferent) {  /* An equal instance is already in centroids */
                    bNewCentroid = false;
                    break;
                }
            }
            
            if (bNewCentroid) {
                /* Copy instance */
                for (int m=0; m < initData.get(0).length; m++) {
                    centroids[k][m] = centroid[m];
                }
                k++;
            }                
            
            /* Swap elements, so that the same instance is not chosen more than once */
            float[] tmp = initData.get(index);
            initData.set(index, initData.get(initData.size()-k-1));
            initData.set(initData.size()-k-1, tmp);
            
            if (initData.size()==counter) {
                System.err.println("Error in randomSampling(): Not enough training data for given codebook size. Reduce codebook size!");
                break;
            }
        } while (k < sizeCodebook);
        
        return centroids;
    }
    
    private boolean areEqual(double f1, double f2) {
        if (f1+1E-4d > f2 && f1-1E-4d < f2) {
            return true;
        }
        else {
            return false;
        }
    }
    
    
    private float[][] randomSamplingPlusPlus(List<float[]> trainingData, int sizeCodebook, int randomSeed) {
        /* kMeans++ initialization */
        int       numFeatures   = trainingData.get(0).length;
        float[][] centroids     = new float[sizeCodebook][trainingData.get(0).length];
        Random    randGenerator = new Random(randomSeed);  /* Seed 10 to keep it consistent with kMeans in Weka */
        
        /* Choose one instance to be the center */
        int center0 = randGenerator.nextInt(trainingData.size());
        for (int m=0; m < trainingData.get(0).length; m++) {
            centroids[0][m] = (float) trainingData.get(center0)[m];
        }
        
        double[] distances      = new double[trainingData.size()];
        double[] probs          = new double[trainingData.size()];
        double[] cumulatedProbs = new double[trainingData.size()];
        
        double diff = 0;
        
        for (int i=0; i < trainingData.size(); i++) {
            distances[i] = 0;
            for (int m=0; m < numFeatures; m++) {
                diff = centroids[0][m] - trainingData.get(i)[m];
                distances[i] += diff * diff;
            }
        }
        
        /* Select the centers of the remaining clusters */
        for (int k = 1; k < sizeCodebook; k++) {
            /* Convert distances to probabilities */
            probs = getProbVector(probs,distances);  /* Note: This is the fastest way */
            
            double sumProbs = 0;
            for (int c = 0; c < trainingData.size(); c++) {
                sumProbs          += probs[c];
                cumulatedProbs[c]  = sumProbs;
            }
            cumulatedProbs[trainingData.size() - 1] = 1.0d;
            
            /* Choose a random instance */
            double randomValue = randGenerator.nextDouble();
            int x = 0;
            while (x < trainingData.size()) {
                if (cumulatedProbs[x] > randomValue) {
                    break;
                }
                x++;
            }
            
            for (int m=0; m < numFeatures; m++) {
                centroids[k][m] = (float) trainingData.get(x)[m];
            }
            
            /* If an instance is now closer to the new centroid, this is the determining one */
            for (int c = 0; c < trainingData.size(); c++) {
                if (distances[c] > 0) {
                    double newDistance = 0;
                    for (int m=0; m < numFeatures; m++) {
                        diff = centroids[k][m] - trainingData.get(c)[m];
                        newDistance += diff * diff;
                    }
                    if (newDistance < distances[c]) {
                        distances[c] = newDistance;
                    }
                }
            }
        }
        
        return centroids;
    }
    
    
    private float[][] kMeans(List<float[]> trainingData, int sizeCodebook, boolean bPlusPlus, int randomSeed, boolean bNormalize) {
        /* Lloyd algorithm */
        final int maxIterations = 500;  /* as in Weka */
        float[][] centroids     = null;
        float[]   width         = null;
        int[]     curCluster    = new int[trainingData.size()];
        int[]     lastCluster   = new int[trainingData.size()];
        int       numFeatures   = trainingData.get(0).length;
        float     diff          = 0;  /* Temp variable */
        
        /* Initialization */
        if  (bPlusPlus) {
            centroids = randomSamplingPlusPlus(trainingData, sizeCodebook, randomSeed);
        }
        else {
            centroids = randomSampling(trainingData, sizeCodebook, randomSeed);
        }
        
        /* Compute parameters for normalization (only used locally for clustering, not for assignment) */
        if (bNormalize) {
            float[] minimum = new float[trainingData.get(0).length];
            float[] maximum = new float[trainingData.get(0).length];
                    width   = new float[trainingData.get(0).length];
            
            for (int m=0; m < trainingData.get(0).length; m++) {
                minimum[m] = trainingData.get(0)[m];
                maximum[m] = trainingData.get(0)[m];
                for (int i=1; i < trainingData.size(); i++) {
                    if (minimum[m] > trainingData.get(i)[m]) {
                        minimum[m] = trainingData.get(i)[m];
                    }
                    if (maximum[m] < trainingData.get(i)[m]) {
                        maximum[m] = trainingData.get(i)[m];
                    }
                }
                width[m] = maximum[m] - minimum[m];
            }
        }
        
        /* Optimization */
        boolean clustersChanged = true;
        int     iter            = 0;
        while (clustersChanged && iter<maxIterations) {
            iter++;
            
            /* Get current assignment */
            for (int i=0; i < trainingData.size(); i++) {
                float[] features    = trainingData.get(i);
                float   minDistance = Float.MAX_VALUE;
                float   distance    = 0;
                
                for (int k=0; k < sizeCodebook; k++) {
                    distance = 0;
                    for (int m=0; m < numFeatures; m++) {
                        if (bNormalize) {
                            diff = (features[m] - centroids[k][m]) / width[m];  /* No need to subtract min[m] */
                        } else {
                            diff = features[m] - centroids[k][m];
                        }
                        distance += diff * diff;
                    }
                    if (distance < minDistance) {
                        minDistance = distance;
                        curCluster[i] = k;
                    }
                }
            }
            
            /* Check if the assignment has changed */
            if (Arrays.equals(curCluster, lastCluster)) {
                clustersChanged = false;
            }
            System.arraycopy(curCluster, 0, lastCluster, 0, trainingData.size());
            
            /* Update centroids */
            if (clustersChanged) {
                for (int k=0; k < sizeCodebook; k++) {
                    for (int m=0; m < numFeatures; m++) {
                        float sum = 0;
                        float num = 0;
                        for (int i=0; i < trainingData.size(); i++) {
                            float[] features = trainingData.get(i);
                            if (curCluster[i]==k) {
                                sum += features[m];
                                num++;
                            }
                        }
                        if (num>0) {
                            centroids[k][m] = sum / num;
                        }
                    }
                }
            }
        }
        
        return centroids;
    }
    
    
    private double[] getProbVector(double[] vectorProb, double[] vector) {
        double   sum        = 0;
        
        for (double v : vector) {
            sum += v;
        }
        
        if (sum > 0) {
            for (int i=0; i < vector.length; i++) {
                vectorProb[i] = vector[i] / sum;
            }
        }
        
        return vectorProb;
    }
    
    
    private float computePCC(float[] vector1, float[] vector2) {
        assert(vector1.length==vector2.length);
        
        float mean1=0.0f, mean2=0.0f;
        for (int m=0; m < vector1.length; m++) {
            mean1 += vector1[m];
            mean2 += vector2[m];
        }
        mean1 /= vector1.length;
        mean2 /= vector2.length;
        
        float cov=0.0f, squared1=0.0f, squared2=0.0f;
        for (int m=0; m < vector1.length; m++) {
            cov += (vector1[m]-mean1) * (vector2[m]-mean2);
            squared1 += (vector1[m]-mean1) * (vector1[m]-mean1);
            squared2 += (vector2[m]-mean2) * (vector2[m]-mean2);
        }
        
        return cov / (float) (Math.sqrt(squared1) * Math.sqrt(squared2));
    }
    
    
//    private float computeCCC(float[] vector1, float[] vector2) {
//        assert(vector1.length==vector2.length);
//        
//        float mean1=0.0f, mean2=0.0f;
//        for (int m=0; m < vector1.length; m++) {
//            mean1 += vector1[m];
//            mean2 += vector2[m];
//        }
//        mean1 /= vector1.length;
//        mean2 /= vector2.length;
//        
//        float cov=0.0f, var1=0.0f, var2=0.0f;
//        for (int m=0; m < vector1.length; m++) {
//            cov += (vector1[m]-mean1) * (vector2[m]-mean2);
//            var1 += (vector1[m]-mean1) * (vector1[m]-mean1);
//            var2 += (vector2[m]-mean2) * (vector2[m]-mean2);
//        }
//        cov /= vector1.length;
//        var1 /= vector1.length;
//        var2 /= vector2.length;
//        
//        return 2 * cov / (var1 + var2 + (mean1-mean2)*(mean1-mean2));
//    }
//    
//    
//    private float computeEucDist(float[] vector1, float[] vector2) {
//        assert(vector1.length==vector2.length);
//        
//        float dist=0.0f;
//        
//        for (int m=0; m < vector1.length; m++) {
//            dist += (vector1[m] - vector2[m]) * (vector1[m] - vector2[m]);
//        }
//        
//        return (float) Math.sqrt(dist);
//    }
    
    
    public int size() {
        return codewords.length;
    }
    
    public float[][] getCodebook() {
        return codewords;
    }
    
    public void setCodebook(float[][] codewords) {
        this.codewords = codewords; 
    }
}
