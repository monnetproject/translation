/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.phrasal.mmap;

import java.io.IOException;
import java.io.File;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class TrieMemoryMapTest {
    
    public TrieMemoryMapTest() {
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

    String[] data = new String[] {
        "aaaa",
        "aab",
        "bbccd",
        "bdddff",
        "bdff"
    };
    long[] idxs = new long[] { 0, 6, 12, 214748364755l, 214748364757l, 214748364758l, 214748364759l, 214748364760l  };
    
    String[] data2 = new String[] {
        "aaaa",
        "bbccd",
        "aab",
        "bdff",
        "bdddff",
        "bdd",    
        "bddccc"            
    };

    
    /**
     * Test of put method, of class TrieMemoryMap.
     */
    @Test
    public void testMemoryMap() throws Exception {
        System.out.println("trieMemoryMap");
        File tmpFile = File.createTempFile("tmp", "tmp");
        tmpFile.deleteOnExit();
        TrieMemoryMap tmm = new TrieMemoryMap(tmpFile);
        for(int i = 0; i < data.length; i++) {
            tmm.put(data[i], idxs[i]);
        }
        tmm.close(idxs[data.length]);
        tmm = new TrieMemoryMap(tmpFile);
        for(int i = 0; i < data.length; i++) {
            long[] val = tmm.get(data[i]);
            long[] expected = new long[2];
            System.arraycopy(idxs, i, expected, 0, 2);
            if(!Arrays.equals(val, expected)) {
                System.err.println(i);
            }
            assertArrayEquals(expected, val);
        }
        long[] result = tmm.get("gggg");
        assertArrayEquals(null, result);
        result = tmm.get("aaaaa");
        assertArrayEquals(null, result);
        result = tmm.get("aaa");
        assertArrayEquals(null, result);
        
    }
    
    /**
     * Test of put method, of class TrieMemoryMap.
     */
    @Test
    public void testMemoryMapNonAscending() throws Exception {
        System.out.println("trieMemoryMap Non-Ascending");
        File tmpFile = File.createTempFile("tmp-noasc", "tmp");
        tmpFile.deleteOnExit();
        TrieMemoryMap tmm = new TrieMemoryMap(tmpFile);
        for(int i = 0; i < data2.length; i++) {
            tmm.put(data2[i], idxs[i]);
        }
        tmm.close(idxs[data2.length]);
        tmm = new TrieMemoryMap(tmpFile);
        for(int i = 0; i < data2.length; i++) {
            long[] val = tmm.get(data2[i]);
            long[] expected = new long[2];
            System.arraycopy(idxs, i, expected, 0, 2);
            if(!Arrays.equals(val, expected)) {
                System.err.println(i);
            }
            assertArrayEquals(expected, val);
        }
        long[] result = tmm.get("gggg");
        assertArrayEquals(null, result);
        result = tmm.get("aaaaa");
        assertArrayEquals(null, result);
        result = tmm.get("aaa");
        assertArrayEquals(null, result);
    }
}
