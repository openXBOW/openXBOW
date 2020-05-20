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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import openxbow.io.Reader;
import openxbow.nlp.String2WordVector;


public class CodebookText extends Codebook {
    private String[] wordVector = null;
    
    
    public CodebookText(CodebookConfig config) {
        super(config);
    }
    
    public void generateCodebook(Reader reader) {
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
                String[] wordVectorInst = s2wv.string2WordVector((String) frame[ind], config.stopChar, config.nGram, config.nCharGram);
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
        if (config.maxTermFreq==0) {
            config.maxTermFreq = Integer.MAX_VALUE;
        }
        int numWords = 0;
        for (Map.Entry<String,Integer> e : wordFreq.entrySet()) {
            if (e.getValue()>=config.minTermFreq && e.getValue()<=config.maxTermFreq) {
                numWords++;
            }
        }
        
        wordVector = new String[numWords];
        int c = 0;
        for (Map.Entry<String,Integer> e : wordFreq.entrySet()) {
            if (e.getValue()>=config.minTermFreq && e.getValue()<=config.maxTermFreq) {
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
        return config.stopChar;
    }
    
    public int getNGram() {
        return config.nGram;
    }
    
    public int getNCharGram() {
        return config.nCharGram;
    }
    
    public void setCodebook(String[] wordVector, String stopCharacters, int nGram, int nCharGram) {
        this.wordVector       = wordVector;
        this.config.stopChar  = stopCharacters;
        this.config.nGram     = nGram;
        this.config.nCharGram = nCharGram;
    }
}
