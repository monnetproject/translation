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
package eu.monnetproject.translation.phrasal.eval;

import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.eval.TranslationEvaluator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 * @author John McCrae
 */
public class OLDBLEUMetric implements TranslationEvaluator {

    private final double[] weights;
    private final NGramPrecision[] nGramPrecisions;
    protected final Map<String, List<String>> references;

    public OLDBLEUMetric(Map<String, List<String>> references) {
        weights = new double[]{0.33, 0.33, 0.33};
        this.nGramPrecisions = new NGramPrecision[weights.length];
        for (int i = 0; i < weights.length; i++) {
            nGramPrecisions[i] = new NGramPrecision(i + 1);
        }
        this.references = references;
    }

    public double score(List<Translation> translations) {
        double sum = 0.0;
        int n = 0;
        for (Translation translation : translations) {
            double[] actualWeights = weights;
            Collection<String> refs = references.get(translation.getSourceLabel().asString());
            if (refs.isEmpty()) {
                return Double.NaN;
            }
            List<String> resultTokens = Arrays.asList(FairlyGoodTokenizer.split(translation.getTargetLabel().asString()));
            if (resultTokens.size() < weights.length) {
                actualWeights = new double[resultTokens.size()];
                double weightSum = 0.0;
                for (int i = 0; i < resultTokens.size(); i++) {
                    actualWeights[i] = weights[i];
                    weightSum += weights[i];
                }
                for (int i = 0; i < resultTokens.size(); i++) {
                    actualWeights[i] = actualWeights[i] / weightSum;
                }
            }
            List<List<String>> referenceTokens = new ArrayList<List<String>>(refs.size());
            for (String reference : refs) {
                referenceTokens.add(Arrays.asList(FairlyGoodTokenizer.split(reference)));
            }
            int bestReferenceLength = Integer.MAX_VALUE;
            for (List<String> tkList : referenceTokens) {
                bestReferenceLength = Math.min(bestReferenceLength, tkList.size());
            }
            double val = 0.0;
            double weightSum = 0.0;
            for (int i = 0; i < Math.min(nGramPrecisions.length, resultTokens.size()); i++) {
                double p_n = nGramPrecisions[i].getEvaluationMetic(resultTokens, referenceTokens);
                val += weights[i] * Math.log(p_n);
                weightSum += weights[i];
            }
            double bleu = Math.min(1 - (double) bestReferenceLength / (double) resultTokens.size(), 0) + val;
            sum += Math.exp(bleu);
            n++;
        }
        return sum / n;
    }
    /*
     * public LabelExtractor getReferenceExtractor() { return
     * referenceExtractor; }
     */

    @Override
    public double maxScore() {
        return 1.0;
    }

    @Override
    public String getName() {
        return "OLDBLEU";
    }
}
