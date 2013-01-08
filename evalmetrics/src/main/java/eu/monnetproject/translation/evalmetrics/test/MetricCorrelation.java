/**
 * ********************************************************************************
 * Copyright (c) 2011, Monnet Project All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. * Neither the name of the Monnet Project nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *******************************************************************************
 */
package eu.monnetproject.translation.evalmetrics.test;

import eu.monnetproject.framework.services.Services;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.eval.TranslationEvaluator;
import eu.monnetproject.translation.eval.TranslationEvaluatorFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math.stat.correlation.SpearmansCorrelation;

/**
 *
 * @author John McCrae
 */
public class MetricCorrelation {

    public void evaluateCorrelation(File dataFile, TranslationEvaluatorFactory evaluatorFactory, String evaluatorName, Language trgLang) throws FileNotFoundException, IOException {
        final BufferedReader in = new BufferedReader(new FileReader(dataFile));
        String s = null;
        final List<Double> autoScores = new ArrayList<Double>(), manualScores = new ArrayList<Double>();
        int linesRead = 0;
        
        READING: do {
            double[] autoScrsToRank = new double[5];
            double[] manScrs = new double[5];
            for (int i = 0; i < 5; i++) {
                s = in.readLine();
                if (s == null) {
                    break READING;
                }
                linesRead++;
                if (s.matches("\\s*")) {
                    System.out.println("blank line!");
                    continue;
                }
                final String[] ss = s.split(" \\|\\|\\| ");
                assert (ss.length == 3);
                String translation = ss[0];
                String reference = ss[1];
                final double manualScore = Double.parseDouble(ss[2]);
                manScrs[i] = manualScore;
                if (manualScore <= 0) {
                    autoScrsToRank[i] = Double.MAX_VALUE;
                    continue;
                }
                final List<List<Translation>> refTranses = Collections.singletonList(Collections.singletonList((Translation) new TranslationImpl(reference, trgLang)));
                final TranslationEvaluator evaluator = evaluatorFactory.getEvaluator(evaluatorName, refTranses);
                final LinearPREvaluator evaluator2 = new LinearPREvaluator(refTranses.get(0), evaluator, new double[] { 0.17073429, 0.15665801, 0.25034485, 0.16368332 ,0.20878645, 0.06301928, 0.08677381 });
                final double autoScore = evaluator2.score(Collections.singletonList((Translation) new TranslationImpl(translation, trgLang)));
                autoScrsToRank[i] = autoScore;
            }
            fixManRanks(manScrs);
            double[] autoScoresUnranked = Arrays.copyOf(autoScrsToRank, 5);
            Arrays.sort(autoScrsToRank);
            for (int i = 0; i < 5; i++) {
                RANK_SEARCH:
                {
                    if(manScrs[i] == -1){
                        autoScores.add(-1.0);
                        manualScores.add(-1.0);
                        break RANK_SEARCH;
                    }
                    for (int rank = 4; rank >= 0; rank--) {
                        if (autoScoresUnranked[i] == autoScrsToRank[rank]) {
                            autoScores.add(new Double(5 - rank));
                            manualScores.add(manScrs[i]);
                            System.err.println((5 - rank) + "," + manScrs[i]);
                            break RANK_SEARCH;
                        }
                    }
                    throw new RuntimeException();
                }
            }
        } while(s != null);

        double[] autoScrs = new double[autoScores.size()];
        double[] manualScrs = new double[manualScores.size()];
        assert (autoScores.size() == manualScores.size());
        int i = 0;
        for (Double autoScr : autoScores) {
            autoScrs[i++] = autoScr;
        }
        i = 0;
        for (Double manualScr : manualScores) {
            manualScrs[i++] = manualScr;
        }
        final double correlation = new SpearmansCorrelation().correlation(autoScrs, manualScrs);
        System.out.println("Correlation: " + correlation);
    }
    
    public static void fixManRanks(double[] manRanks) {
        int currentRank = 1;
        int negs = 0;
        for(int i = 0; i < manRanks.length; i++) {
            if(manRanks[i] < 1)
                negs++;
        }
        for(int expNextRank = 1; expNextRank <= (manRanks.length-negs); ) {
            // Check how many we have at this rank
            for(int i = 0; i < manRanks.length; i++) {
                if(manRanks[i] == currentRank) {
                    expNextRank++;
                }
            }
            // If we have something between the expected range, shuffle forwards
            for(int i = 0; i < manRanks.length; i++) {
                if(manRanks[i] > currentRank && manRanks[i] < expNextRank) {
                    for(int j = 0; j < manRanks.length; j++) {
                        if(manRanks[j] > currentRank) {
                            manRanks[j] += expNextRank - manRanks[i];
                        }
                    }
                }
            }
            // If we found none at this rank
            if(currentRank == expNextRank) {
                // Find the minimal greater than currentRank 
                double maxNextRank = Double.MAX_VALUE;
                for(int i = 0; i < manRanks.length; i++) {
                    if(manRanks[i] > currentRank && manRanks[i] < maxNextRank) {
                        maxNextRank = manRanks[i];
                    }
                }
                if(maxNextRank == currentRank) {
                    // Well here we really screwed up!
                    throw new RuntimeException();
                }
                // Shuffle all the old ranks up
                for(int i = 0; i < manRanks.length; i++) {
                    if(manRanks[i] == maxNextRank) {
                        manRanks[i] = expNextRank;
                    }
                }
            }
            currentRank = expNextRank;
        }
        
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Needed 3 arguments");
        }
        new MetricCorrelation().evaluateCorrelation(new File(args[0]), Services.getFactory(TranslationEvaluatorFactory.class), args[1], Language.get(args[2]));
    }

    private static class LabelImpl implements Label {

        private final String s;
        private final Language l;

        public LabelImpl(String s, Language l) {
            this.s = s;
            this.l = l;
        }

        @Override
        public String asString() {
            return s;
        }

        @Override
        public Language getLanguage() {
            return l;
        }
    }

    static class TranslationImpl implements Translation {

        private final String s;
        private final Language l;

        public TranslationImpl(String s, Language l) {
            this.s = s;
            this.l = l;
        }

        @Override
        public Label getSourceLabel() {
            return new LabelImpl("", Language.EWE);
        }

        @Override
        public Label getTargetLabel() {
            return new LabelImpl(s, l);
        }

        @Override
        public URI getEntity() {
            return null;
        }

        @Override
        public double getScore() {
            return 0.0;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Collection<Feature> getFeatures() {
            return Collections.EMPTY_LIST;
        }
    }
}
