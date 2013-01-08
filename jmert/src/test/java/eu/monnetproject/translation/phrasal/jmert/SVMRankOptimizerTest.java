package eu.monnetproject.translation.phrasal.jmert;

import eu.monnetproject.translation.Feature;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author john
 */
public class SVMRankOptimizerTest {

    public SVMRankOptimizerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of optimizeFeatures method, of class SVMRankOptimizer.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testOptimizeFeatures() {
        System.out.println("optimizeFeatures");
        final Feature[] f0 = new Feature[]{new Feature("F1", 5), new Feature("F2", 3), new Feature("F3", 2)};
        final Feature[] f1 = new Feature[]{new Feature("F1", 2), new Feature("F2", 5), new Feature("F3", 3)};
        final Feature[] f2 = new Feature[]{new Feature("F1", 3), new Feature("F2", 2), new Feature("F3", 5)};
        final Feature[] f3 = new Feature[]{new Feature("F1", 3), new Feature("F2", 2), new Feature("F3", 6)};
        final Feature[] f4 = new Feature[]{new Feature("F1", 3), new Feature("F2", 2), new Feature("F3", 7)};
        @SuppressWarnings("unchecked")
        List<Collection<JMertTranslation>> nBests = Arrays.asList(
                (Collection<JMertTranslation>) Arrays.asList(
                new JMertTranslation(f0, 27),
                new JMertTranslation(f1, 23),
                new JMertTranslation(f2, 22),
                new JMertTranslation(f3, 23),
                new JMertTranslation(f4, 24)));
        Feature[] weights = {new Feature("F1", 3), new Feature("F2", 2), new Feature("F3", 1)};
        Optimizer instance = new SVMRankOptimizer();
        @SuppressWarnings("unchecked")
        final double[] newWts;
        newWts = instance.optimizeFeatures(nBests, weights, 5, Collections.EMPTY_SET);
        assert(innerproduct(f0,newWts) > innerproduct(f4,newWts));
        assert(innerproduct(f4,newWts) > innerproduct(f3,newWts));
        assert(innerproduct(f4,newWts) > innerproduct(f1,newWts));
        assert(innerproduct(f3,newWts) > innerproduct(f2,newWts));
        assert(innerproduct(f1,newWts) > innerproduct(f2,newWts));
        System.err.println(Arrays.toString(newWts));
    }

    private double innerproduct(Feature[] features, double[] wts) {
        double ip = 0.0;
        assert(features.length == wts.length);
        for(int i = 0; i < features.length; i++) {
            ip += features[i].score * wts[i];
        }
        return ip;
    }
}