/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.phrasal.lm;

import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.IStrings;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.base.SimpleSequence;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.LanguageModel;
import java.io.IOException;
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
public class WrappedLanguageModelTest {

    public WrappedLanguageModelTest() {
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
     * Test of score method, of class WrappedLanguageModel.
     */
    @Test
    public void testScore() throws IOException {
        System.out.println("score");
        final ARPALanguageModel lm = new ARPALanguageModel("src/test/resources/sample-models/lm/europarl.srilm.gz", Language.ENGLISH);
        Sequence<IString> sequence = new SimpleSequence<IString>(IStrings.toIStringArray(new String[]{"this", "is", "a", "small", "house"}));
        WrappedLanguageModel instance = new WrappedLanguageModel(lm);
        double expResult = -8.666181;
        double result = instance.score(sequence);
        assertEquals(expResult, result, 0.00001);
    }

    /**
     * Test of getStartToken method, of class WrappedLanguageModel.
     */
    @Test
    public void testGetStartToken() {
        System.out.println("getStartToken");
        WrappedLanguageModel instance = new WrappedLanguageModel(new MockLanguageModel());
        String expResult = "<s>";
        IString result = instance.getStartToken();
        assertEquals(expResult, result.word());
    }

    /**
     * Test of getEndToken method, of class WrappedLanguageModel.
     */
    @Test
    public void testGetEndToken() {
        System.out.println("getEndToken");
        WrappedLanguageModel instance = new WrappedLanguageModel(new MockLanguageModel());
        String expResult = "</s>";
        IString result = instance.getEndToken();
        assertEquals(expResult, result.word());
    }

    /**
     * Test of getName method, of class WrappedLanguageModel.
     */
    @Test
    public void testGetName() {
        System.out.println("getName");
        WrappedLanguageModel instance = new WrappedLanguageModel(new MockLanguageModel());
        String expResult = "Mock LM";
        String result = instance.getName();
        assertEquals(expResult, result);
    }

    /**
     * Test of order method, of class WrappedLanguageModel.
     */
    @Test
    public void testOrder() {
        System.out.println("order");
        WrappedLanguageModel instance = new WrappedLanguageModel(new MockLanguageModel());;
        int expResult = 1;
        int result = instance.order();
        assertEquals(expResult, result);
    }

    /**
     * Test of releventPrefix method, of class WrappedLanguageModel.
     */
    @Test
    public void testReleventPrefix() {
        System.out.println("releventPrefix");
        Sequence<IString> sequence = new SimpleSequence<IString>(IStrings.toIStringArray(new String[]{"test"}));
        WrappedLanguageModel instance = new WrappedLanguageModel(new MockLanguageModel());
        boolean expResult = false;
        boolean result = instance.releventPrefix(sequence);
        assertEquals(expResult, result);
    }

    private static class MockLanguageModel implements LanguageModel {

        @Override
        public Language getLanguage() {
            return Language.ENGLISH;
        }

        @Override
        public int getOrder() {
            return 1;
        }

        @Override
        public String getName() {
            return "Mock LM";
        }

        @Override
        public boolean isRelevantPrefix(List<String> tokens) {
            return false;
        }

        @Override
        public double score(List<String> tokens) {
            return 1.0;
        }

        @Override
        public void close() {
        }

        @Override
        public int quartile(List<String> tokens) {
            return 0;
        }
        
        
    }
}
