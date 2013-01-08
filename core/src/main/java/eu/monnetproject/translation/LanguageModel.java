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
package eu.monnetproject.translation;

import eu.monnetproject.lang.Language;
import java.util.List;

/**
 * A language model capable of estimating the probability of a phrase in a language
 * 
 * @author John McCrae
 */
public interface LanguageModel {
    /**
     * @return The language of the model
     */
    Language getLanguage();
    /**
     * @return The order of the model, e.g., the maximum n-gram
     */
    int getOrder();
    /**
     * @return The name of the model (informative only)
     */
    String getName();
    /**
     * @param tokens The prefix
     * @return true if this prefix can be backed-off to
     */
    boolean isRelevantPrefix(List<String> tokens);
    /**
     * Score a sequence of tokens
     * @param tokens The tokens
     * @return The score
     */
    double score(List<String> tokens);
    
    /**
     * Close any resources held by the model
     */
    void close();
    
    /**
     * Return the quartile of probability the token string belongs to
     * @param tokens The tokens
     * @return 1..4 where 1 is lowest probability quartile or 0 for not found.
     */
    int quartile(List<String> tokens);
}
