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

import openxbow.codebooks.Codebook;
import openxbow.codebooks.CodebookText;
import openxbow.codebooks.HyperCodebook;
import openxbow.io.Reader;

public class HyperBag {
    private DataManager   DM        = null;
    private Reader        reader    = null;
    private HyperCodebook hyperBook = null;
    private Options       options   = null;
    private List<Bag>     subBags   = null;
    private Bag           bag       = null;
    
    private List<Object[]> listAssignments = null;
    
    
    public HyperBag (DataManager DM, HyperCodebook hyperBook, Options options) {
        this.DM        = DM;
        this.reader    = DM.reader;
        this.hyperBook = hyperBook;
        this.options   = options;
    }
    
    
    public void generateSubBagsSVQ() {
        if (!options.bSVQ) {
            System.err.println("Error in generateSubBagsSVQ: Not applicable as SVQ is not chosen.");
            return;
        }
        
        final int numAssignments = 1;  /* a=1 for subwords */
        
        List<Codebook> codebooks = hyperBook.getCodebooks();
        
        int numFeaturesCodebook = 0;
        if (Math.floorMod(reader.getNumFeatures(), options.numSubCodebooksSVQ) > 0) {
            numFeaturesCodebook = (reader.getNumFeatures() / options.numSubCodebooksSVQ) + 1;
        } else {
            numFeaturesCodebook = reader.getNumFeatures() / options.numSubCodebooksSVQ;
        }
        
        subBags = new ArrayList<Bag>();
        for (int s=1; s <= options.numSubCodebooksSVQ; s++) {
            int indexMin = (s-1)*numFeaturesCodebook; 
            int indexMax = Math.min(s*numFeaturesCodebook-1, reader.getNumFeatures()-1);
            
            subBags.add( new Bag(selectData(reader.getIndexesAttributeClass().get(1),indexMin,indexMax), codebooks.get(s), null, null) );
            subBags.get(s-1).generateBoF(numAssignments, options.bGaussianEncoding, options.gaussianStdDev, true);  /* Note: subbags have indexes 0 to numSubCodebooksSVQ-1 */
        }
        
        bofsToList();
    }
    
    
    public void generateBag() {
        if (options.bSVQ) {
            bag = new Bag(listAssignments, hyperBook.getCodebooks().get(0), DM, null);  /* List of assignments */
            bag.generateBoF(options.numAssignments, options.bGaussianEncoding, options.gaussianStdDev, false);
        }
        else  {
            List<Codebook> codebooks = hyperBook.getCodebooks();
            subBags = new ArrayList<Bag>();
            for (Codebook book : codebooks) {
                subBags.add( new Bag(reader.inputData, book, DM, reader.getIndexesAttributeClass().get(hyperBook.getIndexBook(book))) );
            }
            
            for (Bag subBag : subBags) {
                if (subBag.getCodebook() instanceof CodebookText) {
                    subBag.generateBoW();
                } else {
                    if (!options.outputIFileName.isEmpty()) {  /* Output a file with the assigned word indexes */
                        subBag.generateBoF(options.numAssignments, options.bGaussianEncoding, options.gaussianStdDev, true);
                    } else {
                        subBag.generateBoF(options.numAssignments, options.bGaussianEncoding, options.gaussianStdDev, false);
                    }
                }
            }
            
            if (!options.outputIFileName.isEmpty()) {
                bofsToList();
            }
            
            concatenateSubBags();
        }
        
        if (hyperBook.getLogWeighting() || hyperBook.getIDFWeighting()) {
            termWeighting(hyperBook.getLogWeighting(), hyperBook.getIDFWeighting());
        }
        if (options.normalizeBag > 0) {
            normalizeBag(options.normalizeBag);
        }
    }
    
    
    public Bag getBag() {
        return bag;
    }
    
    public HyperCodebook getHyperCodebook() {
        return hyperBook;
    }
    
    public List<Object[]> getListsOfAssignments() {
        return listAssignments;
    }
    
    public int getNumSVQSubBags() {
        return options.numSubCodebooksSVQ;
    }
    
    
    /* Combine all (s) assigned indexes of each instance (id) and each frame (v) into a list (e.g., for the generation of an overall codebook for SVQ) */
    private void bofsToList() {
        listAssignments = new ArrayList<Object[]>();
        
        for (int v=0; v < subBags.get(0).assignments.length; v++) {
            Object[] newLine = new Object[subBags.size()];
            for (int s=0; s < subBags.size(); s++) {
                newLine[s] = subBags.get(s).assignments[v];
            }
            listAssignments.add(newLine);
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
            if (hyperBook.getDocumentFrequencyFactors() != null) { /* Codebook given - NOTE: It is decided whether or not the dffs should be learned here! */
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
            System.err.println("Error: Unknown normalization option!");
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
        float normFactor = 0.0f;
        for (int id=0; id < numIDs; id++) {
            if (optionNormalizeBag==1) {
                normFactor = DM.getNumFrames().get(id);
            } else {
                normFactor = sumTF[id];
            }
            
            for (int w=0; w < sizeCodebook; w++) {
                if (normFactor > Float.MIN_VALUE) {
                    bag.bof[id][w] = bag.bof[id][w] * sizeCodebook / normFactor;
                } else {
                    bag.bof[id][w] = 0.0f;
                }
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
