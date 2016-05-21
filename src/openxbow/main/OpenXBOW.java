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

package openxbow.main;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import openxbow.clparser.CLParser;
import openxbow.codebooks.HyperCodebook;
import openxbow.io.Reader;
import openxbow.io.Writer;

public class OpenXBOW {
    
    public static void main(String[] args) {
        /* Objects */
        Reader        reader    = null;
        Writer        writer    = null;
        HyperCodebook hyperBook = null;
        HyperBag      hyperBag  = null;
        
        /* Parse command-line */
        CLParser OWParser = new CLParser();
        addAllOptions(OWParser);
        OWParser.parseCL(args);
        
        /* Filenames */
        String  inputFileName    = OWParser.getOption("i").getParamList().get(0).toString();
        String  attributes       = OWParser.getOption("attributes").getParamList().get(0).toString();
        String  labelsFileName   = OWParser.getOption("l").getParamList().get(0).toString();
        String  outputFileName   = OWParser.getOption("o").getParamList().get(0).toString();
        boolean bWriteName       = OWParser.getOption("writeName").isPresent();
        boolean bWriteTimeStamp  = OWParser.getOption("writeTimeStamp").isPresent();
        String  loadCodebookName = OWParser.getOption("b").getParamList().get(0).toString();
        String  saveCodebookName = OWParser.getOption("B").getParamList().get(0).toString();
        
        /* Segmentation */
        boolean bTimeStamp =         OWParser.getOption("t").isPresent();
        float   windowSize = (float) OWParser.getOption("t").getParamList().get(0);
        float   hopSize    = (float) OWParser.getOption("t").getParamList().get(1);
        
        /* Preprocessing */
        boolean bRemoveLowEnergy  =         OWParser.getOption("e").isPresent();
        int     energyIndex       = (int)   OWParser.getOption("e").getParamList().get(0);
        float   energyThreshold   = (float) OWParser.getOption("e").getParamList().get(1);
        boolean bStandardizeInput =         OWParser.getOption("standardizeInput").isPresent();
        boolean bNormalizeInput   =         OWParser.getOption("normalizeInput").isPresent();
        
        /* Codebook */
        int     sizeCodebook     = (int) OWParser.getOption("size").getParamList().get(0);     /* Number of codes */
        String  generationMethod =       OWParser.getOption("c").getParamList().get(0).toString();   /* k-means, random */
        boolean bSupervised      =       OWParser.getOption("supervised").isPresent();               /* Create clusters per class first, then merge them */
        int     numTraining      = (int) OWParser.getOption("numTrain").getParamList().get(0); /* Number of features for clustering (0: all input vectors) */
        int     splitVQ          = (int) OWParser.getOption("svq").getParamList().get(0);      /* Number of sub-vectors (=0 if no split VQ) */
        int     sizeSubCodebook  = (int) OWParser.getOption("svq").getParamList().get(1);
        
        /* Bag-of-Features */
        int     numAssignments    = (int) OWParser.getOption("a").getParamList().get(0);
        boolean bGaussianEncoding = OWParser.getOption("gaussian").isPresent();
        float   gaussianStdDev    = (float) OWParser.getOption("gaussian").getParamList().get(0);
        
        /* Bag-of-Words */
        int    minTermFreq    = (int) OWParser.getOption("minTermFreq").getParamList().get(0);
        int    maxTermFreq    = (int) OWParser.getOption("maxTermFreq").getParamList().get(0);
        String stopCharacters =       OWParser.getOption("stopChar").getParamList().get(0).toString();
        int    nGram          = (int) OWParser.getOption("nGram").getParamList().get(0);
        int    nCharGram      = (int) OWParser.getOption("nCharGram").getParamList().get(0);
        
        /* Weighting options */
        boolean bLogWeighting = OWParser.getOption("log").isPresent();
        boolean bIDFWeighting = OWParser.getOption("idf").isPresent();
        int     normalizeBag  = (int) OWParser.getOption("norm").getParamList().get(0);
        
        
        /* Check options */
        if (bRemoveLowEnergy && energyIndex < 0) {
            System.err.println("Error: Energy index not valid!");
        }
        if (!loadCodebookName.isEmpty()) {
            if (bRemoveLowEnergy) {
                System.err.println("Warning: Removing low-energy features is not selectable if codebook is provided!");
                bRemoveLowEnergy = false;
            }
            if (bStandardizeInput) {
                System.err.println("Warning: Standardization is not selectable if codebook is provided!");
                bStandardizeInput = false;
            }
            if (bNormalizeInput) {
                System.err.println("Warning: Normalization is not relevant if codebook is loaded.");
                bNormalizeInput = false;
            }
            if (bLogWeighting || bIDFWeighting) {
                System.err.println("Warning: Term weighting may not be given if codebook is provided!");
                bLogWeighting = false;
                bIDFWeighting = false;
            }
        }
        
        /* Print help */
        if (args.length==0 || OWParser.getOption("h").isPresent()) {
            printHelp(OWParser);
        }
        
        
        /* Read input file */
        boolean bReadSuccess = false;
        if (!inputFileName.isEmpty()) {
            System.out.println("Parsing input ...");
            reader = new Reader(inputFileName, attributes, bTimeStamp);
            bReadSuccess = reader.readFile();
        }
        
        
        if (reader!=null && bReadSuccess) {
            DataManager DM = new DataManager(reader,windowSize,hopSize);
            
            DM.generateMappings();
            
            if (!labelsFileName.isEmpty()) {
                DM.readLabelsFile(labelsFileName);
            }
            
            /* Create (hyper) codebook and load, if given*/
            hyperBook = new HyperCodebook(DM, splitVQ, bLogWeighting, bIDFWeighting, stopCharacters, nGram, nCharGram);
            if (!loadCodebookName.isEmpty()) {
                hyperBook.loadHyperCodebook(loadCodebookName);
            }
            
            /* Preprocessing */
            Preprocessor preProc = new Preprocessor(DM, hyperBook, bRemoveLowEnergy, energyIndex, energyThreshold, bStandardizeInput, bNormalizeInput);
            preProc.preprocessInput();
            
            /* Create openxbow.codebooks */
            if (splitVQ==0) {
                /* Create new codebook */
                if (loadCodebookName.isEmpty()) {
                    System.out.println("Creating codebook ...");
                    hyperBook.generateCodebook(sizeCodebook, generationMethod, bSupervised, numTraining, minTermFreq, maxTermFreq);
                }
                hyperBag = new HyperBag(DM, hyperBook, stopCharacters, nGram, nCharGram);
            }
            else { /* SVQ: create subcodebooks and sub-bags */
                hyperBag = new HyperBag(DM, hyperBook, splitVQ);
                if (loadCodebookName.isEmpty()) {
                    System.out.println("Creating subcodebooks ...");
                    hyperBook.generateSubCodebooksSVQ(DM, sizeSubCodebook, generationMethod, bSupervised, numTraining);
                }
                System.out.println("Creating Bags-of-Features ...");
                hyperBag.generateSubBagsSVQ(false,0.0f);
                if (loadCodebookName.isEmpty()) {
                    System.out.println("Creating top-level codebook ...");
                    hyperBook.generateCodebookSVQ(hyperBag, sizeCodebook, generationMethod, bSupervised);
                }
            }
            
            /* Create the bag-of-features */
            System.out.println("Creating Bag-of-Features ...");
            hyperBag.generateBag(numAssignments, bGaussianEncoding, gaussianStdDev, normalizeBag);
            
            /* Save codebook (including term frequencies) */
            if (!saveCodebookName.isEmpty()) {
                hyperBook.saveHyperCodebook(saveCodebookName);
            }
            
            /* Write output BoF files */
            if (!outputFileName.isEmpty()) {
                System.out.println("Writing output ...");
                writer = new Writer(outputFileName, DM.reader.getRelation(), DM, bWriteName, bWriteTimeStamp);
                writer.writeFile(hyperBag);
            }
        }
    }
    
    
    private static void addAllOptions(CLParser OWParser) {
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
        OWParser.addOption("writeName", properties.getProperty("writeName"));
        OWParser.addOption("writeTimeStamp", properties.getProperty("writeTimeStamp"));
        OWParser.addOption("l", "", properties.getProperty("l"));
        
        /* Preprocessing options */
        OWParser.addOption("t", -1.0f, 0.0f, properties.getProperty("t"));
        OWParser.addOption("e", -1, 0.0f, properties.getProperty("e"));
        OWParser.addOption("standardizeInput", properties.getProperty("standardizeInput"));
        OWParser.addOption("normalizeInput", properties.getProperty("normalizeInput"));
        
        /* Codebook options */
        OWParser.addOption("size", 500, properties.getProperty("size"));
        OWParser.addOption("c", "random", properties.getProperty("c"));
        OWParser.addOption("supervised", properties.getProperty("supervised"));
        OWParser.addOption("numTrain", 0, properties.getProperty("numTrain"));
        OWParser.addOption("svq", 0, 10, properties.getProperty("svq"));
        
        OWParser.addOption("b", "", properties.getProperty("b"));
        OWParser.addOption("B", "", properties.getProperty("B"));
        
        /* Bag-of-features options */
        OWParser.addOption("a", 1, properties.getProperty("a"));
        OWParser.addOption("gaussian", 0.0f, properties.getProperty("gaussian"));
        
        /* Bag-of-words options */
        OWParser.addOption("minTermFreq",           1, properties.getProperty("minTermFreq"));
        OWParser.addOption("maxTermFreq",           0, properties.getProperty("maxTermFreq"));
        OWParser.addOption("stopChar",    ".,;:()?!*", properties.getProperty("stopChar"));
        OWParser.addOption("nGram",                 1, properties.getProperty("nGram"));
        OWParser.addOption("nCharGram",             0, properties.getProperty("nCharGram"));
        
        /* Weighting options */
        OWParser.addOption("log", properties.getProperty("log"));
        OWParser.addOption("idf", properties.getProperty("idf"));
        OWParser.addOption("norm", 0, properties.getProperty("norm"));
    }
    
    private static void printHelp(CLParser OWParser) {
        Properties properties = new Properties();
        try {
            BufferedInputStream stream = new BufferedInputStream(new FileInputStream("props/help.properties"));
            properties.load(stream);
            stream.close();            
        } catch(IOException e) {
            System.err.println("Error: Cannot read properties file help.properties!");
        }
        
        System.out.println(properties.getProperty("help"));
        System.out.println("");
        System.out.println("openXBOW options");
        System.out.println("");
        OWParser.printHelp();
        System.out.println("");
        System.out.println(properties.getProperty("example"));
    }
}

