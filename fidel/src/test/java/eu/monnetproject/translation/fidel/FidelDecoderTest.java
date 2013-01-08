package eu.monnetproject.translation.fidel;

import eu.monnetproject.translation.fidel.FidelDecoder;
import eu.monnetproject.translation.fidel.IntegerLanguageModel;
import eu.monnetproject.translation.fidel.Phrase;
import eu.monnetproject.translation.fidel.PhraseTranslation;
import eu.monnetproject.translation.fidel.Solution;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Collection;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static java.lang.Math.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * @author john
 */
public class FidelDecoderTest extends TestCase {

    public FidelDecoderTest(String testName) {
        super(testName);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }
    private final static Object2IntOpenHashMap<String> w2i = new Object2IntOpenHashMap<String>();

    static {
        w2i.put("er", 0);
        w2i.put("geht", 1);
        w2i.put("ja", 2);
        w2i.put("nicht", 3);
        w2i.put("nach", 4);
        w2i.put("hause", 5);
        w2i.put("he", 6);
        w2i.put("it", 7);
        w2i.put("is", 8);
        w2i.put("goes", 9);
        w2i.put("yes", 10);
        w2i.put("not", 11);
        w2i.put("does", 12);
        w2i.put("after", 13);
        w2i.put("to", 14);
        w2i.put("house", 15);
        w2i.put("home", 16);
    }
    private final static Object2ObjectOpenHashMap<Phrase, Collection<PhraseTranslation>> pt = new Object2ObjectOpenHashMap<Phrase, Collection<PhraseTranslation>>();

    static {
        pt.put(new Phrase(new int[]{0}), Arrays.asList( // er
                new PhraseTranslation(new int[]{6}, new double[]{log10(0.474756), log10(0.427108), log10(0.128955), log10(0.144797), log10(2.718)}), // he
                new PhraseTranslation(new int[]{7}, new double[]{log10(0.0824588), log10(0.0631851), log10(0.370504), log10(0.354335), log10(2.718)}))); // it
        pt.put(new Phrase(new int[]{0, 1}), Arrays.asList( // er geht
                new PhraseTranslation(new int[]{7, 8}, new double[]{log10(5.91106e-06), log10(6.59882e-07), log10(0.278564), log10(0.089717), log10(2.718)}), // it is
                new PhraseTranslation(new int[]{7, 9}, new double[]{log10(0.0928548), log10(0.00613448), log10(0.278564), log10(0.00244537), log10(2.718)}))); // it goes
        pt.put(new Phrase(new int[]{1}), Arrays.asList( // geht
                new PhraseTranslation(new int[]{9}, new double[]{log10(0.00076391), log10(0.0009707), log10(0.0957447), log10(0.089717), log10(2.718)}), // goes
                new PhraseTranslation(new int[]{8}, new double[]{log10(0.195122), log10(0.0970874), log10(0.0106383), log10(0.0069013), log10(2.718)}))); // is
        pt.put(new Phrase(new int[]{2}), Arrays.asList( // ja
                new PhraseTranslation(new int[]{10}, new double[]{log10(0.688525), log10(0.596154), log10(0.0969977), log10(0.124), log10(2.718)}))); // yes
        pt.put(new Phrase(new int[]{2, 3}), Arrays.asList( // ja nicht
                new PhraseTranslation(new int[]{11}, new double[]{log10(1.15264e-05), log10(1.18081e-05), log10(0.557129), log10(0.521708), log10(2.718)}) // not
                ));
        pt.put(new Phrase(new int[]{3}), Arrays.asList( // nicht
                new PhraseTranslation(new int[]{11}, new double[]{log10(0.593731), log10(0.624766), log10(0.391964), log10(0.521708), log10(2.718)}), // not
                new PhraseTranslation(new int[]{12, 11}, new double[]{log10(0.518193), log10(0.53734), log10(0.0443482), log10(0.0226303), log10(2.718)})));
        pt.put(new Phrase(new int[]{4}), Arrays.asList( // nach
                new PhraseTranslation(new int[]{13}, new double[]{log10(0.623099), log10(0.678103), log10(0.0951965), log10(0.0844883), log10(2.718)}), // after
                new PhraseTranslation(new int[]{14}, new double[]{log10(0.00914233), log10(0.0189595), log10(0.0386796), log10(0.102775), log10(2.718)})));
        pt.put(new Phrase(new int[]{4, 5}), Arrays.asList( // nach hause
                new PhraseTranslation(new int[]{16}, new double[]{log10(0.0462963), log10(0.0122825), log10(0.625), log10(0.6), log10(2.718)})));
        pt.put(new Phrase(new int[]{5}), Arrays.asList( // hause
                new PhraseTranslation(new int[]{15}, new double[]{log10(0.0462963), log10(0.0122825), log10(0.625), log10(0.6), log10(2.718)})));
    }
    private static final IntegerLanguageModelImpl lm = new IntegerLanguageModelImpl();

    static {
        lm.put(new Phrase(new int[]{13}), new double[]{-3.048014, -0.5595744});
        lm.put(new Phrase(new int[]{12}), new double[]{-2.942701, -0.7269722});
        lm.put(new Phrase(new int[]{9}), new double[]{-3.485496, -0.8144186});
        lm.put(new Phrase(new int[]{6}), new double[]{-3.320179, -0.4907385});
        lm.put(new Phrase(new int[]{16}), new double[]{-3.894, -0.4098516});
        lm.put(new Phrase(new int[]{15}), new double[]{-4.069491, -0.1530379});
        lm.put(new Phrase(new int[]{8}), new double[]{-2.257655, -0.6430779});
        lm.put(new Phrase(new int[]{7}), new double[]{-2.622992, -0.5401071});
        lm.put(new Phrase(new int[]{11}), new double[]{-2.871661, -0.2037089});
        lm.put(new Phrase(new int[]{14}), new double[]{-2.203269, -0.6064922});
        lm.put(new Phrase(new int[]{10}), new double[]{-4.335011, -0.5965024});

        lm.put(new Phrase(new int[]{13, 6}), new double[]{-2.456458});
        lm.put(new Phrase(new int[]{13, 8}), new double[]{-3.731876});
        lm.put(new Phrase(new int[]{13, 7}), new double[]{-1.941688});
        lm.put(new Phrase(new int[]{13, 11}), new double[]{-3.731876});
        lm.put(new Phrase(new int[]{12, 13}), new double[]{-3.280287});
        lm.put(new Phrase(new int[]{12, 6}), new double[]{-2.15744});
        lm.put(new Phrase(new int[]{12, 8}), new double[]{-2.210782});
        lm.put(new Phrase(new int[]{12, 7}), new double[]{-1.685437});
        lm.put(new Phrase(new int[]{12, 11}), new double[]{-0.4060034});
        lm.put(new Phrase(new int[]{12, 14}), new double[]{-2.426915});
        lm.put(new Phrase(new int[]{9, 13}), new double[]{-3.661454});
        lm.put(new Phrase(new int[]{9, 16}), new double[]{-2.932294});
        lm.put(new Phrase(new int[]{9, 8}), new double[]{-3.661454});
        lm.put(new Phrase(new int[]{9, 7}), new double[]{-3.168323});
        lm.put(new Phrase(new int[]{9, 11}), new double[]{-3.168323});
        lm.put(new Phrase(new int[]{9, 14}), new double[]{-1.046571});
        lm.put(new Phrase(new int[]{6, 12}), new double[]{-2.046349});
        lm.put(new Phrase(new int[]{6, 9}), new double[]{-2.74139});
        lm.put(new Phrase(new int[]{6, 15}), new double[]{-4.246866});
        lm.put(new Phrase(new int[]{6, 8}), new double[]{-1.39645});
        lm.put(new Phrase(new int[]{6, 7}), new double[]{-4.246866});
        lm.put(new Phrase(new int[]{6, 11}), new double[]{-2.623379});
        lm.put(new Phrase(new int[]{6, 14}), new double[]{-3.753734});
        lm.put(new Phrase(new int[]{16, 13}), new double[]{-2.988746});
        lm.put(new Phrase(new int[]{16, 12}), new double[]{-2.988746});
        lm.put(new Phrase(new int[]{16, 15}), new double[]{-3.481877});
        lm.put(new Phrase(new int[]{16, 8}), new double[]{-1.976402});
        lm.put(new Phrase(new int[]{16, 7}), new double[]{-2.988746});
        lm.put(new Phrase(new int[]{16, 11}), new double[]{-2.988746});
        lm.put(new Phrase(new int[]{16, 14}), new double[]{-1.28136});
        lm.put(new Phrase(new int[]{15, 13}), new double[]{-2.974661});
        lm.put(new Phrase(new int[]{15, 12}), new double[]{-2.974661});
        lm.put(new Phrase(new int[]{15, 6}), new double[]{-3.703822});
        lm.put(new Phrase(new int[]{15, 8}), new double[]{-1.987634});
        lm.put(new Phrase(new int[]{15, 7}), new double[]{-3.703822});
        lm.put(new Phrase(new int[]{15, 11}), new double[]{-3.21069});
        lm.put(new Phrase(new int[]{15, 14}), new double[]{-2.198346});
        lm.put(new Phrase(new int[]{8, 13}), new double[]{-3.562478});
        lm.put(new Phrase(new int[]{8, 12}), new double[]{-4.242198});
        lm.put(new Phrase(new int[]{8, 9}), new double[]{-5.091981});
        lm.put(new Phrase(new int[]{8, 6}), new double[]{-3.769841});
        lm.put(new Phrase(new int[]{8, 16}), new double[]{-3.961626});
        lm.put(new Phrase(new int[]{8, 8}), new double[]{-4.398751});
        lm.put(new Phrase(new int[]{8, 7}), new double[]{-2.911189});
        lm.put(new Phrase(new int[]{8, 11}), new double[]{-1.577322});
        lm.put(new Phrase(new int[]{8, 14}), new double[]{-1.801822});
        lm.put(new Phrase(new int[]{8, 10}), new double[]{-3.961626});
        lm.put(new Phrase(new int[]{7, 13}), new double[]{-3.088698});
        lm.put(new Phrase(new int[]{7, 12}), new double[]{-2.247777});
        lm.put(new Phrase(new int[]{7, 9}), new double[]{-2.734638});
        lm.put(new Phrase(new int[]{7, 6}), new double[]{-4.273452});
        lm.put(new Phrase(new int[]{7, 16}), new double[]{-3.816251});
        lm.put(new Phrase(new int[]{7, 8}), new double[]{-1.335013});
        lm.put(new Phrase(new int[]{7, 7}), new double[]{-3.922493});
        lm.put(new Phrase(new int[]{7, 11}), new double[]{-2.750546});
        lm.put(new Phrase(new int[]{7, 14}), new double[]{-1.818335});
        lm.put(new Phrase(new int[]{11, 13}), new double[]{-1.121721});
        lm.put(new Phrase(new int[]{11, 12}), new double[]{-4.015676});
        lm.put(new Phrase(new int[]{11, 9}), new double[]{-4.954843});
        lm.put(new Phrase(new int[]{11, 6}), new double[]{-4.461711});
        lm.put(new Phrase(new int[]{11, 8}), new double[]{-3.768481});
        lm.put(new Phrase(new int[]{11, 7}), new double[]{-3.550728});
        lm.put(new Phrase(new int[]{11, 14}), new double[]{-1.549705});
        lm.put(new Phrase(new int[]{14, 13}), new double[]{-4.531987});
        lm.put(new Phrase(new int[]{14, 12}), new double[]{-5.381771});
        lm.put(new Phrase(new int[]{14, 16}), new double[]{-4.287304});
        lm.put(new Phrase(new int[]{14, 15}), new double[]{-4.287304});
        lm.put(new Phrase(new int[]{14, 8}), new double[]{-3.732888});
        lm.put(new Phrase(new int[]{14, 7}), new double[]{-2.673905});
        lm.put(new Phrase(new int[]{14, 11}), new double[]{-3.631014});
        lm.put(new Phrase(new int[]{14, 14}), new double[]{-4.531987});
        lm.put(new Phrase(new int[]{10, 13}), new double[]{-2.92767});
        lm.put(new Phrase(new int[]{10, 7}), new double[]{-2.198509});
        lm.put(new Phrase(new int[]{10, 14}), new double[]{-0.9798192});
    }
    private final static Object2ObjectOpenHashMap<Phrase, Collection<PhraseTranslation>> invPt = new Object2ObjectOpenHashMap<Phrase, Collection<PhraseTranslation>>();

    static {
        for (Map.Entry<Phrase, Collection<PhraseTranslation>> ptE : pt.entrySet()) {
            for (PhraseTranslation pt : ptE.getValue()) {
                final Phrase key = new Phrase(pt.words);
                if (!invPt.containsKey(key)) {
                    invPt.put(key, new LinkedList<PhraseTranslation>());
                }
                invPt.get(key).add(new PhraseTranslation(ptE.getKey().p, pt.scores));
            }
        }
    }
/*
    private double directEvalSoln(int[] soln) {
        double score = 0;
        for (int i = 1; i <= soln.length; i++) {
            score += FidelDecoder.lmScore(soln, i, lm, 2);
        }
        for (int i = 0; i < soln.length;) {
            for (int j = soln.length; j >= i + 1; j--) {
                final Collection<PhraseTranslation> candidate = invPt.get(new Phrase(soln, i, j - i));
                if (candidate != null) {
                    double bestPtScore = Double.NEGATIVE_INFINITY;
                    int bestLength = 1;
                    for (PhraseTranslation pt : candidate) {
                        double ptScore = 0.0;
                        for (int k = 0; k < pt.w.length; k++) {
                            ptScore += pt.w[k];
                        }
                        if (ptScore > bestPtScore) {
                            bestPtScore = ptScore;
                            bestLength = pt.p.length;
                        }
                    }
                    score += bestPtScore;
                    i += bestLength;
                    break;
                }
            }
        }
        return score;
    }*/

    public void testDecodeSimple() {
        System.out.println("decodeSimple");
        int[] src = { 0, 1, 2 };
        Object2ObjectMap<Phrase, Collection<PhraseTranslation>> phraseTable = new Object2ObjectOpenHashMap<Phrase, Collection<PhraseTranslation>>();
        phraseTable.put(new Phrase(new int[] { 0 }), Arrays.asList(new PhraseTranslation(new int[] { 3 }, new double[] { -1 })));
        phraseTable.put(new Phrase(new int[] { 1 }), Arrays.asList(new PhraseTranslation(new int[] { 4 }, new double[] { -1 })));
        phraseTable.put(new Phrase(new int[] { 2 }), Arrays.asList(new PhraseTranslation(new int[] { 5 }, new double[] { -1 })));
        final IntegerLanguageModelImpl languageModel = new IntegerLanguageModelImpl();
        languageModel.put(new Phrase(new int[] { 3 }), new double[] { -1, -10 });
        languageModel.put(new Phrase(new int[] { 4 }), new double[] { -1, -10 });
        languageModel.put(new Phrase(new int[] { 5 }), new double[] { -1, -10 });
        languageModel.put(new Phrase(new int[] { 3,4 }), new double[] { -1, });
        languageModel.put(new Phrase(new int[] { 4,5 }), new double[] { -1, });
        int lmN = 2;
        double[] weights = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
        int distiortionLimit = 3;
        int nBest = 1;
        Solution[] result = FidelDecoder.decode(src, phraseTable, languageModel, lmN, weights, distiortionLimit, nBest, 1000);
        assertArrayEquals(new int[] { 3,4,5 },result[0].soln);
        assertEquals(-6, result[0].score,0.01);
        
    }
    
    /**
     * Test of decode method, of class FidelDecoder.
     */
    public void testDecode() {
        System.out.println("decode");
        int[] src = {0, 1, 2, 3, 4, 5};
        Object2ObjectMap<Phrase, Collection<PhraseTranslation>> phraseTable = pt;
        IntegerLanguageModelImpl languageModel = lm;
        int lmN = 2;
        double[] weights = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
        int distiortionLimit = 3;
        int nBest = 1;
        Solution[] result = FidelDecoder.decode(src, phraseTable, languageModel, lmN, weights, distiortionLimit, nBest, 1000);
        final int[] expSoln = new int[]{7,9,10,11,16};
        System.out.println("My Guess:" +  Arrays.toString(expSoln));
        System.out.println("Fidel's soln: " + Arrays.toString(result[0].soln));
        assertArrayEquals(expSoln, result[0].soln);
    }

    public void testHash() {
        final HashSet<Phrase> hs = new HashSet<Phrase>();
        hs.add(new Phrase(new int[]{1, 2}));
        assertTrue(hs.contains(new Phrase(new int[]{0, 1, 2, 3}, 1, 2)));
        hs.clear();
        hs.add(new Phrase(new int[]{0, 1, 2, 3}, 1, 2));
        assertTrue(hs.contains(new Phrase(new int[]{1, 2})));
    }

    /**
     * Test of lmScore method, of class FidelDecoder.
     */
    public void testLmScoreDirect() {
        System.out.println("lmScore");
        int[] buf = {6, 9, 12};
        IntegerLanguageModelImpl languageModel = lm;
        int lmN = 2;
        assertEquals(-2.74139, FidelDecoder.lmScore(buf, 2, languageModel, lmN,Double.NEGATIVE_INFINITY), 0.01);
    }

    /**
     * Test of lmScore method, of class FidelDecoder.
     */
    public void testLmScoreBackoff() {
        System.out.println("lmScore");
        int[] buf = {6, 9, 12};
        int p = 3;
        IntegerLanguageModelImpl languageModel = lm;
        int lmN = 2;
        double expResult = -2.942701 + -0.8144186;
        double result = FidelDecoder.lmScore(buf, p, languageModel, lmN,Double.NEGATIVE_INFINITY);
        assertEquals(expResult, result, 0.001);
    }

    /**
     * Test of rightShiftBuffer method, of class FidelDecoder.
     */
    public void testRightShiftBuffer() {
        System.out.println("rightShiftBuffer");
        int[] buf = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0};
        int shift = 2;
        int at = 6;
        FidelDecoder.rightShiftBuffer(buf, shift, at);
        Assert.assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 6, 7, 8, 9}, buf);
        final int[] buf2 = new int[]{7, 9, 0};
        FidelDecoder.rightShiftBuffer(buf2, 1, 1);
        Assert.assertArrayEquals(new int[]{7, 9, 9}, buf2);
    }

    /**
     * Test of leftShiftBuffer method, of class FidelDecoder.
     */
    @Test
    public void testLeftShiftBuffer() {
        System.out.println("leftShiftBuffer");
        int[] buf = {1, 2, 3, 4, 5, 6, 7, 6, 7, 8, 9};
        int shift = 2;
        int at = 6;
        FidelDecoder.leftShiftBuffer(buf, shift, at);
        int[] expResult = {1, 2, 3, 4, 5, 6, 7, 8, 9, 8, 9};
        assertArrayEquals(expResult, buf);
    }

    /**
     * Test of tryPutTranslation method, of class FidelDecoder.
     */
    @Test
    public void testTryPutTranslation_6args() {
        System.out.println("tryPutTranslation");
        PhraseTranslation pt = new PhraseTranslation(new int[]{6}, new double[]{log10(0.474756), log10(0.427108), log10(0.128955), log10(0.144797), log10(2.718)});
        double[] weights = {1, 1, 1, 1, 1, 1, 1, 1};
        int[] baselineBuffer = new int[2];
        int pos = 0;
        IntegerLanguageModelImpl languageModel = lm;
        int lmN = 2;
        double expResult = log10(0.474756) + log10(0.427108) + log10(0.128955) + log10(0.144797) + log10(2.718) - 3.320179;
        double result = FidelDecoder.tryPutTranslation(pt, weights, baselineBuffer, pos, languageModel, lmN);
        assertEquals(expResult, result, 0.001);
    }

    /**
     * Test of recalcDist method, of class FidelDecoder.
     */
    @Test
    public void testRecalcDist() {
        System.out.println("recalcDist");
        int[] dist = {0, 0, -1, 1, 0};
        int length = 2;
        int shift = 2;
        int[] expResult = {0, 0, -1, -2, -2, 3, 2};
        int[] result = FidelDecoder.recalcDist(dist, length, shift);
        assertArrayEquals(expResult, result);
        assertArrayEquals(new int[]{-2, 1, 1}, FidelDecoder.recalcDist(new int[]{0, 0}, 1, 2));
    }
    
    @Test
    public void testDeltaDist() {
        System.out.println("deltaDist");
        int[] dist = {0, 0, -1, 1, 0};
        int length = 2;
        int shift = 2;
        double[] weights = {1, 1, 1, 1, 1, 1, 1, 1};
        double result = FidelDecoder.deltaDist(dist, length, shift, weights);
        assertEquals(-8,result,0.0);
    }

    /**
     * Test of tryPutTranslation method, of class FidelDecoder.
     */
    @Test
    public void testTryPutTranslation_7args() {
        System.out.println("tryPutTranslation");
        PhraseTranslation pt = new PhraseTranslation(new int[]{6}, new double[]{log10(0.474756), log10(0.427108), log10(0.128955), log10(0.144797), log10(2.718)});
        double[] weights = {1, 1, 1, 1, 1, 1, 1, 1};
        int[] buf = new int[2];
        int pos = 0;
        IntegerLanguageModelImpl languageModel = lm;
        int lmN = 2;
        int dist = 0;
        double expResult = log10(0.474756) + log10(0.427108) + log10(0.128955) + log10(0.144797) + log10(2.718) - 3.320179;
        double result = FidelDecoder.tryPutTranslation(pt, weights, buf, pos, languageModel, lmN, dist);
        assertEquals(expResult, result, 0.001);
    }

    /**
     * Test of calcPartialScore method, of class FidelDecoder.
     */
    @Test
    public void testCalcPartialScore() {
        System.out.println("calcPartialScore");
        int[] src = {0, 1, 2, 3, 4, 5};
        Object2ObjectMap<Phrase, Collection<PhraseTranslation>> phraseTable = pt;
        double[] weights = {1, 1, 1, 1, 1, 1, 1, 1};
        IntegerLanguageModelImpl languageModel = lm;
        int lmN = 2;
        double[] expResult = {-5.3066, -7.6801, -6.2073, -3.5574, -5.0824, -7.3064};
        double[] result = FidelDecoder.calcPartialScore(src, phraseTable, weights, languageModel, lmN);
        assertArrayEquals(expResult, result, 0.01);
    }
    
    private static class IntegerLanguageModelImpl extends Object2ObjectOpenHashMap<Phrase, double[]> implements IntegerLanguageModel {

        public double[] get(Phrase phrase) {
            return super.get(phrase);
        }

        public int order() {
            return 2;
        }

        public Object2IntMap<String> wordMap() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Int2ObjectMap<String> invWordMap() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
        
    }
}
