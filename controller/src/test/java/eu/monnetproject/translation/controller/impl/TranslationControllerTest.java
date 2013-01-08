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
import eu.monnetproject.translation.phrasal.lm.ARPALanguageModelFactory;
import eu.monnetproject.translation.phrasal.pt.MemoryMappedPhraseTableSourceFactory;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
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
        Configurator.setConfig("eu.monnetproject.translation.phrasal.lm", "en", "src/test/resources/sample-models/lm/europarl.srilm.gz");
        Configurator.setConfig("eu.monnetproject.translation.phrasal.pt", "de/en/1", "src/test/resources/sample-models/phrase-model/phrase-table.sorted");
        Configurator.setConfig("eu.monnetproject.translation.topics.lda");
        final TranslationController instance = new TranslationController(Collections.singleton((LanguageModelFactory)new ARPALanguageModelFactory()),
                Services.get(DecoderFactory.class),
                Services.getAll(TranslationPhraseChunkerFactory.class),
                Collections.singletonList((TranslationSourceFactory)new MemoryMappedPhraseTableSourceFactory()),
                Services.getAll(TranslationFeaturizerFactory.class),
                Services.getFactory(TokenizerFactory.class),
                Services.getAll(TranslationConfidenceFactory.class));
        instance.translate(ontology, sourceLexicons, targetLexicon, scope, namePrefix, nBest);
        assertEquals(1, targetLexicon.getEntrys().size());
    }
}
