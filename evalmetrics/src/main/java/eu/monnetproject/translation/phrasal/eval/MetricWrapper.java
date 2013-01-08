/**********************************************************************************
 * Copyright (c) 2011, Monnet Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Monnet Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
package eu.monnetproject.translation.phrasal.eval;

import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.IStrings;
import edu.stanford.nlp.mt.base.ScoredFeaturizedTranslation;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.base.SimpleSequence;
import edu.stanford.nlp.mt.metrics.EvaluationMetric;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.eval.TranslationEvaluator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author John McCrae
 */
public abstract class MetricWrapper implements TranslationEvaluator {

    protected final Map<String, List<String>> references;

    public MetricWrapper(Map<String, List<String>> references) {
        this.references = references;
    }

    public abstract EvaluationMetric<IString, String> getMetric(List<List<Sequence<IString>>> references);

    @Override
    public double score(List<Translation> translations) {
        final List<List<Sequence<IString>>> referenceTranslations = new ArrayList<List<Sequence<IString>>>(translations.size());
        final List<ScoredFeaturizedTranslation<IString, String>> candidates = new ArrayList<ScoredFeaturizedTranslation<IString, String>>();
        for (Translation translation : translations) {
            if (references.containsKey(translation.getSourceLabel().asString())) {
                final List<Sequence<IString>> refs = new ArrayList<Sequence<IString>>();
                for (String refStr : references.get(translation.getSourceLabel().asString())) {
                    refs.add(new SimpleSequence<IString>(IStrings.toIStringArray(FairlyGoodTokenizer.split(refStr))));
                }
                referenceTranslations.add(refs);
                candidates.add(new ScoredFeaturizedTranslation<IString, String>(
                        new SimpleSequence<IString>(IStrings.toIStringArray(FairlyGoodTokenizer.split(translation.getTargetLabel().asString()))),
                        null,
                        translation.getScore()));
            }
        }
        return getMetric(referenceTranslations).score(candidates);
    }

    @Override
    public double maxScore() {
        return getMetric(Collections.EMPTY_LIST).maxScore();
    }
}
