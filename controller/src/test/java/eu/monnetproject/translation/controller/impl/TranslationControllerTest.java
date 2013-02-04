package eu.monnetproject.translation.controller.impl;

import eu.monnetproject.lang.Language;
import eu.monnetproject.lemon.LemonModel;
import eu.monnetproject.lemon.LemonModels;
import eu.monnetproject.lemon.LemonSerializer;
import eu.monnetproject.lemon.model.Lexicon;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.ontology.OntologySerializer;
import eu.monnetproject.config.Configurator;
import eu.monnetproject.framework.services.Services;
import eu.monnetproject.translation.*;
import java.io.File;
import java.io.IOException;
//import eu.monnetproject.translation.phrasal.lm.ARPALanguageModelFactory;
//import eu.monnetproject.translation.phrasal.pt.MemoryMappedPhraseTableSourceFactory;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class TranslationControllerTest {

    public TranslationControllerTest() {
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
     * Test of translate method, of class TranslationController.
     */
    @Test
    public void testTranslate() {
        System.out.println("translate");
        final OntologySerializer ontoSerializer = Services.get(OntologySerializer.class);
        Ontology ontology = ontoSerializer.create(URI.create("file:test"));
        ontology.addClass(ontology.getFactory().makeClass(URI.create("file:test#example")));
        final LemonSerializer lemonSerializer = LemonSerializer.newInstance();
        final LemonModel model = lemonSerializer.create(null);
        final Lexicon lexicon = model.addLexicon(URI.create("file:test#lexicon_de"), Language.GERMAN.toString());
        LemonModels.addEntryToLexicon(lexicon, URI.create("fiele:test#entry_de"), "das ist ein kleines haus", URI.create("file:test#example"));
        Collection<Lexicon> sourceLexicons = Collections.singletonList(lexicon);
        Lexicon targetLexicon = model.addLexicon(URI.create("file:test#lexicon_en"), Language.ENGLISH.toString());
        Collection<URI> scope = null;
        String namePrefix = "file:test#";
        int nBest = 1;
        Configurator.setConfig("eu.monnetproject.translation.phrasal", "weight-d", "1", "weight-t", "1", "weight-l", "1", "weight-w", "0");
        Configurator.setConfig("eu.monnetproject.translation.langmodel", "en", "src/test/resources/sample-models/lm/europarl.srilm.gz","method","mem");
        Configurator.setConfig("eu.monnetproject.translation.phrasal.pt", "de/en/1", "src/test/resources/sample-models/phrase-model/phrase-table.sorted");
        Configurator.setConfig("eu.monnetproject.translation.topics.lda");
        final TranslationController instance = new TranslationController(Collections.singleton((LanguageModelFactory)new MockLanguageModelFactory()),
                Services.get(DecoderFactory.class),
                Services.getAll(TranslationPhraseChunkerFactory.class),
		Collections.singletonList((TranslationSourceFactory)new MockTranslationSourceFactory()),
                Services.getAll(TranslationFeaturizerFactory.class),
                Services.getFactory(TokenizerFactory.class),
                Services.getAll(TranslationConfidenceFactory.class));
        instance.translate(ontology, sourceLexicons, targetLexicon, scope, namePrefix, nBest);
        assertEquals(1, targetLexicon.getEntrys().size());
    }
    
    private static final class MockPhraseTableEntry implements PhraseTableEntry {

        private final String foreign, translation;
        private final double score;

        public MockPhraseTableEntry(String foreign, String translation, double score) {
            this.foreign = foreign;
            this.translation = translation;
            this.score = score;
        }

        @Override
        public double getApproxScore() {
            return score;
        }
        
        
        
        @Override
        public Label getForeign() {
            return new MockLabel(foreign, Language.GERMAN);
        }

        @Override
        public Label getTranslation() {
            return new MockLabel(translation, Language.ENGLISH);
        }

        @Override
        public Feature[] getFeatures() {
            return new Feature[] { new Feature("p(t|f)", score) };
        }
        
    }
    
    private static final class MockTranslationSource implements TranslationSource {
        private final String[] featureNames = {
          "p(t|f)"
        };
        
        private final static HashMap<String,MockPhraseTableEntry> translations = new HashMap<String,MockPhraseTableEntry>();
        
        static {
            try {
                final Scanner scanner = new Scanner(new File("src/test/resources/sample-models/phrase-model/phrase-table.sorted")).useDelimiter("\\|\\|\\|");
                while(scanner.hasNext()) {
                    final String foreign = scanner.next().trim();
                    if(foreign.equals("")) {
                        break;
                    }
                    translations.put(foreign, new MockPhraseTableEntry(foreign, scanner.next().trim(), Double.parseDouble(scanner.next())));
                    scanner.next();
                }
                scanner.close();
            } catch(IOException x) {
                x.printStackTrace();
            }
        }
        
        
        @Override
        public String[] featureNames() {
            return featureNames;
        }

        @Override
        public PhraseTable candidates(Chunk label) {
            final MockPhraseTableEntry trans = translations.get(label.getSource());
            if(trans == null) {
                return new PhraseTableImpl(Language.GERMAN, Language.ENGLISH, "Mock", 0, Arrays.asList(featureNames));
            } else {
                final PhraseTableImpl pt = new PhraseTableImpl(Language.GERMAN, Language.ENGLISH, "Mock", 0, Arrays.asList(featureNames));
                pt.put(trans,trans);
                return pt;
            }
        }

        @Override
        public String getName() {
            return "Mock";
        }

        @Override
        public void close() {
            
        }
        
    }
    
    private static class MockTranslationSourceFactory implements TranslationSourceFactory {

        @Override
        public TranslationSource getSource(Language srcLang, Language trgLang) {
            return new MockTranslationSource();
        }
        
    }
    
    private static class MockLanguageModel implements LanguageModel {

        @Override
        public Language getLanguage() {
            return Language.ENGLISH;
        }

        @Override
        public int getOrder() {
            return 3;
        }

        @Override
        public String getName() {
            return "Mock";
        }

        @Override
        public boolean isRelevantPrefix(List<String> tokens) {
            return false;
        }

        @Override
        public double score(List<String> tokens) {
            return 0.0;
        }

        @Override
        public void close() {
            
        }

        @Override
        public int quartile(List<String> tokens) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
    private static class MockLanguageModelFactory implements LanguageModelFactory {

        @Override
        public LanguageModel getModel(Language language) {
            return new MockLanguageModel();
                    
        }

        @Override
        public TrueCaser getTrueCaser(Language language) {
            return null;
        }
        
    }
    
    private static class MockLabel implements Label {
        private final String s;
        private final Language l;

        public MockLabel(String s, Language l) {
            this.s = s;
            this.l = l;
        }
        
        
        
        @Override
        public String asString() {
            return s;
        }

        @Override
        public Language getLanguage() {
            return l;
        }
        
    }
}
