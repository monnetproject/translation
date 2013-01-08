package eu.monnetproject.translation.langmodel;

import java.util.HashMap;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author john
 */
public class DirectDataHashMapTest {

    public DirectDataHashMapTest() {
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
     * Test of put method, of class DirectDataHashMap.
     */
    @Test
    public void testMap() {
        System.out.println("ddMap");
        final Random r = new Random();
        final HashMap<int[], double[]> refMap = new HashMap<int[],double[]>();
        final DirectDataHashMap map = new DirectDataHashMap();
        for(int i = 0; i < 100; i++) {
            int[] key = new int[] {
                r.nextInt(),
                r.nextInt()
            };
            double[] val = new double[] {
                r.nextDouble(),
                r.nextDouble()
            };
            if(refMap.containsKey(key)) {
                continue;
            }
            refMap.put(key,val);
            map.put(key, val);
        }
        for(int[] key : refMap.keySet()) {
            Assert.assertArrayEquals(refMap.get(key), map.get(key),0.0);
        }
        for(int i = 0; i < 10; i++) {
            int[] key = new int[] {
                r.nextInt(),
                r.nextInt()
            };
            if(refMap.containsKey(key)) {
                continue;
            }
            assert(map.get(key) == null);
        }
    }

}