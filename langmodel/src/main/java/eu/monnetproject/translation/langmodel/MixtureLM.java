/*********************************************************************************
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
package eu.monnetproject.translation.langmodel;

import eu.monnetproject.lang.Language;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class MixtureLM implements LanguageModelAndTrueCaser {
    private final AbstractLM lm1, lm2;
    private final double lambda;

    public MixtureLM(AbstractLM lm1, AbstractLM lm2, double lambda) {
        assert(lm1.getLanguage().equals(lm2.getLanguage()));
        assert(lm1.getOrder() == lm2.getOrder());
        this.lm1 = lm1;
        this.lm2 = lm2;
        this.lambda = lambda;
    }
    
    @Override
    public Language getLanguage() {
        return lm1.getLanguage();
    }

    @Override
    public int getOrder() {
        return lm1.getOrder();
    }

    @Override
    public String getName() {
        return "MixtureOf" + lm1.getName() + "And" + lm2.getName();
    }

    @Override
    public void close() {
        try {
            lm1.close();
        } catch(Exception x) {
            
        }
        try {
            lm2.close();
        } catch(Exception x) {
            
        }
    }

    @Override
    public boolean isRelevantPrefix(List<String> tokens) {
        return lm1.isRelevantPrefix(tokens) || lm2.isRelevantPrefix(tokens);
    }

    @Override
    public double score(List<String> tokens) {
        double p1 = lm1.score(tokens);
        double p2 = lm2.score(tokens);
        if(Double.isInfinite(p1) || Double.isNaN(p1)) {
            return p2;
        } else if(Double.isInfinite(p2) || Double.isNaN(p2)) {
            return p1;
        } else {
            return lambda * p1 + (1 - lambda) * p2;
        }
    }

    @Override
    public int quartile(List<String> tokens) {
        int q1 = lm1.quartile(tokens);
        int q2 = lm2.quartile(tokens);
        // OK this is a hack, but I suspect there is no better way to do it
        return Math.max(q1, q2);
    }

    @Override
    public String[] trueCase(String[] tokens, int id) {
        return lm1.trueCase(tokens, id);
    }
    
    

}
