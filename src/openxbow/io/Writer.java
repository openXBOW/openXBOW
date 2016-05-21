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

package openxbow.io;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;

import openxbow.main.DataManager;
import openxbow.main.HyperBag;


public class Writer {
    private enum ftype {ARFF,CSV,LIBSVM};
    
    private String[]    fileNames;
    private ftype[]     fileTypes;
    private String      relation;
    private String[]    strLabels;
    private DataManager DM;
    private boolean     bWriteName;
    private boolean     bWriteTime;
    
    public Writer(String fileName, String relation, DataManager DM, boolean bWriteName, boolean bWriteTimeStamp) {
        this.fileNames  = fileName.split(",");  /* Multiple output files, separated by comma, may be given (so far, multiple labels are required in this case) */
        this.DM         = DM;
        this.bWriteName = bWriteName;
        this.bWriteTime = bWriteTimeStamp;
        this.relation   = "'bag-of-features representation of " + relation.replace("'", "") + "'";
        
        this.fileTypes = new ftype[this.fileNames.length];
        
        for (int f=0; f < this.fileNames.length; f++) {
            if (fileNames[f].endsWith(".arff")) {
                fileTypes[f] = ftype.ARFF;
            }
            else if (fileNames[f].endsWith(".csv")) {
                fileTypes[f] = ftype.CSV;
            }
            else if (fileNames[f].endsWith(".libsvm")) {
                fileTypes[f] = ftype.LIBSVM;
            }
            else {
                System.out.println("Error: Output file " + fileNames[f] + " type unknown!");
            }
        }
    }
    
    
    public boolean writeFile(HyperBag hyperBag) {
        float[][] bof = hyperBag.getBag().bof;
        
        if (!DM.getMappingIDLabels().isEmpty()) {
            generateStrLabels();
        }
        
        try {
            for (int f=0; f < fileNames.length; f++) {
                File outputFile = new File(fileNames[f]);
                
                if (!outputFile.exists()) {
                    outputFile.createNewFile();
                }
                
                FileWriter     fw = new FileWriter(outputFile.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                
                if (fileTypes[f]==ftype.ARFF) {
                    /* relation */
                    bw.write("@relation " + relation);
                    bw.newLine(); bw.newLine();
                    
                    /* attributes */
                    if (bWriteName) {
                        bw.write("@attribute name string");
                        bw.newLine();
                    }
                    if (bWriteTime) {
                        bw.write("@attribute time numeric");
                        bw.newLine();
                    }
                    for (int i=0; i < bof[0].length; i++) {
                        bw.write("@attribute W(" + String.valueOf(i) + ") numeric");
                        bw.newLine();
                    }
                    if (strLabels!=null) {
                        for (int m=0; m < strLabels.length; m++) {
                            if (m==0) {
                                bw.write("@attribute class " + strLabels[m]);
                            } else {
                                bw.write("@attribute class" + String.valueOf(m) + " " + strLabels[m]);
                            }
                            bw.newLine();
                        }
                    }
                    bw.newLine();
                    
                    // data
                    bw.write("@data");
                    bw.newLine();
                    for (int i=0; i < bof.length; i++) {
                        if (bWriteName) {
                            bw.write("'" + DM.getMappingIDName().get(i) + "',");
                        }
                        if (bWriteTime) {
                            bw.write(DM.getMappingIDTime().get(i) + ",");
                        }
                        for (int j=0; j < bof[0].length; j++) {
                            bw.write(String.valueOf(bof[i][j]));
                            if (j < bof[0].length-1) {
                                bw.write(",");
                            }
                        }
                        if (strLabels!=null) {
                            for (int m=0; m < strLabels.length; m++) {
                                String curLabel = editLabel(DM.getMappingIDLabels().get(i)[m]);
                                bw.write(",");
                                bw.write(String.valueOf(curLabel));
                            }
                        }
                        bw.newLine();
                    }
                }      
                
                else if (fileTypes[f]==ftype.CSV) {
                    for (int i=0; i < bof.length; i++) {
                        if (bWriteName) {
                            bw.write("'" + DM.getMappingIDName().get(i) + "';");
                        }
                        if (bWriteTime) {
                            bw.write(DM.getMappingIDTime().get(i) + ";");
                        }
                        for (int j=0; j < bof[0].length; j++) {
                            bw.write(String.valueOf(bof[i][j]));
                            if (j < bof[0].length-1) {
                                bw.write(";");
                            }
                        }
                        if (strLabels!=null) {
                            if (fileNames.length > 1) {
                                bw.write(";" + editLabel(DM.getMappingIDLabels().get(i)[f])); /* If several output files are given, write only one label*/
                            } else {
                                for (int m=0; m < strLabels.length; m++) {
                                    bw.write(";" + editLabel(DM.getMappingIDLabels().get(i)[m]));
                                }   
                            }
                        }
                        bw.newLine();
                    }
                }
                
                else if (fileTypes[f]==ftype.LIBSVM) {
                    for (int i=0; i < bof.length; i++) {
                        if (strLabels!=null) {
                            String curLabel = editLabel(DM.getMappingIDLabels().get(i)[f]);  /* Multi-label is currently not supported in libSVM, thus several output files (separator ,) should be given. */
                            bw.write(String.valueOf(curLabel));
                        } else {
                            bw.write("0");
                        }
                        bw.write(" ");
                        for (int j=0; j < bof[0].length; j++) {
                            if (bof[i][j] > Float.MIN_NORMAL) {
                                bw.write(String.valueOf(j+1) + ":" + String.valueOf(bof[i][j]));
                                bw.write(" ");
                            }
                        }
                        bw.newLine();
                    }
                }
                
                bw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return true;
    }
    
    
    private void generateStrLabels() {
        boolean[] bNominalLabels = labelsNominal();
                  strLabels      = new String[bNominalLabels.length];
        
        for (int m=0; m < bNominalLabels.length; m++) {
            if (bNominalLabels[m]) {
                Vector<String> vecLabels = new Vector<String>();
                for (int i=0; i < DM.getMappingIDLabels().size(); i++) {
                    String curLabel = editLabel(DM.getMappingIDLabels().get(i)[m]);
                    if (!vecLabels.contains(curLabel)) {
                        vecLabels.add(curLabel);
                    }
                }
                
                Collections.sort(vecLabels);
                
                strLabels[m] = "{";
                for (int i=0; i<vecLabels.size(); i++) {
                    strLabels[m] = strLabels[m].concat(String.valueOf(vecLabels.elementAt(i)));
                    
                    if (i<vecLabels.size()-1) {
                        strLabels[m] = strLabels[m].concat(",");
                    }
                }
                strLabels[m] = strLabels[m].concat("}");
            }
            else {
                strLabels[m] = "numeric";
            }
        }
    }
    
    
    private String editLabel(String curLabel) {
        if (isNumeric(curLabel) && curLabel.endsWith(".0")) {
            curLabel = curLabel.substring(0, curLabel.length()-2);
        }
        return curLabel;
    }
    
    
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch(NumberFormatException e) {
            return false;
        }
        
        return true;
    }
    
    
    private boolean[] labelsNominal() {
        /* In case of ARFF input or given attributes string, this function is actually not necessary */
        /* However, it might be easier to run this function instead at the moment                    */
        if (DM.getMappingIDLabels().isEmpty()) {
            return new boolean[0];
        }
        
        boolean[] bLabelsNominal = new boolean[DM.getMappingIDLabels().get(0).length];
        for (int l=0; l < DM.getMappingIDLabels().get(0).length; l++) {
            bLabelsNominal[l] = true;
            for (int id=0; id < DM.getMappingIDLabels().size(); id++) {
                if (!isNominal(DM.getMappingIDLabels().get(id)[l])) {
                    bLabelsNominal[l] = false;  /* If there is at least one non-nominal label */
                    break;
                }
            }
        }
        
        return bLabelsNominal;
    }
    
    private boolean isNominal(String str) {
        try {
            Double.parseDouble(str);
        } catch(NumberFormatException e) {
            return true;  /* No number */
        }
        
        double dTest = Double.parseDouble(str);
        int    iTest = (int) Math.round(dTest);
        if (areEqual(dTest,iTest)) {
            return true;  /* Integer */
        }
        
        return false;  /* Float / Double */
    }
    
    private boolean areEqual(double f1, double f2) {
        if (f1+1E-4d > f2 && f1-1E-4d < f2) {
            return true;
        }
        else {
            return false;
        }
    }
}
