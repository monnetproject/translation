package eu.monnetproject.translation.langmodel;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
public class DirectDataMMapTest {

    public DirectDataMMapTest() {
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
     * Test of rawScore method, of class DirectDataMMap.
     */
    @Test
    public void testRawScore() throws Exception {
        System.out.println("rawScore");
        int[] map = {
           1,2,
           0,0, /*0l*/
           0,96 /*0l*/
        };
        int[] data = {
            1,1,1072693248,0,/*1.0*/ 1073741824,0,/*2.0*/
            1,2,1072693248,0,/*1.0*/0,0,
            0,0,0,0,0,0,
            0,0,0,0,0,0
        };
        
        final File f = new File("testScores_tmp");
        final DataOutputStream dosMap = new DataOutputStream(new FileOutputStream(f.getPath() + ".map"));
        for(int i : map) {
            dosMap.writeInt(i);
        }
        dosMap.flush();
        dosMap.close();
        final DataOutputStream dosData = new DataOutputStream(new FileOutputStream(f.getPath()+".data"));
        for(int i : data) {
            dosData.writeInt(i);
        }
        dosData.flush();
        dosData.close();
        
        DirectDataMMap instance = new DirectDataMMap(f);
        assertArrayEquals(new double[] { 1, 2 }, instance.rawScore(new int[] { 1,1 }),0.0);
        assertArrayEquals(new double[] { 1 }, instance.rawScore(new int[] { 1,2 }),0.0);
        assertArrayEquals(null, instance.rawScore(new int[] { 1, 3 }),0.0);
        assertArrayEquals(null, instance.rawScore(new int[] { 3, 2 }),0.0);
        assert(new File(f.getPath()+".map").delete());
        assert(new File(f.getPath()+".data").delete());
    }

}