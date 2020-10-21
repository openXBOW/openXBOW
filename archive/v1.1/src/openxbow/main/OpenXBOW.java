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

package openxbow.main;

import openxbow.clparser.CLParser;
import openxbow.codebooks.HyperCodebook;
import openxbow.io.PredictSVM;
import openxbow.io.Reader;
import openxbow.io.Writer;
import openxbow.io.WriterIndex;


public class OpenXBOW {
    
    public static void main(String[] args) {
        String VERSION = "1.1";
        
        /* Objects */
        Reader        reader     = null;
        Writer        writer     = null;
        WriterIndex   writerInd  = null;
        PredictSVM    predictSVM = null;
        HyperCodebook hyperBook  = null;
        HyperBag      hyperBag   = null;
        
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
            
            /* Create codebook and load, if given */
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
            
            
            /* Generate new codebook */
            if (options.loadCodebookName.isEmpty()) {
                System.out.println("Creating codebook ...");
                hyperBook.generateCodebook();
            }
            
            
            /* Create the bag-of-word */
            System.out.println("Creating Bag-of-Words ...");
            hyperBag = new HyperBag(DM, hyperBook, options);
            hyperBag.generateBag();
            
            
            /* Postprocessing */
            Postprocessor postProc = new Postprocessor(hyperBook, hyperBag, options);
            postProc.postprocessOutput();
            
            
            /* Save codebook (including information on IDF and output standardization/normalization, if chosen) */
            if (!options.saveCodebookName.isEmpty()) {
                hyperBook.saveHyperCodebook(options.saveCodebookName);
            }
            
            
            /* Write output BoW files */
            if (!options.outputFileName.isEmpty()) {
                System.out.println("Writing output ...");
                writer = new Writer(options.outputFileName, DM, options.csvHeader, options.csvSep, options.bWriteName, options.bWriteTimeStamp, options.bNoWriteLabels, options.arffLabels, options.bAppend);
                writer.writeFile(hyperBag);
            }
            
            /* Write word index file */
            if (!options.outputIFileName.isEmpty()) {
                System.out.println("Writing indexes ...");
                writerInd = new WriterIndex(options.outputIFileName, DM);
                writerInd.writeFile(hyperBag);
            }
            
            /* Predict labels and write output */
            if (!options.modelFileName.isEmpty()) {
                System.out.println("Predicting labels and writing JSON output ...");
                predictSVM = new PredictSVM(options.modelFileName);
                predictSVM.predictLabelsAndWriteJSON(hyperBag, options.jsonFileName);
            }
        }
    }
    
    
    private static void printHelp(CLParser OWParser, String VERSION) {
        String strHelp = "OpenXBOW Generates an ARFF, CSV, or LibSVM file (separator: semicolon) from an ARFF or CSV file of\n"
                                     + "numeric low-level descriptors and/or text.\n\n"
                                     + "Input format:\n"
                                     + "The first feature must always be an identifier for the corresponding file / instance / analysis window,\n"
                                     + "i.e., string containing the filename or an index, e.g. 'corpus_001.wav'.\n"
                                     + "A header line in CSV files is mandatory if there are only text features and labels, otherwise it is optional.\n"
                                     + "The last feature may be a nominal or numeric class label. In this case, there must be a header line.\n"
                                     + "If the class labels are not given in the input data file, an additional CSV file with class labels can be given\n"
                                     + "(the first line can be a header line, the first column contains the identifier string for each instance,\n"
                                     + "the second column the corresponding class label.\n\n"
                                     + "Example for an input CSV file:\n"
                                     + "'corpus_0001.wav';1.04E+01;2.3E+00;2.7E-01;classA\n"
                                     + "'corpus_0001.wav';9.02E+00;7.0E+01;1.1E-01;classA\n"
                                     + "'corpus_0001.wav';5.19E+01;4.4E+00;2.7E-01;classA\n"
                                     + "'corpus_0002.wav';1.24E+00;1.3E+01;2.8E-01;classB\n"
                                     + "'corpus_0002.wav';2.51E+01;6.7E+00;3.1E-01;classB\n"
                                     + "'corpus_0002.wav';4.24E+01;2.2E+01;8.0E-02;classB\n"
                                     + "'corpus_0003.wav';1.23E+01;4.3E+00;1.6E-01;classA\n"
                                     + "...";
        
        String strExample = "Example:\n"
                          + "java -jar openXBOW.jar -i features.arff -o boaw.arff -l labels.csv -size 100";
        
        System.out.println("openXBOW - version " + VERSION + " (published under GPL v3)");
        System.out.println("");
        System.out.println(strHelp);
        System.out.println("");
        System.out.println("openXBOW options");
        System.out.println("");
        OWParser.printHelp();
        System.out.println("");
        System.out.println(strExample);
    }
}

