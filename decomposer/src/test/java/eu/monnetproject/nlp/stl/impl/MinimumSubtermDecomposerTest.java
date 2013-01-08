package eu.monnetproject.nlp.stl.impl;

import eu.monnetproject.nlp.stl.Termbase;
import eu.monnetproject.config.Configurator;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Tobias Wunner
 */
public class MinimumSubtermDecomposerTest {

    public static final String TXTDIR = "src/test/resources/txt";

    public MinimumSubtermDecomposerTest() {
      Configurator.setConfig("eu.monnetproject.nlp.stl","termbase.lucene.index.de","src/test/resources/index/de","termbase.lucene.index.nl","src/test/resources/index/nl");
    }

    public static Termbase createTermbaseSimple(String language, String[] terms) {
      TermbaseImpl termbaseimpl = new TermbaseImpl(language);
      for(String term:terms)
        termbaseimpl.add(term);
      return termbaseimpl;
    }

    /**
     * Test decomposer with simple termbase implementation
     */
    @Test
    public void testDecomposerSimple() throws Exception {
      System.out.println("Test decomposer simple");
      String[] terms = {"tangible","intangible","fixed","asset","assets"};
      Termbase termbase = createTermbaseSimple("en",terms);
      MinimumSubtermDecomposer decomposer = new MinimumSubtermDecomposer(termbase);
      System.out.println(decomposer.decomposeBest("intangible fixed assets"));
    }

    /**
     * Test decomposer with German Lucene Index - simple test
     */
//    @Test
//    public void testDecomposerLuceneDeSimple() throws Exception {
//      System.out.println("Test decomposer lucene simple");
//      MinimumSubtermDecomposerFactory factory = new MinimumSubtermDecomposerFactory();
//      MinimumSubtermDecomposer decomposer = factory.makeDecomposer("de");
//      String[] terms = {"immaterielle Vermögensgegenstände",
//                        "immaterielle Vermögensgegenstände",
//                        "Lizenzen an Rechten",
//                        "Rechten und Werten",
//                        "EDV-Software",
//                        "Lizenzen an Rechten und Werten"};
//      for(String term:terms) {
//        List<String> decomp = decomposer.decomposeBest(term);
//        if(decomp.size() <= 1) {
//            System.out.println("FAIL:" + term);
//        }
//        assertTrue(decomp.size()>1);
//      }
//    }

    /**
     * Test decomposer with German Lucene Index - extended test
     */
    @Test
    public void testDecomposerLuceneDeExtended() throws Exception {
      System.out.println("Test decomposer lucene de extended");
      MinimumSubtermDecomposerFactory factory = new MinimumSubtermDecomposerFactory();
      MinimumSubtermDecomposer decomposer = factory.makeDecomposer("de");
      String testtermsfn = TXTDIR+"/de/stw100longest";
      String term = "";
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(testtermsfn), "UTF-8"));
      while( (term = reader.readLine()) != null ) {
        List<String> decomp = decomposer.decomposeBest(term);
        if (decomp.size()<2)
          System.out.println("No decomposition found for: "+term);
      }
    }

    /**
     * Test decomposer with Dutch Lucene Index - extended test
     */
    @Test
    public void testDecomposerLuceneNlExtended() throws Exception {
      System.out.println("Test decomposer lucene nl extended");
      MinimumSubtermDecomposerFactory factory = new MinimumSubtermDecomposerFactory();
      MinimumSubtermDecomposer decomposer = factory.makeDecomposer("nl");
      String testtermsfn = TXTDIR+"/nl/dict100longest";
      String term = "";
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(testtermsfn), "UTF-8"));
      while( (term = reader.readLine()) != null ) {
        List<String> decomp = decomposer.decomposeBest(term);
        if (decomp.size()<2)
          System.out.println("No decomposition found for: "+term);
      }
    }

    @Test
    public void testRecase() throws Exception {
        final List<String> expResult1 = Arrays.asList("DAMPF","SCHIFF","FAHRT");
        final List<String> result1 = MinimumSubtermDecomposer.recase(Arrays.asList("dampf","schiff","fahrt"), "DAMPFSCHIFFFAHRT");
        Assert.assertEquals(expResult1.size(),result1.size());
        for(int i = 0; i < expResult1.size(); i++) {
            Assert.assertEquals(expResult1.get(i), result1.get(i));
        }
        
        final List<String> expResult2 = Arrays.asList("Dampf","Schiff","Fahrt");
        final List<String> result2 = MinimumSubtermDecomposer.recase(Arrays.asList("dampf","schiff","fahrt"), "Dampfschifffahrt");
        Assert.assertEquals(expResult2.size(),result2.size());
        for(int i = 0; i < expResult2.size(); i++) {
            Assert.assertEquals(expResult2.get(i), result2.get(i));
        }
        final List<String> expResult3 = Arrays.asList("dampf","schiff","fahrt");
        final List<String> result3 = MinimumSubtermDecomposer.recase(Arrays.asList("dampf","schiff","fahrt"), "dampfschifffahrt");
        Assert.assertEquals(expResult3.size(),result3.size());
        for(int i = 0; i < expResult3.size(); i++) {
            Assert.assertEquals(expResult3.get(i), result3.get(i));
        }
        
    }

}
