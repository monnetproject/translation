package eu.monnetproject.translation.phrasal;

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
public class FairlyGoodTokenizerTest {

    public FairlyGoodTokenizerTest() {
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
     * Test of split method, of class FairlyGoodTokenizer.
     */
    @Test
    public void testSplit() {
        System.out.println("split");
        String s = "This is a test, there are some things, that are hard like a\u00a0nbsp and some odd \u201cquotes\u201d";
        String[] expResult = {"This","is","a","test",",","there","are","some","things",",","that","are","hard","like","a","nbsp","and","some","odd","\u201c","quotes","\u201d" };
        String[] result = FairlyGoodTokenizer.split(s);
        assertArrayEquals(expResult, result);
    }

}