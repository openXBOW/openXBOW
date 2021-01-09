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

package openxbow.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Attributes {
    private boolean bAttributesSpecified = false;
    
    private int numAttributes = 0;  /* Dimension of the input data instances */
    private int numFeatures   = 0;  /* Dimension of the input features,i.e., without side information (name, time stamp, labels, removed features) */
    
    private int indexName = -1;
    private int indexTime = -1;  /* -1 means that there is no time stamp */
    private Map<Integer,List<Integer>> indexesAttributeClass = null;  /* A map which specifies for each feature class the corresponding indexes in the input data */
    private List<Integer> indexesLabels = null;
    private List<Integer> indexesRemove = null;
    
    
    public Attributes (String strAttributes, String strAttributesAlt) {
        indexesAttributeClass = new HashMap<Integer,List<Integer>>();
        indexesLabels = new ArrayList<Integer>();
        indexesRemove = new ArrayList<Integer>();
        
        if (!strAttributes.isEmpty()) {
            bAttributesSpecified = true;
            parseInputString(strAttributes);
        }
        else if (!strAttributesAlt.isEmpty()) {  /* Only relevant if strAttributes is not defined. */
            bAttributesSpecified = true;
            parseInputStringAlt(strAttributesAlt);
        }
    }
    
    public boolean areAttributesSpecified() {
        return bAttributesSpecified;
    }
    public int getNumAttributes() {
        return numAttributes;
    }
    public int getNumFeatures() {
        return numFeatures;
    }
    public int getIndexName() {
        return indexName;
    }
    public int getIndexTime() {
        return indexTime;
    }
    public List<Integer> getIndexesLabels() {
        return indexesLabels; 
    }
    public Map<Integer,List<Integer>> getIndexesAttributeClass() {
        return indexesAttributeClass;
    }
    public int getNumberOfFeatureClasses() {
        int num = 1;  /* A symbolic feature class is always taken into account, even if not present in indexesAttributeClass */
        for (Entry<Integer,List<Integer>> e : indexesAttributeClass.entrySet()) {
            if (e.getKey()>0) {
                num++;
            }
        }
        return num;
    }
    
    public void addAttributeARFF(String[] line, boolean bTimeStamp) {
        /* Uses a heuristic to add the correct attribute (for ARFF) */
        if (bAttributesSpecified) {
            System.err.println("Error in addAttributeARFF(): Not allowed if attribute string is given");
        }
        
        /* Heuristic */
        if (numAttributes==0) {
            indexName = 0;
            numAttributes = 1;
        } else if (numAttributes==1 && bTimeStamp) {
            indexTime = 1;
            numAttributes = 2;
        } else if (line[1].trim().toLowerCase().startsWith("class") || line[1].trim().toLowerCase().startsWith("label")) {
            if (line[line.length-1].contains("{")) {
                indexesLabels.add(numAttributes++);
            }
        } else {
            if (line[line.length-1].trim().toLowerCase().equals("string")) {
                if (!indexesAttributeClass.containsKey(0)) {
                    indexesAttributeClass.put(0, new ArrayList<Integer>());
                }
                indexesAttributeClass.get(0).add(numAttributes++);
                numFeatures++;
            } else {
                if (!indexesAttributeClass.containsKey(1)) {
                    indexesAttributeClass.put(1, new ArrayList<Integer>());
                }
                indexesAttributeClass.get(1).add(numAttributes++);
                numFeatures++;
            }
        }
    }
    
    
    public void addAttributesCSV(int inputLength, boolean bTimeStamp, boolean bLabelGiven) {
        /* Uses a heuristic to add the correct attribute (for CSV) */
        if (bAttributesSpecified) {
            System.err.println("Error in addAttributesCSV(): Not allowed if attribute string is given");
        }
        
        numAttributes = 0;
        numFeatures   = 0;
        
        /* Heuristic */
        indexName = 0;
        numAttributes++;
        if (bTimeStamp) {
            indexTime = 1;
            numAttributes++;
        }
        
        int startIndex = numAttributes;
        int stopIndex  = inputLength;
        if (bLabelGiven) {
            stopIndex--;
        }
        
        for (int m=startIndex; m < stopIndex; m++) {
            if (!indexesAttributeClass.containsKey(1)) {  /* Default feature is numeric */
                indexesAttributeClass.put(1, new ArrayList<Integer>());
            }
            indexesAttributeClass.get(1).add(numAttributes++);
            numFeatures++;
        }
        
        if (bLabelGiven) {
            indexesLabels.add(numAttributes++);
        }
        
        assert(this.numAttributes==inputLength);
    }
    
    
    public void updateTextFeatures(Object[] dataLine) {
        if (bAttributesSpecified || indexesAttributeClass.containsKey(0) || indexesAttributeClass.size() > 1) {
            System.err.println("Error in updateTextFeatures(): May not be called if attributes are specified.");
            return;
        }
        
        Map<Integer,List<Integer>> tmpMap = new HashMap<Integer,List<Integer>>();
        
        for (int m : indexesAttributeClass.get(1)) {  /* So far, only numeric features (1) are present */
            if (isNumeric(dataLine[m].toString())) { /* Numeric (1) */
                if (!tmpMap.containsKey(1)) {
                    tmpMap.put(1, new ArrayList<Integer>());
                }
                tmpMap.get(1).add(m);                
            } else { /* Symbolic (0) */
                if (!tmpMap.containsKey(0)) {
                    tmpMap.put(0, new ArrayList<Integer>());
                }
                tmpMap.get(0).add(m);
            }
        }
        
        indexesAttributeClass = tmpMap;
    }
    
    
    private void parseInputString(String strAttributes) {
        numAttributes = 0;
        numFeatures   = 0;
        
        /* Set indexes of name, time, labels and attributes to be removed and features in attribute list */
        int m=0;  /* Index within the attributes string */
        
        while (m < strAttributes.length()) {
            if (strAttributes.toLowerCase().charAt(m)=='n') {
                indexName = numAttributes;
                numAttributes++;
            } else if (strAttributes.toLowerCase().charAt(m)=='t') {
                indexTime = numAttributes;
                numAttributes++;
            } else if (strAttributes.toLowerCase().charAt(m)=='c' || strAttributes.charAt(m)=='l') {
                indexesLabels.add(numAttributes);
                numAttributes++;
            } else if (strAttributes.toLowerCase().charAt(m)=='r') {
                if (m+1 < strAttributes.length() && strAttributes.charAt(m+1)=='[') {
                    /* Multiple features */
                    m = m + 2;
                    String strNumIndexes = "";
                    int    intNumIndexes = 0;
                    while (strAttributes.charAt(m)!=']') {  /* Parse number of subsequent features */
                        strNumIndexes = strNumIndexes.concat(strAttributes.substring(m,m+1));
                        m++;
                    }
                    intNumIndexes = Integer.parseInt(strNumIndexes);
                    for (int c=1; c<=intNumIndexes; c++) {
                        indexesRemove.add(numAttributes);
                        numAttributes++;
                    }
                } else {
                    indexesRemove.add(numAttributes);
                    numAttributes++;
                }
            } else if (Character.isDigit(strAttributes.charAt(m))) {
                int featureType = Character.getNumericValue(strAttributes.charAt(m));
                if (!indexesAttributeClass.containsKey(featureType)) {
                    indexesAttributeClass.put(featureType, new ArrayList<Integer>());
                }
                
                if (m+1 < strAttributes.length() && strAttributes.charAt(m+1)=='[') {
                    /* Multiple features */
                    m = m + 2;
                    String strNumIndexes = "";
                    int    intNumIndexes = 0;
                    while (strAttributes.charAt(m)!=']') {  /* Parse number of subsequent features */
                        strNumIndexes = strNumIndexes.concat(strAttributes.substring(m,m+1));
                        m++;
                    }
                    intNumIndexes = Integer.parseInt(strNumIndexes);
                    for (int c=1; c<=intNumIndexes; c++) {
                        indexesAttributeClass.get(featureType).add(numAttributes);
                        numFeatures++;
                        numAttributes++;
                    }
                } else {
                    indexesAttributeClass.get(featureType).add(numAttributes);
                    numFeatures++;
                    numAttributes++;
                }
            }
            
            m++;
        }
    }
    
    
    private void parseInputStringAlt(String strAttributes) {
        numAttributes = 0;
        numFeatures   = 0;
        
        List<Integer> indexesNumericFeatures = new ArrayList<Integer>();
        
        /* Set indexes of name, time, labels and attributes to be removed and features in attribute list */
        int m=0;  /* Index within the attributes string */
        
        while (m < strAttributes.length() && strAttributes.toLowerCase().charAt(m)!='_') {
            if (strAttributes.toLowerCase().charAt(m)=='n') {
                indexName = numAttributes;
                numAttributes++;
            } else if (strAttributes.toLowerCase().charAt(m)=='t') {
                indexTime = numAttributes;
                numAttributes++;
            } else if (strAttributes.toLowerCase().charAt(m)=='c' || strAttributes.charAt(m)=='l') {
                indexesLabels.add(numAttributes);
                numAttributes++;
            } else if (strAttributes.toLowerCase().charAt(m)=='r') {
                if (m+1 < strAttributes.length() && strAttributes.charAt(m+1)=='[') {
                    /* Multiple features */
                    m = m + 2;
                    String strNumIndexes = "";
                    int    intNumIndexes = 0;
                    while (strAttributes.charAt(m)!=']') {  /* Parse number of subsequent features */
                        strNumIndexes = strNumIndexes.concat(strAttributes.substring(m,m+1));
                        m++;
                    }
                    intNumIndexes = Integer.parseInt(strNumIndexes);
                    for (int c=1; c<=intNumIndexes; c++) {
                        indexesRemove.add(numAttributes);
                        numAttributes++;
                    }
                } else {
                    indexesRemove.add(numAttributes);
                    numAttributes++;
                }
            } else if (strAttributes.toLowerCase().charAt(m)=='m') {  
                if (m+1 < strAttributes.length() && strAttributes.charAt(m+1)=='[') {
                    /* Multiple features */
                    m = m + 2;
                    String strNumIndexes = "";
                    int    intNumIndexes = 0;
                    while (strAttributes.charAt(m)!=']') {  /* Parse number of subsequent features */
                        strNumIndexes = strNumIndexes.concat(strAttributes.substring(m,m+1));
                        m++;
                    }
                    intNumIndexes = Integer.parseInt(strNumIndexes);
                    for (int c=1; c<=intNumIndexes; c++) {
                        indexesNumericFeatures.add(numAttributes);
                        numAttributes++;
                    }
                } else {
                    indexesNumericFeatures.add(numAttributes);
                    numAttributes++;
                }
            } else if (Character.isDigit(strAttributes.charAt(m))) {
                int featureType = Character.getNumericValue(strAttributes.charAt(m));
                if (featureType!=0) {
                    System.err.println("Error (Attributes): Only 0 (symbolic features) may be specified in -attributesAlt. Use 'm' to specify numeric features.");
                }
                if (!indexesAttributeClass.containsKey(featureType)) {
                    indexesAttributeClass.put(featureType, new ArrayList<Integer>());
                }
                
                if (m+1 < strAttributes.length() && strAttributes.charAt(m+1)=='[') {
                    /* Multiple features */
                    m = m + 2;
                    String strNumIndexes = "";
                    int    intNumIndexes = 0;
                    while (strAttributes.charAt(m)!=']') {  /* Parse number of subsequent features */
                        strNumIndexes = strNumIndexes.concat(strAttributes.substring(m,m+1));
                        m++;
                    }
                    intNumIndexes = Integer.parseInt(strNumIndexes);
                    for (int c=1; c<=intNumIndexes; c++) {
                        indexesAttributeClass.get(featureType).add(numAttributes);
                        numFeatures++;
                        numAttributes++;
                    }
                } else {
                    indexesAttributeClass.get(featureType).add(numAttributes);
                    numFeatures++;
                    numAttributes++;
                }
            }
            
            m++;
        }
        
        /* Now, construct the numeric codebooks. */
        if (strAttributes.length()>=m && strAttributes.toLowerCase().charAt(m)!='_') {
            System.err.println("Error (Attributes): Please check the format of -attributesAlt carefully.");
        }
        
        m++;
        
        int featureType = 0;
        while (m < strAttributes.length()) {
            if (strAttributes.toLowerCase().charAt(m)!='[') {
                System.err.println("Error (Attributes): Please check the format of -attributesAlt carefully.");
            }
            m++;
            featureType++;
            while (strAttributes.charAt(m)!=']') {
                if (strAttributes.charAt(m)=='+') {
                    m++;
                    continue;  /* + can just be ignored */
                }
                String strIndex = "";
                int    intIndex = 0;
                while (strAttributes.charAt(m)!='+' && strAttributes.charAt(m)!='-' && strAttributes.charAt(m)!=']') {
                    strIndex = strIndex.concat(strAttributes.substring(m,m+1));
                    m++;
                }
                intIndex = Integer.parseInt(strIndex);
                if (intIndex==0) {
                    System.err.println("Error (Attributes): Feature index max not be 0. Please check the format of -attributesAlt carefully.");
                }
                if (!indexesAttributeClass.containsKey(featureType)) {
                    indexesAttributeClass.put(featureType, new ArrayList<Integer>());
                }
                indexesAttributeClass.get(featureType).add(indexesNumericFeatures.get(intIndex-1));
                
                /* Add range */
                if (strAttributes.charAt(m)=='-') {
                    int intIndexStart = intIndex;
                    m++;
                    strIndex = "";
                    intIndex = 0;
                    while (strAttributes.charAt(m)!='+' && strAttributes.charAt(m)!=']') {
                        if (strAttributes.charAt(m)=='-') {
                            System.err.println("Error (Attributes): Please check the format of -attributesAlt carefully.");
                        }
                        strIndex = strIndex.concat(strAttributes.substring(m,m+1));
                        m++;
                    }
                    int intIndexEnd = Integer.parseInt(strIndex);
                    if (intIndexEnd<intIndexStart) {
                        System.err.println("Error (Attributes): Please check the format of -attributesAlt carefully.");
                    }
                    for (int c=intIndexStart+1; c<=intIndexEnd; c++) {
                        indexesAttributeClass.get(featureType).add(indexesNumericFeatures.get(c-1));
                    }
                }
            }
            
            m++;
        }
    }
    
    
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch(NumberFormatException e) {
            return false;
        }
        
        return true;
    }
}
