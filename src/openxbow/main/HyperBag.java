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

import openxbow.codebooks.Codebook;
import openxbow.codebooks.CodebookText;
import openxbow.codebooks.HyperCodebook;
import openxbow.io.Reader;

public class HyperBag {
    private DataManager   DM        = null;
    private Reader        reader    = null;
    private HyperCodebook hyperBook = null;
    private List<Bag>     subBags   = null;
    private Bag           bag       = null;
    
    /* Options SVQ */
    private int           splitVQ   = 0;
    
    /* Options relevant for Bag-of-Words */
    private String        stopCharacters;
    private int           nGram;
    private int           nCharGram;
    
    private List<Object[]> listsBof = null;
    
    
    public HyperBag (DataManager DM, HyperCodebook hyperBook) {
        this.DM        = DM;
        this.reader    = DM.reader;
        this.hyperBook = hyperBook;
    }
    
    public HyperBag (DataManager DM, HyperCodebook hyperBook, int splitVQ) {
        this(DM,hyperBook);
        this.splitVQ = splitVQ;
    }
    
    public HyperBag (DataManager DM, HyperCodebook hyperBook, String stopCharacters, int nGram, int nCharGram) {
        this(DM,hyperBook);
        this.stopCharacters = stopCharacters;
        this.nGram          = nGram;
        this.nCharGram      = nCharGram;
    }
    
    
    public void generateSubBagsSVQ(boolean bGaussianEncoding, float gaussianStdDev) {
        if (splitVQ == 0) {
            System.err.println("Error in generateSubBagsSVQ: Not applicable as SVQ is not chosen.");
            return;
        }
        
        final int numAssignments = 1;  /* a=1 for subwords */
        
        List<Codebook> codebooks = hyperBook.getCodebooks();
        
        int numFeaturesCodebook = 0;
        if (Math.floorMod(reader.getNumFeatures(), splitVQ) > 0) {
            numFeaturesCodebook = (reader.getNumFeatures() / splitVQ) + 1;
        } else {
            numFeaturesCodebook = reader.getNumFeatures() / splitVQ;
        }
        
        subBags = new ArrayList<Bag>();
        for (int s=0; s < splitVQ; s++) {
            int indexMin = s*numFeaturesCodebook; 
            int indexMax = Math.min((s+1)*numFeaturesCodebook-1, reader.getNumFeatures()-1);
            
            subBags.add( new Bag(selectData(reader.getIndexesAttributeClass().get(1),indexMin,indexMax), codebooks.get(s), null, null) );
            subBags.get(s).generateBoF(numAssignments, bGaussianEncoding, gaussianStdDev);
        }
        
        bofsToList();
    }
    
    
    public void generateBag(int numAssignments, int optionNormalizeBag) {
        generateBag(numAssignments, false, 0.0f, optionNormalizeBag);
    }
    
    
    public void generateBag(int numAssignments, boolean bGaussianEncoding, float gaussianStdDev, int optionNormalizeBag) {
        if (splitVQ > 0) {
            bag = new Bag(listsBof, hyperBook.getCodebookSVQ(), DM, null);  /* List of assignments */
            bag.generateBoF(numAssignments, bGaussianEncoding, gaussianStdDev);
        }
        else  {
            List<Codebook> codebooks = hyperBook.getCodebooks();
            subBags = new ArrayList<Bag>();
            for (Codebook book : codebooks) {
                subBags.add( new Bag(reader.inputData, book, DM, reader.getIndexesAttributeClass().get(hyperBook.getIndexBook(book))) );
            }
            
            for (Bag subBag : subBags) {
                if (subBag.getCodebook() instanceof CodebookText) {
                    subBag.generateBoW(stopCharacters, nGram, nCharGram);
                } else {
                    subBag.generateBoF(numAssignments, bGaussianEncoding, gaussianStdDev);
                }
            }
            
            concatenateSubBags();
        }
        
        if (hyperBook.getLogWeighting() || hyperBook.getIDFWeighting()) {
            termWeighting(hyperBook.getLogWeighting(), hyperBook.getIDFWeighting());
        }
        if (optionNormalizeBag > 0) {
            normalizeBag(optionNormalizeBag);
        }
    }
    
    
    public Bag getBag() {
        return bag;
    }
    
    public HyperCodebook getHyperCodebook() {
        return hyperBook;
    }
    
    public List<Object[]> getListsOfBags() {
        return listsBof;
    }
    
    public int getNumSVQSubBags() {
        return splitVQ;
    }
    
    
    /* For SVQ: Combine all (s) assigned indexes of each instance (id) and each frame (v) into a list for the generation of an overall codebook */
    private void bofsToList() {
        listsBof = new ArrayList<Object[]>();
        
        for (int v=0; v < subBags.get(0).assignments.length; v++) {
            Object[] newLine = new Object[splitVQ];
            for (int s=0; s < splitVQ; s++) {
                newLine[s] = subBags.get(s).assignments[v];
            }
            listsBof.add(newLine);
        }
    }
    
    
    /* For SVQ */
    private List<Object[]> selectData(List<Integer> indexesNumericFeatures, int indexMin, int indexMax) {
        List<Object[]> selData = new ArrayList<Object[]>();
        int numFeatures = indexMax - indexMin + 1;
        
        for (int v=0; v < reader.inputData.size(); v++) {
            Object[] fVector = new Object[numFeatures];
            for (int f=0; f < numFeatures; f++) {
                fVector[f] = reader.inputData.get(v)[indexesNumericFeatures.get(indexMin+f)];
            }
            selData.add(fVector);
        }
        
        return selData;
    }
    
    
    private void concatenateSubBags() {
        bag = new Bag();
        
        int numIDs  = DM.getNumIDs();
        int sizeBag = 0;
        for (Bag subBag : subBags) {
            sizeBag += subBag.bof[0].length;
        }
        
        if (subBags.size()==1) {
            bag.bof = subBags.get(0).bof;
        } else {
            bag.bof = new float[numIDs][sizeBag];
            sizeBag = 0;
            for (Bag subBag : subBags) {
                int sizeSubBag = subBag.bof[0].length;
                for (int id=0; id < numIDs; id++) {
                    for (int k=0; k < sizeSubBag; k++) {
                        bag.bof[id][sizeBag+k] = subBag.bof[id][k];
                    }
                }
                sizeBag += sizeSubBag;
            }
        }
    }
    
    
    private void termWeighting(boolean bLogWeighting, boolean bIDFWeighting) {
        int numIDs       = bag.bof.length;
        int sizeCodebook = hyperBook.getSize(); 
        
        float[] dff = null; /* document frequency factor */
        
        if (bIDFWeighting) {
            if (hyperBook.getDocumentFrequencyFactors() != null) { /* Codebook given */
                dff = hyperBook.getDocumentFrequencyFactors();
            }
            else {
                dff = computeDocumentFrequencyFactors();
                hyperBook.setIDFWeighting();
                hyperBook.setDocumentFrequencyFactors(dff); /* Save dff to codebook */
            }
        }
        
        /* TF, IDF, TF-IDF */
        for (int w = 0; w < sizeCodebook; w++) {
            for (int id = 0; id < numIDs; id++) {
                if (bLogWeighting && !bIDFWeighting) {
                    bag.bof[id][w] = (float) Math.log10(bag.bof[id][w] + 1);
                }
                else if (!bLogWeighting && bIDFWeighting) {
                    bag.bof[id][w] = bag.bof[id][w] * dff[w];
                }
                else if (bLogWeighting && bIDFWeighting) {
                    bag.bof[id][w] = ((float) Math.log10(bag.bof[id][w] + 1)) * dff[w];
                }
                else {
                    System.out.println("Error: termWeighting() should not be called if no term weighting is desired!");
                }
            }
        }
    }
    
    
    private void normalizeBag(int optionNormalizeBag) {
        int numIDs       = bag.bof.length;
        int sizeCodebook = hyperBook.getSize();
        
        if (optionNormalizeBag<1 || optionNormalizeBag>3) {
            System.out.println("Error: Unknown normalization option!");
            return;
        }
        
        /* TF for each instance */
        float[] sumTF = new float[numIDs];
        if (optionNormalizeBag==2 || optionNormalizeBag==3) {
            for (int id=0; id < numIDs; id++) {
                for (int w=0; w < sizeCodebook; w++) {
                    if (optionNormalizeBag==2) {
                        sumTF[id] += bag.bof[id][w];
                    } else if (optionNormalizeBag==3) {
                        sumTF[id] += Math.pow(bag.bof[id][w],2);
                    }
                }
                if (optionNormalizeBag==3) {
                    sumTF[id] = (float) Math.sqrt(sumTF[id]);
                }
            }
        }
        
        /* Normalization */
        float normFactor = 0;
        for (int id=0; id < numIDs; id++) {
            if (optionNormalizeBag==1) {
                normFactor = DM.getNumFrames().get(id);
            } else {
                normFactor = sumTF[id];
            }
            
            for (int w=0; w < sizeCodebook; w++) {
                bag.bof[id][w] = bag.bof[id][w] * sizeCodebook / normFactor;
            }
        }
    }
    
    
    private float[] computeDocumentFrequencyFactors() {
        int numIDs       = bag.bof.length;
        int sizeCodebook = hyperBook.getSize();
        
        /* Number of instances where each codeword appears */
        float[] numAppear  = new float[sizeCodebook];
        for (int w=0; w < sizeCodebook; w++) {
            for (int id=0; id < numIDs; id++) {
                if (bag.bof[id][w] > Float.MIN_NORMAL) {
                    numAppear[w]++;
                }
            }
        }
        
        /* IDF, TF-IDF */
        float[] dff = new float[sizeCodebook]; /* document frequency factor */
        for (int w = 0; w < sizeCodebook; w++) {
            if (numAppear[w] > 0) {
                dff[w] = (float) Math.log10(numIDs / numAppear[w]);
            } else {
                dff[w] = 1;
            }
        }
        
        return dff;
    }
    
}
