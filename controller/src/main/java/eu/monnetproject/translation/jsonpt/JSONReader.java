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
import eu.monnetproject.translation.PhraseTableEntry;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author John McCrae
 */
public class JSONReader {

    public Map<String, Map<Language, List<PhraseTableEntry>>> readJSON(Reader file, Language srcLang) {
        try {
            final JSONObject jsonObject = new JSONObject(new org.json.JSONTokener(file));
            final Map<String, Map<Language, List<PhraseTableEntry>>> map = new HashMap<String, Map<Language, List<PhraseTableEntry>>>();
            final Iterator<String> keysIter = jsonObject.keys();
            while(keysIter.hasNext()) {
                String key = keysIter.next();
                try {
                    final JSONObject translationsJSON = jsonObject.getJSONObject(key);
                    final HashMap<Language, List<PhraseTableEntry>> translations = new HashMap<Language,List<PhraseTableEntry>>();
                    final Iterator<String> transIter = translationsJSON.keys();
                    while(transIter.hasNext()) {
                        final String langStr = transIter.next();
                        final JSONObject scoredTransesJSON = translationsJSON.getJSONObject(langStr);
                        final LinkedList<PhraseTableEntry> ptes = new LinkedList<PhraseTableEntry>();
                        final Iterator<String> indivTranses = scoredTransesJSON.keys();
                        while(indivTranses.hasNext()) {
                            String indivTrans = indivTranses.next();
                            ptes.add(new JSONPhraseTableEntryImpl(key, indivTrans, srcLang, Language.get(langStr), scoredTransesJSON.getDouble(indivTrans)));
                        }
                        translations.put(Language.get(langStr), ptes);
                    }
                    map.put(key, translations);
                } catch (JSONException x) {
                    throw new RuntimeException(x);
                }
            }
            return map;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
        
    }
}
