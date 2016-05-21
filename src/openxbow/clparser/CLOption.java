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

package openxbow.clparser;

import java.util.ArrayList;
import java.util.List;

public class CLOption {
    private String       strOption;
    private int          numParams;
    private List<Object> listParams;
    private String       strHelpText;
    
    private boolean bPresent;
    
    public CLOption(String strOption, String strHelpText) {
        this.strOption   = strOption;
        this.numParams   = 0;
        this.listParams  = null;
        this.strHelpText = strHelpText;
        this.bPresent    = false;
    }
    
    public CLOption(String strOption, Object defaultParam1, String strHelpText) {
        this.strOption   = strOption;
        this.numParams   = 1;
        this.listParams  = new ArrayList<Object>();
        this.strHelpText = strHelpText;
        this.bPresent    = false;
        
        this.listParams.add(defaultParam1);
    }
    
    public CLOption(String strOption, Object defaultParam1, Object defaultParam2, String strHelpText) {
        this.strOption   = strOption;
        this.numParams   = 2;
        this.listParams  = new ArrayList<Object>();
        this.strHelpText = strHelpText;
        this.bPresent    = false;
        
        this.listParams.add(defaultParam1);
        this.listParams.add(defaultParam2);
    }
    
    public void setPresent() {
        this.bPresent = true;
    }
    
    public boolean isPresent() {
        return this.bPresent;
    }
    
    public String getOptionString() {
        return this.strOption;
    }
    
    public int getNumParams() {
        return this.numParams;
    }
    
    public List<Object> getParamList() {
        return this.listParams;
    }
    
    public void setParam(int index, Object obj) {
        if (this.listParams.get(index).getClass()==String.class) {
            this.listParams.set(index, obj);
        }
        else if (this.listParams.get(index).getClass()==Integer.class) {
            this.listParams.set(index, Integer.parseInt(obj.toString()));
        }
        else if (this.listParams.get(index).getClass()==Float.class|| this.listParams.get(index).getClass()==Double.class) {
            this.listParams.set(index, Float.parseFloat(obj.toString()));
        }
        else {
            System.err.println("Error in CL parser: Parameter type not recognized.");
        }
    }
    
    public String getHelpText() {
        return this.strHelpText;
    }
    
}

