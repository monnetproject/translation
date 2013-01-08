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
package eu.monnetproject.translation.sqlpt;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.TokenizedLabel;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.Sequence;

/**
 *
 * @author John McCrae
 */
public class TokenizedLabelImpl implements TokenizedLabel {
    private final List<String> tokens;
    private final String untokenized;
    private final Language language;
    
    public TokenizedLabelImpl(List<String> tokens, Language language) {
        this.tokens = tokens;
        this.language = language;
        final StringBuilder sb = new StringBuilder();
        for(String tk : tokens) {
            sb.append(tk).append(" ");
        }
        this.untokenized = sb.toString().trim();
    }

    @Override
    public String asString() {
        return untokenized;
    }

    @Override
    public Language getLanguage() {
        return language;
    }

    @Override
    public List<String> getTokens() {
        return tokens;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TokenizedLabelImpl other = (TokenizedLabelImpl) obj;
        if (this.tokens != other.tokens && (this.tokens == null || !this.tokens.equals(other.tokens))) {
            return false;
        }
        if (this.language != other.language && (this.language == null || !this.language.equals(other.language))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (this.tokens != null ? this.tokens.hashCode() : 0);
        hash = 37 * hash + (this.language != null ? this.language.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "TokenizedLabelImpl{" + "tokens=" + tokens + ", language=" + language + '}';
    }

}
