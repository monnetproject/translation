package eu.monnetproject.translation.jmert;

import eu.monnetproject.translation.jmert.OLSOptimizer;
import eu.monnetproject.translation.jmert.JMertTranslation;
import eu.monnetproject.translation.jmert.Optimizer;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import eu.monnetproject.translation.Feature;
import java.util.Collections;

public class GLSOptimizerTest {

    @Test
    public void testOptimizeFeatures() {
        System.out.println("optimizeFeatures");
        @SuppressWarnings("unchecked")
        List<Collection<JMertTranslation>> nBests = Arrays.asList(
                (Collection<JMertTranslation>) Arrays.asList(new JMertTranslation(new Feature[]{new Feature("F1", 5), new Feature("F2", 3), new Feature("F3", 2)}, 27),
                new JMertTranslation(new Feature[]{new Feature("F1", 2), new Feature("F2", 5), new Feature("F3", 3)}, 23),
                new JMertTranslation(new Feature[]{new Feature("F1", 3), new Feature("F2", 2), new Feature("F3", 5)}, 22),
                new JMertTranslation(new Feature[]{new Feature("F1", 3), new Feature("F2", 2), new Feature("F3", 6)}, 23),
                new JMertTranslation(new Feature[]{new Feature("F1", 3), new Feature("F2", 2), new Feature("F3", 7)}, 24)));
        Feature[] weights = {new Feature("F1", 3), new Feature("F2", 2), new Feature("F3", 1)};
        Optimizer instance = new OLSOptimizer();
        final double[] newWts = instance.optimizeFeatures(nBests, weights, 5, Collections.EMPTY_SET);
        System.err.println(Arrays.toString(newWts));
//        assertArrayEquals(new double[]{0.5, 1.0 / 3.0, 1.0 / 6.0}, newWts, 0.001);
    }
}
