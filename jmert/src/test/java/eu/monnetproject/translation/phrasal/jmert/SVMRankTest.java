/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.phrasal.jmert;

import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class SVMRankTest {

    public SVMRankTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of rankSVM method, of class SVMRank.
     */
    @Test
    public void testRankSVM() {
        System.out.println("rankSVM");
        double[][] X = {
            {1.0, 2.0, 3.0},
            {2, 4, 6},
            {3, 6, 9}
        };
        double[][] A = {
            {-1.0, 1.0, 0.0},
            {0.0, -1.0, 1.0},
            {-1.0, 0.0, 1.0}
        };
        double C = 1;
        SVMRank instance = new SVMRank(X, A, C);
        double[] expResult = {0.424, 0.847, 1.271};
        double[] result = instance.solve(false);
        assertArrayEquals(expResult, result, 0.01);
    }

    /**
     * Test of rankSVM method, of class SVMRank.
     */
    @Test
    public void testRankSVMSparse() {
        System.out.println("rankSVMSparse");
        double[][] X = {
            {1.0, 2.0, 3.0},
            {2, 4, 6},
            {3, 6, 9}
        };
        int[][] spA = {
            {1, 0},
            {2, 1},
            {2, 0}
        };
        double C = 1;
        SVMRank instance = new SVMRank(X, spA, C);
        double[] expResult = {0.424, 0.847, 1.271};
        double[] result = instance.solve(false);
        assertArrayEquals(expResult, result, 0.01);
    }
    
    /**
     * Test of rankSVM method, of class SVMRank.
     */
    @Test
    public void testRankSVMSparseMinRes() {
        System.out.println("rankSVMMinRes");
        double[][] X = {
            {1.0, 2.0, 3.0},
            {2, 4, 6},
            {3, 6, 9}
        };
        int[][] spA = {
            {1, 0},
            {2, 1},
            {2, 0}
        };
        double C = 1;
        SVMRank instance = new SVMRank(X, spA, C);
        double[] expResult = {0.424, 0.847, 1.271};
        double[] result = instance.solve(true);
        assertArrayEquals(expResult, result, 0.01);
    }

    @Test
    public void testHessMult() {
        System.out.println("hessMult");
        final double[] result = SVMRank.hessVectMult(new double[][]{{0, 1}, {1, 0}}, 
                new int[][]{{0, 1}}, 
                new double[]{3, 5}, 
                new int[]{0}, 1, 
                1);
        double[] expResult = {-1,9};
        assertArrayEquals(expResult, result, 0.01);
    }
    
    @Test
    public void testGmres() {
        System.out.println("gmres");
        double[] result = SVMRank.gmres(new double[][]{ {0,1},{1,0}},
                new double[] { 1, 0 }, 
                new double[] { 1, 1 },
                2);
        double[] expResult = { 0,1 };
        assertArrayEquals(expResult, result, 0.01);
        
    }
}
