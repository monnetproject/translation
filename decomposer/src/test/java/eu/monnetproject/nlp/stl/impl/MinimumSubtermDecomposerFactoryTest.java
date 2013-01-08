package eu.monnetproject.nlp.stl.impl;

import eu.monnetproject.lang.Language;
import eu.monnetproject.nlp.stl.Decomposer;
import eu.monnetproject.nlp.stl.impl.MinimumSubtermDecomposerFactory;
import eu.monnetproject.config.Configurator;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.List;
import java.util.Properties;
import org.junit.Test;
import org.junit.Assert;

/**
 *
 * @author Tobias Wunner
 */
public class MinimumSubtermDecomposerFactoryTest {


    public MinimumSubtermDecomposerFactoryTest() {
      Configurator.setConfig("eu.monnetproject.nlp.stl","termbase.lucene.index.de","src/test/resources/index/de","termbase.lucene.index.nl","src/test/resources/index/nl");
    }

    /**
     * Test decomposer factory for German
     */
    @Test
    public void testDecomposerFactoryGerman() throws Exception {
      String lang = "de";
      System.out.println("Test decomposer factory for "+lang);
      String term = "Kraftfahrzeughaftpflichtversicherung";
      MinimumSubtermDecomposerFactory factory = new MinimumSubtermDecomposerFactory();
      MinimumSubtermDecomposer decomposer = factory.makeDecomposer(lang);
      List<String> decompositions = decomposer.decomposeBest(term);
      Assert.assertTrue(decompositions.size()>1);
    //  term = "Lizenzen an Rechten";
     // Assert.assertTrue(decomposer.decomposeBest(term).size() > 1);
    }

    /**
     * Test decomposer factory for Dutch
     */
    @Test
    public void testDecomposerFactoryDutch() throws Exception {
      String lang = "nl";
      System.out.println("Test decomposer factory for "+lang);
      //String term = "Remigratieregeling";
      String term = "Aardappel";
      MinimumSubtermDecomposerFactory factory = new MinimumSubtermDecomposerFactory();
      MinimumSubtermDecomposer decomposer = factory.makeDecomposer(lang);
      List<String> decompositions = decomposer.decomposeBest(term);
      Assert.assertTrue(decompositions.size()>1);
    }

}
