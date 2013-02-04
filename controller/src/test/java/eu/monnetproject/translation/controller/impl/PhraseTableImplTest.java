package eu.monnetproject.translation.controller.impl;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author john
 */
public class PhraseTableImplTest {

    public PhraseTableImplTest() {
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

    @Test
    public void testMultiSources() {
        System.err.println("multiSources");
        final PhraseTableImpl ptImpl = new PhraseTableImpl(Language.MALTESE, Language.DANISH, "test", 2, Arrays.asList("p(t|f)", PhraseTableImpl.PHRASE_TABLE_SRC_PREFIX + "mock2"));
        ptImpl.addAll(new MockPhraseTable1());
        ptImpl.addAll(new MockPhraseTable2());
        final ArrayList<PhraseTableEntry> result = new ArrayList<PhraseTableEntry>();
        int b0 = 0, b1 = 0;
        int i = 0;
        for (PhraseTableEntry pte : ptImpl) {
            if (pte.getTranslation().asString().equals("b\u0036d")) {
                b0 = i;
            } else if (pte.getTranslation().asString().equals("boat")) {
                b1 = i;
            }
            result.add(pte);
            i++;
        }
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("b\u0036d", result.get(b0).getTranslation().asString());
        Assert.assertArrayEquals(new Feature[]{new Feature("p(t|f)", -1), new Feature(PhraseTableImpl.PHRASE_TABLE_SRC_PREFIX + "mock2", 0.0)}, result.get(b0).getFeatures());
        Assert.assertArrayEquals(new Feature[]{new Feature("p(t|f)", -7), new Feature(PhraseTableImpl.PHRASE_TABLE_SRC_PREFIX + "mock2", 1.0)}, result.get(b1).getFeatures());
        System.err.println("done");

    }

    private static class MockPhraseTableEntry implements PhraseTableEntry {

        private final String foreign, translation;
        private final Feature[] feature;

        public MockPhraseTableEntry(String foreign, String translation, Feature[] feature) {
            this.foreign = foreign;
            this.translation = translation;
            this.feature = feature;
        }

        @Override
        public double getApproxScore() {
            double approxScore = 0.0;
            for (Feature f : feature) {
                approxScore += f.score;
            }
            return approxScore;
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
                    return Language.MALTESE;
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
                    return Language.DANISH;
                }
            };
        }

        @Override
        public Feature[] getFeatures() {
            return feature;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + (this.foreign != null ? this.foreign.hashCode() : 0);
            hash = 41 * hash + (this.translation != null ? this.translation.hashCode() : 0);
            hash = 41 * hash + Arrays.deepHashCode(this.feature);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MockPhraseTableEntry other = (MockPhraseTableEntry) obj;
            if ((this.foreign == null) ? (other.foreign != null) : !this.foreign.equals(other.foreign)) {
                return false;
            }
            if ((this.translation == null) ? (other.translation != null) : !this.translation.equals(other.translation)) {
                return false;
            }
            if (!Arrays.deepEquals(this.feature, other.feature)) {
                return false;
            }
            return true;
        }
    }

    private static class MockPhraseTable1 extends ArrayList<PhraseTableEntry> implements PhraseTable {

        public MockPhraseTable1() {
            super(Arrays.asList(new MockPhraseTableEntry("dg\u0127ajsa", "b\u0036d", new Feature[]{new Feature("p(t|f)", -1)}),
                    new MockPhraseTableEntry("fwar", "damp", new Feature[]{new Feature("p(t|f)", -1)})));
        }

        @Override
        public Language getForeignLanguage() {
            return Language.MALTESE;
        }

        @Override
        public Language getTranslationLanguage() {
            return Language.DANISH;
        }

        @Override
        public String getName() {
            return "mock1";
        }

        @Override
        public int getLongestForeignPhrase() {
            return 1;
        }
    }

    private static class MockPhraseTable2 extends ArrayList<PhraseTableEntry> implements PhraseTable {

        public MockPhraseTable2() {
            super(Arrays.asList(new MockPhraseTableEntry("dg\u0127ajsa", "boat", new Feature[]{}),
                    new MockPhraseTableEntry("fwar", "damp", new Feature[]{})));
        }

        @Override
        public Language getForeignLanguage() {
            return Language.MALTESE;
        }

        @Override
        public Language getTranslationLanguage() {
            return Language.DANISH;
        }

        @Override
        public String getName() {
            return "mock2";
        }

        @Override
        public int getLongestForeignPhrase() {
            return 1;
        }
    }
}
