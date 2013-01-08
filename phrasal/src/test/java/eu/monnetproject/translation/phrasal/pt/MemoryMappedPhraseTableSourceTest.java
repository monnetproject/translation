/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.phrasal.pt;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.phrasal.ChunkImpl;
import eu.monnetproject.translation.phrasal.pt.cache.Cache;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class MemoryMappedPhraseTableSourceTest {

    public MemoryMappedPhraseTableSourceTest() {
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
    private final Cache<CharSequence, PhraseTableEntry> cache = new Cache<CharSequence, PhraseTableEntry>();

    /**
     * Test of candidates method, of class MemoryMappedPhraseTableSource.
     */
    @Test
    public void testCandidates() throws IOException {
        System.out.println("candidates");
        Chunk label = new ChunkImpl("das ist");
        MemoryMappedPhraseTableSource instance = new TrieMemoryMappedPhraseTableSource(cache, "src/test/resources/sample-models/phrase-model/phrase-table.sorted", 5, Language.GERMAN, Language.ENGLISH);
        PhraseTable result = instance.candidates(label);
        for (PhraseTableEntry pte : result) {
            if (pte.getTranslation().asString().equals("this is")) {
                return;
            }
        }
        fail("'this is' should be a candiate of 'das ist'.");
    }

    /**
     * Test of featureCount method, of class MemoryMappedPhraseTableSource.
     */
    @Test
    public void testFeatureCount() throws IOException {
        System.out.println("featureCount");
        MemoryMappedPhraseTableSource instance = new TrieMemoryMappedPhraseTableSource(cache, "src/test/resources/sample-models/phrase-model/phrase-table.sorted", 5, Language.GERMAN, Language.ENGLISH);;
        int expResult = 5;
        int result = instance.featureCount();
        assertEquals(expResult, result);
    }
    
//    @Test
//    public void debugTestPleaseDelete() throws IOException {
//        System.err.println("debug");
//        MemoryMappedPhraseTableSource instance = new TrieMemoryMappedPhraseTableSource(cache, "/home/jmccrae/projects/en-es/phrase-table", 5, Language.ENGLISH, Language.SPANISH);
//        final PhraseTable candidates = instance.candidates(new ChunkImpl("e-commerce"));
//        for(Object pte : candidates) {
//            System.err.println(pte);
//        }
//    }
}
