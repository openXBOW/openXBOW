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

public class CodebookNumericGeneric {
    private CodebookConfig config = null;
    
    protected CodebookNumericGeneric(CodebookConfig config) {
        this.config = config;
    }
    
    protected float[][] generateCodebook(int numFeatures) {
        int   codebookSize  = (int) Math.round(Math.pow(2, numFeatures));
        float genericOffset = config.genericOffset;
        
        float[][] codewords = new float[codebookSize][numFeatures];
        
        if (codebookSize != config.sizeCodebookInitial) {
            System.err.println("Warning in CodebookNumericGeneric.generateCodebook(...): Codebook size does not conform with the number of features given.");
        }
        
        for (int k=0; k < codebookSize; k++) {
            for (int m=0; m < numFeatures; m++) {
                int denom = (int) Math.pow(2, numFeatures-m-1);
                codewords[k][m] = (float) Math.pow(-1.0f, Math.floor(k/denom)+1) * genericOffset;
            }
        }
        
        return codewords;
    }
}
