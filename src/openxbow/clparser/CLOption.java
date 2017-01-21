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

