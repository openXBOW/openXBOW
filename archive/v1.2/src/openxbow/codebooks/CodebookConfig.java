/*F************************************************************************
 * openXBOW - the Passau Open-Source Crossmodal Bag-of-Words Toolkit
 * Copyright (C) 2016-2020, 
 *   Maximilian Schmitt & Björn Schuller: University of Passau, 
 *    University of Augsburg.
 *   Contact: maximilian.schmitt@mailbox.org
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

public class CodebookConfig {
    protected enum cbtype {text,numeric};
    protected enum cbgenmethod {random,randompp,kmeans,kmeanspp,kmeansnorm,kmeansppnorm,em,empp,emkm,emkmpp,generic};
    
    protected cbtype codebookType;
    
    /* Numeric */
    protected int         sizeCodebookInitial;  /* Desired size of the codebook (can be different because of reduction, supervised, text, ...)*/ 
    protected cbgenmethod generationMethod;
    protected float       genericOffset = 1.0f;
    protected boolean     bReduceCodebook  = false;
    protected float       thReduceCodebook = 0.0f;
    protected boolean     bSupervised = false;
    protected int         randomSeed = 10;  /* 10 is default in Weka */
    protected int         numTraining = 0;
    
    /* Numeric n-grams */
    protected int maxSizeUnigram = 0;  /* 0: no unigrams */
    protected int maxSizeBigram  = 0;  /* 0: no bigrams */
    protected int maxSizeTrigram = 0;  /* 0: no trigrams */
    
    /* Text */
    protected int    minTermFreq = 1;
    protected int    maxTermFreq = 0;  /* 0: no limit */
    protected String stopChar    = "";
    protected int    nGram       = 1;
    protected int    nCharGram   = 0;  /* 0: no n-character-grams */
    
    
    /* Numeric */
    public CodebookConfig(int     sizeCodebookInitial,
                          String  strGenerationMethod,
                          float   genericOffset,
                          boolean bReduceCodebook,
                          float   thReduceCodebook,
                          boolean bSupervised,
                          int     randomSeed,
                          int     numTraining)
    {
        this.codebookType        = cbtype.numeric;
        this.sizeCodebookInitial = sizeCodebookInitial;
        
        if (strGenerationMethod.equals("random")) {
            this.generationMethod = cbgenmethod.random;
        } else if (strGenerationMethod.equals("random++")) {
            this.generationMethod = cbgenmethod.randompp;
        } else if (strGenerationMethod.equals("kmeans")) {
            this.generationMethod = cbgenmethod.kmeans;
        } else if (strGenerationMethod.equals("kmeans++")) {
            this.generationMethod = cbgenmethod.kmeanspp;
        } else if (strGenerationMethod.equals("kmeansnorm")) {
            this.generationMethod = cbgenmethod.kmeansnorm;
        } else if (strGenerationMethod.equals("kmeans++norm")) {
            this.generationMethod = cbgenmethod.kmeansppnorm;
        } else if (strGenerationMethod.equals("em")) {
            this.generationMethod = cbgenmethod.em;
        } else if (strGenerationMethod.equals("em++")) {
            this.generationMethod = cbgenmethod.empp;
        } else if (strGenerationMethod.equals("em-kmeans")) {
            this.generationMethod = cbgenmethod.emkm;
        } else if (strGenerationMethod.equals("em-kmeans++")) {
            this.generationMethod = cbgenmethod.emkmpp;
        } else if (strGenerationMethod.equals("generic")) {
            this.generationMethod = cbgenmethod.generic;
        } else {
            System.err.println("Error: CodebookConfig: Codebook generation method is not valid!");
        }
        
        this.genericOffset    = genericOffset;
        
        this.bReduceCodebook  = bReduceCodebook;
        this.thReduceCodebook = thReduceCodebook;
        
        this.bSupervised      = bSupervised;
        this.randomSeed       = randomSeed;
        this.numTraining      = numTraining;
    }
    
    public CodebookConfig(int     sizeCodebookInitial,
                          String  strGenerationMethod,
                          float   genericOffset,
                          boolean bReduceCodebook,
                          float   thReduceCodebook,
                          boolean bSupervised,
                          int     randomSeed,
                          int     numTraining,
                          int     maxSizeUnigram,
                          int     maxSizeBigram,
                          int     maxSizeTrigram)
    {
        this(sizeCodebookInitial, 
             strGenerationMethod, 
             genericOffset, 
             bReduceCodebook, 
             thReduceCodebook, 
             bSupervised, 
             randomSeed, 
             numTraining);
        
        this.maxSizeUnigram = maxSizeUnigram;
        this.maxSizeBigram  = maxSizeBigram;
        this.maxSizeTrigram = maxSizeTrigram;
    }
    
    
    /* Text */
    public CodebookConfig(int    minTermFreq,
                          int    maxTermFreq,
                          String stopChar,
                          int    nGram,
                          int    nCharGram)
    {
        this.codebookType = cbtype.text;        
        this.minTermFreq  = minTermFreq;
        this.maxTermFreq  = maxTermFreq;
        this.stopChar     = stopChar;
        this.nGram        = nGram;
        this.nCharGram    = nCharGram;
    }
    
    
    public boolean isNumeric() {
    	if (this.codebookType==cbtype.numeric) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }
}
