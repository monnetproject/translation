/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.phrasal.pt.cache;

import java.util.List;
import java.util.Collections;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class CacheTest {

    public CacheTest() {
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
     * Test of put method, of class Cache.
     */
    @Test
    public void testPut() {
        System.out.println("put");
        String objId = "objId";
        String obj = "obj";
        Cache<String, String> instance = new Cache<String, String>(/*1024*/);
        instance.put(objId, Collections.singletonList(obj));
    }

    /**
     * Test of get method, of class Cache.
     */
    @Test
    public void testGet() {
        System.out.println("get");
        String objId = "objId";
        Cache<String, String> instance = new Cache<String, String>(/*1024*/);
        List<String> expResult = Collections.singletonList("exp");
        instance.put(objId, expResult);
        List<String> result = instance.get(objId);
        assertEquals(expResult, result);
    }
//
//    /**
//     * Test of remove method, of class Cache.
//     */
//    @Test
//    public void testRemove() {
//        System.out.println("remove");
//        String objId = "objId";
//        Cache<String, String> instance = new Cache<String, String>(/*1024*/);
//        String res = "res";
//        instance.put(objId, Collections.singletonList(res));
//        instance.remove(objId);
//    }
//
//    /**
//     * Test of size method, of class Cache.
//     */
//    @Test
//    public void testSize() {
//        System.out.println("size");
//        Cache<String, String> instance = new Cache<String, String>(1024);
//        int expResult = 0;
//        int result = instance.size();
//        assertEquals(expResult, result);
//    }
//
//    @Test
//    public void testLRU() throws InterruptedException {
//        System.out.println("lru");
//        final Cache<String, String> instance = new Cache<String, String>(3);
//        instance.put("a", Collections.singletonList("b"));
//        instance.put("b", Collections.singletonList("c"));
//        instance.put("c", Collections.singletonList("d"));
//        // These operations need to be at least 1ms apart
//        synchronized (this) {
//            wait(20);
//        }
//        assertEquals(Collections.singletonList("b"), instance.get("a"));
//        synchronized (this) {
//            wait(20);
//        }
//        instance.put("d", Collections.singletonList("e"));
//        assertEquals(null, instance.get("b"));
//
//    }
}
