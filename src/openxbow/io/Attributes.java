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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Attributes {
    private boolean bAttributesSpecified = false;
    
    private int numAttributes = 0;  /* Dimension of the input data instances */
    private int numFeatures   = 0;  /* Dimension of the input features,i.e., without side information (name, time stamp, labels) */
    
    private int indexName = -1;
    private int indexTime = -1;  /* -1 means that there is no time stamp */
    private Map<Integer,List<Integer>> indexesAttributeClass = null;  /* A map which specifies for each feature class the corresponding indexes in the input data */
    private List<Integer> indexesLabels = null;
    private List<Integer> indexesRemove = null;
    
    
    public Attributes (String strAttributes) {
        indexesAttributeClass = new HashMap<Integer,List<Integer>>();
        indexesLabels = new ArrayList<Integer>();
        indexesRemove = new ArrayList<Integer>();
        
        if (!strAttributes.isEmpty()) {
            bAttributesSpecified = true;
            parseInputString(strAttributes);
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
        
        int m = indexesAttributeClass.get(1).get(0);  /* Until now, only numeric features may be present */
        while (m < indexesAttributeClass.get(1).size()) {
            if (!isNumeric(dataLine[indexesAttributeClass.get(1).get(m)].toString())) {
                if (!indexesAttributeClass.containsKey(0)) {  /* Add text feature list */
                    indexesAttributeClass.put(0, new ArrayList<Integer>());
                }
                indexesAttributeClass.get(0).add(indexesAttributeClass.get(1).get(m));
                indexesAttributeClass.get(1).remove(indexesAttributeClass.get(1).get(m));
            }
            else {
                m++;
            }
        }
    }
    
    
    private void parseInputString(String strAttributes) {
        numAttributes = strAttributes.length();
        numFeatures   = 0;
        
        /* Set indexes of name, time, labels and attributes to be removed and features in attribute list */
        for (int m=0; m < strAttributes.length(); m++) {
            if (strAttributes.toLowerCase().charAt(m)=='n') {
                indexName = m;
            } else if (strAttributes.toLowerCase().charAt(m)=='t') {
                indexTime = m;
            } else if (strAttributes.toLowerCase().charAt(m)=='c' || strAttributes.charAt(m)=='l') {
                indexesLabels.add(m);
            } else if (strAttributes.toLowerCase().charAt(m)=='r') {
                indexesRemove.add(m);
            } else if (Character.isDigit(strAttributes.charAt(m))) {
                int featureType = Character.getNumericValue(strAttributes.charAt(m));
                if (!indexesAttributeClass.containsKey(featureType)) {
                    indexesAttributeClass.put(featureType, new ArrayList<Integer>());
                }
                indexesAttributeClass.get(featureType).add(m);
                numFeatures++;
            }
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
