/**
 * ********************************************************************************
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
 ********************************************************************************
 */
package eu.monnetproject.translation.phrasal.pt;

import eu.monnetproject.lang.Language;
import eu.monnetproject.lang.LanguageCodeFormatException;
import eu.monnetproject.config.Configurator;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.TranslationSourceFactory;
import eu.monnetproject.translation.phrasal.mmap.StableHashByteArray;
import eu.monnetproject.translation.phrasal.pt.cache.Cache;
import eu.monnetproject.translation.monitor.Messages;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author John McCrae
 */
public class MemoryMappedPhraseTableSourceFactory implements TranslationSourceFactory {

    private final HashMap<LangPair, String> ptFiles = new HashMap<LangPair, String>();
    private final HashMap<LangPair, Integer> ptFeatures = new HashMap<LangPair, Integer>();
    private LangPair lastCache;
    //private Cache<StableHashByteArray, PhraseTableEntry> cache;
    private Cache cache;
    private final int cacheSize;
    private final String method;
    public static final String CACHE_PROPERTY = "cache";
    public static final String METHOD_PROPERTY = "method";

    public MemoryMappedPhraseTableSourceFactory() {
        final Properties config = Configurator.getConfig("eu.monnetproject.translation.phrasal.pt");
        this.cacheSize = config.getProperty(CACHE_PROPERTY) == null ? 100000 : Integer.parseInt(config.getProperty(CACHE_PROPERTY));
        this.method = config.getProperty(METHOD_PROPERTY) == null ? "trie" : config.getProperty(METHOD_PROPERTY);
        final Enumeration propertyNames = config.propertyNames();
        while (propertyNames.hasMoreElements()) {

            String langPairStr = propertyNames.nextElement().toString();
            if (langPairStr.equals(CACHE_PROPERTY) || langPairStr.equals(METHOD_PROPERTY)) {
                continue;
            }
            if (!langPairStr.contains("/")) {
                Messages.warning("Bad Key in eu.monnetproject.translation.phrasal.pt.cfg" + langPairStr);
                continue;
            }
            String[] langs = langPairStr.split("/");
            if (langs.length != 3) {
                Messages.warning("Bad Key in eu.monnetproject.translation.phrasal.pt.cfg" + langPairStr);
                continue;
            }
            try {
                Language lang1 = Language.get(langs[0]);
                Language lang2 = Language.get(langs[1]);
                final LangPair lp = new LangPair(lang1, lang2);
                ptFiles.put(lp, config.getProperty(langPairStr));
                final int features = Integer.parseInt(langs[2]);
                ptFeatures.put(lp, features);
            } catch (LanguageCodeFormatException x) {
                Messages.warning("Bad Key in eu.monnetproject.translation.phrasal.pt.cfg" + langPairStr);
                continue;
            }
        }
    }

    @Override
    public TranslationSource getSource(Language srcLang, Language trgLang) {
        final LangPair lp = new LangPair(srcLang, trgLang);
        if (lastCache == null || !lastCache.equals(lp)) {
            //cache = new Cache<StableHashByteArray, PhraseTableEntry>(cacheSize);
            if(method.equals("tree")) {
                cache = new Cache<StableHashByteArray,PhraseTableEntry>(/*cacheSize*/);
            } else if(method.equals("trie")) {
                cache = new Cache<String, PhraseTableEntry>(/*cacheSize*/);
            } else {
                throw new RuntimeException("Unknown mapping method: " + method);
            }
            lastCache = lp;
        }
        if (ptFeatures.containsKey(lp)) {
            try {
                if (method.equals("tree")) {
                    return new TreeMemoryMappedPhraseTableSource(cache, ptFiles.get(lp), ptFeatures.get(lp), srcLang, trgLang);
                } else {
                    return new TrieMemoryMappedPhraseTableSource(cache, ptFiles.get(lp), ptFeatures.get(lp), srcLang, trgLang);
                }
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
        } else {
            return null;
        }
    }
}
