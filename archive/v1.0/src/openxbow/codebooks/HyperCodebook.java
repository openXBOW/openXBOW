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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import openxbow.main.DataManager;
import openxbow.main.Options;


public class HyperCodebook {
    private DataManager DM      = null;
    private Options     options = null;
    
    private Map<Codebook,Integer> indexBook = null;  /* Feature index for each codebook */
    private List<Codebook>        codebooks = null;
    
    /* Preprocessing */
    private float[] fMeans        = null;
    private float[] fStandardDevs = null;
    private float[] fMIN          = null;
    private float[] fWIDTH        = null;
    
    /* Term frequency weighting parameters */
    private float[] dff = null;
    
    /* Postprocessing */
    private float[] fMeansOutput        = null;
    private float[] fStandardDevsOutput = null;
    private float[] fMINOutput          = null;
    private float[] fWIDTHOutput        = null;
    
    
    public HyperCodebook(DataManager DM, Options options) {
        this.DM      = DM;
        this.options = options;
        
        indexBook = new HashMap<Codebook,Integer>();
        codebooks = new ArrayList<Codebook>();
        
        for (Entry<Integer,List<Integer>> e : DM.reader.getIndexesAttributeClass().entrySet()) {
            if (e.getKey()==0) {  /* At the moment, the index in Attributes (0 OR 1-9) decides whether a feature is textual or numeric */
                codebooks.add(new CodebookText(options.cbConfig.get(e.getKey())));
            } else {
                codebooks.add(new CodebookNumeric(options.cbConfig.get(e.getKey())));
            }
            indexBook.put(codebooks.get(codebooks.size()-1), e.getKey());  /* Later on, we must know to which feature class the codebooks correspond to */
        }
    }
    
    
    public void generateCodebook() {
        for (Codebook book : codebooks) {
            if (book instanceof CodebookText) {
                ((CodebookText) book).generateCodebook(DM.reader);
            } else {
                CodebookNumericTrainingSelector train = new CodebookNumericTrainingSelector(DM, book.config, indexBook.get(book));
                ((CodebookNumeric) book).generateCodebook(train);
            }
        }
    }
    
    
    public boolean loadHyperCodebook(String fileName) {
        BufferedReader br = null;
        
        String   thisLine         = null;
        String[] content          = null;
        
        int[]   numAssignments   = new int [10];    // TODO: Should be more generic
        float[] gaussianEncoding = new float [10];  // TODO: Should be more generic
        float[] offCodewords     = new float [10];  // TODO: Should be more generic
        
        try {
            File inputFile = new File(fileName);
                 br        = new BufferedReader(new FileReader(inputFile));
            
            int featureClass = 1;
            
            /* Check for preprocessing instructions */
            while ((thisLine = br.readLine()) != null) {
                content = thisLine.split(";");
                
                if (content[0].equals("removeLowEnergy")) {
                    options.bRemoveLowEnergy = true;
                    options.energyIndex      = Integer.parseInt(content[1]);
                    options.energyThreshold  = Float.parseFloat(content[2]); 
                }
                else if (content[0].equals("standardizeInput")) {
                    options.bStandardizeInput = true;
                    fMeans        = parseFloatLine(br.readLine().split(";"));
                    fStandardDevs = parseFloatLine(br.readLine().split(";"));
                }
                else if (content[0].equals("normalizeInput")) {
                    options.bNormalizeInput = true;
                    fMIN       = parseFloatLine(br.readLine().split(";"));
                    fWIDTH     = parseFloatLine(br.readLine().split(";"));
                }
                else if (content[0].equals("log")) {
                    options.bLogWeighting = true;
                }
                else if (content[0].equals("idf")) {
                    options.bIDFWeighting = true;
                    dff = parseFloatLine(br.readLine().split(";"));
                }
                else if (content[0].equals("standardizeOutput")) {
                    options.bStandardizeOutput = true;
                    fMeansOutput        = parseFloatLine(br.readLine().split(";"));
                    fStandardDevsOutput = parseFloatLine(br.readLine().split(";"));
                }
                else if (content[0].equals("normalizeOutput")) {
                    options.bNormalizeOutput = true;
                    fMINOutput   = parseFloatLine(br.readLine().split(";"));
                    fWIDTHOutput = parseFloatLine(br.readLine().split(";"));                    
                }
                else if (content[0].equals("codebookText")) {
                    String stopWords = br.readLine();
                    int    nGram     = Integer.parseInt(br.readLine());
                    int    nCharGram = Integer.parseInt(br.readLine());
                    
                    int numCodewords = Integer.parseInt(br.readLine());
                    
                    String[] codewords = new String[numCodewords];
                    for (int w=0; w < numCodewords; w++) {
                        codewords[w] = br.readLine();
                    }
                    
                    ((CodebookText)codebooks.get(0)).setCodebook(codewords, stopWords, nGram, nCharGram);
                }
                else if (content[0].equals("codebookNumeric") || content[0].equals("codebookNumericNGram")) {
                    boolean bNumGrams = false;
                    if (content[0].equals("codebookNumericNGram")) {
                        bNumGrams = true;
                    }
                    
                    /* Parse the given header line and codewords */
                    content = br.readLine().split(";");
                    int numCodewords               = Integer.parseInt(content[0]);
                    int numFeatures                = Integer.parseInt(content[1]);
                    numAssignments[featureClass]   = Integer.parseInt(content[2]);
                    gaussianEncoding[featureClass] = Float.parseFloat(content[3]);
                    offCodewords[featureClass]     = Float.parseFloat(content[4]);
                    
                    float[][] codewords = new float[numCodewords][numFeatures];
                    for (int w=0; w < numCodewords; w++) {
                        content = br.readLine().split(";");
                        for (int k=0; k < numFeatures; k++) {
                            codewords[w][k] = Float.parseFloat(content[k]);
                        }
                    }
                    
                    /* Numeric n-grams */
                    int[][] unigrams = null;
                    int[][] bigrams  = null;
                    int[][] trigrams = null;
                    if (bNumGrams) {
                        List<int[][]> listGrams = readUniBiTrigrams(br);
                        unigrams = listGrams.get(0);
                        bigrams  = listGrams.get(1);
                        trigrams = listGrams.get(2);
                        if (unigrams != null) {
                            options.bUnigram[featureClass] = true;
                        }
                        if (bigrams != null) {
                            options.bBigram[featureClass] = true;
                        }
                        if (trigrams != null) {
                            options.bTrigram[featureClass] = true;
                        }
                    }
                    
                    /* Determine the correct codebook */
                    CodebookNumeric thisBook = null;
                    for (Codebook book : codebooks) {
                        if (indexBook.get(book)==featureClass) {
                            thisBook = (CodebookNumeric) book;
                        }
                    }
                    featureClass++;
                    thisBook.setCodebook(codewords);
                    
                    if (bNumGrams) {
                        thisBook.setNGramCodebooks(unigrams, bigrams, trigrams);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error: Codebook file " + fileName + " cannot be read!");
            e.printStackTrace();
            return false;
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        
        options.numAssignments   = numAssignments;
        options.gaussianEncoding = gaussianEncoding;
        options.offCodewords     = offCodewords;
        
        return true;
    }
    
    
    private float[] parseFloatLine(String[] line) {
        float[] A = new float[line.length];
        for (int k=0; k < line.length; k++) {
            A[k] = Float.parseFloat(line[k]);
        }
        return A;
    }
    
    
    public boolean saveHyperCodebook(String fileName) {
        try {
            File outputFile = new File(fileName);
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            FileWriter     fw = new FileWriter(outputFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            
            if (options.bRemoveLowEnergy) {
                bw.write("removeLowEnergy;" + String.valueOf(options.energyIndex) + ";" + String.valueOf(options.energyThreshold));
                bw.newLine();
            }
            if (options.bStandardizeInput) {
                bw.write("standardizeInput");
                bw.newLine();
                for (int k=0; k < fMeans.length; k++) {
                    bw.write(String.valueOf(fMeans[k]));
                    if (k < fMeans.length-1) { bw.write(";"); }
                }
                bw.newLine();
                for (int k=0; k < fStandardDevs.length; k++) {
                    bw.write(String.valueOf(fStandardDevs[k]));
                    if (k < fStandardDevs.length-1) { bw.write(";"); }
                }
                bw.newLine();
            }
            if (options.bNormalizeInput) {
                bw.write("normalizeInput");
                bw.newLine();
                for (int k=0; k < fMIN.length; k++) {
                    bw.write(String.valueOf(fMIN[k]));
                    if (k < fMIN.length-1) { bw.write(";"); }
                }
                bw.newLine();
                for (int k=0; k < fWIDTH.length; k++) {
                    bw.write(String.valueOf(fWIDTH[k]));
                    if (k < fWIDTH.length-1) { bw.write(";"); }
                }
                bw.newLine();
            }
            if (options.bLogWeighting) {
                bw.write("log");
                bw.newLine();
            }
            if (options.bIDFWeighting) {
                bw.write("idf");
                bw.newLine();
                for (int k=0; k < dff.length; k++) {
                    bw.write(String.valueOf(dff[k]));
                    if (k < dff.length-1) { bw.write(";"); }
                }
                bw.newLine();
            }
            if (options.bStandardizeOutput) {
                bw.write("standardizeOutput");
                bw.newLine();
                for (int k=0; k < fMeansOutput.length; k++) {
                    bw.write(String.valueOf(fMeansOutput[k]));
                    if (k < fMeansOutput.length-1) { bw.write(";"); }
                }
                bw.newLine();
                for (int k=0; k < fStandardDevsOutput.length; k++) {
                    bw.write(String.valueOf(fStandardDevsOutput[k]));
                    if (k < fStandardDevsOutput.length-1) { bw.write(";"); }
                }
                bw.newLine();
            }
            if (options.bNormalizeOutput) {
                bw.write("normalizeOutput");
                bw.newLine();
                for (int k=0; k < fMINOutput.length; k++) {
                    bw.write(String.valueOf(fMINOutput[k]));
                    if (k < fMINOutput.length-1) { bw.write(";"); }
                }
                bw.newLine();
                for (int k=0; k < fWIDTHOutput.length; k++) {
                    bw.write(String.valueOf(fWIDTHOutput[k]));
                    if (k < fWIDTHOutput.length-1) { bw.write(";"); }
                }
                bw.newLine();
            }
            
            /* Codebooks */
            for (Codebook codeBook : codebooks) {
                if (codeBook instanceof CodebookText) {
                    CodebookText book = (CodebookText) codeBook;
                    String[] codewords = book.getCodebook();
                    
                    bw.write("codebookText"); bw.newLine();
                    bw.write(book.getStopCharacters()); bw.newLine();
                    bw.write(String.valueOf(book.getNGram())); bw.newLine();
                    bw.write(String.valueOf(book.getNCharGram())); bw.newLine();
                    
                    int numCodewords = codewords.length;
                    bw.write(String.valueOf(numCodewords)); bw.newLine();
                    for (int w=0; w < numCodewords; w++) {
                        bw.write(codewords[w]); bw.newLine();
                    }
                } else {
                    CodebookNumeric book = (CodebookNumeric) codeBook;
                    float[][] codewords = book.getCodebook();
                    
                    int numCodewords = codewords.length;
                    int numFeatures  = codewords[0].length;
                    
                    if (options.bUnigram[this.getIndexBook(book)] 
                     || options.bBigram[this.getIndexBook(book)] 
                     || options.bTrigram[this.getIndexBook(book)]) 
                    {
                        bw.write("codebookNumericNGram"); bw.newLine();
                    } else {
                        bw.write("codebookNumeric"); bw.newLine();
                    }
                    bw.write(String.valueOf(numCodewords) + ";" 
                            + String.valueOf(numFeatures) + ";" 
                            + String.valueOf(options.numAssignments[this.getIndexBook(book)]) + ";" 
                            + String.valueOf(options.gaussianEncoding[this.getIndexBook(book)]) + ";"
                            + String.valueOf(options.offCodewords[this.getIndexBook(book)]));
                    bw.newLine();
                    
                    /* Write codewords */
                    for (int w=0; w < numCodewords; w++) {
                        for (int k=0; k < numFeatures; k++) {
                            bw.write(String.valueOf(codewords[w][k]));
                            if (k < numFeatures-1) { bw.write(";"); }
                        } bw.newLine();
                    }
                    
                    /* NumericNGrams */
                    if (options.bUnigram[this.getIndexBook(book)] 
                     || options.bBigram[this.getIndexBook(book)] 
                     || options.bTrigram[this.getIndexBook(book)]) 
                    {
                        String strGrams = getNGramHeaderString(options.bUnigram[this.getIndexBook(book)],
                                                               options.bBigram[this.getIndexBook(book)],
                                                               options.bTrigram[this.getIndexBook(book)]);
                        bw.write(String.valueOf(strGrams));
                        bw.newLine();
                    }
                    
                    if (options.bUnigram[this.getIndexBook(book)])
                    {
                        int[][] unigrams = book.getUnigrams();
                        numCodewords = unigrams.length; 
                        int numGrams = unigrams[0].length;
                        bw.write("num;" + String.valueOf(numCodewords)); bw.newLine();
                        for (int w=0; w < numCodewords; w++) {
                            for (int k=0; k < numGrams; k++) {
                                bw.write(String.valueOf(unigrams[w][k]));
                                if (k < numGrams-1) { bw.write(";"); }
                            } bw.newLine();
                        }
                    }
                    if (options.bBigram[this.getIndexBook(book)])
                    {
                        int[][] bigrams = book.getBigrams();
                        numCodewords = bigrams.length;
                        int numGrams = bigrams[0].length;
                        bw.write("num;" + String.valueOf(numCodewords)); bw.newLine();
                        for (int w=0; w < numCodewords; w++) {
                            for (int k=0; k < numGrams; k++) {
                                bw.write(String.valueOf(bigrams[w][k]));
                                if (k < numGrams-1) { bw.write(";"); }
                            } bw.newLine();
                        }
                    }
                    if (options.bTrigram[this.getIndexBook(book)])
                    {
                        int[][] trigrams = book.getTrigrams();
                        numCodewords = trigrams.length;
                        int numGrams = trigrams[0].length;
                        bw.write("num;" + String.valueOf(numCodewords)); bw.newLine();
                        for (int w=0; w < numCodewords; w++) {
                            for (int k=0; k < numGrams; k++) {
                                bw.write(String.valueOf(trigrams[w][k]));
                                if (k < numGrams-1) { bw.write(";"); }
                            } bw.newLine();
                        }
                    }
                }
            }
            
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return true;
    }
    
    
    public List<Codebook> getCodebooks() {
        return codebooks;
    }
    public int getIndexBook(Codebook book) {
        return indexBook.get(book);
    }
    
    /* Low-energy removal */
    public boolean isLowEnergyRemoved() {
        return options.bRemoveLowEnergy;
    }
    public int getEnergyIndex() {
        return options.energyIndex;
    }
    public float getEnergyThreshold() {
        return options.energyThreshold;
    }
    
    public void setRemoveLowEnergy() {
        this.options.bRemoveLowEnergy = true;
    }
    public void setEnergyIndex(int index) {
        this.options.energyIndex = index;
    }
    public void setEnergyThreshold(float threshold) {
        this.options.energyThreshold = threshold;
    }
    
    
    /* Input features */
    public boolean isStandardized() {
        return options.bStandardizeInput;
    }
    public float[] getMeans() {
        return fMeans;
    }
    public float[] getStandardDevs() {
        return fStandardDevs;
    }
    public void setStandardize() {
        this.options.bStandardizeInput = true;
    }
    public void setMeans(float[] means) {
        this.fMeans = means;
    }
    public void setStandardDevs(float[] standardDevs) {
        this.fStandardDevs = standardDevs;
    }
    
    public boolean isNormalized() {
        return options.bNormalizeInput;
    }
    public float[] getMIN() {
        return fMIN;
    }
    public float[] getWIDTH() {
        return fWIDTH;
    }
    public void setNormalize() {
        this.options.bNormalizeInput = true;
    }
    public void setMIN(float[] MIN) {
        this.fMIN = MIN;
    }
    public void setWIDTH (float[] WIDTH) {
        this.fWIDTH = WIDTH;
    }
    
    
    /* Standardization / Normalization of the term frequencies */
    public boolean isOutputStandardized() {
        return options.bStandardizeOutput;
    }
    public float[] getMeansOutput() {
        return fMeansOutput;
    }
    public float[] getStandardDevsOutput() {
        return fStandardDevsOutput;
    }
    public void setStandardizeOutput() {
        this.options.bStandardizeOutput = true;
    }
    public void setMeansOutput(float[] means) {
        this.fMeansOutput = means;
    }
    public void setStandardDevsOutput(float[] standardDevs) {
        this.fStandardDevsOutput = standardDevs;
    }
    
    public boolean isOutputNormalized() {
        return options.bNormalizeOutput;
    }
    public float[] getMINOutput() {
        return fMINOutput;
    }
    public float[] getWIDTHOutput() {
        return fWIDTHOutput;
    }
    public void setNormalizeOutput() {
        this.options.bNormalizeOutput = true;
    }
    public void setMINOutput(float[] MIN) {
        this.fMINOutput = MIN;
    }
    public void setWIDTHOutput (float[] WIDTH) {
        this.fWIDTHOutput = WIDTH;
    }
    
    
    /* Term weighting */
    public boolean getIDFWeighting() {
        return options.bIDFWeighting;
    }
    public float[] getDocumentFrequencyFactors() {
        return dff;
    }
    
    public void setIDFWeighting() {
        this.options.bIDFWeighting = true;
    }
    public void setDocumentFrequencyFactors(float[] dff) {
        this.dff = dff;
    }

    public boolean getLogWeighting() {
        return options.bLogWeighting;
    }
    public void setLogWeighting() {
        this.options.bLogWeighting = true;
    }
    
    
    private List<int[][]> readUniBiTrigrams(BufferedReader br) {
        List<int[][]> listGrams = new ArrayList<int[][]>();
        
        try {
            int[][] unigrams = null;
            int[][] bigrams  = null;
            int[][] trigrams = null;
            
            String[] content = br.readLine().split(";");  // Format uni;bi;tri (0/1)
            boolean bUni = false;
            boolean bBi  = false;
            boolean bTri = false;
            if (Integer.parseInt(content[0]) == 1) {
                bUni = true;
            }
            if (Integer.parseInt(content[1]) == 1) {
                bBi = true;
            }
            if (Integer.parseInt(content[2]) == 1) {
                bTri = true;
            }
            
            if (bUni) {
                content          = br.readLine().split(";");
                int numCodewords = Integer.parseInt(content[1]);
                int numGrams     = 1;
                unigrams = new int[numCodewords][numGrams];
                for (int w=0; w < numCodewords; w++) {
                    content = br.readLine().split(";");
                    unigrams[w][0] = Integer.parseInt(content[0]);
                }
            }
            
            if (bBi) {
                content          = br.readLine().split(";");
                int numCodewords = Integer.parseInt(content[1]);
                int numGrams     = 2;
                bigrams = new int[numCodewords][numGrams];
                for (int w=0; w < numCodewords; w++) {
                    content = br.readLine().split(";");
                    bigrams[w][0] = Integer.parseInt(content[0]);
                    bigrams[w][1] = Integer.parseInt(content[1]);
                }
            }
            
            if (bTri) {
                content          = br.readLine().split(";");
                int numCodewords = Integer.parseInt(content[1]);
                int numGrams     = 3;
                trigrams = new int[numCodewords][numGrams];
                for (int w=0; w < numCodewords; w++) {
                    content = br.readLine().split(";");
                    trigrams[w][0] = Integer.parseInt(content[0]);
                    trigrams[w][1] = Integer.parseInt(content[1]);
                    trigrams[w][2] = Integer.parseInt(content[2]);
                }
            }
            
            listGrams.add(unigrams);
            listGrams.add(bigrams);
            listGrams.add(trigrams);
        } catch (IOException e) {
            System.err.println("Error in reading numeric n gram codebooks!");
            e.printStackTrace();
        }
        
        return listGrams;
    }
    
    
    private String getNGramHeaderString(boolean bUni, boolean bBi, boolean bTri) {
        String str = "";
        if (bUni) {
            str = str.concat("1;");
        } else {
            str = str.concat("0;");
        }
        
        if (bBi) {
            str = str.concat("1;");
        } else {
            str = str.concat("0;");
        }
        
        if (bTri) {
            str = str.concat("1");
        } else {
            str = str.concat("0");
        }
        
        return str;
    }
}
