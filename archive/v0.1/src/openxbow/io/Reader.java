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

package openxbow.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Reader {
    private enum ftype {ARFF,CSV};
    
    private String  fileName;
    private ftype   fileType;
    private String  relation;
    private boolean bTimeStamp;  /* true, if window size and hop size are given, means that the features and labels file must have time stamps in their second column */
    
    private Attributes                 attributes            = null;  /* Attributes of the input feature file */
    private Map<Integer,List<Integer>> indexesAttributeClass = null;  /* A map which specifies for each feature class the corresponding indexes in the input data */
    public  List<Object[]>             inputData             = null;  /* Stores a list of all input instances */
    
    
    public Reader(String fileName) {
        this(fileName,"",false);
    }
    
    public Reader(String fileName, String strAttributes) {
        this(fileName,strAttributes,false);
    }
    
    public Reader(String fileName, String strAttributes, boolean bTimeStamp) {
        this.fileName    = fileName;
        this.bTimeStamp  = bTimeStamp;
        this.attributes  = new Attributes(strAttributes);
        this.inputData   = new ArrayList<Object[]>();
        
        if (fileName.endsWith(".arff")) {
            this.fileType = ftype.ARFF; /* relation is recognized in readFile() */
        }
        else if (fileName.endsWith(".csv")) {
            this.fileType = ftype.CSV;
            this.relation = fileName.substring(0, fileName.indexOf(".csv"));
        }
        else {
            System.err.println("Error: Input file type unknown!");
        }
    }
    
    
    public boolean readFile() {
        BufferedReader br        = null;
        
        try {
            File inputFile = new File(fileName);
                 br        = new BufferedReader(new FileReader(inputFile));
            
            int checkNumAttributes = 0;  /* Just to check whether the specified number and the actual number are the same */
            
            if (fileType==ftype.ARFF) {
                String  thisLine      = null;
                boolean bDataSection  = false;
                
                while ((thisLine = br.readLine()) != null) {
                    if (!thisLine.isEmpty() && !thisLine.startsWith("%") && thisLine.trim().length() > 0) {
                        if (bDataSection) {
                            readDataLine(inputData, thisLine.split(","), attributes.getNumAttributes(), ",");
                        }
                        else if (thisLine.startsWith("@relation") && thisLine.length() > 10) {
                            relation = thisLine.substring(10,thisLine.length());
                        }
                        else if (thisLine.startsWith("@attribute") && thisLine.length() > 11) {
                            if (!attributes.areAttributesSpecified()) {
                                String[] dataLine = thisLine.split(" ");
                                attributes.addAttributeARFF(dataLine,bTimeStamp);
                            }
                            checkNumAttributes++;
                        }
                        else if (thisLine.startsWith("@data")) {
                            bDataSection = true;
                            if (attributes.areAttributesSpecified() && (checkNumAttributes != attributes.getNumAttributes())) {
                                System.err.println("Error: Specified number of attributes does not conform to the number in the input file.");
                                return false;
                            }
                        }
                        else {
                            System.err.println("Error: Input ARFF file cannot be read.");
                            return false;
                        }
                    }
                }
            }
            
            else if (fileType==ftype.CSV) {
                String  separator     = ";";
                String  altSeparator  = ",";
                String  thisLine      = null;
                boolean bFirstLine    = true;
                boolean bHeaderLine   = true;
                
                while ((thisLine = br.readLine()) != null) {
                    if (!thisLine.isEmpty() && thisLine.trim().length() > 0) {
                        String[] dataLine = thisLine.split(separator);
                        
                        if (bFirstLine) {
                            /* If no semicolon has been found, the separator must be comma */ // TODO: Handle case that a string attribute includes a separator 
                            if (dataLine.length==1) {
                                separator = altSeparator;
                                dataLine = thisLine.split(separator);
                            }
                            
                            /* Heuristic to check if there is no header line (if there is a numeric entry, there should be no header line) */
                            for (int c=0; c<dataLine.length; c++) {
                                if (isNumeric(dataLine[c])) {
                                    bHeaderLine = false;
                                    break;
                                }
                            }
                            
                            boolean bLabelGiven = false;
                            /* Heuristic to check if the file includes labels (if labels are included, there must be a header line and the last column must be named either "class" or "label") */
                            if (bHeaderLine && (dataLine[dataLine.length-1].trim().toLowerCase().equals("class") || dataLine[dataLine.length-1].trim().toLowerCase().equals("label"))) {
                                bLabelGiven = true;
                            }
                            
                            if (!attributes.areAttributesSpecified()) {
                                attributes.addAttributesCSV(dataLine.length, bTimeStamp, bLabelGiven);
                            } else if (dataLine.length != attributes.getNumAttributes()) {
                                System.err.println("Error: Specified number of attributes does not conform to the number in the input file.");
                                return false;
                            }
                        }
                        
                        if (!bFirstLine || !bHeaderLine) {
                            readDataLine(inputData, dataLine, attributes.getNumAttributes(), separator);
                        }
                        
                        bFirstLine = false;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error: Input file " + fileName + " cannot be read.");
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
        
        if (fileType==ftype.CSV && !attributes.areAttributesSpecified()) {
            attributes.updateTextFeatures(inputData.get(0));  /* Check if there are text features (is not yet clear for CSV files) */
        }
        
        indexesAttributeClass = attributes.getIndexesAttributeClass();
        
        return true;
    }
    
    
    private void readDataLine(List<Object[]> inputData, String[] dataLine, int numAttributes, String separator) {
        /* Data line has the format: name, [time stamp], feature1, feature2, ..., featureX, [label] */
        Object[] objData = new Object[numAttributes];
        
        /* Add all entries */
        int iF = 0;
        for (int iA=0; iA < numAttributes; iA++) {
            String entry = dataLine[iF++];
            boolean isString = false;
            try {
                objData[iA] = Float.parseFloat(entry);
            } catch(NumberFormatException e) { /* String */
                isString = true;
            }
            if (isString || iA==0) {  /* First attribute (name) is always interpreted as a string object. */
                String data = entry;
                if (data.startsWith("'") || data.startsWith("\"")) { /* If string is not beginning with ' or ": There mustn't be a comma in the string attribute */
                    String delimiter = data.substring(0,1);
                    while (!data.endsWith(delimiter) && iF < dataLine.length) { /* Comma within the string */
                        entry = dataLine[iF++];
                        data  = data.concat(separator);
                        data  = data.concat(entry);
                    }
                    data = data.substring(1);
                    if (data.endsWith(delimiter)) {
                        data = data.substring(0,data.length()-1);  /* Final delimiter might be missing */
                    }
                }
                objData[iA] = data;
            }
        }
        
        assert(objData.length==numAttributes);
        
        inputData.add(objData);
    }
    
    
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch(NumberFormatException e) {
            return false;
        }
        
        return true;
    }
    
    
    public String getRelation() {
        return relation;
    }
    
    public Map<Integer,List<Integer>> getIndexesAttributeClass() {
        return indexesAttributeClass;
    }
    public int getNumAttributes() {
        return attributes.getNumAttributes();
    }
    public int getNumFeatures() {
        return attributes.getNumFeatures();
    }
    public int getIndexName() {
        return attributes.getIndexName();
    }
    public int getIndexTime() {
        return attributes.getIndexTime();
    }
    public List<Integer> getIndexesLabels() {
        return attributes.getIndexesLabels();
    }
    
}
