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
