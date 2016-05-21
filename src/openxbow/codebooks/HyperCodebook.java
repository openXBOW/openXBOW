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

public class HyperCodebook {
    private DataManager     DM            = null;
    private int             numSubVectors = 0;
    private List<Codebook>  codebooks     = null;
    private CodebookNumeric codebookSVQ   = null;
    
    private Map<Codebook,Integer> indexBook = null;  /* Feature index for each codebook, only if no SVQ is used */
    
    /* Preprocessing parameters */
    private boolean bRemoveLowEnergy = false;
    private int     energyIndex      = -1;
    private float   energyThreshold  = 0.0f;
    
    private boolean bStandardize  = false;
    private float[] fMeans        = null;
    private float[] fStandardDevs = null;
    private boolean bNormalize    = false;
    private float[] fMIN          = null;
    private float[] fWIDTH        = null;
    
    /* Term weighting parameters (for codebook) */
    private boolean bLogWeighting = false;
    private boolean bIDFWeighting = false;
    private float[] dff           = null;
    
    
    public HyperCodebook(DataManager DM, boolean bLogWeighting, boolean bIDFWeighting) {
        this(DM, 0, bLogWeighting, bIDFWeighting, "", 0, 0);
    }
    public HyperCodebook(DataManager DM, boolean bLogWeighting, boolean bIDFWeighting, String stopCharacters, int nGram, int nCharGram) {
        this(DM, 0, bLogWeighting, bIDFWeighting, stopCharacters, nGram, nCharGram);
    }
    public HyperCodebook(DataManager DM, int numSubVectors, boolean bLogWeighting, boolean bIDFWeighting) {
        this(DM, numSubVectors, bLogWeighting, bIDFWeighting, "", 0, 0);
    }
    public HyperCodebook(DataManager DM, int numSubVectors, boolean bLogWeighting, boolean bIDFWeighting, String stopCharacters, int nGram, int nCharGram) {
        this.DM                  = DM;
        this.numSubVectors       = numSubVectors;
        this.bLogWeighting       = bLogWeighting;
        this.bIDFWeighting       = bIDFWeighting;
        
        indexBook = new HashMap<Codebook,Integer>();
        
        if (numSubVectors > 0) {
            codebookSVQ = new CodebookNumeric();
            codebooks   = new ArrayList<Codebook>();
            for (int s=0; s < numSubVectors; s++) {
                codebooks.add(new CodebookNumeric());  /* All features are numeric for SVQ */
            }
        }
        else {
            codebooks = new ArrayList<Codebook>();
            
            for (Entry<Integer,List<Integer>> e : DM.reader.getIndexesAttributeClass().entrySet()) {
                if (e.getKey()==0) {  /* So far, the index in Attributes (0 OR 1-9) decides whether a feature is textual or numeric */
                    codebooks.add(new CodebookText(stopCharacters, nGram, nCharGram));
                } else {
                    codebooks.add(new CodebookNumeric());
                }
                indexBook.put(codebooks.get(codebooks.size()-1), e.getKey());  /* Later on, we must know to which feature class the openxbow.codebooks correspond to */
            }
        }
    }
    
    
    public void generateCodebook(int     sizeCodebook, 
                                 String  generationMethod, 
                                 boolean bSupervised, 
                                 int     numTraining,
                                 int     minTermFreq, 
                                 int     maxTermFreq) 
    {
        /* This function may not be called in case of SVQ */
        for (Codebook book : codebooks) {
            if (book instanceof CodebookText) {
                ((CodebookText) book).generateCodebook(DM.reader, minTermFreq, maxTermFreq);
            } else {
                CodebookNumericTrainingSelector train = new CodebookNumericTrainingSelector(DM, bSupervised, indexBook.get(book), numTraining);
                ((CodebookNumeric) book).generateCodebook(train, generationMethod, sizeCodebook);
            }
        }
    }
    
    
    public void generateCodebookSVQ(HyperBag hyperBag,
                                    int      sizeCodebook,
                                    String   generationMethod,
                                    boolean  bSupervised)
    {
        if (numSubVectors > 0) { /* SVQ */
            CodebookNumericTrainingSelector train = new CodebookNumericTrainingSelector(hyperBag, bSupervised);
            codebookSVQ.generateCodebook(train, generationMethod, sizeCodebook);
        }
    }
    
    
    public void generateSubCodebooksSVQ(DataManager DM, int sizeSubCodebook, String generationMethod, boolean bSupervised, int numTraining) {
        if (numSubVectors == 0) {
            System.err.println("Error in generateSubCodebooks: Not applicable as SVQ is not chosen.");
            return;
        }
        
        int numFeaturesCodebook = 0;
        if (Math.floorMod(DM.reader.getNumFeatures(), numSubVectors) > 0) {
            numFeaturesCodebook = (DM.reader.getNumFeatures() / numSubVectors) + 1;
        } else {
            numFeaturesCodebook = DM.reader.getNumFeatures() / numSubVectors;
        }
        
        for (int s=0; s < numSubVectors; s++) {
            List<Integer> listIndexes = new ArrayList<Integer>();
            int indexMin = s*numFeaturesCodebook;
            int indexMax = Math.min((s+1)*numFeaturesCodebook-1, DM.reader.getNumFeatures()-1);
            for (int k=indexMin; k <= indexMax; k++) {
                listIndexes.add(DM.reader.getIndexesAttributeClass().get(1).get(k));  /* All features must have index 1 for SVQ */
            }
            
            CodebookNumericTrainingSelector train = new CodebookNumericTrainingSelector(DM, bSupervised, listIndexes, numTraining);
            ((CodebookNumeric) codebooks.get(s)).generateCodebook(train, generationMethod, sizeSubCodebook);
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
                    bRemoveLowEnergy = true;
                    energyIndex      = Integer.parseInt(content[1]);
                    energyThreshold  = Float.parseFloat(content[2]); 
                }
                else if (content[0].equals("standardization")) {
                    bStandardize  = true;
                    fMeans        = parseFloatLine(br.readLine().split(";"));
                    fStandardDevs = parseFloatLine(br.readLine().split(";"));
                }
                else if (content[0].equals("normalization")) {
                    bNormalize = true;
                    fMIN       = parseFloatLine(br.readLine().split(";"));
                    fWIDTH     = parseFloatLine(br.readLine().split(";"));
                }
                else if (content[0].equals("log")) {
                    bLogWeighting = true;
                }
                else if (content[0].equals("idf")) {
                    bIDFWeighting = true;
                    dff = parseFloatLine(br.readLine().split(";"));
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
                    
                    codebookSVQ.setCodebook(codewords);
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
            
            /* Preprocessing options */
            if (bRemoveLowEnergy) {
                bw.write("removeLowEnergy;" + String.valueOf(energyIndex) + ";" + String.valueOf(energyThreshold));
                bw.newLine();
            }
            if (bStandardize) {
                bw.write("standardization");
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
            if (bNormalize) {
                bw.write("normalization");
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
            if (bLogWeighting) {
                bw.write("log");
                bw.newLine();
            }
            if (bIDFWeighting) {
                bw.write("idf");
                bw.newLine();
                for (int k=0; k < dff.length; k++) {
                    bw.write(String.valueOf(dff[k]));
                    if (k < dff.length-1) { bw.write(";"); }
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
                    
                    bw.write("codebookNumeric"); bw.newLine();
                    
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
            
            if (codebookSVQ != null) {
                CodebookNumeric book = codebookSVQ;
                float[][] codewords = book.getCodebook();
                
                numCodewords = codewords.length;
                numFeatures  = codewords[0].length;
                
                bw.write("codebookSVQ"); bw.newLine();
                
                /* Write codewords */
                bw.write(String.valueOf(numCodewords) + ";" + String.valueOf(numFeatures)); bw.newLine();
                
                for (int w=0; w < numCodewords; w++) {
                    for (int k=0; k < numFeatures; k++) {
                        bw.write(String.valueOf(codewords[w][k]));
                        if (k < numFeatures-1) { bw.write(";"); }
                    } bw.newLine();
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
    public Codebook getCodebookSVQ() {
        return codebookSVQ;
    }
    public int getIndexBook(Codebook book) {
        return indexBook.get(book);
    }
    
    /* Low-energy removal */
    public boolean isLowEnergyRemoved() {
        return bRemoveLowEnergy;
    }
    public int getEnergyIndex() {
        return energyIndex;
    }
    public float getEnergyThreshold() {
        return energyThreshold;
    }
    
    public void setRemoveLowEnergy() {
        this.bRemoveLowEnergy = true;
    }
    public void setEnergyIndex(int index) {
        this.energyIndex = index;
    }
    public void setEnergyThreshold(float threshold) {
        this.energyThreshold = threshold;
    }
    
    
    /* Standardization */
    public boolean isStandardized() {
        return bStandardize;
    }
    public float[] getMeans() {
        return fMeans;
    }
    public float[] getStandardDevs() {
        return fStandardDevs;
    }
    public void setStandardize() {
        this.bStandardize = true;
    }
    public void setMeans(float[] means) {
        this.fMeans = means;
    }
    public void setStandardDevs(float[] standardDevs) {
        this.fStandardDevs = standardDevs;
    }
    
    /* Normalization */
    public boolean isNormalized() {
        return bNormalize;
    }
    public float[] getMIN() {
        return fMIN;
    }
    public float[] getWIDTH() {
        return fWIDTH;
    }
    public void setNormalize() {
        this.bNormalize = true;
    }
    public void setMIN(float[] MIN) {
        this.fMIN = MIN;
    }
    public void setWIDTH (float[] WIDTH) {
        this.fWIDTH = WIDTH;
    }
    
    
    public int getSize() {
        /* SVQ:    Returns the size of the top codebook  */
        /* No SVQ: Returns the sum of all codebook sizes */
        int size = 0;
        if (numSubVectors > 0) {
            size = codebookSVQ.size();
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
        return bIDFWeighting;
    }
    public float[] getDocumentFrequencyFactors() {
        return dff;
    }
    
    public void setIDFWeighting() {
        this.bIDFWeighting = true;
    }
    public void setDocumentFrequencyFactors(float[] dff) {
        this.dff = dff;
    }

    public boolean getLogWeighting() {
        return bLogWeighting;
    }
    public void setLogWeighting() {
        this.bLogWeighting = true;
    }
}
