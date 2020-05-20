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
    protected String  modelFileName   = "";
    protected String  jsonFileName    = "";
    protected boolean bWriteName;
    protected boolean bWriteTimeStamp;
    protected boolean bNoWriteLabels;
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
    
    /* Bag-of-Features */
    public int[]   numAssignments   = new int [10];   // TODO: Should be more generic
    public float[] gaussianEncoding = new float [10]; // TODO: Should be more generic
    public float[] offCodewords     = new float [10]; // TODO: Should be more generic
    
    /* Numeric n-grams */
    public boolean[] bUnigram  = new boolean [10]; // TODO: Should be more generic
    public boolean[] bBigram   = new boolean [10]; // TODO: Should be more generic
    public boolean[] bTrigram  = new boolean [10]; // TODO: Should be more generic
    
    /* Weighting options */
    public boolean bLogWeighting = false;
    public boolean bIDFWeighting = false;
    public int     normalizeBag  = 0;
    
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
        modelFileName    = OWParser.getOption("svmModel").getParamList().get(0).toString();
        jsonFileName     = OWParser.getOption("oJson").getParamList().get(0).toString();
        bWriteName       = OWParser.getOption("writeName").isPresent();
        bWriteTimeStamp  = OWParser.getOption("writeTimeStamp").isPresent();
        bNoWriteLabels   = OWParser.getOption("noLabels").isPresent();
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
        
        boolean bReduceCodebook = false;
        if (OWParser.getOption("reduce").isPresent()) {  /* Codebook reduction applies to all codebooks - parameter may be 1.0 if reduction is not wanted. */
            bReduceCodebook = true;
        }
        
        /* Codebook numeric - codebook specific options */
        String strSizeCodebook       = OWParser.getOption("size").getParamList().get(0).toString();      /* (Initial) number of words */
        String strGenerationMethod   = OWParser.getOption("c").getParamList().get(0).toString();         /* k-means, random */
        String strGenericOffset      = OWParser.getOption("gen").getParamList().get(0).toString();       /* Value for generic codebook generation */
        String strReduceCodebook     = OWParser.getOption("reduce").getParamList().get(0).toString();    /* Merge similar codewords (threshold) */
        String strNumTraining        = OWParser.getOption("numTrain").getParamList().get(0).toString();  /* Number of features considered for clustering (0: all input vectors) */
        
        String[] aStrSizeCodebook       = strSizeCodebook.split(",");
        String[] aStrGenerationMethod   = strGenerationMethod.split(",");
        String[] aStrGenericOffset      = strGenericOffset.split(",");
        String[] aStrReduceCodebook     = strReduceCodebook.split(",");
        String[] aStrNumTraining        = strNumTraining.split(",");
        
        
        /* Numeric n-grams */
        String strUnigram = OWParser.getOption("unigram").getParamList().get(0).toString();
        String strBigram  = OWParser.getOption("bigram").getParamList().get(0).toString();
        String strTrigram = OWParser.getOption("trigram").getParamList().get(0).toString();
        
        String[] aStrUnigram = strUnigram.split(",");
        String[] aStrBigram  = strBigram.split(",");
        String[] aStrTrigram = strTrigram.split(",");
        
        int[] maxSizeUnigram = new int[10];
        int[] maxSizeBigram  = new int[10];
        int[] maxSizeTrigram = new int[10];
        
        bUnigram[0] = false;   bBigram[0] = false;    bTrigram[0] = false;   // Symbolic codebook
        maxSizeUnigram[0] = 0; maxSizeTrigram[0] = 0; maxSizeTrigram[0] = 0; // Symbolic codebook
        
        for (int i=1; i <= 9; i++) {  // TODO: 9 is the maximum number of numeric codebooks for the moment
            bUnigram[i] = true;
            bBigram[i]  = true;
            bTrigram[i] = true;
            if (OWParser.getOption("unigram").isPresent() && aStrUnigram.length >= i && Integer.parseInt(aStrUnigram[i-1]) > 0) {
                maxSizeUnigram[i] = Integer.parseInt(aStrUnigram[i-1]);
            } else {
                maxSizeUnigram[i] = Integer.parseInt(aStrUnigram[aStrUnigram.length-1]);  // default parameter
                if (maxSizeUnigram[i]==0) {
                    bUnigram[i] = false;  // default parameter
                }
            }
            if (OWParser.getOption("bigram").isPresent() && aStrBigram.length >= i && Integer.parseInt(aStrBigram[i-1]) > 0) {
                maxSizeBigram[i] = Integer.parseInt(aStrBigram[i-1]);
            } else {
                maxSizeBigram[i] = Integer.parseInt(aStrBigram[aStrBigram.length-1]);
                if (maxSizeBigram[i]==0) {
                    bBigram[i] = false;  // default parameter
                }
            }
            if (OWParser.getOption("trigram").isPresent() && aStrTrigram.length >= i && Integer.parseInt(aStrTrigram[i-1]) > 0) {
                maxSizeTrigram[i] = Integer.parseInt(aStrTrigram[i-1]);
            } else {
                maxSizeTrigram[i] = Integer.parseInt(aStrTrigram[aStrTrigram.length-1]);
                if (maxSizeTrigram[i]==0) {
                    bTrigram[i] = false;  // default parameter
                }
            }
        }
        
        
        if (aStrSizeCodebook.length > 1 
           || aStrGenerationMethod.length > 1 
           || aStrGenericOffset.length > 1 
           || aStrReduceCodebook.length > 1 
           || aStrNumTraining.length > 1
           || aStrUnigram.length > 1
           || aStrBigram.length > 1
           || aStrTrigram.length > 1) 
        {
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
                if (aStrGenericOffset.length >= i) {
                    strGenericOffset = aStrGenericOffset[i-1];
                } else {
                    strGenericOffset = aStrGenericOffset[aStrGenericOffset.length-1];
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
                                                   (float) Float.valueOf(strGenericOffset),
                                                   bReduceCodebook, 
                                                   (float) Float.valueOf(strReduceCodebook), 
                                                   bSupervised, 
                                                   randomSeed, 
                                                   (int) Integer.valueOf(strNumTraining),
                                                   maxSizeUnigram[i],
                                                   maxSizeBigram[i],
                                                   maxSizeTrigram[i]));
            }
        } else {
            /* All numeric codebooks have the same configuration */
            for (int i=1; i <= 9; i++) {  // TODO: 9 is the maximum number of numeric codebooks for the moment
                cbConfig.add(i, new CodebookConfig((int) Integer.valueOf(strSizeCodebook), 
                                                   strGenerationMethod,
                                                   (float) Float.valueOf(strGenericOffset), 
                                                   bReduceCodebook, 
                                                   (float) Float.valueOf(strReduceCodebook), 
                                                   bSupervised, 
                                                   randomSeed, 
                                                   (int) Integer.valueOf(strNumTraining),
                                                   maxSizeUnigram[i],
                                                   maxSizeBigram[i],
                                                   maxSizeTrigram[i]));
            }
        }
        
        /* Bag-of-Features */
        /* Num assignments */
        String   strNumAssignments  = OWParser.getOption("a").getParamList().get(0).toString();
        String[] aStrNumAssignments = strNumAssignments.split(",");
        numAssignments[0] = 0;  // Text codebook
        for (int i=1; i <= 9; i++) {  // TODO: 9 is the maximum number of numeric codebooks for the moment
            if (aStrNumAssignments.length >= i) {
                numAssignments[i] = Integer.parseInt(aStrNumAssignments[i-1]);
            } else {
                numAssignments[i] = Integer.parseInt(aStrNumAssignments[aStrNumAssignments.length-1]);
            }
        }
        /* Gaussian encoding */
        String   strGaussianEncoding  = OWParser.getOption("gaussian").getParamList().get(0).toString();
        String[] astrGaussianEncoding = strGaussianEncoding.split(",");
        gaussianEncoding[0] = 0;  // Symbolic codebook
        for (int i=1; i <= 9; i++) {  // TODO: 9 is the maximum number of numeric codebooks for the moment
            if (astrGaussianEncoding.length >= i) {
                gaussianEncoding[i] = Float.parseFloat(astrGaussianEncoding[i-1]);
            } else {
                gaussianEncoding[i] = Float.parseFloat(astrGaussianEncoding[astrGaussianEncoding.length-1]);
            }
        }
        /* Off codewords */
        String   strOffCodewords = OWParser.getOption("off").getParamList().get(0).toString();
        String[] astrOffCodewords = strOffCodewords.split(",");
        offCodewords[0] = 0;  // Symbolic codebook
        for (int i=1; i <= 9; i++) {  // TODO: 9 is the maximum number of numeric codebooks for the moment
            if (astrOffCodewords.length >= i) {
                offCodewords[i] = Float.parseFloat(astrOffCodewords[i-1]);
            } else {
                offCodewords[i] = Float.parseFloat(astrOffCodewords[astrOffCodewords.length-1]);
            }
        }
        
        
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
            if (OWParser.getOption("a").isPresent()) {
                System.err.println("Warning: -a is not relevant if codebook is provided.");
            }
            if (OWParser.getOption("gaussian").isPresent()) {
                System.err.println("Warning: -gaussian is not relevant if codebook is provided.");
            }
            if (OWParser.getOption("off").isPresent()) {
                System.err.println("Warning: -off is not relevant if codebook is provided.");
            }
            if (OWParser.getOption("unigram").isPresent()) {
                System.err.println("Warning: -unigram is not relevant if codebook is provided.");
            }
            if (OWParser.getOption("bigram").isPresent()) {
                System.err.println("Warning: -bigram is not relevant if codebook is provided.");
            }
            if (OWParser.getOption("trigram").isPresent()) {
                System.err.println("Warning: -trigram is not relevant if codebook is provided.");
            }
        }
    }
    
    
    /* List of all options */
    private void addAllOptions(CLParser OWParser) {
        OWParser.addOption("h", "Print this help\n");
        
        /* Input / output options */
        OWParser.addOption("i", "", "Name/Path of an input (ARFF or CSV) file p containing feature vectors over time\n"
                                  + "The first feature must be a string or number which specifies all feature vectors which belong to one analysis window/instance.");
        OWParser.addOption("attributes", "", "An optional string, specifying all input attributes (mandatory in case of multiple labels):\n"
                                           + "n=name, t=time stamp, 0=text feature, 1-9=numeric feature, c=class label/numeric label, r=remove attribute\n"
                                           + "Using different numbers for numeric features will create a separate codebook and bag for all features belonging to the same index.\n"
                                           + "The codebook index is followed by brackets [] specifying the number of consecutive input features belonging to this index.\n"
                                           + "Example: -attributes nt1[14]2[14]c\n"
                                           + "Input file with the structure: name,timestamp,28 numeric features split into two codebooks (14 features each) and one label.");
        OWParser.addOption("o", "", "Name / Path of an output ARFF, CSV or LibSVM file p containing the bag-of-words representation.\n"
                                  + "The file format depends on the given file ending (*.arff, *.csv or *.libsvm).");
        OWParser.addOption("oi", "", "Name / Path of an output CSV file p containing the word indexes.");
        OWParser.addOption("svmModel", "", "Name / Path of a Liblinear model (must be L2R_LR_DUAL) to decode the BoW.\n"
                                         + "p specifies the model file. openXBOW outputs a JSON file with the same name (unless given by oJson option).");
        OWParser.addOption("oJson", "", "Name / Path of the JSON output file including the predictions of the Liblinear model (must be given by the option svmModel).");
        OWParser.addOption("writeName", "Output the id string/number in the output file (only ARFF & CSV).");
        OWParser.addOption("writeTimeStamp", "Output the time stamp in the output file (only ARFF & CSV).");
        OWParser.addOption("noLabels", "Do not output the labels in the output file. This option is useful in two cases:\n"
                                     + "1) The input file (-i) contains labels, but they are not desired in the output (-o).\n"
                                     + "2) A labels file (-l) was given only to restrict the output (-o) to a certain interval in time (see -l).");
        OWParser.addOption("arffLabels", "", "String containing all potential class labels (separator comma without whitespaces) for ARFF output file.\n"
                                           + "Only required if not all labels are found in the input data or (?=unknown). Example: -arffLabels class1,class2,class3");
        OWParser.addOption("append", "Append output to file (if output file already exists).");
        OWParser.addOption("l", "", "CSV file p with the class labels for each analysis window/instance.\n"
                                  + "In case a label file is given, the output is restricted to the instances, where labels are given.\n"
                                  + "Both nominal and numeric classes are supported. Format:\n"
                                  + "1st line (optional): name (according to the input file); label1; label2; ...\n"
                                  + "2nd line:            'file_1.wav'; class1; ...\n"
                                  + "                     [and so on]\n");
        
        /* Preprocessing options */
        OWParser.addOption("t", -1.0f, 0.0f, "Segment the input files with a windows size (segment width) of p1 seconds and a hop size (shift) of p2 seconds\n"
                                           + "If this option is used, the second column of the input file must be a time index (in seconds) of the current frame and \n"
                                           + "the (optional) labels file must have the corresponding time stamp as the 2nd column (name; time; label).\n");
        OWParser.addOption("e", -1, 0.0f, "Remove all feature vectors from the input, where the activity (or energy) is below p2. Index p1 specifies the index of the activity attribute (first index: 1).");
        OWParser.addOption("standardizeInput", "Standardize all numeric input features.\n"
                                             + "The parameters are stored in the codebook file (-B) and then used for standardization of test data (-b) in an online approach.");
        OWParser.addOption("normalizeInput", "Normalize all numeric input features (min->max is normalized to 0->1).\n"
                                           + "The parameters are stored in the codebook file (-B) and then used for standardization of test data (-b) in an online approach.");
        
        /* Codebook numeric */
        OWParser.addOption("size", "500", "Set the (initial) size p of the codebook. (default: size=500)\n"
                                        + "In case of several codebooks (see -attributes) different sizes can be specified using separator comma, e.g., -size 200,500,100");
        OWParser.addOption("c", "random++", "Method of creating the codebook:\n"
                                          + "p=random (default): Generate the codebook by random sampling of the input feature vectors.\n"
                                          + "p=random++: Generate the codebook by random sampling of the input feature vectors with weighting similiar to initialization of kmeans++.\n"
                                          + "p=kmeans: Employ kmeans clustering (Lloyd's algorithm).\n"
                                          + "p=kmeans++: Employ kmeans++ clustering (Lloyd's algorithm).\n"
                                          + "p=generic: Generate a generic codebook (independent from data). The parameter '-size' is not relevant when selecting this method.");
        OWParser.addOption("gen", "1.0", "Offset for the values in the generic codebook.");
        OWParser.addOption("reduce", "0.0", "Reduce the size of the codebook by merging words which are correlated with each other. PCC with threshold p is considered.");
        OWParser.addOption("supervised", "Generate a codebook for each class separately, first, then merge all codebooks. (Not available for numeric labels.)");
        OWParser.addOption("seed", 10, "Select the random seed p used for codebook creation. (Has no effect on training selection configured by -numTrain).");  /* 10 is the default random seed in Weka */
        OWParser.addOption("numTrain", "0", "Randomly choose p feature vectors from the input data for the creation of the codebook (should not be used for random sampling).\n");
        OWParser.addOption("unigram", "0", "Apply the n-gram approach to numeric features using unigrams. Only the p most frequent codewords are taken into account.");
        OWParser.addOption("bigram", "0", "Apply the n-gram approach to numeric features using bigrams. The p most frequent codewords are taken into account.");
        OWParser.addOption("trigram", "0", "Apply the n-gram approach to numeric features using trigrams. The p most frequent codewords are taken into account.\n"
                                         + "The uni-/bi-/trigram codebooks are stored in the codebook file (-B) and used when loading a codebook (-b).\n"
                                         + "In case of several codebooks (see -attributes) different sizes can be specified using separator comma, e.g., -bigram 200,600\n"
                                         + "In case of using uni-/bi-/trigrams, the standard BoW are no longer generated for the respective codebook.\n"
                                         + "p=0 results in the standard BoW approach. \n");
        
        OWParser.addOption("b", "", "Load codebook p (do not create one).");
        OWParser.addOption("B", "", "Save the created codebook as a file p.\n");
        
        /* Codebook text */
        OWParser.addOption("minTermFreq",           1, "Gives a minimum threshold for the number of occurrences of each word/n-gram to be considered for codebook generation (default: minTermFreq=1)");
        OWParser.addOption("maxTermFreq",           0, "Gives a maximum threshold for the number of occurrences of each word/n-gram to be considered for codebook generation (default: maxTermFreq=0(inf))");
        OWParser.addOption("stopChar",    ".,;:()?!*", "Specifies characters which are removed from all input instances (default: .,;:()?!* )");
        OWParser.addOption("nGram",                 1, "N-gram (default: nGram=1)");
        OWParser.addOption("nCharGram",             0, "N-character-gram (default: nCharGram=0)\n");
        
        /* Bag-of-features options */
        OWParser.addOption("a", "1", "When creating the bag-of-words, assign each input feature vector to p closest words from the codebook. (default: a=1, only closest word)\n"
                                   + "In case of several codebooks (see -attributes), a different number can be specified for each codebook using separator comma, e.g., -a 5,2\n"
                                   + "This parameter is stored in the codebook file (-B) and used when the respective codebook is loaded (-b).");
        OWParser.addOption("gaussian", "0.0", "Soft assignment using Gaussian encoding with standard deviation (stddev) p.\n"
                                            + "In case of several codebooks (see -attributes), a different stddev can be specified for each codebook using separator comma, e.g., -a 25.0,30.0\n"
                                            + "This parameter is stored in the codebook file (-B) and used when the respective codebook is loaded (-b).");
        OWParser.addOption("off", "0.0", "Off codebook words: Features with an Euclidean distance above threshold p to codewords are not be considered in the assignment step.\n"
                                       + "In case of several codebooks (see -attributes), a different stddev can be specified for each codebook using separator comma, e.g., -off 25.0,30.0\n"
                                       + "This parameter is stored in the codebook file (-B) and used when the respective codebook is loaded (-b).\n");
        
        /* Weighting options */
        OWParser.addOption("log", "Logarithmic term weighting 'lg(TF+1)' of the term frequency.\n"
                                + "This parameter is stored in the codebook file (-B) and used when the respective codebook is loaded (-b).");
        OWParser.addOption("idf", "Inverse document frequency transform: Multiply the term frequency (TF) with the logarithm of the ratio of the \n"
                                + "total number of instances and the number of instances where the respective word is present.\n"
                                + "This parameter is stored in the codebook file (-B) and used when the respective codebook is loaded (-b).");
        OWParser.addOption("norm", 0, "Normalize the bag-of-words (3 options of normalization).\n"
                                    + "p=1: Divides the term frequencies (TF) by the number of input frames.\n"
                                    + "p=2: Divides the TF by the sum of all TFs.\n"
                                    + "p=3: Divides the TF by a factor so that the resulting Euclidean length is 1.\n");
        
        /* Postprocessing options */
        OWParser.addOption("standardizeOutput", "Standardize all output features (term frequencies).\n"
                                              + "The parameters are stored in the codebook file (-B) and then used for standardization of test data (-b) in an online approach.");
        OWParser.addOption("normalizeOutput", "Normalize all output features (term frequencies, min->max is normalized to 0->1).\n"
                                            + "The parameters are stored in the codebook file (-B) and then used for standardization of test data (-b) in an online approach.");
    }
}
