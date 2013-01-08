/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.phrasal.eval;

import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.lang.Language;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class BLEUTest {
    
    public BLEUTest() {
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

    @Test
    public void testScore() {
        final HashMap<String, List<String>> references = new HashMap<String,List<String>>();
        references.put("sample input", Arrays.asList(new String[] { 
           "It is a guide to action that ensures that the military will forever heed Party commands",
           "It is the guiding principle which guarantees the military forces always being under the command of the Party",
           "It is the practical guide for the army always to heed the directions of the party"
        }));
        final List<Translation> translations = Collections.singletonList((Translation)new TranslationImpl(new StringLabel("sample input", Language.ENGLISH), new StringLabel("It is a guide to action which ensures that the military always obeys the commands of the party", Language.ENGLISH), null,1.0,Collections.EMPTY_LIST));
        final BLEU instance = new BLEU(references);
        final double result = instance.score(translations);
        assertEquals(0.504, result, 0.001);
        
        final HashMap<String, List<String>> references2 = new HashMap<String,List<String>>();
        references2.put("test", Arrays.asList(new String[] { "test" }));
        final List<Translation> translations2 = Collections.singletonList((Translation)new TranslationImpl(new StringLabel("test", Language.ENGLISH), new StringLabel("test", Language.GERMAN),null,1.0,Collections.EMPTY_LIST));
        final BLEU instance2 = new BLEU(references2);
        final double result2 = instance2.score(translations2);
        assertEquals(1.0,result2,0.000001);
    }
    
    private final class TranslationImpl implements Translation {
        private final Label sourceLabel, targetLabel;
        private final URI entity;
        private final double score;
        private final Collection<Feature> features;

        public TranslationImpl(Label sourceLabel, Label targetLabel, URI entity, double score, Collection<Feature> features) {
            this.sourceLabel = sourceLabel;
            this.targetLabel = targetLabel;
            this.entity = entity;
            this.score = score;
            this.features = features;
        }
        
        
        
        @Override
        public Label getSourceLabel() {
            return sourceLabel;
        }

        @Override
        public Label getTargetLabel() {
            return targetLabel;
        }

        @Override
        public URI getEntity() {
            return entity;
        }

        @Override
        public double getScore() {
            return score;
        }

        @Override
        public Collection<Feature> getFeatures() {
            return features;
        }
    }

    /**
     * Test of getMetric method, of class BLEU.
     */
    @Test
    public void testGetMetric() {
        System.out.println("getMetric");
    }

    /**
     * Test of getName method, of class BLEU.
     */
    @Test
    public void testGetName() {
        System.out.println("getName");
    }
}
