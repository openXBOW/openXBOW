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
import java.util.Properties;

import openxbow.clparser.CLParser;
import openxbow.codebooks.HyperCodebook;
import openxbow.io.Reader;
import openxbow.io.Writer;
import openxbow.io.WriterIndex;


public class OpenXBOW {
    
    public static void main(String[] args) {
        String VERSION = "0.1";
        
        /* Objects */
        Reader        reader    = null;
        Writer        writer    = null;
        WriterIndex   writerInd = null;
        HyperCodebook hyperBook = null;
        HyperBag      hyperBag  = null;
        
        /* Parse command-line */
        CLParser OWParser = new CLParser();
        Options  options  = new Options(OWParser);
        options.parseCL(args);
        
        /* Check options */
        options.checkCLoptions();
        
        /* Print help */
        if (args.length==0 || OWParser.getOption("h").isPresent()) {
            printHelp(OWParser,VERSION);
        }
        
        
        /* Read input file */
        boolean bReadSuccess = false;
        if (!options.inputFileName.isEmpty()) {
            System.out.println("Parsing input ...");
            reader = new Reader(options.inputFileName, options.attributes, options.bTimeStamp);
            bReadSuccess = reader.readFile();
        }
        
        
        if (reader!=null && bReadSuccess) {
            /* Initialize data manager */
            DataManager DM = new DataManager(reader,options.windowSize,options.hopSize);
            
            /* Create (hyper) codebook and load, if given*/
            hyperBook = new HyperCodebook(DM, options);
            if (!options.loadCodebookName.isEmpty()) {
                hyperBook.loadHyperCodebook(options.loadCodebookName);
            }
            
            /* Preprocessing */
            Preprocessor preProc = new Preprocessor(DM, hyperBook, options);
            preProc.preprocessInput();
            
            /* Generate the mappings (after activity detection) */
            System.out.println("Reorganizing input data ...");
            DM.generateMappings();
            
            if (!options.labelsFileName.isEmpty()) {
                DM.readLabelsFile(options.labelsFileName);
            }
            
            /* Create codebooks */
            if (!options.bSVQ) {
                /* Create new codebook */
                if (options.loadCodebookName.isEmpty()) {
                    System.out.println("Creating codebook ...");
                    hyperBook.generateCodebook();
                }
                hyperBag = new HyperBag(DM, hyperBook, options);
            }
            else { /* SVQ: create subcodebooks and sub-bags */
                if (options.loadCodebookName.isEmpty()) {
                    System.out.println("Creating subcodebooks ...");
                    hyperBook.generateSubCodebooksSVQ(DM);
                }
                System.out.println("Creating Bags-of-Features ...");
                hyperBag = new HyperBag(DM, hyperBook, options);
                hyperBag.generateSubBagsSVQ();
                if (options.loadCodebookName.isEmpty()) {
                    System.out.println("Creating top-level codebook ...");
                    hyperBook.generateCodebookSVQ(hyperBag);
                }
            }
            
            /* Create the bag-of-features */
            System.out.println("Creating Bag-of-Features ...");
            hyperBag.generateBag();
            
            /* Postprocessing */
            Postprocessor postProc = new Postprocessor(hyperBook, hyperBag, options);
            postProc.postprocessOutput();
            
            /* Save codebook (including information on IDF and output standardization/normalization, if chosen) */
            if (!options.saveCodebookName.isEmpty()) {
                hyperBook.saveHyperCodebook(options.saveCodebookName);
            }
            
            /* Write output BoF files */
            if (!options.outputFileName.isEmpty()) {
                System.out.println("Writing output ...");
                writer = new Writer(options.outputFileName, DM, options.bWriteName, options.bWriteTimeStamp, options.arffLabels, options.bAppend);
                writer.writeFile(hyperBag);
            }
            
            /* Write word index file */
            if (!options.outputIFileName.isEmpty()) {
                System.out.println("Writing indexes ...");
                writerInd = new WriterIndex(options.outputIFileName, DM);
                writerInd.writeFile(hyperBag);
            }
        }
    }
    
    
    private static void printHelp(CLParser OWParser, String VERSION) {
        Properties properties = new Properties();
        try {
            BufferedInputStream stream = new BufferedInputStream(new FileInputStream("props/help.properties"));
            properties.load(stream);
            stream.close();            
        } catch(IOException e) {
            System.err.println("Error: Cannot read properties file help.properties!");
        }
        
        System.out.println("openXBOW - version " + VERSION + " (published under GPL v3)");
        System.out.println("");
        System.out.println(properties.getProperty("help"));
        System.out.println("");
        System.out.println("openXBOW options");
        System.out.println("");
        OWParser.printHelp();
        System.out.println("");
        System.out.println(properties.getProperty("example"));
    }
}

