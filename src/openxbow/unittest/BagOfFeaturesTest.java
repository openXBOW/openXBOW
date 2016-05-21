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

package openxbow.unittest;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import openxbow.codebooks.Codebook;
import openxbow.codebooks.CodebookNumeric;
import openxbow.codebooks.CodebookText;
import openxbow.codebooks.HyperCodebook;
import openxbow.io.Reader;
import openxbow.io.Writer;
import openxbow.main.DataManager;
import openxbow.main.HyperBag;
import openxbow.main.Preprocessor;

public class BagOfFeaturesTest {
    @Test
    public void testRead1() {
        System.out.print("Test: read ARFF vs. CSV 1 (with/without labels) ...");
        
        Reader OWReader1ARF = null;
        Reader OWReader1CSV = null;
        Reader OWReader1LARF = null;
        Reader OWReader1LCSV = null;
        
        OWReader1ARF = new Reader("JUnitTestData/random1.arff","",false);
        OWReader1CSV = new Reader("JUnitTestData/random1.csv","",false);
        OWReader1LARF = new Reader("JUnitTestData/random1_wLabels.arff","",false);
        OWReader1LCSV = new Reader("JUnitTestData/random1_wLabels.csv","",false);
        
        OWReader1ARF.readFile();
        OWReader1CSV.readFile();
        OWReader1LARF.readFile();
        OWReader1LCSV.readFile();
        
        DataManager DM1ARF = new DataManager(OWReader1ARF);
        DataManager DM1CSV = new DataManager(OWReader1CSV);
        DataManager DM1LARF = new DataManager(OWReader1LARF);
        DataManager DM1LCSV = new DataManager(OWReader1LCSV);
        
        DM1ARF.generateMappings();
        DM1CSV.generateMappings();
        DM1LARF.generateMappings();
        DM1LCSV.generateMappings();
        
        DM1ARF.readLabelsFile("JUnitTestData/Labels_random1.csv");
        DM1CSV.readLabelsFile("JUnitTestData/Labels_random1.csv");
        
        try {
            Assert.assertEquals("Data size", OWReader1ARF.inputData.size(), OWReader1CSV.inputData.size());
            Assert.assertEquals("Data size", OWReader1LARF.inputData.size(), OWReader1LCSV.inputData.size());
            Assert.assertEquals("Data size", OWReader1LARF.inputData.size(), OWReader1CSV.inputData.size());
            Assert.assertEquals("Map name size", DM1ARF.getMappingIDName().size(), DM1CSV.getMappingIDName().size());
            Assert.assertEquals("Map name size", DM1LARF.getMappingIDName().size(), DM1LCSV.getMappingIDName().size());
            Assert.assertEquals("Map name size", DM1LARF.getMappingIDName().size(), DM1CSV.getMappingIDName().size());
            Assert.assertEquals("Map label size", DM1ARF.getMappingIDLabels().size(), DM1CSV.getMappingIDLabels().size());
            Assert.assertEquals("Map label size", DM1LARF.getMappingIDLabels().size(), DM1LCSV.getMappingIDLabels().size());
            Assert.assertEquals("Map label size", DM1LARF.getMappingIDLabels().size(), DM1CSV.getMappingIDLabels().size());
            for (int k=1; k < DM1ARF.getMappingIDName().size(); k++) {
                Assert.assertEquals("Mapping name id " + k, DM1ARF.getMappingIDName().get(k), DM1CSV.getMappingIDName().get(k));
                Assert.assertEquals("Mapping name id " + k, DM1LARF.getMappingIDName().get(k), DM1LCSV.getMappingIDName().get(k));
                Assert.assertEquals("Mapping name id " + k, DM1LARF.getMappingIDName().get(k), DM1CSV.getMappingIDName().get(k));
                Assert.assertEquals("Mapping label id " + k, DM1ARF.getMappingIDLabels().get(k)[0], DM1CSV.getMappingIDLabels().get(k)[0]);
                Assert.assertEquals("Mapping label id " + k, DM1LARF.getMappingIDLabels().get(k)[0], DM1LCSV.getMappingIDLabels().get(k)[0]);
                Assert.assertEquals("Mapping label id " + k, DM1LARF.getMappingIDLabels().get(k)[0], DM1CSV.getMappingIDLabels().get(k)[0]);
            }
        } catch (AssertionError e) {
            System.err.println("Error in Reader: " + e);
        }
        System.out.println(" finished!");        
    }
    
    @Test
    public void testRead2() {
        System.out.print("Test: read ARFF vs. CSV 2 ...");
        
        Reader OWReader2ARF = null;
        Reader OWReader2CSV = null;
        OWReader2ARF = new Reader("JUnitTestData/random2.arff","",true);
        OWReader2CSV = new Reader("JUnitTestData/random2.csv","",true);
        OWReader2ARF.readFile();
        OWReader2CSV.readFile();
        
        DataManager DM2ARF = new DataManager(OWReader2ARF,2.5f,0.5f);
        DataManager DM2CSV = new DataManager(OWReader2CSV,2.5f,0.5f);
        DM2ARF.generateMappings();
        DM2CSV.generateMappings();
        DM2ARF.readLabelsFile("JUnitTestData/Labels_random2.csv");
        DM2CSV.readLabelsFile("JUnitTestData/Labels_random2.csv");
        
        try {
            Assert.assertEquals("Data size", OWReader2ARF.inputData.size(), OWReader2ARF.inputData.size());
            Assert.assertEquals("Map name size", DM2ARF.getMappingIDName().size(), DM2CSV.getMappingIDName().size());
            Assert.assertEquals("Map label size", DM2ARF.getMappingIDLabels().size(), DM2CSV.getMappingIDLabels().size());
            Assert.assertEquals("Map time size", DM2ARF.getMappingIDTime().size(), DM2CSV.getMappingIDTime().size());
            for (int k=1; k < DM2ARF.getMappingIDName().size(); k++) {
                Assert.assertEquals("Mapping name id " + k, DM2ARF.getMappingIDName().get(k), DM2CSV.getMappingIDName().get(k));
                Assert.assertEquals("Mapping label id " + k, DM2ARF.getMappingIDLabels().get(k)[0], DM2CSV.getMappingIDLabels().get(k)[0]);
                Assert.assertEquals("Mapping time id " + k, DM2ARF.getMappingIDTime().get(k), DM2CSV.getMappingIDTime().get(k));
            }
        } catch (AssertionError e) {
            System.err.println("Error in Reader: " + e);
        }
        System.out.println(" finished!");
    }
    
    
    @Test
    public void testRun1() {
        System.out.print("Test: Run with numeric input file and different configs, e.g. SVQ, s, norm, idf, ...");
        
        Reader OWReaderARF3 = new Reader("JUnitTestData/testdata3.arff");
        OWReaderARF3.readFile();
        DataManager DM3ARF = new DataManager(OWReaderARF3);
        DM3ARF.generateMappings();
        
        /* Check SVQ, standardization, term weighting and codebook output */
        int splitVQ = 3;
        int sizeSubCodebook = 4;
        HyperCodebook hyperBook  = new HyperCodebook(DM3ARF, splitVQ, true, true, "", 0, 0);
        HyperCodebook hyperBook2 = new HyperCodebook(DM3ARF, splitVQ, true, true, "", 0, 0);
        Preprocessor preProc = new Preprocessor(DM3ARF, hyperBook, false, 0, 0, true, false);  /* Test standardization */
        preProc.preprocessInput();
        HyperBag hyperBag = new HyperBag(DM3ARF, hyperBook, splitVQ);
        hyperBook.generateSubCodebooksSVQ(DM3ARF, sizeSubCodebook, "kmeans", false, 100);  /* Test k-means */
        hyperBook.generateSubCodebooksSVQ(DM3ARF, sizeSubCodebook, "random++", false, 0);
        hyperBag.generateSubBagsSVQ(false,0.0f);
        hyperBook.generateCodebookSVQ(hyperBag, 20, "random", false);
        hyperBag.generateBag(1,1);  /* Test three normalization options */
        hyperBag.generateBag(1,2);
        hyperBag.generateBag(1,3);
        hyperBook.saveHyperCodebook("tmpbook.txt");
        hyperBook2.loadHyperCodebook("tmpbook.txt");
        Writer writer = new Writer("tmp.arff", OWReaderARF3.getRelation(), DM3ARF, true, false);
        writer.writeFile(hyperBag);
        
        deleteTmpFile("tmpbook.txt");
        deleteTmpFile("tmp.arff");
        
        try {
            final double delta = 1E-4;
            
            /* Check writing of hyper codebook */
            boolean bStandardized1 = hyperBook.isStandardized();
            boolean bStandardized2 = hyperBook2.isStandardized();
            boolean bLog1 = hyperBook.getLogWeighting();
            boolean bLog2 = hyperBook2.getLogWeighting();
            boolean bIDF1 = hyperBook.getIDFWeighting();
            boolean bIDF2 = hyperBook2.getIDFWeighting();
            int size1 = hyperBook.getSize();
            int size2 = hyperBook2.getSize();
            
            Assert.assertEquals("IsStandardized", bStandardized1, bStandardized2);
            Assert.assertEquals("Log", bLog1, bLog2);
            Assert.assertEquals("IDF", bIDF1, bIDF2);
            Assert.assertEquals("Codebook size", size1, size2);
            
            float[][] cb1 = ((CodebookNumeric)hyperBook.getCodebookSVQ()).getCodebook();
            float[][] cb2 = ((CodebookNumeric)hyperBook2.getCodebookSVQ()).getCodebook();
            float[] dff1 = hyperBook.getDocumentFrequencyFactors();
            float[] dff2 = hyperBook2.getDocumentFrequencyFactors();
            
            for (int m=0; m < cb1.length; m++) {
                Assert.assertEquals("Document frequency factor " + m, dff1[m], dff2[m], delta);
                for (int n=0; n < cb1[0].length; n++) {
                    Assert.assertEquals("Codebook element " + m + " " + n, cb1[m][n], cb2[m][n], delta);
                }
            }
        } catch (AssertionError e) {
            System.err.println("Error in SVQ test: " + e);
        }
        
        System.out.println(" finished!");        
    }
    
    
    @Test
    public void testRun2() {
        System.out.print("Test: Run with mixed numeric and text input file ...");
        
        /* Check Attributes class */
        Reader OWReaderARF4a = new Reader("JUnitTestData/testdata4.arff","");
        Reader OWReaderARF4b = new Reader("JUnitTestData/testdata4.arff","n11110c");
        OWReaderARF4a.readFile();
        OWReaderARF4b.readFile();
        
        DataManager DM4a = new DataManager(OWReaderARF4a);
        DataManager DM4b = new DataManager(OWReaderARF4b);
        DM4a.generateMappings();
        DM4b.generateMappings();
        
        try {
            Assert.assertEquals("Data size", OWReaderARF4a.inputData.size(), OWReaderARF4b.inputData.size());
            Assert.assertEquals("Map name size", DM4a.getMappingIDName().size(), DM4b.getMappingIDName().size());
            Assert.assertEquals("Map label size", DM4a.getMappingIDLabels().size(), DM4b.getMappingIDLabels().size());
            Assert.assertEquals("Map name - Data crosscheck size", DM4a.getMappingIDName().size(), OWReaderARF4b.inputData.size());
            Assert.assertEquals("Map label - Data crosscheck size", DM4a.getMappingIDLabels().size(), OWReaderARF4b.inputData.size());
            for (int k=1; k < DM4a.getMappingIDName().size(); k++) {
                Assert.assertEquals("Mapping name id " + k, DM4a.getMappingIDName().get(k), DM4b.getMappingIDName().get(k));
                Assert.assertEquals("Mapping label id " + k, DM4a.getMappingIDLabels().get(k)[0], DM4b.getMappingIDLabels().get(k)[0]);
            }
        } catch (AssertionError e) {
            System.err.println("Error in reading mixed data: " + e);
        }
        
        int nGram = 2;
        int nCharGram = 0;
        int minTermFreq = 2;
        int stopping = 0;
        HyperCodebook hyperBook  = new HyperCodebook(DM4a, 0, false, false, "", nGram, nCharGram);
        HyperCodebook hyperBook2 = new HyperCodebook(DM4a, 0, false, false, "", nGram, nCharGram);
        Preprocessor preProc = new Preprocessor(DM4a, hyperBook, false, 0, 0, false, false);
        preProc.preprocessInput();
        HyperBag hyperBag = new HyperBag(DM4a, hyperBook, "", nGram, nCharGram);
        hyperBook.generateCodebook(20, "random", false, 0, minTermFreq, stopping);
        hyperBag.generateBag(2, 0);
        hyperBook.saveHyperCodebook("tmpbook.txt");
        hyperBook2.loadHyperCodebook("tmpbook.txt");
        Writer writer = new Writer("tmp.arff", OWReaderARF4a.getRelation(), DM4a, true, false);
        writer.writeFile(hyperBag);
        
        deleteTmpFile("tmpbook.txt");
        deleteTmpFile("tmp.arff");
        
        try {
            final double delta = 1E-4;
            
            /* Check writing of hyper codebook */
            boolean bStandardized1 = hyperBook.isStandardized();
            boolean bStandardized2 = hyperBook2.isStandardized();
            boolean bLog1 = hyperBook.getLogWeighting();
            boolean bLog2 = hyperBook2.getLogWeighting();
            boolean bIDF1 = hyperBook.getIDFWeighting();
            boolean bIDF2 = hyperBook2.getIDFWeighting();
            int size1 = hyperBook.getSize();
            int size2 = hyperBook2.getSize();
            
            Assert.assertEquals("IsStandardized", bStandardized1, bStandardized2);
            Assert.assertEquals("Log", bLog1, bLog2);
            Assert.assertEquals("IDF", bIDF1, bIDF2);
            Assert.assertEquals("Codebook size", size1, size2);
            
            List<Codebook> cb1 = hyperBook.getCodebooks();
            List<Codebook> cb2 = hyperBook2.getCodebooks();
            String[]  cb10 = ((CodebookText)cb1.get(0)).getCodebook();
            float[][] cb11 = ((CodebookNumeric)cb1.get(1)).getCodebook();
            String[]  cb20 = ((CodebookText)cb2.get(0)).getCodebook();
            float[][] cb21 = ((CodebookNumeric)cb2.get(1)).getCodebook();
            
            for (int m=0; m < cb10.length; m++) {
                Assert.assertEquals("Codebook text element " + m, cb10[m], cb20[m]);
            }
            for (int m=0; m < cb11.length; m++) {
                for (int n=0; n < cb11[0].length; n++) {
                    Assert.assertEquals("Codebook element " + m + " " + n, cb11[m][n], cb21[m][n], delta);
                }
            }
        } catch (AssertionError e) {
            System.err.println("Error in mixed BoW and BoF: " + e);
        }
        
        System.out.println(" finished!");        
    }
    
    
    @Test
    public void testBag() {
        System.out.print("Test: regression test BoF ...");
        
        Reader rInput = new Reader("JUnitTestData/random1_wLabels.arff","",false);
        rInput.readFile();
        Reader rRef = new Reader("JUnitTestData/boaw_a2size50_random1.arff","",false);
        rRef.readFile();
        
        DataManager DMinput = new DataManager(rInput); 
        DataManager DMref   = new DataManager(rRef);
        DMinput.generateMappings();
        DMref.generateMappings();
        
        HyperCodebook cb = new HyperCodebook(DMinput, 0, false, false, "", 1, 1);
        cb.generateCodebook(50, "random", false, 0, 0, 0);
        
        HyperBag bagOfFeatures = new HyperBag(DMinput, cb);
        bagOfFeatures.generateBag(2,0);
        
        try {
            Assert.assertEquals("Num IDs", rRef.inputData.size(), bagOfFeatures.getBag().bof.length);
            Assert.assertEquals("Codebook size", rRef.inputData.get(0).length-2, bagOfFeatures.getBag().bof[0].length);  /* subtract name and label */
            for (int id=0; id < bagOfFeatures.getBag().bof.length; id++) {
                for (int k=0; k < bagOfFeatures.getBag().bof[0].length; k++) {
                    Assert.assertEquals("Bag of Features id=" + id + " k=" + k, rRef.inputData.get(id)[k+1], bagOfFeatures.getBag().bof[id][k]);
                }
            }
        } catch (AssertionError e) {
            System.err.println("Error in Reader: " + e);
        }
        System.out.println(" finished!");        
    }
    
    
    private void deleteTmpFile(String filename) {
        Path path = null;
        try { /* Delete temporary files */
            path = FileSystems.getDefault().getPath(filename);
            Files.delete(path);
        } catch (NoSuchFileException x) {
            System.err.format("%s: no such" + " file or directory%n", path);
        } catch (DirectoryNotEmptyException x) {
            System.err.format("%s not empty%n", path);
        } catch (IOException x) {  /* File permission problems are caught here. */
            System.err.println(x);
        }
    }
}
