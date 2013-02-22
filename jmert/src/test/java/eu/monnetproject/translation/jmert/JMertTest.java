/**********************************************************************************
 * Copyright (c) 2011, Monnet Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Monnet Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
package eu.monnetproject.translation.jmert;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.framework.services.Services;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.*;
import eu.monnetproject.translation.corpus.ParallelCorpus;
import eu.monnetproject.translation.corpus.ParallelDocument;
import eu.monnetproject.translation.corpus.SentencePair;
import eu.monnetproject.translation.phrasal.PhrasalDecoder;
import eu.monnetproject.translation.phrasal.eval.MetricWrapperFactory;
import eu.monnetproject.translation.phrasal.mert.DefaultTranslatorSetup;
import eu.monnetproject.translation.phrasal.pt.PhraseTableEntryImpl;
import eu.monnetproject.translation.tune.TranslatorSetup;
import eu.monnetproject.translation.tune.Tuner;
import java.util.*;
import org.junit.After;
import org.junit.Test;

/**
 *
 * @author John McCrae
 */
public class JMertTest {

    public JMertTest() {
    }

    @After
    public void tearDown() {
    }

    
    
    @Test
    public void testTune() throws Exception {
        System.out.println("tune");
        Configurator.setConfig("eu.monnetproject.translation.phrasal", "LinearDistortion", "0.3",
                "TM\\:p(t|f)", "0.2",
                // Here is the mistake, this value should of course never be negative
                "LM", "-0.01",
                "WordPenalty", "-1");
               // "UnknownWord", "1.0",
               // "SentenceBoundary", "1.0");
        Configurator.setConfig("eu.monnetproject.translation.phrasal.pt", "de/en/1", "src/test/resources/sample-models/phrase-model/phrase-table.sorted");
        Configurator.setConfig("eu.monnetproject.translation.phrasal.lm", "en", "src/test/resources/sample-models/lm/europarl.srilm.gz");
        class MockSetup extends DefaultTranslatorSetup {

            public MockSetup() {
                super(Language.GERMAN, Language.ENGLISH);
            }

            @Override
            public DecoderWeights weights() {
                final DecoderWeights wts = super.weights();
                wts.put("TM:p(t|f)",1.0);
                return wts;
            }
        }
        TranslatorSetup setup = new MockSetup();
        ParallelCorpus corpus = new ParallelCorpusImpl();
        int n = 5;
        Tuner instance = new JMert(Services.get(TokenizerFactory.class));
        DecoderWeights wts = instance.tune(setup, corpus, new MetricWrapperFactory(), "BLEU-2", n, OntologyTranslator.DECODE_FAST);
        System.out.println("decodeWithWts");
        List<String> phrase = Arrays.asList(new String[]{"das", "ist", "ein", "kleines", "haus"});
        PhraseTable phraseTable = makePhraseTable();
        int nBest = 2;
        final LanguageModel lm = setup.languageModel();
        PhrasalDecoder decoder = new PhrasalDecoder(lm,wts);
        List<Translation> result = decoder.decode(phrase, phraseTable, Arrays.asList("TM:p(t|f)"),nBest);
        String expResult = "this is a small house";
//        assertEquals(expResult, result.get(0).getTargetLabel().asString());
    }

    public PhraseTable makePhraseTable() {
        final MockPhraseTable pt = new MockPhraseTable();
        pt.add(new PhraseTableEntryImpl(new StringLabel("altes", Language.GERMAN), new StringLabel("old", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.2) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("alt", Language.GERMAN), new StringLabel("old", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.8) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("das ist", Language.GERMAN), new StringLabel("it is", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.2) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("das ist", Language.GERMAN), new StringLabel("this is", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.8) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("das", Language.GERMAN), new StringLabel("it", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.1) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("das", Language.GERMAN), new StringLabel("the", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.4) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("das", Language.GERMAN), new StringLabel("this", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.1) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("der", Language.GERMAN), new StringLabel("the", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.3) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("die", Language.GERMAN), new StringLabel("the", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.3) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("ein", Language.GERMAN), new StringLabel("a", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("ein", Language.GERMAN), new StringLabel("an", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("es gibt", Language.GERMAN), new StringLabel("there is", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("es ist", Language.GERMAN), new StringLabel("it is", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.8) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("es ist", Language.GERMAN), new StringLabel("this is", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.2) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("gibt", Language.GERMAN), new StringLabel("gives", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("haus", Language.GERMAN), new StringLabel("house", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("ist", Language.GERMAN), new StringLabel("is", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("ist", Language.GERMAN), new StringLabel("'s", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",1.0) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("kleines", Language.GERMAN), new StringLabel("little", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.2) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("kleines", Language.GERMAN), new StringLabel("small", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.2) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("klein", Language.GERMAN), new StringLabel("little", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.8) }, null));
        pt.add(new PhraseTableEntryImpl(new StringLabel("klein", Language.GERMAN), new StringLabel("small", Language.ENGLISH), new Feature[]{ new Feature("TM:p(t|f)",0.8) }, null));
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