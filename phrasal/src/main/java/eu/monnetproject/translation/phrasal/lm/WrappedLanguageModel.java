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
package eu.monnetproject.translation.phrasal.lm;

import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.LanguageModel;
import edu.stanford.nlp.mt.base.Sequence;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class WrappedLanguageModel implements LanguageModel<IString> {

    private final eu.monnetproject.translation.LanguageModel lm;
    public static final IString START_TOKEN = new IString("<s>");
    public static final IString END_TOKEN = new IString("</s>");

    public WrappedLanguageModel(eu.monnetproject.translation.LanguageModel lm) {
        if(lm == null) {
            throw new IllegalArgumentException("LM cannot be null");
        }
        this.lm = lm;
    }

    @Override
    public double score(Sequence<IString> sequence) {
        List<String> seq2 = new LinkedList<String>();
        for (int i = 0; i < sequence.size(); i++) {
            seq2.add(sequence.get(i).word());
        }
        return lm.score(seq2);
    }

    @Override
    public IString getStartToken() {
        return START_TOKEN;
    }

    @Override
    public IString getEndToken() {
        return END_TOKEN;
    }

    @Override
    public String getName() {
        return lm.getName();
    }

    @Override
    public int order() {
        return lm.getOrder();
    }

    @Override
    public boolean releventPrefix(Sequence<IString> sequence) {
        List<String> seq2 = new LinkedList<String>();
        for (int i = 0; i < sequence.size(); i++) {
            seq2.add(sequence.get(i).word());
        }
        return lm.isRelevantPrefix(seq2);
    }
}
