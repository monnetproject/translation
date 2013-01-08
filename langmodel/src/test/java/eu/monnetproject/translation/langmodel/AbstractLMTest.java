package eu.monnetproject.translation.langmodel;

import eu.monnetproject.lang.Language;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.List;
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
public class AbstractLMTest {

    public AbstractLMTest() {
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
     * Test of trueCase method, of class AbstractLM.
     */
    @Test
    public void testTrueCase() {
        System.out.println("trueCase");
        String[] tokens = new String[] { "this", "is", "A", "test" };
        int id = 0;
        AbstractLM instance = new AbstractLMImpl();
        String[] expResult = new String[] { "This", "is", "a", "test" };
        String[] result = instance.trueCase(tokens, id);
        assertArrayEquals(expResult, result);
    }
    
    @Test
    public void testQuartile() {
        System.out.println("quartile");
        AbstractLM instance = new AbstractLMImpl();
        assertEquals(4, instance.quartile(Arrays.asList("This")));
        assertEquals(2, instance.quartile(Arrays.asList("this")));
        assertEquals(4, instance.quartile(Arrays.asList("IS")));
        assertEquals(2, instance.quartile(Arrays.asList("is")));
        assertEquals(4, instance.quartile(Arrays.asList("a")));
        assertEquals(4, instance.quartile(Arrays.asList("test")));
    }

    public class AbstractLMImpl extends AbstractLM {
        private Object2IntMap<String> keyMap = new Object2IntOpenHashMap<String>(new String[] {
            "This",//1
            "this",//2
            "IS",//3
            "is",//4
            "a",//5
            "test"//6
        }, new int[] { 
            1,2,3,4,5,6
        });
        private Object2ObjectMap<NGram,double[]> scoreMap = new Object2ObjectOpenHashMap<NGram, double[]>(
                new NGram[] {
                    new NGram(new int[] {1}),
                    new NGram(new int[] {2}),
                    new NGram(new int[] {3}),
                    new NGram(new int[] {4}),
                    new NGram(new int[] {5}),
                    new NGram(new int[] {6}),
                    new NGram(new int[] {1,4}),
                    new NGram(new int[] {1,3})                        
                },new double[][] {
                    {0.0},
                    {-1.0},
                    {0.0},
                    {-1.0},
                    {0.0},
                    {0.0},
                    {1.0},
                    {-10.0}
                });
        
        
        @Override
        public NGram toKey(List<String> tokens) {
            final int[] ng = new int[tokens.size()];
            int i = 0;
            for(String token : tokens) {
                ng[i++] = keyMap.get(token);
            }
            return new NGram(ng);
        }

        @Override
        public int toKey(String token, boolean useUnk) {
            if(keyMap.containsKey(token)) {
                return keyMap.getInt(token);
            } else {
                return useUnk ? 7 : -1;
            }
        }

        @Override
        public double[] rawScore(int n, NGram key) {
            return scoreMap.get(key);
        }

        @Override
        public Language getLanguage() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getOrder() {
            return 2;
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void close() {
            
        }

        @Override
        protected double mean(int n) {
            return 0.6;
        }

        @Override
        protected double sd(int n) {
            return 0.35;
        }
        
    }

}