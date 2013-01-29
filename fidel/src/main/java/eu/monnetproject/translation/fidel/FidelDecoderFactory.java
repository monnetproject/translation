/**
 * *******************************************************************************
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
package eu.monnetproject.translation.fidel;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.translation.Decoder;
import eu.monnetproject.translation.DecoderFactory;
import eu.monnetproject.translation.DecoderWeights;
import eu.monnetproject.translation.LanguageModel;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author John McCrae
 */
public class FidelDecoderFactory implements DecoderFactory {

    @Override
    public Decoder getDecoder(LanguageModel model, int tFeatureCount) {
        if (model instanceof IntegerLanguageModel) {
            return new FidelDecoderWrapper((IntegerLanguageModel)model, getDefaultWeights());
        } else {
            return new FidelDecoderWrapper(new IntegerLanguageModelWrapper(model), getDefaultWeights());
        }
    }

    @Override
    public Decoder getDecoder(LanguageModel model, DecoderWeights weights) {
        if (model instanceof IntegerLanguageModel) {
            return new FidelDecoderWrapper((IntegerLanguageModel)model, weights);
        } else {
            return new FidelDecoderWrapper(new IntegerLanguageModelWrapper(model), weights);
        }
    }

    @Override
    public DecoderWeights getDefaultWeights() {
        Properties config = Configurator.getConfig("eu.monnetproject.translation.wts");
        if (!config.isEmpty()) {
            final DecoderWeightsImpl dwi = new DecoderWeightsImpl();
            for (Object key : config.keySet()) {
                String keyStr = key.toString();
                String value = config.getProperty(key.toString());
                dwi.put(keyStr, Double.parseDouble(value));
            }
            return dwi;
        } else {
            return new DecoderWeightsImpl();
        }
    }

    private static class DecoderWeightsImpl extends HashMap<String,Double> implements DecoderWeights {

    }
    
}
