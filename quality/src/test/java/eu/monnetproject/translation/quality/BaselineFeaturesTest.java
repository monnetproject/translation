/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.quality;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.LanguageModel;
import eu.monnetproject.translation.LanguageModelFactory;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.TranslationSourceFactory;
import eu.monnetproject.translation.TrueCaser;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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
public class BaselineFeaturesTest {
    
    public BaselineFeaturesTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    private BaselineFeatures instance;
    
    @Before
    public void setUp() {
        instance = new BaselineFeatures(new LanguageModelFactory() {

            @Override
            public LanguageModel getModel(Language language) {
                return new MockLanguageModel();
            }

            @Override
            public TrueCaser getTrueCaser(Language language) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            
        }, new TranslationSourceFactory() {

            @Override
            public TranslationSource getSource(Language srcLang, Language trgLang) {
                return new MockTranslationSource();
            }
            
        });
    }
    
    @After
    public void tearDown() {
    }

    private static class MockTranslation implements Translation {
        private final String source, target;

        public MockTranslation(String source, String target) {
            this.source = source;
            this.target = target;
        }
        
        @Override
        public Label getSourceLabel() {
            return new Label() {

                @Override
                public String asString() {
                    return source;
                }

                @Override
                public Language getLanguage() {
                    return Language.MALAY;
                }
            };
        }

        @Override
        public Label getTargetLabel() {
            return new Label() {

                @Override
                public String asString() {
                    return target;
                }

                @Override
                public Language getLanguage() {
                    return Language.PASHTO;
                }
            };
        }

        @Override
        public URI getEntity() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double getScore() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Collection<Feature> getFeatures() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
    private static class MockLanguageModel implements LanguageModel {

        @Override
        public Language getLanguage() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getOrder() {
            return 3;
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isRelevantPrefix(List<String> tokens) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double score(List<String> tokens) {
            return 0.5;
        }

        @Override
        public void close() {
            
        }

        @Override
        public int quartile(List<String> tokens) {
            return 4;
        }
        
        
    }
    
    private static class MockTranslationSource implements TranslationSource {
        
        @Override
        public String[] featureNames() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public PhraseTable candidates(Chunk label) {
            return new MockPhraseTable();
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
    private static class MockPhraseTableEntry implements PhraseTableEntry {
        private final String foreign, translation;
        private final double thirdScore;

        public MockPhraseTableEntry(String foreign, String translation, double thirdScore) {
            this.foreign = foreign;
            this.translation = translation;
            this.thirdScore = thirdScore;
        }

        @Override
        public double getApproxScore() {
            return Math.log(thirdScore);
        }
        
        
        
        
        @Override
        public Label getForeign() {
            return new Label() {

                @Override
                public String asString() {
                    return foreign;
                }

                @Override
                public Language getLanguage() {
                    return Language.MALAY;
                }
            };
        }

        @Override
        public Label getTranslation() {
            return new Label() {

                @Override
                public String asString() {
                    return translation;
                }

                @Override
                public Language getLanguage() {
                    return Language.PASHTO;
                }
            };
        }

        @Override
        public Feature[] getFeatures() {
            return new Feature[] {
                null,
                null,
                new Feature("t", Math.log(thirdScore)),
                null,
                null
            };
        }
                
    }
    
    private static class MockPhraseTable extends LinkedList<PhraseTableEntry> implements PhraseTable {

        public MockPhraseTable() {
            super(Arrays.asList(new MockPhraseTableEntry("A", "1", 0.5), new MockPhraseTableEntry("B", "2", 0.1)));
        }
        
        @Override
        public Language getForeignLanguage() {
            return Language.MALAY;
        }

        @Override
        public Language getTranslationLanguage() {
            return Language.PASHTO;
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getLongestForeignPhrase() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    
    /**
     * Test of countTksInSrc method, of class BaselineFeatures.
     */
    @Test
    public void testCountTksInSrc() {
        System.out.println("countTksInSrc");
        Translation translation = new MockTranslation("A B C", "1 2 3 4");
        double expResult = 3.0;
        double result = BaselineFeatures.countTksInSrc(translation);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of countTksInTrg method, of class BaselineFeatures.
     */
    @Test
    public void testCountTksInTrg() {
        System.out.println("countTksInTrg");
        Translation translation = new MockTranslation("A B C", "1 2 3 4");
        double expResult = 4.0;
        double result = BaselineFeatures.countTksInTrg(translation);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of aveSrcTkLen method, of class BaselineFeatures.
     */
    @Test
    public void testAveSrcTkLen() {
        System.out.println("aveSrcTkLen");
        Translation translation = new MockTranslation("AA BBB C", "1 2 3 4");
        double expResult = 2.0;
        double result = BaselineFeatures.aveSrcTkLen(translation);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of sourceLMProb method, of class BaselineFeatures.
     */
    @Test
    public void testSourceLMProb() {
        System.out.println("sourceLMProb");
        Translation translation = new MockTranslation("AA BBB C", "1 2 3 4");
        double expResult = 1.5;
        double result = instance.sourceLMProb(translation);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of targetLMProb method, of class BaselineFeatures.
     */
    @Test
    public void testTargetLMProb() {
        System.out.println("targetLMProb");
        Translation translation = new MockTranslation("AA BBB C", "1 2 3 4");
        double expResult = 2.0;
        double result = instance.targetLMProb(translation);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of aveOccurencesInTarget method, of class BaselineFeatures.
     */
    @Test
    public void testAveOccurencesInTarget() {
        System.out.println("aveOccurencesInTarget");
        Translation translation = new MockTranslation("A B C", "1 2 3 2 2 3 4");
        double expResult = 7.0 / 4.0;
        double result = BaselineFeatures.aveOccurencesInTarget(translation);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of aveTranslationCount method, of class BaselineFeatures.
     */
    @Test
    public void testAveTranslationCount() {
        System.out.println("aveTranslationCount");
        Translation translation = new MockTranslation("A B C", "1 2 3 2 2 3 4");
        double minProb = 0.2;
        double expResult = 1.0;
        double result = instance.aveTranslationCount(translation, minProb);
        assertEquals(expResult, result, 0.0);
        double result2 = instance.aveTranslationCount(translation, 0.01);
        double expResult2 = 2.0;
        assertEquals(expResult2, result2, 0.0);
    }

    /**
     * Test of percentNGramsInTopBotQuartile method, of class BaselineFeatures.
     */
    @Test
    public void testPercentNGramsInTopBotQuartile() {
        System.out.println("percentNGramsInTopBotQuartile");
        Configurator.setConfig("eu.monnetproject.translation.langmodel", "ms","src/test/resources/model.lm");
        Translation translation = new MockTranslation("A B C", "1 2 3");
        int n = 1;
        double[] expResult = new double[] { 1.0, 0.0 };
        double[] result = instance.percentNGramsInTopBotQuartile(translation, n);
        assertArrayEquals(expResult, result,0.0);
    }

    /**
     * Test of percentUnigramsInLM method, of class BaselineFeatures.
     */
    @Test
    public void testPercentUnigramsInLM() {
        System.out.println("percentUnigramsInLM");
        Configurator.setConfig("eu.monnetproject.translation.langmodel", "ms","src/test/resources/model.lm");
        Translation translation = new MockTranslation("A B C E", "1 2 3");
        double expResult = 1.0;
        double result = instance.percentUnigramsInLM(translation);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of countPunctuationInSource method, of class BaselineFeatures.
     */
    @Test
    public void testCountPunctuationInSource() {
        System.out.println("countPunctuationInSource");
        Translation translation = new MockTranslation("A B C E.", "1 2 3");
        double expResult = 1.0;
        double result = BaselineFeatures.countPunctuationInSource(translation);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of countPunctuationInTarget method, of class BaselineFeatures.
     */
    @Test
    public void testCountPunctuationInTarget() {
        System.out.println("countPunctuationInTarget");
        Translation translation = new MockTranslation("A B C E.", "1 2 3 !,");
        double expResult = 1.0;
        double result = BaselineFeatures.countPunctuationInTarget(translation);
        assertEquals(expResult, result, 0.0);
    }
}
