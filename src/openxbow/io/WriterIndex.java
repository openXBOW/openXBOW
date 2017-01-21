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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import openxbow.main.DataManager;
import openxbow.main.HyperBag;

public class WriterIndex {
    private String fileName;
    
    public WriterIndex(String fileName, DataManager DM) {
        this.fileName = fileName;
    }
    
    
    public boolean writeFile(HyperBag hyperBag) {
        List<Object[]> listAssignments = hyperBag.getListsOfAssignments();
        
        try {
            File outputFile = new File(fileName);
            
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            
            FileWriter     fw = new FileWriter(outputFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            
            /* CSV file */
            for (int v=0; v < listAssignments.size(); v++) {
                for (int s=0; s < listAssignments.get(0).length; s++) {
                    bw.write(String.valueOf(listAssignments.get(v)[s]));
                    if (s < listAssignments.get(0).length-1) {
                        bw.write(";");
                    }
                }
                bw.newLine();
            }
            
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return true;
    }
    
}
