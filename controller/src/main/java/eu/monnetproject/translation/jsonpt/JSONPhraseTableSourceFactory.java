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

import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.TranslationSourceFactory;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author John McCrae
 */
public class JSONPhraseTableSourceFactory implements TranslationSourceFactory {

    private final JSONReader reader = new JSONReader();

    @Override
    public TranslationSource getSource(Language srcLang, Language trgLang) {
        final Properties props = Configurator.getConfig("eu.monnetproject.translation.jsonpt");
        if (props.containsKey(srcLang.toString())) {
            try {
                final Map<String, PhraseTable> pt = select(srcLang, trgLang, reader.readJSON(new FileReader(props.getProperty(srcLang.toString())), srcLang));
                return new JSONPhraseTableSource(pt, new PhraseTableImpl(srcLang, trgLang, 0, Collections.EMPTY_LIST));

            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        } else {
            return null;
        }
    }

    private Map<String, PhraseTable> select(Language srcLang, Language trgLang, Map<String, Map<Language, List<PhraseTableEntry>>> readJSON) {
        Map<String,PhraseTable> rval = new HashMap<String, PhraseTable>();
        for(String key : readJSON.keySet()) {
            if(readJSON.get(key).containsKey(trgLang)) {
                Language foreign = srcLang, translation = trgLang;
                int longest = 0;
                for(PhraseTableEntry pte : readJSON.get(key).get(trgLang)) {
                    foreign = pte.getForeign().getLanguage();
                    translation = pte.getTranslation().getLanguage();
                    longest = Math.max(longest, pte.getTranslation().asString().split("\\s+").length);
                }
                rval.put(key, new PhraseTableImpl(foreign, translation, longest, readJSON.get(key).get(trgLang)));
            }
        }
        return rval;
    }

    private static class PhraseTableImpl extends ArrayList<PhraseTableEntry> implements PhraseTable {
        private final Language foreignLanguage, translationLanguage;
        private final int longestForeignPhrase;

        public PhraseTableImpl(Language foreignLanguage, Language translationLanguage, int longestForeignPhrase, Collection<? extends PhraseTableEntry> c) {
            super(c);
            this.foreignLanguage = foreignLanguage;
            this.translationLanguage = translationLanguage;
            this.longestForeignPhrase = longestForeignPhrase;
        }
        
        @Override
        public Language getForeignLanguage() {
            return foreignLanguage;
        }

        @Override
        public Language getTranslationLanguage() {
            return translationLanguage;
        }

        @Override
        public String getName() {
            return "JSON-table";
        }

        @Override
        public int getLongestForeignPhrase() {
            return longestForeignPhrase;
        }
    }
}
