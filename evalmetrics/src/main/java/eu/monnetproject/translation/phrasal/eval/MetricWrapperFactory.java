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

import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.eval.TranslationEvaluator;
import eu.monnetproject.translation.eval.TranslationEvaluatorFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author John McCrae
 */
public class MetricWrapperFactory implements TranslationEvaluatorFactory {

    @Override
    public Collection<TranslationEvaluator> getEvaluators(List<List<Translation>> references) {
        final Map<String, List<String>> mapped = mapTranslation(references);
        final LinkedList<TranslationEvaluator> evaluators = new LinkedList<TranslationEvaluator>();
        evaluators.add(new BLEU(mapped));
        evaluators.add(new BLEU(2, mapped));
        evaluators.add(new METEOR(mapped));
        evaluators.add(new NIST(mapped));
        evaluators.add(new PER(mapped));
        evaluators.add(new TER(mapped));
        evaluators.add(new WER(mapped));
        //evaluators.add(new OLDBLEUMetric(mapped));
        return Collections.unmodifiableList(evaluators);
    }

    @Override
    public TranslationEvaluator getEvaluator(String name, List<List<Translation>> references) {
        final Map<String, List<String>> mapped = references == null ? null : mapTranslation(references);
        if(name.equalsIgnoreCase("BLEU")) {
            return new BLEU(mapped);
        } else if(name.equalsIgnoreCase("BLEU-2")) {
            return new BLEU(2, mapped);
        } else if(name.equalsIgnoreCase("METEOR")) {
            return new METEOR(mapped);
        } else if(name.equalsIgnoreCase("NIST")) {
            return new NIST(mapped);
        } else if(name.equalsIgnoreCase("PER")) {
            return new PER(mapped);
        } else if(name.equalsIgnoreCase("TER")) {
            return new TER(mapped);
        } else if(name.equalsIgnoreCase("WER")) {
            return new WER(mapped);
        } else {
            throw new IllegalArgumentException();
        }
    }

    
    
    private Map<String, List<String>> mapTranslation(List<List<Translation>> references) {
        final HashMap<String, List<String>> mapped = new HashMap<String, List<String>>();
        for (List<Translation> translations : references) {
            for (Translation translation : translations) {
                final String sourceLabel = translation.getSourceLabel().asString();
                if (!mapped.containsKey(sourceLabel)) {
                    mapped.put(translation.getSourceLabel().asString(), new LinkedList<String>());
                }
                mapped.get(sourceLabel).add(translation.getTargetLabel().asString());
            }
        }
        return mapped;
    }
}
