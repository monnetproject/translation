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
package eu.monnetproject.translation.jsonpt;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.PhraseTableEntry;

/**
 *
 * @author John McCrae
 */
public class JSONPhraseTableEntryImpl implements PhraseTableEntry {

    private final String src, trg;
    private final Language srcLang, trgLang;
    private final double feature;

    public JSONPhraseTableEntryImpl(String src, String trg, Language srcLang, Language trgLang, double feature) {
        this.src = src;
        this.trg = trg;
        this.srcLang = srcLang;
        this.trgLang = trgLang;
        this.feature = feature;
    }

    @Override
    public Label getForeign() {
        return new Label() {

            @Override
            public String asString() {
                return src;
            }

            @Override
            public Language getLanguage() {
                return srcLang;
            }
        };
    }

    @Override
    public Label getTranslation() {
        return new Label() {

            @Override
            public String asString() {
                return trg;
            }

            @Override
            public Language getLanguage() {
                return trgLang;
            }
        };
    }

    @Override
    public Feature[] getFeatures() {
        return new Feature[]{new Feature("TM:phi(t|f)",feature), 
            new Feature("TM:lex(t|f)",feature), 
            new Feature("TM:phi(f|t)",feature), 
            new Feature("TM:lex(f|t)",feature), 
            new Feature("TM:phrasePenalty",feature)};
    }
}
