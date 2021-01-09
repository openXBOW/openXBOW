package openxbow.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import openxbow.main.HyperBag;

public class PredictSVM {
    private String   modelFileName = "";
    private String   targetName    = "";
    
    /* Model */
    private String     solverType  = "";
    private int        numClasses  = 0;
    private int[]      labels      = null;
    private int        numFeatures = 0;
    private double     bias        = -1.0f;
    private int        sizeW       = 0;
    private double[][] w           = null;
    
    /* Predictions */
    private int[]     predictions   = null; 
    private float[][] probEstimates = null;
    
    
    public PredictSVM(String modelFileName) {
        this.modelFileName = modelFileName;
        
        Path pFileName    = Paths.get(modelFileName);
        int posFileEnding = pFileName.getFileName().toString().lastIndexOf(".");
        this.targetName   = pFileName.getFileName().toString().substring(0, posFileEnding);        
    }
    
    
    public void predictLabelsAndWriteJSON(HyperBag hyperBag, String jsonFileName) {
        boolean bReadSuccess = readLiblinearModel();
        if (bReadSuccess) {
            predictLabels(hyperBag);
            writeJSON(jsonFileName);
        }
    }
    
    
    private boolean readLiblinearModel() {
        BufferedReader br = null;
        
        try {
            File inputFile = new File(modelFileName);
                 br        = new BufferedReader(new FileReader(inputFile));
            
            String  thisLine      = null;
            boolean bWeightVector = false;
            int     countWm       = 0;
            
            while ((thisLine = br.readLine()) != null) {
                String[] content = thisLine.split(" ");
                
                if (!bWeightVector) {
                    if (content[0].equals("solver_type")) {
                        solverType = content[1];
                        if (!solverType.equals("L2R_LR_DUAL")) {
                            System.err.println("Error PredictSVM: Only solver type L2R_LR_DUAL is supported at the moment.");
                            return false;
                        }
                    }
                    else if (content[0].equals("nr_class")) {
                        numClasses = Integer.parseInt(content[1]);
                    }
                    else if (content[0].equals("label")) {
                        labels = new int[numClasses];
                        for (int i=0; i<numClasses; i++) {
                            labels[i] = Integer.parseInt(content[i+1]);
                        }
                    }
                    else if (content[0].equals("nr_feature")) {
                        numFeatures = Integer.parseInt(content[1]);
                    }
                    else if (content[0].equals("bias")) {
                        bias = Double.parseDouble(content[1]);
                        if (bias > -Double.MIN_NORMAL) {  /* Bias is used */
                            sizeW = numFeatures + 1;
                        } else {
                            sizeW = numFeatures;
                        }
                    }
                    else if (content[0].equals("w")) {
                        bWeightVector = true;
                        if (numClasses==2) {
                            w = new double[sizeW][1];
                        } else {
                            w = new double[sizeW][numClasses];
                        }
                    }
                    else {
                        System.err.println("Error PredictSVM: Unknown file structure.");
                        return false;
                    }
                }
                else {  /* Weight vector */
                    for (int n=0; n < w[0].length; n++) {
                        w[countWm][n] = Double.parseDouble(content[n]);
                    }
                    countWm++;
                }
            }
        } catch (IOException e) {
            System.err.println("Error PredictSVM: Input file " + modelFileName + " cannot be read.");
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
    
    
    private void predictLabels(HyperBag hyperBag) {
        float [][] bof = hyperBag.getBag().bof;
        
        predictions   = new int[bof.length];
        probEstimates = new float[bof.length][numClasses];
        
        for (int i=0; i < bof.length; i++) {
            /* Compute scalar product with weight vector */
            for (int c=0; c < w[0].length; c++) {
                probEstimates[i][c] = 0.0f;
                for (int k=0; k < numFeatures; k++) {
                    probEstimates[i][c] += bof[i][k] * w[k][c];
                }
                if (sizeW > numFeatures) {  /* bias */
                    probEstimates[i][c] += bias * w[sizeW-1][c];
                }
            }
            
            for (int c=0; c < w[0].length; c++) {
                probEstimates[i][c] = 1 / (1 + (float)Math.exp(-probEstimates[i][c]));
            }
            
            if (numClasses==2) {
                probEstimates[i][1] = 1.0f - probEstimates[i][0];
            } else {
                double sumProbEstimates   = 0.0d;
                for(int cl=0; cl < numClasses; cl++) {
                    sumProbEstimates += probEstimates[i][cl];
                }
                for(int cl=0; cl < numClasses; cl++) {
                    probEstimates[i][cl] = (float) (probEstimates[i][cl] / sumProbEstimates);
                }
            }
            
            float  maxProbEstimate    = 0.0f;
            int    indMaxProbEstimate = 0;
            for(int cl=0; cl < numClasses; cl++) {
                if (probEstimates[i][cl] > maxProbEstimate) {
                    maxProbEstimate    = probEstimates[i][cl];
                    indMaxProbEstimate = cl;
                }                
            }
            
            predictions[i] = labels[indMaxProbEstimate];  /* Classes not necessarily in ascending order, so we CANNOT use: predictions[i] = indMaxProbEstimate + 1; */
        }
    }
    
    
    private void writeJSON(String jsonFileName) {
        if (jsonFileName.isEmpty()) {
            jsonFileName = targetName + ".json";
        }
        
        try {
            File outputFile = new File(jsonFileName);
            
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            
            FileWriter     fw = new FileWriter(outputFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            
            /* JSON file */
            for (int i=0; i < predictions.length; i++) {
                bw.write("{"); bw.newLine();
                
                bw.write("    \"Index\": "   + "\"" + String.valueOf(i) + "\","); bw.newLine();
                bw.write("    \"Feature\": " + "\"" + targetName + "\","); bw.newLine();
                for (int realClass=1; realClass <= numClasses; realClass++) {  /* To avoid the problem of wrong class order in Liblinear. */
                    for (int cl=0; cl < numClasses; cl++) {
                        if (labels[cl]==realClass) {
                            bw.write("    \"Class" + String.valueOf(labels[cl]) + "prob\": " + String.valueOf(probEstimates[i][cl]) + ","); bw.newLine();
                        }
                    }
                }
                bw.write("    \"Prediction\": " + String.valueOf(predictions[i]) + ","); bw.newLine();
                
                bw.write("}"); bw.newLine();
            }
            
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

