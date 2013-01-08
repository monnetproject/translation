/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.jsonpt;

import java.util.List;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.lang.Language;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class JSONReaderTest {
    
    public JSONReaderTest() {
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
     * Test of readJSON method, of class JSONReader.
     */
    @Test
    public void testReadJSON() {
        System.out.println("readJSON");
        Reader file = new StringReader("{ \"cat\" : { \"es\" : { \"gato\" : 1 } , \"de\" : { \"katze\" : 0.8 } } }");
        JSONReader instance = new JSONReader();
        Map<String,Map<Language,List<PhraseTableEntry>>> result = instance.readJSON(file,Language.ENGLISH);
        assertTrue(result.containsKey("cat"));
        assertTrue(result.get("cat").containsKey(Language.SPANISH));
    }
}
