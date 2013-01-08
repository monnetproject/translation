/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.controller.evaluate.nfolds;

import java.net.URI;
import eu.monnetproject.lemon.LemonModel;
import eu.monnetproject.lemon.LemonSerializer;
import eu.monnetproject.lemon.model.Lexicon;
import eu.monnetproject.translation.evaluation.evaluate.FoldLexicon;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class FoldLexiconTest {
    
    public FoldLexiconTest() {
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
     * Test of fold method, of class FoldLexicon.
     */
    @Test
    public void testFold() {
        System.out.println("fold");
        LemonModel model = LemonSerializer.newInstance().create(null);
        Lexicon lexicon = model.addLexicon(URI.create("file:test#lexicon"), "en");
        lexicon.addEntry(model.getFactory().makeLexicalEntry(URI.create("file:test#entry1")));
        lexicon.addEntry(model.getFactory().makeLexicalEntry(URI.create("file:test#entry2")));
        lexicon.addEntry(model.getFactory().makeLexicalEntry(URI.create("file:test#entry3")));
        lexicon.addEntry(model.getFactory().makeLexicalEntry(URI.create("file:test#entry4")));
        Lexicon result = FoldLexicon.fold(lexicon, 0.25,0.5);
        assertEquals(2,result.getEntrys().size());
    }
}
