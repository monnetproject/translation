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
package eu.monnetproject.translation.feat;

import eu.monnetproject.lang.Language;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.TranslationRankerFactory;
import java.util.Set;

/**
 *
 * @author John McCrae
 */
public class MultiplyingFeaturizer extends FunctionalFeaturizer {

    
    public MultiplyingFeaturizer(Ontology ontology, Language srcLang, Language trgLang, Iterable<TranslationRankerFactory> factories) {
        super(ontology, srcLang, trgLang,null,factories);
    }

    public MultiplyingFeaturizer(Ontology ontology, Language srcLang, Language trgLang, Set<Language> pivotLangs, Iterable<TranslationRankerFactory> factories) {
        super(ontology, srcLang, trgLang,pivotLangs,factories);
    }
    
    @Override
    protected Feature[] rescore(Feature[] scores, double factor, String rankerName) {
        for (int i = 0; i < scores.length; i++) {
            // Hack... assume negative scores are log probs, positive probs.
            if (scores[i].score > 0) {
                if (factor > 0) {
                    scores[i] = new Feature(scores[i].name, scores[i].score * factor);
                } else {

                    scores[i] = new Feature(scores[i].name, scores[i].score * Math.exp(factor));
                }
            } else {
                if (factor > 0) {

                    scores[i] = new Feature(scores[i].name, scores[i].score + Math.log(factor));
                } else {

                    scores[i] = new Feature(scores[i].name, scores[i].score + factor);
                }
            }
        }
        return scores;
    }
}
