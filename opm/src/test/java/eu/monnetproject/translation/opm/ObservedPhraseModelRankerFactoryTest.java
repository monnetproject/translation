/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.opm;

import eu.monnetproject.framework.services.Services;
import eu.monnetproject.translation.TranslationRankerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author jmccrae
 */
public class ObservedPhraseModelRankerFactoryTest {
    
    public ObservedPhraseModelRankerFactoryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getRanker method, of class ObservedPhraseModelRankerFactory.
     */
    @Test
    public void testGetRanker_3args() {
        assertNotNull(Services.get(TranslationRankerFactory.class));
    }

}
