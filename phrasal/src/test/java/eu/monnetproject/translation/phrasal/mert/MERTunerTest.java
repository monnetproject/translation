/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.phrasal.mert;

import eu.monnetproject.translation.LanguageModel;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.phrasal.PhrasalDecoder;
import eu.monnetproject.translation.phrasal.StringLabel;
import eu.monnetproject.translation.phrasal.pt.PhraseTableEntryImpl;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.DecoderWeights;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.corpus.ParallelCorpus;
import eu.monnetproject.translation.corpus.ParallelDocument;
import eu.monnetproject.translation.corpus.SentencePair;
import eu.monnetproject.translation.tune.TranslatorSetup;
import java.util.Collections;
import java.util.Iterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class MERTunerTest {

    public MERTunerTest() {
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
     * Test of tune method, of class MERTuner.
     */
    @Test
    public void testTune() throws Exception {
        System.out.println("tune");
        Configurator.setConfig("eu.monnetproject.translation.phrasal", "LinearDistortion", "0.3",
                "p(t|f)", "0.2",
                // Here is the mistake, this value should of course never be negative
                "LM", "-0.01",
                "WordPenalty", "-1",
                "UnknownWord", "1.0",
                "SentenceBoundary", "1.0");
        Configurator.setConfig("eu.monnetproject.translation.phrasal.pt", "de/en/1", "src/test/resources/sample-models/phrase-model/phrase-table.sorted");
        Configurator.setConfig("eu.monnetproject.translation.phrasal.lm", "en", "src/test/resources/sample-models/lm/europarl.srilm.gz");
        TranslatorSetup setup = new DefaultTranslatorSetup(Language.GERMAN, Language.ENGLISH);
        ParallelCorpus corpus = new ParallelCorpusImpl();
        int n = 5;
        MERTuner instance = new MERTuner();
        return;
//        DecoderWeights wts = instance.tune(setup, corpus, new MetricWrapperFactory(), "BLEU-2", n);
//        System.out.println("decodeWithWts");
//        List<String> phrase = Arrays.asList(new String[]{"das", "ist", "ein", "kleines", "haus"});
//        PhraseTable phraseTable = makePhraseTable();
//        int nBest = 2;
//        final LanguageModel lm = setup.languageModel();
//        PhrasalDecoder decoder = new PhrasalDecoder(lm,wts);
//        List<Translation> result = decoder.decode(phrase, phraseTable, Arrays.asList("p(t|f)"),nBest);
//        String expResult = "this is a small house";
//        assertEquals(expResult, result.get(0).getTargetLabel().asString());
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
    
    private static class ParallelCorpusImpl implements ParallelCorpus {

        @Override
        public Iterator<ParallelDocument> iterator() {
            return Collections.singletonList((ParallelDocument)new ParallelDocument() {

                @Override
                public Iterator<SentencePair> iterator() {
                    return Collections.singletonList((SentencePair)new SentencePairImpl()).iterator();
                }
            }).iterator();
        }
        
    }
    
    private static class SentencePairImpl implements SentencePair {

        @Override
        public String getSourceSentence() {
            return "das ist ein kleines haus";
        }

        @Override
        public String getTargetSentence() {
            return "that is a small house";
        }
        
        
    }
}
