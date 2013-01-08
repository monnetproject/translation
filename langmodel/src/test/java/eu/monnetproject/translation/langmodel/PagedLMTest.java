package eu.monnetproject.translation.langmodel;

import eu.monnetproject.translation.langmodel.PagedLM;
import eu.monnetproject.lang.Language;
import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
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
public class PagedLMTest {

    public PagedLMTest() {
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
     * Test of getLanguage method, of class PagedMapDBLM.
     */
    @Test
    public void testGetLanguage() {
        System.out.println("getLanguage");
    }

    /**
     * Test of getOrder method, of class PagedMapDBLM.
     */
    @Test
    public void testGetOrder() {
        System.out.println("getOrder");
    }

    /**
     * Test of getName method, of class PagedMapDBLM.
     */
    @Test
    public void testGetName() {
        System.out.println("getName");
    }

    /**
     * Test of isRelevantPrefix method, of class PagedMapDBLM.
     */
    @Test
    public void testIsRelevantPrefix() {
        System.out.println("isRelevantPrefix");
    }

    /**
     * Test of score method, of class PagedMapDBLM.
     */
    @Test
    public void testScore() throws Exception {
        PagedLM instance = null;
        try {
            System.out.println("score");
            final File tmpFile = File.createTempFile("enen", "lm");
            tmpFile.deleteOnExit();
            final PrintWriter out = new PrintWriter(tmpFile);
            out.println("");
            out.println("\\data\\");
            out.println("ngram 1=1");
            out.println("ngram 2=0");
            out.println("ngram 3=0");
            out.println("");
            out.println("\\1-grams:");
            out.println("-1\tand\t-1");
            out.println("");
            out.println("\\2-grams:");
            out.println("");
            out.println("\\3-grams:");
            out.println("");
            out.println("\\end\\");
            out.flush();
            out.close();
            List<String> tokens = Arrays.asList("material", "income", "and");
            instance = new PagedLM(Language.ENGLISH, tmpFile);
            double expResult = -1;
            double result = instance.score(tokens);
            assertEquals(expResult, result, 0.0);
        } finally {
            if(instance != null) {
                instance.close();
            }
            clearCache();
        }
    }

    @Test
    public void testToKey() throws Exception {
        PagedLM lm = null;
        try {
            System.err.println("toKey");
            final File tmpFile = File.createTempFile("enen", "lm");
            tmpFile.deleteOnExit();
            final PrintWriter out = new PrintWriter(tmpFile);
            out.println("");
            out.println("\\data\\");
            out.println("ngram 1=3");
            out.println("ngram 2=2");
            out.println("ngram 3=0");
            out.println("");
            out.println("\\1-grams:");
            out.println("-1\tand\t-1");
            out.println("-1\tor\t-1");
            out.println("-1\tbut\t-1");
            out.println("");
            out.println("\\2-grams:");
            out.println("-1\tand but\t-1");
            out.println("-1\tand or\t-1");
            out.println("");
            out.println("\\3-grams:");
            out.println("");
            out.println("\\end\\");
            out.flush();
            out.close();
            lm = new PagedLM(Language.AMHARIC, tmpFile);
            
            assertEquals(-3.0,lm.score(Arrays.asList("and","but","or")),0.0);
        } finally {
            if(lm != null) {
                lm.close();
            }
            clearCache();
        }
    }

    private void clearCache() {
        final File dir = new File("mapDBcache");
        if(dir.exists() && dir.isDirectory()) {
            for(File f : dir.listFiles()) {
                f.delete();
            }
            dir.delete();
        }
    }
    
    /**
     * Test of close method, of class PagedMapDBLM.
     */
    @Test
    public void testClose() {
        System.out.println("close");
    }
}