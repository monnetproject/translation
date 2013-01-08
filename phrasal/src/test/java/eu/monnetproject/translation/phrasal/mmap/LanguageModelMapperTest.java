/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.phrasal.mmap;

import java.io.File;
import java.io.PrintWriter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class LanguageModelMapperTest {
    
    public LanguageModelMapperTest() {
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

    private final String data = "\n\\data\\\nngram 1=5\nngram 2=2\n\n\\1-grams\n"
            + "-2.1701261\tcat\t-1.816745\n"
            + "-56\tdog\t-67\n"
            + "-48\tfish\t56\n"
            + "-465\tkoala\t565\n"
            + "-232\tocelot\t56\n\n"
            + "\\2-grams\n"
            + "-564\tdog fish\t-456\n"
            + "-4556\tocelot koala\t-456\n\n"
            + "\\end\\\n";
    
    /**
     * Test of mapLanguageModel method, of class LanguageModelMapper.
     */
    @Test
    public void testMapLanguageModel() throws Exception {
        System.out.println("mapLanguageModel");
        File languageModel = File.createTempFile("langmodel", ".lm");
        languageModel.deleteOnExit();
        final PrintWriter pw = new PrintWriter(languageModel);
        pw.print(data);
        pw.close();
        File mapFile = File.createTempFile("langmodel", ".map");
        mapFile.deleteOnExit();
        LanguageModelMapper instance = new LanguageModelMapper();
        instance.mapLanguageModel(languageModel, mapFile);
    }

}
