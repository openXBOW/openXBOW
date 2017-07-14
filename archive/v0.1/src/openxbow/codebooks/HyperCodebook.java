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
import openxbow.main.HyperBag;
import openxbow.main.Options;

public class HyperCodebook {
    private DataManager DM      = null;
    private Options     options = null;
    
    private List<Codebook>        codebooks = null;
    private Map<Codebook,Integer> indexBook = null;  /* Feature index for each codebook, only if no SVQ is used */
    
    /* Preprocessing */
    private float[] fMeans        = null;
    private float[] fStandardDevs = null;
    private float[] fMIN          = null;
    private float[] fWIDTH        = null;
    
    /* Term frequency weighting parameters */
    private float[] dff           = null;
    
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
        
        if (options.bSVQ) {
            codebooks.add(new CodebookNumeric(options.cbConfig.get(0)));  /* Top-level codebook (index 0) */
            for (int s=1; s <= options.numSubCodebooksSVQ; s++) {
                codebooks.add(new CodebookNumeric(options.cbConfig.get(s)));  /* All features are numeric for SVQ */
            }
        }
        else {
            for (Entry<Integer,List<Integer>> e : DM.reader.getIndexesAttributeClass().entrySet()) {
                if (e.getKey()==0) {  /* So far, the index in Attributes (0 OR 1-9) decides whether a feature is textual or numeric */
                    codebooks.add(new CodebookText(options.cbConfig.get(e.getKey())));
                } else {
                    codebooks.add(new CodebookNumeric(options.cbConfig.get(e.getKey())));
                }
                indexBook.put(codebooks.get(codebooks.size()-1), e.getKey());  /* Later on, we must know to which feature class the codebooks correspond to */
            }
        }
    }
    
    
    public void generateCodebook() {
        /* This function may not be called in case of SVQ */
        for (Codebook book : codebooks) {
            if (book instanceof CodebookText) {
                ((CodebookText) book).generateCodebook(DM.reader);
            } else {
                CodebookNumericTrainingSelector train = new CodebookNumericTrainingSelector(DM, book.config, indexBook.get(book));
                ((CodebookNumeric) book).generateCodebook(train);
            }
        }
    }
    
    
    public void generateCodebookSVQ(HyperBag hyperBag) {
        if (options.bSVQ) {
            CodebookNumericTrainingSelector train = new CodebookNumericTrainingSelector(hyperBag, options.cbConfig.get(0));
            ((CodebookNumeric) codebooks.get(0)).generateCodebook(train);
        }
    }
    
    
    public void generateSubCodebooksSVQ(DataManager DM) {
        if (!options.bSVQ) {
            System.err.println("Error in generateSubCodebooksSVQ: Not applicable as SVQ is not chosen.");
            return;
        }
        
        int numFeaturesCodebook = 0;
        if (Math.floorMod(DM.reader.getNumFeatures(), options.numSubCodebooksSVQ) > 0) {
            numFeaturesCodebook = (DM.reader.getNumFeatures() / options.numSubCodebooksSVQ) + 1;
        } else {
            numFeaturesCodebook = DM.reader.getNumFeatures() / options.numSubCodebooksSVQ;
        }
        
        for (int s=1; s <= options.numSubCodebooksSVQ; s++) {
            List<Integer> listIndexes = new ArrayList<Integer>();
            int indexMin = (s-1)*numFeaturesCodebook;
            int indexMax = Math.min(s*numFeaturesCodebook-1, DM.reader.getNumFeatures()-1);
            for (int k=indexMin; k <= indexMax; k++) {
                listIndexes.add(DM.reader.getIndexesAttributeClass().get(1).get(k));  /* All features must have index 1 for SVQ */
            }
            
            CodebookNumericTrainingSelector train = new CodebookNumericTrainingSelector(DM, options.cbConfig.get(s),listIndexes);
            ((CodebookNumeric) codebooks.get(s)).generateCodebook(train);
        }
    }
    
    
    public boolean loadHyperCodebook(String fileName) {
        BufferedReader br = null;
        
        try {
            File     inputFile    = new File(fileName);
                     br           = new BufferedReader(new FileReader(inputFile));
            String   thisLine     = null;
            String[] content      = null;
            int      numFeatures  = 0;
            int      numCodewords = 0;
            
            int      iCodebook    = 0;
            
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
                else if (content[0].equals("codebookNumeric")) {
                    content = br.readLine().split(";");
                    
                    numCodewords = Integer.parseInt(content[0]);
                    numFeatures  = Integer.parseInt(content[1]);
                    
                    float[][] codewords = new float[numCodewords][numFeatures];
                    for (int w=0; w < numCodewords; w++) {
                        content = br.readLine().split(";");
                        for (int k=0; k < numFeatures; k++) {
                            codewords[w][k] = Float.parseFloat(content[k]);
                        }
                    }
                    
                    ((CodebookNumeric)codebooks.get(iCodebook)).setCodebook(codewords);
                    iCodebook++;
                }
                else if (content[0].equals("codebookText")) {
                    String stopWords = br.readLine();
                    int    nGram     = Integer.parseInt(br.readLine());
                    int    nCharGram = Integer.parseInt(br.readLine());
                    
                    numCodewords = Integer.parseInt(br.readLine());
                    
                    String[] codewords = new String[numCodewords];
                    for (int w=0; w < numCodewords; w++) {
                        codewords[w] = br.readLine();
                    }
                    
                    ((CodebookText)codebooks.get(iCodebook)).setCodebook(codewords, stopWords, nGram, nCharGram);
                    iCodebook++;
                }
                else if (content[0].equals("codebookSVQ")) {
                    options.bSVQ = true;
                    content = br.readLine().split(";");
                    
                    numCodewords = Integer.parseInt(content[0]);
                    numFeatures  = Integer.parseInt(content[1]);
                    
                    float[][] codewords = new float[numCodewords][numFeatures];
                    for (int w=0; w < numCodewords; w++) {
                        content = br.readLine().split(";");
                        for (int k=0; k < numFeatures; k++) {
                            codewords[w][k] = Float.parseFloat(content[k]);
                        }
                    }
                    
                    ((CodebookNumeric)codebooks.get(iCodebook)).setCodebook(codewords);
                    iCodebook++;
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
        int numCodewords = 0;
        int numFeatures  = 0;
        
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
            
            /* Numeric Codebook */
            for (Codebook codeBook : codebooks) {
                if (codeBook instanceof CodebookText) {
                    CodebookText book = (CodebookText) codeBook;
                    String[] codewords = book.getCodebook();
                    
                    bw.write("codebookText"); bw.newLine();
                    bw.write(book.getStopCharacters()); bw.newLine();
                    bw.write(String.valueOf(book.getNGram())); bw.newLine();
                    bw.write(String.valueOf(book.getNCharGram())); bw.newLine();
                    
                    numCodewords = codewords.length;
                    bw.write(String.valueOf(numCodewords)); bw.newLine();
                    for (int w=0; w < numCodewords; w++) {
                        bw.write(codewords[w]); bw.newLine();
                    }
                } else {
                    CodebookNumeric book = (CodebookNumeric) codeBook;
                    float[][] codewords = book.getCodebook();
                    
                    numCodewords = codewords.length;
                    numFeatures  = codewords[0].length;
                    
                    if (options.bSVQ && codeBook.equals(codebooks.get(0))) {
                        bw.write("codebookSVQ"); bw.newLine();
                    } else {
                        bw.write("codebookNumeric"); bw.newLine();
                    }
                    
                    /* Write codewords */
                    bw.write(String.valueOf(numCodewords) + ";" + String.valueOf(numFeatures)); bw.newLine();
                    
                    for (int w=0; w < numCodewords; w++) {
                        for (int k=0; k < numFeatures; k++) {
                            bw.write(String.valueOf(codewords[w][k]));
                            if (k < numFeatures-1) { bw.write(";"); }
                        } bw.newLine();
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
    
    
    public int getSize() {
        /* SVQ:    Returns the size of the top codebook  */
        /* No SVQ: Returns the sum of all codebook sizes */
        int size = 0;
        if (options.bSVQ) {
            size = ((CodebookNumeric) codebooks.get(0)).size();
        }
        else {
            for (Codebook book : codebooks) {
                size += book.size();
            }
        }
        return size;
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
}
