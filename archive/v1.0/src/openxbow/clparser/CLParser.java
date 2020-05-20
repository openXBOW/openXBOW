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

public class CLParser {
    List<CLOption> listCLOptions = null;
    
    public CLParser() {
        listCLOptions = new ArrayList<CLOption>();
    }
    
    
    public void addOption(String strOption, String strHelpText) {
        listCLOptions.add(new CLOption(strOption, strHelpText));
    }
    
    public void addOption(String strOption, Object defaultParam1, String strHelpText) {
        listCLOptions.add(new CLOption(strOption, defaultParam1, strHelpText));
    }
    
    public void addOption(String strOption, Object defaultParam1, Object defaultParam2, String strHelpText) {
        listCLOptions.add(new CLOption(strOption, defaultParam1, defaultParam2, strHelpText));
    }
    
    public void parseCL(String[] args) {
        for (int i=0; i<args.length; i++) {
            if (args[i].startsWith("-")) {
                CLOption opt = getOption(args[i].substring(1));
                
                if (opt!=null) {
                    opt.setPresent();
                    for (int k=0; k<opt.getNumParams(); k++) {
                        opt.setParam(k, args[i+1+k]);
                    }
                }
            }
        }
    }
    
    public CLOption getOption(String arg) {
        for (CLOption opt : listCLOptions) {
            if (opt.getOptionString().equals(arg)) {
                return opt;
            }
        }
        
        return null;
    }
    
    public void printHelp() {
        for (CLOption opt : listCLOptions) {
            String whitespace = genString(14);
            String helpText = opt.getHelpText();
            String paramString = "";
            if (opt.getNumParams()==1) {
                paramString = " p";
            }
            else if (opt.getNumParams()==2) {
                paramString = " p1 p2";
            }
            else if (opt.getNumParams() > 2) {
                paramString = " p1 p2 ...";
            }
            
            helpText = helpText.replace("\n", "\n   " + whitespace);
            if ((opt.getOptionString().length() + paramString.length()) > whitespace.length()) {
                whitespace = genString(opt.getOptionString().length() + paramString.length() + 1);
            }
            System.out.println("-" + opt.getOptionString() + paramString + whitespace.substring(opt.getOptionString().length() + paramString.length()) + " " + helpText);
        }
    }
    
    private String genString(int len) {
        String whitespace = "";
        for (int i=0; i < len; i++) {
            whitespace = whitespace + " ";
        }
        return whitespace;
    }
}
