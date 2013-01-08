/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.jsonpt;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.TranslationSource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class JSONPhraseTableSourceFactoryTest {
    
    public JSONPhraseTableSourceFactoryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getSource method, of class JSONPhraseTableSourceFactory.
     */
    @Test
    public void testGetSource() {
        System.out.println("getSource");
        Configurator.setConfig("eu.monnetproject.translation.jsonpt", "en", "src/test/resources/fin_with_probs.json");
        Language srcLang = Language.ENGLISH;
        Language trgLang = Language.SPANISH;
        JSONPhraseTableSourceFactory instance = new JSONPhraseTableSourceFactory();
        TranslationSource result = instance.getSource(srcLang, trgLang);
        final PhraseTable candidates = result.candidates(new Chunk() {

                                @Override
                                public String getSource() {
                                    return "Routing";
                                }
                            });
        assertTrue(candidates.iterator().hasNext());
    }
    
    
    @Test
    public void testGetSource2() {
        System.out.println("getSource2");
        Configurator.setConfig("eu.monnetproject.translation.jsonpt", "en", "src/test/resources/finance_en_de.json");
        Language srcLang = Language.ENGLISH;
        Language trgLang = Language.SPANISH;
        JSONPhraseTableSourceFactory instance = new JSONPhraseTableSourceFactory();
        TranslationSource result = instance.getSource(srcLang, trgLang);
        final PhraseTable candidates = result.candidates(new Chunk() {

                                @Override
                                public String getSource() {
                                    return "Routing";
                                }
                            });
        assertTrue(candidates.iterator().hasNext());
    }
}
