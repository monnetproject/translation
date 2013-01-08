/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.phrasal.mmap;

import eu.monnetproject.translation.phrasal.mmap.CountingLineReader.Line;
import java.io.ByteArrayInputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class CountingLineReaderTest {
    
    public CountingLineReaderTest() {
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
     * Test of readLine method, of class CountingLineReader.
     */
    @Test
    public void testReadLine() throws Exception {
        System.out.println("readLine");
        String x = "this is a test\nand this should be a really long line with lots of bytes\nAnd a third line\nx";
        CountingLineReader instance = new CountingLineReader(new ByteArrayInputStream(x.getBytes()),64);
        Line expResult = new Line("this is a test", 0, 14);
        Line result = instance.readLine();
        assertEquals(expResult, result);
        expResult = new Line("and this should be a really long line with lots of bytes",15,71);
        result = instance.readLine();
        assertEquals(expResult, result);
        expResult = new Line("And a third line",72,88);
        result = instance.readLine();
        assertEquals(expResult, result);
        expResult = new Line("x",89,90);
        result = instance.readLine();
        assertEquals(expResult, result);
        expResult = null;
        result = instance.readLine();
        assertEquals(expResult, result);
    }
}
