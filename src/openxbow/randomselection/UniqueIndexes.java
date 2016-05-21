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

package openxbow.randomselection;

import java.util.Random;
import java.util.Vector;

public class UniqueIndexes {
    private Vector<Integer> uniqueIndexes = null;
    
    public UniqueIndexes(int inSize, int outSize) {
        this.uniqueIndexes = generateUniqueIndexes(inSize, outSize);
    }
    
    public Vector<Integer> getIndexes() {
        return uniqueIndexes;
    }
    
    /* If outSize < inSize: returns a vector of outSize randomized indexes from 0 to inSize-1 */
    /* Else: returns a vector of indexes from 0 to inSize-1 */
    private Vector<Integer> generateUniqueIndexes(int inSize, int outSize) {
        Vector<Integer> randHistory = new Vector<Integer>();
        
        if (outSize>=inSize) {
            for (int i=0; i < inSize; i++) {
                randHistory.add(i);
            }
        }
        else {
            Random randGenerator = new Random(0);
            int    randNext      = 0;
            
            for (int i=0; i < outSize; i++) {
                do {
                    randNext = randGenerator.nextInt(inSize);
                } while (randHistory.contains(randNext));
                
                randHistory.add(randNext);
            }
        }
        
        return randHistory;
    }
}
