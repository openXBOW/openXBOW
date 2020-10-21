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
import java.util.Map;

import openxbow.codebooks.CodebookNumeric;

public class NumGrams {
    private DataManager     dm;
    private Bag             bag;
    private CodebookNumeric book;
    public  int[]           idOrigInstances;
    
    public NumGrams(DataManager dm, Bag bag, CodebookNumeric book) {
        this.dm   = dm;
        this.bag  = bag;
        this.book = book;
        
        this.idOrigInstances = generateListOrigInstances(); 
    }
    
    public void convertToBagOfNumGrams(boolean bUni, boolean bBi, boolean bTri) {
        /* Builds the numeric n-gram bags and replaces this bag (subbag). */
        
        int numIDs  = bag.bof.length;
        int sizeBag = 0;
        
        float[][] unibof = null;
        float[][] bibof  = null;
        float[][] tribof = null;
        
        if (bUni) {
            unibof = generateSpecificNumericGramBag(book.getUnigrams());
            sizeBag += unibof[0].length;
        }
        if (bBi) {
            bibof = generateSpecificNumericGramBag(book.getBigrams());
            sizeBag += bibof[0].length;
        }
        if (bTri) {
            tribof =  generateSpecificNumericGramBag(book.getTrigrams());
            sizeBag += tribof[0].length;
        }
        
        float[][] grambof = new float[numIDs][sizeBag];
        
        int offset = 0;
        if (bUni) {
            for (int id=0; id < numIDs; id++) {
                for (int k=0; k < unibof[0].length; k++) {
                    grambof[id][offset+k] = unibof[id][k];
                }
            }
            offset += unibof[0].length;
        }
        if (bBi) {
            for (int id=0; id < numIDs; id++) {
                for (int k=0; k < bibof[0].length; k++) {
                    grambof[id][offset+k] = bibof[id][k];
                }
            }
            offset += bibof[0].length;
        }
        if (bTri) {
            for (int id=0; id < numIDs; id++) {
                for (int k=0; k < tribof[0].length; k++) {
                    grambof[id][offset+k] = tribof[id][k];
                }
            }
            offset += tribof[0].length;
        }
        
        bag.bof = grambof;  /* Replace the "usual" BoW */
    }
    
    
    private float[][] generateSpecificNumericGramBag (int[][] grams) {
        int sizeCodebook   = grams.length;
        int numGrams       = grams[0].length;
        
        Map<Integer,List<Integer>> mapFrameIDs = dm.getMappingFrameIDs();
        
        int[][]   assignments = bag.assignments;
        float[][] gramBof     = new float[dm.getNumIDs()][sizeCodebook];
        
        int numAssignments = assignments[0].length;
        
        /* Assign */
        int[][] frame = new int[numGrams][numAssignments];
        List<Integer> indexes = new ArrayList<Integer>();
        
        for (int frameIndex=0; frameIndex < assignments.length-numGrams; frameIndex++) {
            /* Continue in case the end of one input sequence is reached. */
            if ((numGrams==3 && (idOrigInstances[frameIndex]!=idOrigInstances[frameIndex+1] || idOrigInstances[frameIndex]!=idOrigInstances[frameIndex+2]))
                || numGrams==2 && idOrigInstances[frameIndex]!=idOrigInstances[frameIndex+1])
            {
                continue;
            }
            
            /* Collect all assignments within the range of the grams */
            for (int g=0; g < numGrams; g++) {
                for (int a=0; a < numAssignments; a++) {
                    frame[g][a] = assignments[frameIndex+g][a];
                }
            }
            
            findIndex(indexes, grams, frame, numGrams);
            
            /* Increase the counter for all corresponding instances (IDs) */
            for (int id=0; id < mapFrameIDs.get(frameIndex).size(); id++) {
                for (int index : indexes) {
                    gramBof[mapFrameIDs.get(frameIndex).get(id)][index]++;
                }
            }
        }
        
        /* Make sure that we do not have a bag of only zeros (in case the number of frames for one id is 0) */
        for (int id=0; id < gramBof.length; id++) {
            if (dm.getNumFrames().get(id)==0) {
                for (int w=0; w < gramBof[0].length; w++) {
                    gramBof[id][w] = 0.001f;
                }
            }
        }
        
        return gramBof;
    }
    
    
    private void findIndex(List<Integer> indexesRet, int[][] grams, int[][] frame, int numGrams) {
        int index;
        indexesRet.clear();
        
        /* Meaningful speed up */
        int frame0 = 0;
        int frame1 = 0;
        int frame2 = 0;
        
        for (int a=0; a < frame[0].length; a++) {
            for (int a1=0; a1 < frame[0].length; a1++) {
                for (int a2=0; a2 < frame[0].length; a2++) {
                    frame0 = frame[0][a];
                    if (numGrams>1) {
                        frame1 = frame[1][a1];
                    }
                    if (numGrams>2) {
                        frame2 = frame[2][a2];
                    }
                    
                    index = 0;
                    while (index < grams.length) {
                        if (numGrams==1 && grams[index][0]==frame0) {
                            indexesRet.add(index);
                            break;
                        }
                        else if (numGrams==2 && grams[index][0]==frame0 && grams[index][1]==frame1) {
                            indexesRet.add(index);
                            break;                
                        }
                        else if (numGrams==3 && grams[index][0]==frame0 && grams[index][1]==frame1 && grams[index][2]==frame2) {
                            indexesRet.add(index);
                            break;                
                        }
                        
                        index++;
                    }
                }
            }
        }
    }
    
    
    private int[] generateListOrigInstances() {
        /* Generates a list of indexes 1,1,1,1,2,2,2,... giving information to which original file each frame belongs to */
        /* Presumes that the input is ordered */
        int[] idOrigInstances = new int[dm.getMappingFrameIDs().size()];
        
        int    indexIn   = 0;
        String strBefore = "";
        for (int k=0; k < idOrigInstances.length; k++) {
            String str = dm.getMappingIDName().get(dm.getMappingFrameIDs().get(k).get(0));  /* All elements in the ID list belong to the same original instance (name), so we can take element 0 */
            if (!str.equals(strBefore)) {
                strBefore = str;
                indexIn++;
            }
            idOrigInstances[k] = indexIn;
        }
        
        return idOrigInstances;
    }
}
