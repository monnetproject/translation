/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.phrasal.mmap;

import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class TreeMemoryMapTest {
    
    public TreeMemoryMapTest() {
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

    byte[][] data = new byte[][] {
        { 0, 0, 0, 0 },
        { 0, 0, 0, 1 },
        { 1, 1, 2, 2 },
        { 1, 3, 3, 3 },
        { 1, 3, 4, 4 }
    };
    long[] idxs = new long[] { 0, 6, 12, 214748364755l, 214748364757l, 214748364757l  };
    
    /**
     * Test of put method, of class TreeMemoryMap.
     */
    @Test
    public void testMemoryMap() throws Exception {
        System.out.println("treeMemoryMap");
        File tmpFile = File.createTempFile("tmp", "tmp");
        tmpFile.deleteOnExit();
        TreeMemoryMap tmm = new TreeMemoryMap(tmpFile, 4);
        for(int i = 0; i < data.length; i++) {
            tmm.put(data[i], idxs[i]);
        }
        tmm.close(idxs[data.length]);
        tmm = new TreeMemoryMap(tmpFile, 4);
        for(int i = 0; i < data.length; i++) {
            long[] val = tmm.get(data[i]);
            long[] expected = new long[2];
            System.arraycopy(idxs, i, expected, 0, 2);
            assertArrayEquals(expected, val);
        }
        long[] result = tmm.get(new byte[] { 5,5,5,5 });
        assertArrayEquals(null, result);
    }
}
