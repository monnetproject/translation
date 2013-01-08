/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.phrasal;

import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.phrasal.lm.ARPALanguageModel;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.PhraseTableEntry;
import java.io.IOException;
import java.util.LinkedList;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.phrasal.pt.PhraseTableEntryImpl;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author jmccrae
 */
public class PhrasalDecoderTest {

    public PhrasalDecoderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        lm = new ARPALanguageModel("src/test/resources/sample-models/lm/europarl.srilm.gz", Language.ENGLISH);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    static ARPALanguageModel lm;
    
    @Before
    public void setUp() throws Exception {
    }
    
    @After
    public void tearDown() {
    }

    private static final List<String> features = Arrays.asList("p(e|f)");
    
    /**
     * Test of decode method, of class PhrasalDecoder.
     */
    @Test
    public void testDecode() throws IOException, ClassNotFoundException {
        System.out.println("decode");
        List<String> phrase = Arrays.asList(new String[]{"das", "ist", "ein", "kleines", "haus"});
        PhraseTable phraseTable = makePhraseTable();
        int nBest = 2;
        System.setProperty("eu.monnetproject.translation.phrasal", PhrasalDecoder.DISTORTION_WT_OPT +"=1\n"
                + PhrasalDecoder.LANGUAGE_MODEL_WT_OPT + "=1\n"
                + PhrasalDecoder.TRANSLATION_MODEL_WT_OPT + "=1\n"
                + PhrasalDecoder.WORD_PENALTY_WT_OPT + "=0\n");
        PhrasalDecoder instance = new PhrasalDecoder(lm,PhrasalDecoderFactory.oneWeight);
        List<Translation> result = instance.decode(phrase, phraseTable, features,nBest);
        String expResult = "this is a small house";
        assertEquals(expResult, result.get(0).getTargetLabel().asString());
    }
    
    @Test
    public void testDecodeWithWts() throws IOException, ClassNotFoundException {
        System.out.println("decodeWithWts");
        List<String> phrase = Arrays.asList(new String[]{"das", "ist", "ein", "kleines", "haus"});
        PhraseTable phraseTable = makePhraseTable();
        int nBest = 2;
        //final ARPALanguageModel lm = new ARPALanguageModel("src/test/resources/sample-models/lm/europarl.srilm.gz", Language.ENGLISH);
        System.setProperty("eu.monnetproject.translation.phrasal", PhrasalDecoder.DISTORTION_WT_OPT +"=1\n"
                + PhrasalDecoder.LANGUAGE_MODEL_WT_OPT + "=1\n"
                + PhrasalDecoder.TRANSLATION_MODEL_WT_OPT + "=1\n"
                + PhrasalDecoder.WORD_PENALTY_WT_OPT + "=0\n");
        final DecoderWeightsImpl wts = new DecoderWeightsImpl();
        PhrasalDecoder instance = new PhrasalDecoder(lm,PhrasalDecoderFactory.oneWeight);
        List<Translation> result = instance.decode(phrase, phraseTable,features, nBest);
        String expResult = "this is a small house";
        assertEquals(expResult, result.get(0).getTargetLabel().asString());
    }
    
    @Test
    public void testDecodeBrackets() throws IOException, ClassNotFoundException {
        System.out.println("testDecodeBrackets");
        
        List<String> phrase = Arrays.asList(new String[]{"das", "ist",  "(", "ein", ")", "kleines",  "haus"});
        PhraseTable phraseTable = makePhraseTable();
        int nBest = 100;
        //final ARPALanguageModel lm = new ARPALanguageModel("src/test/resources/sample-models/lm/europarl.srilm.gz", Language.ENGLISH);
        System.setProperty("eu.monnetproject.translation.phrasal", PhrasalDecoder.DISTORTION_WT_OPT +"=1\n"
                + PhrasalDecoder.LANGUAGE_MODEL_WT_OPT + "=1\n"
                + PhrasalDecoder.TRANSLATION_MODEL_WT_OPT + "=1\n"
                + PhrasalDecoder.WORD_PENALTY_WT_OPT + "=0\n");
        final DecoderWeightsImpl wts = new DecoderWeightsImpl();
        PhrasalDecoder instance = new PhrasalDecoder(lm,PhrasalDecoderFactory.oneWeight);
        List<Translation> result = instance.decode(phrase, phraseTable, features, nBest);
        String expResult = "( the house is a little )";
        for(Translation t : result) {
            System.out.println(t.getTargetLabel().asString() + "(" + t.getScore()+")");
        }
        assertEquals(expResult, result.get(0).getTargetLabel().asString());
    }

    @Test
    public void testDecodeWithCase() throws IOException, ClassNotFoundException {
        System.out.println("test decode with case");
        PhraseTable phraseTable = makePhraseTableWithCase();
        List<String> phrase = Arrays.asList(new String[]{"Das", "ist", "ein", "Schiff"});
        int nBest = 2;
        final ARPALanguageModel lm = new ARPALanguageModel("src/test/resources/sample-models/lm/europarl.srilm.gz", Language.ENGLISH);
        System.setProperty("eu.monnetproject.translation.phrasal", PhrasalDecoder.DISTORTION_WT_OPT +"=1\n"
                + PhrasalDecoder.LANGUAGE_MODEL_WT_OPT + "=1\n"
                + PhrasalDecoder.TRANSLATION_MODEL_WT_OPT + "=1\n"
                + PhrasalDecoder.WORD_PENALTY_WT_OPT + "=0\n");
        final DecoderWeightsImpl wts = new DecoderWeightsImpl();
        PhrasalDecoder instance = new PhrasalDecoder(lm,PhrasalDecoderFactory.oneWeight);
        List<Translation> result = instance.decode(phrase, phraseTable, features, nBest);
        String expResult = "it is a ship";
        //System.out.println(result.get(0).getTargetLabel().asString());
        assertEquals(expResult, result.get(0).getTargetLabel().asString());
    }

    public PhraseTable makePhraseTableWithCase() {
        final MockPhraseTable pt = new MockPhraseTable();
        pt.add(new PhraseTableEntryImpl(new StringLabel("Das ist", Language.GERMAN), new StringLabel("it is", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.2) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("das ist", Language.GERMAN), new StringLabel("this is", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.8) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("das", Language.GERMAN), new StringLabel("it", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.1) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("ist", Language.GERMAN), new StringLabel("is", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("ist", Language.GERMAN), new StringLabel("'s", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("ein", Language.GERMAN), new StringLabel("a", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("schiff", Language.GERMAN), new StringLabel("schiff", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("Schiff", Language.GERMAN), new StringLabel("ship", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.2) }, null));
        return pt;
    }

    public PhraseTable makePhraseTable() {
        final MockPhraseTable pt = new MockPhraseTable();
        pt.add(new PhraseTableEntryImpl(new StringLabel("altes", Language.GERMAN), new StringLabel("old", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.2) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("alt", Language.GERMAN), new StringLabel("old", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.8) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("das ist", Language.GERMAN), new StringLabel("it is", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.2) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("das ist", Language.GERMAN), new StringLabel("this is", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.8) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("das", Language.GERMAN), new StringLabel("it", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.1) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("das", Language.GERMAN), new StringLabel("the", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.4) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("das", Language.GERMAN), new StringLabel("this", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.1) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("der", Language.GERMAN), new StringLabel("the", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.3) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("die", Language.GERMAN), new StringLabel("the", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.3) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("ein", Language.GERMAN), new StringLabel("a", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("ein", Language.GERMAN), new StringLabel("an", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("es gibt", Language.GERMAN), new StringLabel("there is", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("es ist", Language.GERMAN), new StringLabel("it is", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.8) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("es ist", Language.GERMAN), new StringLabel("this is", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.2) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("gibt", Language.GERMAN), new StringLabel("gives", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("haus", Language.GERMAN), new StringLabel("house", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("ist", Language.GERMAN), new StringLabel("is", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("ist", Language.GERMAN), new StringLabel("'s", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("kleines", Language.GERMAN), new StringLabel("little", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.2) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("kleines", Language.GERMAN), new StringLabel("small", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.2) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("klein", Language.GERMAN), new StringLabel("little", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.8) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("klein", Language.GERMAN), new StringLabel("small", Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",0.8) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("(",Language.GERMAN), new StringLabel("(",Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel(")",Language.GERMAN), new StringLabel(")",Language.ENGLISH), new Feature[]{ new Feature("p(e|f)",1.0) }, null));
        return pt;
    }

    private static class MockPhraseTable extends LinkedList<PhraseTableEntry> implements PhraseTable {

        @Override
        public Language getForeignLanguage() {
            return Language.GERMAN;
        }

        @Override
        public Language getTranslationLanguage() {
            return Language.ENGLISH;
        }

        @Override
        public String getName() {
            return "Mock";
        }

        @Override
        public int getLongestForeignPhrase() {
            return 2;
        }
    }
}
