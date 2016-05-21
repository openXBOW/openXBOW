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

import openxbow.io.Reader;
import openxbow.nlp.String2WordVector;


public class CodebookText extends Codebook {
    private String[] wordVector = null;
    
    private String stopCharacters; /* if stopCharacters='' default are the following characters: .,;:"()?!* */
    private int    nGram;
    private int    nCharGram;
    
    
    public CodebookText(String stopCharacters, int nGram, int nCharGram) {
        this.stopCharacters = stopCharacters;
        this.nGram          = nGram;
        this.nCharGram      = nCharGram;
    }
    
    public void generateCodebook(Reader reader, 
                                 int    minTermFreq,
                                 int    maxTermFreq)
    {
        /* Supervised training is not available (probably not meaningful) */
        
        String2WordVector   s2wv      = new String2WordVector();
        List<Integer>       indexText = new ArrayList<Integer>();
        Map<String,Integer> wordFreq  = new HashMap<String,Integer>();
        
        for (int k=0; k < reader.getIndexesAttributeClass().get(0).size(); k++) {
            indexText.add(reader.getIndexesAttributeClass().get(0).get(k));
        }
        
        /* Get all words from the input data */
        for (Object[] frame : reader.inputData) {
            for (Integer ind : indexText) {  /* All text features */
                String[] wordVectorInst = s2wv.string2WordVector((String) frame[ind], stopCharacters, nGram, nCharGram);
                for (String s : wordVectorInst) {
                    if (wordFreq.containsKey(s)) {
                        wordFreq.replace(s, wordFreq.get(s)+1);
                    } else {
                        wordFreq.put(s, 1);
                    }
                }
            }
        }
        
        /* Do thresholding and convert to static codebook array */
        int numWords = 0;
        for (Map.Entry<String,Integer> e : wordFreq.entrySet()) {
            if (e.getValue()>=minTermFreq && e.getValue()<=maxTermFreq) {
                numWords++;
            }
        }
        
        wordVector = new String[numWords];
        int c = 0;
        for (Map.Entry<String,Integer> e : wordFreq.entrySet()) {
            if (e.getValue()>=minTermFreq && e.getValue()<=maxTermFreq) {
                wordVector[c++] = e.getKey();
            }
        }
    }
    
    
    public int size() {
        return wordVector.length;
    }
    
    public String[] getCodebook() {
        return wordVector;
    }
    
    public String getStopCharacters() {
        return stopCharacters;
    }
    
    public int getNGram() {
        return nGram;
    }
    
    public int getNCharGram() {
        return nCharGram;
    }
    
    public void setCodebook(String[] wordVector, String stopCharacters, int nGram, int nCharGram) {
        this.wordVector     = wordVector;
        this.stopCharacters = stopCharacters;
        this.nGram          = nGram;
        this.nCharGram      = nCharGram;
    }
}
