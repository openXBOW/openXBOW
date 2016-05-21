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
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class CodebookNumeric extends Codebook {
    private float[][] codewords = null;
    
    
    public CodebookNumeric() {
        
    }
    
    public void generateCodebook(CodebookNumericTrainingSelector train, String generationMethod, int sizeCodebook)
    {
        if (train.trainingDataSupervised != null) {
            int       numClasses          = train.trainingDataSupervised.size();
            int       numClustersPerClass = (int) Math.ceil((double) sizeCodebook / (double) numClasses);
            float[][] centroidsClass      = null;
            
            codewords = new float[numClustersPerClass*numClasses][train.trainingDataSupervised.get(0).get(0).length];
            
            for (int c=0; c < numClasses; c++) {
                if (train.trainingDataSupervised.get(c).size() < numClustersPerClass) {
                    System.err.println("Error in CodebookNumeric.generateCodebook(...): sizeCodebook is larger than number of class-specific training data.");
                    return;
                }
                
                if (generationMethod.equals("kmeans")) {
                    centroidsClass = kMeans(train.trainingDataSupervised.get(c),numClustersPerClass,false,false);
                }
                else if (generationMethod.equals("kmeans++")) {
                    centroidsClass = kMeans(train.trainingDataSupervised.get(c),numClustersPerClass,true,false);
                }
                else if (generationMethod.equals("kmeansnorm")) {
                    centroidsClass = kMeans(train.trainingDataSupervised.get(c),numClustersPerClass,false,true);
                }
                else if (generationMethod.equals("kmeans++norm")) {
                    centroidsClass = kMeans(train.trainingDataSupervised.get(c),numClustersPerClass,true,true);
                }
                else if (generationMethod.equals("random")) {
                    centroidsClass = randomSampling(train.trainingDataSupervised.get(c),numClustersPerClass);
                }
                else if (generationMethod.equals("random++")) {
                    centroidsClass = randomSamplingPlusPlus(train.trainingDataSupervised.get(c),numClustersPerClass);
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
            if (train.trainingData.size() < sizeCodebook) {
                System.err.println("Error in CodebookNumeric.generateCodebook(...): sizeCodebook is larger than number of training data.");
                return;
            }
            
            /* Codebook generation */
            if (generationMethod.equals("kmeans")) {
                codewords = kMeans(train.trainingData,sizeCodebook,false,false);
            }
            else if (generationMethod.equals("kmeans++")) {
                codewords = kMeans(train.trainingData,sizeCodebook,true,false);
            }
            else if (generationMethod.equals("kmeansnorm")) {
                codewords = kMeans(train.trainingData,sizeCodebook,false,true);
            }
            else if (generationMethod.equals("kmeans++norm")) {
                codewords = kMeans(train.trainingData,sizeCodebook,true,true);
            }
            else if (generationMethod.equals("random")) {
                codewords = randomSampling(train.trainingData,sizeCodebook);
            }
            else if (generationMethod.equals("random++"))
                codewords = randomSamplingPlusPlus(train.trainingData,sizeCodebook);
            else {
                System.err.println("Error: Codebook generation method unknown.");
            }
        }
    }
    
    
    private float[][] randomSampling(List<float[]> trainingData, int sizeCodebook) {
        float[][] centroids     = new float[sizeCodebook][trainingData.get(0).length];
        Random    randGenerator = new Random(10);  /* Seed 10 to keep it consistent with simpleKMeans in Weka */
        
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
    
    
    private float[][] randomSamplingPlusPlus(List<float[]> trainingData, int sizeCodebook) {
        /* kMeans++ initialization */
        int       numFeatures   = trainingData.get(0).length;
        float[][] centroids     = new float[sizeCodebook][trainingData.get(0).length];
        Random    randGenerator = new Random(10);  /* Seed 10 to keep it consistent with kMeans in Weka */
        
        /* Choose one instance to be the center */
        int center0 = randGenerator.nextInt(trainingData.size());
        for (int m=0; m < trainingData.get(0).length; m++) {
            centroids[0][m] = trainingData.get(center0)[m];
        }
        
        double[] distances      = new double[trainingData.size()];
        double[] cumulatedProbs = new double[trainingData.size()];
        
        float diff = 0;
        float[] features = null;
        
        for (int i=0; i < trainingData.size(); i++) {
            distances[i] = 0;
            features = trainingData.get(i);
            for (int m=0; m < numFeatures; m++) {
                diff = centroids[0][m] - features[m];
                distances[i] += diff * diff;
            }
        }
        
        /* Select the centers of the remaining clusters */
        for (int k = 1; k < sizeCodebook; k++) {
            /* Convert distances to probabilities */
            double[] probs = new double[trainingData.size()];
            System.arraycopy(distances, 0, probs, 0, distances.length);
            probs = normalizeVector(probs);
            
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
            
            features = trainingData.get(x);
            for (int m=0; m < numFeatures; m++) {
                centroids[k][m] = features[m];
            }
            
            /* If an instance is now closer to another cluster center, this is the determining cluster */
            for (int c = 0; c < trainingData.size(); c++) {
                if (distances[c] > 0) {
                    double newDistance = 0;
                    features = trainingData.get(c);
                    for (int m=0; m < numFeatures; m++) {
                        diff = centroids[k][m] - features[m];
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
    
    
    private float[][] kMeans(List<float[]> trainingData, int sizeCodebook, boolean bPlusPlus, boolean bNormalize) {
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
            centroids = randomSamplingPlusPlus(trainingData, sizeCodebook);
        }
        else {
            centroids = randomSampling(trainingData, sizeCodebook);
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
    
    
    private double[] normalizeVector(double[] vector) {
        double   sum = 0;
        double[] vectorNorm = new double[vector.length];
        
        for (double v : vector) {
            sum += v;
        }
        
        if (sum > 0) {
            for (int i=0; i < vector.length; i++) {
                vectorNorm[i] = vector[i] / sum;
            }
        }
        
        return vectorNorm;
    }
    
    
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
