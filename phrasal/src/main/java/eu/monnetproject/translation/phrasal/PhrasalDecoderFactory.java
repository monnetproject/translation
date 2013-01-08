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
package eu.monnetproject.translation.phrasal;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.translation.Decoder;
import eu.monnetproject.translation.DecoderFactory;
import eu.monnetproject.translation.DecoderWeights;
import eu.monnetproject.translation.LanguageModel;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.Translation;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author John McCrae
 */
public class PhrasalDecoderFactory implements DecoderFactory {

    public static final DecoderWeights oneWeight = new DecoderWeightsImpl();
    public static final DecoderWeights fiveWeight = new DecoderWeightsImpl();

    static {
        oneWeight.put("UnknownWord", 1.0);
        oneWeight.put("WordPenalty", 0.0);
        oneWeight.put("SentenceBoundary", 1.0);
        oneWeight.put("TM:p(t|f)", 1.0);
        oneWeight.put("LM", 1.0);
        oneWeight.put("LinearDistorion", 1.0);

        fiveWeight.put("UnknownWord", 1.0);
        fiveWeight.put("WordPenalty", 0.0);
        fiveWeight.put("SentenceBoundary", 1.0);
        fiveWeight.put("TM:phi(t|f)", 0.2);
        fiveWeight.put("TM:phi(f|t)", 0.2);
        fiveWeight.put("TM:lex(f|t)", 0.2);
        fiveWeight.put("TM:lex(f|t)", 0.2);
        fiveWeight.put("TM:phrasePenalty", 0.2);
        fiveWeight.put("LM", 1.0);
        fiveWeight.put("LinearDistorion", 1.0);

    }

    ;
    
    @Override
    public Decoder getDecoder(LanguageModel model, int tFeatureCount) {
        if (tFeatureCount == 1) {
            return getDecoder(model, oneWeight);
        } else if (tFeatureCount == 5) {
            return getDecoder(model, fiveWeight);
        } else {
            throw new IllegalArgumentException("Phrasal supports a default of 1 or 5 args");
        }
    }

    @Override
    public Decoder getDecoder(final LanguageModel model, final DecoderWeights weights) {
        //return new PhrasalDecoder(model, weights);
        return new ThreadLocalDecoder(model, weights);
    }

    // Some parts of phrasal have memory, therefore we cannot have two threads 
    // simultaneously using the same decoder... hence we create a decoder for each
    // thread. The thread local ensures that the same thread always gets the same
    // decoder
    private static final class ThreadLocalDecoder implements Decoder {

        private final LanguageModel model;
        private final DecoderWeights weights;

        public ThreadLocalDecoder(LanguageModel model, DecoderWeights weights) {
            this.model = model;
            this.weights = weights;
        }

        @Override
        public List<Translation> decode(List<String> phrase, PhraseTable phraseTable, List<String> featureNames, int nBest) {
            return decoder.get().decode(phrase, phraseTable,featureNames, nBest);
        }

        @Override
        public List<Translation> decodeFast(List<String> phrase, PhraseTable phraseTable, List<String> featureNames, int nBest) {
            return decoder.get().decodeFast(phrase, phraseTable, featureNames, nBest);
        }
        
        
        private final ThreadLocal<Decoder> decoder = new ThreadLocal<Decoder>() {

            @Override
            protected Decoder initialValue() {
                try {
                    return new PhrasalDecoder(model, weights);
                } catch (IOException x) {
                    throw new RuntimeException(x);
                } catch (ClassNotFoundException x) {
                    throw new RuntimeException(x);
                }
            }
        };
    }

    @Override
    public DecoderWeights getDefaultWeights() {

        Properties config = Configurator.getConfig("eu.monnetproject.translation.phrasal");
        if (!config.isEmpty()) {
            final DecoderWeightsImpl dwi = new DecoderWeightsImpl();
            for (Object key : config.keySet()) {
                String keyStr = key.toString();
                String value = config.getProperty(key.toString());
                dwi.put(keyStr, Double.parseDouble(value));
            }
            return dwi;
        } else {
            return null;
        }
    }
}
