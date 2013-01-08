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
 ********************************************************************************
 */
package eu.monnetproject.translation.evalmetrics.test;

import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.eval.TranslationEvaluator;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class LinearPREvaluator implements TranslationEvaluator {

    private final List<Translation> references;
    private final TranslationEvaluator semanticScore;
    private final double[] wts;

    public LinearPREvaluator(List<Translation> references, TranslationEvaluator semanticScore, double[] wts) {
        this.references = references;
        this.semanticScore = semanticScore;
        this.wts = wts;
    }

    @Override
    public double score(List<Translation> translations) {
        double score = 0.0;
        int n = 0;
        final Iterator<Translation> refIter = references.iterator();
        for (Translation translation : translations) {
            if (!refIter.hasNext()) {
                throw new RuntimeException("More translations than references");
            }
            final Translation reference = refIter.next();
            for (int i = 1; i < wts.length; i += 2) {
                final double[] nGramPrecisionRecall = DumpScores.nGramPrecisionRecall(translation.getTargetLabel().asString(), reference.getTargetLabel().asString(), (i+1)/2);
                score += wts[i] * nGramPrecisionRecall[0];
                score += wts[i+1] * nGramPrecisionRecall[1];
            }
            n++;
        }
        score += wts[0] * n * semanticScore.score(translations);
        return score / n;
    }

    @Override
    public double maxScore() {
        double maxScore = wts[0] * semanticScore.maxScore();
        for (int i = 1; i < wts.length; i++) {
            maxScore += wts[i];
        }
        return maxScore;
    }

    @Override
    public String getName() {
        return "LINPRSEM";
    }
}
