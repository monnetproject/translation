/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.controller.impl;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jmccrae
 */
public class SimpleLexicalizerTest {
    
    public SimpleLexicalizerTest() {
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
     * Test of lexicalize method, of class SimpleLexicalizer.
     */
    @Test
    public void testLexicalize() throws Exception {
        System.out.println("lexicalize");
//        OntologySerializer os = new OWLAPISerializer();
//       final Ontology ontology = os.read(new FileReader("/home/jmccrae/projects/MultilingualOntologies/ifrs-cor_2009-04-01.rdf"));
//        SimpleLexicalizer instance = new SimpleLexicalizer();
//        Collection<Lexicon> result = instance.lexicalize(ontology);
//        for(Lexicon lexicon : result) {
//            System.err.println(lexicon.getLanguage() + " -> ");
//            System.err.println(lexicon.getEntrys().size());
//        }
        
    }

    /**
     * Test of getBlankLexicon method, of class SimpleLexicalizer.
     */
    @Test
    public void testGetBlankLexicon() {
        System.out.println("getBlankLexicon");
    }
}
