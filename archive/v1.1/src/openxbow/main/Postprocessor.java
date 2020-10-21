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

package openxbow.main;

import java.util.ArrayList;
import java.util.List;

import openxbow.codebooks.HyperCodebook;

public class Postprocessor {
    private HyperCodebook book;
    private HyperBag      hyperBag;
    private boolean       bStandardize;
    private boolean       bNormalize;
    
    public Postprocessor(HyperCodebook book, HyperBag hyperBag, Options options) {
        this.book             = book;
        this.hyperBag         = hyperBag;
        this.bStandardize     = options.bStandardizeOutput;
        this.bNormalize       = options.bNormalizeOutput;
    }
    
    public void postprocessOutput() {
        /* Standardize/normalize output */
        if (bStandardize && bNormalize) {
            System.err.println("Error: Both standardization and normalization of the output is not possible!");
            return;
        }
        
        if (bStandardize) {
            if (book.getMeansOutput()==null) {  /* NOTE: It is decided whether or not a codebook is given! */
                float[] means        = getMeans(hyperBag.getBag().bof);
                float[] standardDevs = getStandardDevs(hyperBag.getBag().bof, means);
                
                book.setStandardizeOutput();
                book.setMeansOutput(means);
                book.setStandardDevsOutput(standardDevs);
                standardize(hyperBag.getBag().bof, means, standardDevs);
            } else {
                standardize(hyperBag.getBag().bof, book.getMeansOutput(), book.getStandardDevsOutput());
            }
        }
        
        if (bNormalize) {
            if (book.getMINOutput()==null) {  /* NOTE: It is decided whether or not a codebook is given! */
                List<float[]> MINandWIDTH = getMINandWIDTH(hyperBag.getBag().bof);
                float[] MIN   = MINandWIDTH.get(0);
                float[] WIDTH = MINandWIDTH.get(1);
                book.setNormalizeOutput();
                book.setMINOutput(MIN);
                book.setWIDTHOutput(WIDTH);
                normalize(hyperBag.getBag().bof, MIN, WIDTH);
            } else {
                normalize(hyperBag.getBag().bof, book.getMINOutput(), book.getWIDTHOutput());
            }
        }
    }
    
    private float[] getMeans(float[][] bof) {
        float[] mean    = new float[bof[0].length];
        int     counter = 0;  /* Counts the samples */
        
        /* Arithmetic mean */
        for (int id=0; id < bof.length; id++) {
            for (int w=0; w < bof[0].length; w++) {
                mean[w] += bof[id][w];
            }
            counter++;
        }
        
        for (int w=0; w < mean.length; w++) {
            mean[w] = mean[w] / counter;
        }
        
        return mean;
    }
    
    private float[] getStandardDevs(float[][] bof, float[] mean) {
        float[] std     = new float[bof[0].length];
        int     counter = 0;  /* Counts the samples */
        float   diff    = 0;
        
        /* Standard deviation */
        for (int id=0; id < bof.length; id++) {
            for (int w=0; w < bof[0].length; w++) {
                diff    = (bof[id][w]) - mean[w];
                std[w] += diff * diff;
            }
            counter++;
        }
        
        for (int w=0; w < std.length; w++) {
            std[w] = std[w] / (counter - 1);
            std[w] = (float) Math.sqrt(std[w]);
            if (std[w] < Float.MIN_NORMAL) {
                std[w] = 1.0f;
            }
        }
        
        return std;
    }
    
    private void standardize(float bof[][], float[] mean, float[] std) {
        System.out.println("Standardization of the output ...");
        
        for (int id=0; id < bof.length; id++) {
            for (int w=0; w < bof[0].length; w++) {
                if (std[w] > Float.MIN_VALUE) {
                    bof[id][w] = (bof[id][w] - mean[w]) / std[w];
                }
            }
        }
    }
    
    
    private List<float[]> getMINandWIDTH(float[][] bof) {
        float[] MIN   = new float[bof[0].length];
        float[] MAX   = new float[bof[0].length];
        float[] WIDTH = new float[bof[0].length];
        
        for (int f=0; f < MIN.length; f++) {
            MIN[f] = Float.POSITIVE_INFINITY;
            MAX[f] = Float.NEGATIVE_INFINITY;
        }
        
        for (int id=0; id < bof.length; id++) {
            for (int w=0; w < bof[0].length; w++) {
                if (MIN[w] > bof[id][w]) {
                    MIN[w] = bof[id][w];
                }
                if (MAX[w] < bof[id][w]) {
                    MAX[w] = bof[id][w];
                }
            }
        }
        
        for (int w=0; w < MIN.length; w++) {
            WIDTH[w] = MAX[w] - MIN[w];
            if (WIDTH[w] < Float.MIN_NORMAL) {
                WIDTH[w] = 1.0f;
            }
        }
        
        /* Put MIN and WIDTH in a list */
        List<float[]> result = new ArrayList<float[]>();
        result.add(MIN);
        result.add(WIDTH);
        
        return result;
    }
    
    private void normalize(float[][] bof, float[] MIN, float[] WIDTH) {
        System.out.println("Normalization of the output ...");
        
        for (int id=0; id < bof.length; id++) {
            for (int w=0; w < bof[0].length; w++) {
                if (WIDTH[w] > Float.MIN_VALUE) {
                    bof[id][w] = (bof[id][w] - MIN[w]) / WIDTH[w];
                }
            }
        }
    }
}
