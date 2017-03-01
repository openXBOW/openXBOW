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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import openxbow.clparser.CLParser;
import openxbow.codebooks.CodebookConfig;

public class Options {
    private CLParser OWParser = null;
    
    /* Filenames */
    protected String  inputFileName   = "";
    protected String  attributes      = "";
    protected String  labelsFileName  = "";
    protected String  outputFileName  = "";
    protected String  outputIFileName = "";
    protected boolean bWriteName;
    protected boolean bWriteTimeStamp;
    protected String  arffLabels       = "";
    protected boolean bAppend;
    protected String  loadCodebookName = "";
    protected String  saveCodebookName = "";
    
    /* Segmentation */
    protected boolean bTimeStamp;
    protected float   windowSize;
    protected float   hopSize;
    
    /* Preprocessing */
    public boolean bRemoveLowEnergy = false;
    public int     energyIndex;
    public float   energyThreshold;
    public boolean bStandardizeInput = false;
    public boolean bNormalizeInput   = false;
    
    /* Codebook configuration */
    public List<CodebookConfig> cbConfig;
    public boolean              bSVQ;
    public int                  numSubCodebooksSVQ;
    
    /* Bag-of-Features */
    public int     numAssignments = 1;
    public boolean bGaussianEncoding = false;
    public float   gaussianStdDev = 1.0f;
    
    /* Weighting options */
    public boolean bLogWeighting = false;
    public boolean bIDFWeighting = false;
    public int     normalizeBag = 0;
    
    /* Postprocessing */
    public boolean bStandardizeOutput = false;
    public boolean bNormalizeOutput   = false;
    
    
    /* Methods */
    public Options() {
        /* To set the options manually */
    }    
    public Options(CLParser OWParser) {
        this.OWParser = OWParser;
        addAllOptions(OWParser);
    }
    
    public void parseCL(String[] args) {
        OWParser.parseCL(args);
        
        /* Filenames */
        inputFileName    = OWParser.getOption("i").getParamList().get(0).toString();
        attributes       = OWParser.getOption("attributes").getParamList().get(0).toString();
        labelsFileName   = OWParser.getOption("l").getParamList().get(0).toString();
        outputFileName   = OWParser.getOption("o").getParamList().get(0).toString();
        outputIFileName  = OWParser.getOption("oi").getParamList().get(0).toString();
        bWriteName       = OWParser.getOption("writeName").isPresent();
        bWriteTimeStamp  = OWParser.getOption("writeTimeStamp").isPresent();
        arffLabels       = OWParser.getOption("arffLabels").getParamList().get(0).toString();
        bAppend          = OWParser.getOption("append").isPresent();
        loadCodebookName = OWParser.getOption("b").getParamList().get(0).toString();
        saveCodebookName = OWParser.getOption("B").getParamList().get(0).toString();
        
        /* Segmentation */
        bTimeStamp =         OWParser.getOption("t").isPresent();
        windowSize = (float) OWParser.getOption("t").getParamList().get(0);
        hopSize    = (float) OWParser.getOption("t").getParamList().get(1);
        
        /* Preprocessing */
        bRemoveLowEnergy  =         OWParser.getOption("e").isPresent();
        energyIndex       = (int)   OWParser.getOption("e").getParamList().get(0);
        energyThreshold   = (float) OWParser.getOption("e").getParamList().get(1);
        bStandardizeInput =         OWParser.getOption("standardizeInput").isPresent();
        bNormalizeInput   =         OWParser.getOption("normalizeInput").isPresent();
        
        /* Postprocessing */
        bStandardizeOutput = OWParser.getOption("standardizeOutput").isPresent();
        bNormalizeOutput   = OWParser.getOption("normalizeOutput").isPresent();
        
        /* Codebook configuration */
        cbConfig           = new ArrayList<CodebookConfig>();
        bSVQ               = OWParser.getOption("svq").isPresent();
        numSubCodebooksSVQ = (int) OWParser.getOption("svq").getParamList().get(0);   /* Number of sub-vectors (=0 if no split VQ) */
        
        /* Codebook text (all options are global) */
        int minTermFreq       = (int) OWParser.getOption("minTermFreq").getParamList().get(0);
        int maxTermFreq       = (int) OWParser.getOption("maxTermFreq").getParamList().get(0);
        String stopCharacters =       OWParser.getOption("stopChar").getParamList().get(0).toString();
        int nGram             = (int) OWParser.getOption("nGram").getParamList().get(0);
        int nCharGram         = (int) OWParser.getOption("nCharGram").getParamList().get(0);
        
        cbConfig.add(0, new CodebookConfig(minTermFreq,maxTermFreq,stopCharacters,nGram,nCharGram));
        
        /* Codebook numeric - global options */
        boolean bSupervised        =       OWParser.getOption("supervised").isPresent();      /* Create clusters per class first, then merge them */
        int     randomSeed         = (int) OWParser.getOption("seed").getParamList().get(0);  /* Random seed for codebook generation (has no effect on Training Selector) */
        int     sizeSubCodebookSVQ = (int) OWParser.getOption("svq").getParamList().get(1);   /* Size of the sub-codebooks in SVQ */
        /* Codebook numeric - codebook specific options */
        String strSizeCodebook     = OWParser.getOption("size").getParamList().get(0).toString();      /* (Initial) number of words */
        String strGenerationMethod = OWParser.getOption("c").getParamList().get(0).toString();         /* k-means, random */
        String strReduceCodebook   = OWParser.getOption("reduce").getParamList().get(0).toString();    /* Merge similar codewords (threshold) */
        String strNumTraining      = OWParser.getOption("numTrain").getParamList().get(0).toString();  /* Number of features considered for clustering (0: all input vectors) */
        
        String[] aStrSizeCodebook     = strSizeCodebook.split(",");
        String[] aStrGenerationMethod = strGenerationMethod.split(",");
        String[] aStrReduceCodebook   = strReduceCodebook.split(",");
        String[] aStrNumTraining      = strNumTraining.split(",");
        
        boolean bReduceCodebook = false;
        if (OWParser.getOption("reduce").isPresent()) {  /* Codebook reduction applies to all codebooks - parameter may be 1.0 if reduction is not wanted. */
            bReduceCodebook = true;
        }
        
        if (bSVQ) {
            /* In case of SVQ, options are special */
            if (aStrSizeCodebook.length > 1 || aStrGenerationMethod.length > 1 || aStrReduceCodebook.length > 1 || aStrNumTraining.length > 1) {
                System.err.println("Error Options.parseCL: In case of SVQ, multiple parameters are not allowed.");
            }
            /* In SVQ, codebook 0 is the top-level codebook, sub-codebooks have indexes 1 to numSubcodebooks */ 
            cbConfig.add(0, new CodebookConfig((int) Integer.valueOf(strSizeCodebook), 
                                               strGenerationMethod,   /* So far, generation methods are the same (with SVQ). TODO: Change command line interface. */
                                               bReduceCodebook, 
                                               (float) Float.valueOf(strReduceCodebook), 
                                               bSupervised, 
                                               randomSeed, 
                                               (int) Integer.valueOf(strNumTraining)));
            /* Sub-codebooks */
            for (int i=1; i <= numSubCodebooksSVQ; i++) {
                cbConfig.add(i, new CodebookConfig(sizeSubCodebookSVQ, 
                                                   strGenerationMethod,  /* So far, generation methods are the same (with SVQ). TODO: Change command line interface. */ 
                                                   bReduceCodebook, 
                                                   (float) Float.valueOf(strReduceCodebook), 
                                                   bSupervised, 
                                                   randomSeed, 
                                                   (int) Integer.valueOf(strNumTraining)));
            }
        }
        else if (aStrSizeCodebook.length > 1 || aStrGenerationMethod.length > 1 || aStrReduceCodebook.length > 1 || aStrNumTraining.length > 1) {
            /* Numeric codebooks have different configurations */
            for (int i=1; i <= 9; i++) {  // TODO: 9 is the maximum number of numeric codebooks for the moment
                if (aStrSizeCodebook.length >= i) {
                    strSizeCodebook = aStrSizeCodebook[i-1];
                } else {
                    strSizeCodebook = aStrSizeCodebook[aStrSizeCodebook.length-1];
                }
                if (aStrGenerationMethod.length >= i) {
                    strGenerationMethod = aStrGenerationMethod[i-1];
                } else {
                    strGenerationMethod = aStrGenerationMethod[aStrGenerationMethod.length-1];
                }
                if (aStrReduceCodebook.length >= i) {
                    strReduceCodebook = aStrReduceCodebook[i-1];
                } else {
                    strReduceCodebook = aStrReduceCodebook[aStrReduceCodebook.length-1];
                }
                if (aStrNumTraining.length >= i) {
                    strNumTraining = aStrNumTraining[i-1];
                } else {
                    strNumTraining = aStrNumTraining[aStrNumTraining.length-1];
                }
                
                cbConfig.add(i, new CodebookConfig((int) Integer.valueOf(strSizeCodebook), 
                                                   strGenerationMethod, 
                                                   bReduceCodebook, 
                                                   (float) Float.valueOf(strReduceCodebook), 
                                                   bSupervised, 
                                                   randomSeed, 
                                                   (int) Integer.valueOf(strNumTraining)));
            }
        } else {
            /* All numeric codebooks have the same configuration */
            for (int i=1; i < 9; i++) {  // TODO: 9 is the maximum number of numeric codebooks for the moment
                cbConfig.add(i, new CodebookConfig((int) Integer.valueOf(strSizeCodebook), 
                                                   strGenerationMethod, 
                                                   bReduceCodebook, 
                                                   (float) Float.valueOf(strReduceCodebook), 
                                                   bSupervised, 
                                                   randomSeed, 
                                                   (int) Integer.valueOf(strNumTraining)));
            }
        }
        
        /* Bag-of-Features */
        numAssignments    = (int) OWParser.getOption("a").getParamList().get(0);
        bGaussianEncoding = OWParser.getOption("gaussian").isPresent();
        gaussianStdDev    = (float) OWParser.getOption("gaussian").getParamList().get(0);
        
        /* Weighting options */
        bLogWeighting = OWParser.getOption("log").isPresent();
        bIDFWeighting = OWParser.getOption("idf").isPresent();
        normalizeBag  = (int) OWParser.getOption("norm").getParamList().get(0);
    }
    
    
    /* Checks command-line options for plausibility and outputs warnings if desired configuration seems not suitable */
    public void checkCLoptions() {
        if (bRemoveLowEnergy && energyIndex < 0) {
            System.err.println("Error: Energy index not valid!");
        }
        if (!loadCodebookName.isEmpty()) {
            if (bRemoveLowEnergy) {
                System.err.println("Warning: Removing low-energy features is not selectable if codebook is provided!");
                bRemoveLowEnergy = false;
            }
            if (bStandardizeInput) {
                System.err.println("Warning: -standardizeInput is not relevant if codebook is provided.");
                bStandardizeInput = false;
            }
            if (bNormalizeInput) {
                System.err.println("Warning: -normalizeInput is not relevant if codebook is provided.");
                bNormalizeInput = false;
            }
            if (bLogWeighting || bIDFWeighting) {
                System.err.println("Warning: Term weighting may not be given if codebook is provided!");
                bLogWeighting = false;
                bIDFWeighting = false;
            }
            if (bStandardizeOutput) {
                System.err.println("Warning: -standardizeOutput is not relevant if codebook is provided.");
                bStandardizeOutput = false;
            }
            if (bNormalizeOutput) {
                System.err.println("Warning: -normalizeOutput is not relevant if codebook is provided.");
                bNormalizeOutput = false;
            }
        }
    }
    
    
    /* List of all options */
    private void addAllOptions(CLParser OWParser) {
        Properties properties = new Properties();
        try {
            BufferedInputStream stream = new BufferedInputStream(new FileInputStream("props/options.properties"));
            properties.load(stream);
            stream.close();
        } catch(IOException e) {
            System.err.println("Error: Cannot read properties file options.properties!");
        }
        
        OWParser.addOption("h", properties.getProperty("h"));
        
        /* Input / output options */
        OWParser.addOption("i", "", properties.getProperty("i"));
        OWParser.addOption("attributes", "", properties.getProperty("attributes"));
        OWParser.addOption("o", "", properties.getProperty("o"));
        OWParser.addOption("oi", "", properties.getProperty("oi"));
        OWParser.addOption("writeName", properties.getProperty("writeName"));
        OWParser.addOption("writeTimeStamp", properties.getProperty("writeTimeStamp"));
        OWParser.addOption("arffLabels", "", properties.getProperty("arffLabels"));
        OWParser.addOption("append", properties.getProperty("append"));
        OWParser.addOption("l", "", properties.getProperty("l"));
        
        /* Preprocessing options */
        OWParser.addOption("t", -1.0f, 0.0f, properties.getProperty("t"));
        OWParser.addOption("e", -1, 0.0f, properties.getProperty("e"));
        OWParser.addOption("standardizeInput", properties.getProperty("standardizeInput"));
        OWParser.addOption("normalizeInput", properties.getProperty("normalizeInput"));
        
        /* Codebook numeric */
        OWParser.addOption("size", "500", properties.getProperty("size"));
        OWParser.addOption("c", "random++", properties.getProperty("c"));
        OWParser.addOption("reduce", "0.0", properties.getProperty("reduce"));
        OWParser.addOption("supervised", properties.getProperty("supervised"));
        OWParser.addOption("seed", 10, properties.getProperty("seed"));  /* 10 is the default random seed in Weka */
        OWParser.addOption("numTrain", "0", properties.getProperty("numTrain"));
        OWParser.addOption("svq", 0, 10, properties.getProperty("svq"));
        
        OWParser.addOption("b", "", properties.getProperty("b"));
        OWParser.addOption("B", "", properties.getProperty("B"));
        
        /* Codebook text */
        OWParser.addOption("minTermFreq",           1, properties.getProperty("minTermFreq"));
        OWParser.addOption("maxTermFreq",           0, properties.getProperty("maxTermFreq"));
        OWParser.addOption("stopChar",    ".,;:()?!*", properties.getProperty("stopChar"));
        OWParser.addOption("nGram",                 1, properties.getProperty("nGram"));
        OWParser.addOption("nCharGram",             0, properties.getProperty("nCharGram"));
        
        /* Bag-of-features options */
        OWParser.addOption("a", 1, properties.getProperty("a"));
        OWParser.addOption("gaussian", 0.0f, properties.getProperty("gaussian"));
        
        /* Weighting options */
        OWParser.addOption("log", properties.getProperty("log"));
        OWParser.addOption("idf", properties.getProperty("idf"));
        OWParser.addOption("norm", 0, properties.getProperty("norm"));
        
        /* Postprocessing options */
        OWParser.addOption("standardizeOutput", properties.getProperty("standardizeOutput"));
        OWParser.addOption("normalizeOutput", properties.getProperty("normalizeOutput"));
    }
}

