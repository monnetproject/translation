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
package eu.monnetproject.translation.phrasal.lm;

import edu.stanford.nlp.mt.tools.LanguageModelTrueCaser;
import eu.monnetproject.lang.Language;
import eu.monnetproject.lang.LanguageCodeFormatException;
import eu.monnetproject.config.Configurator;
import eu.monnetproject.translation.LanguageModel;
import eu.monnetproject.translation.LanguageModelFactory;
import eu.monnetproject.translation.TrueCaser;
import eu.monnetproject.translation.monitor.Messages;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author John McCrae
 */
public class ARPALanguageModelFactory implements LanguageModelFactory {

    private final HashMap<Language, String> lmFiles = new HashMap<Language, String>();
    private static Language lastLang;
    private static LanguageModel lastModel;
    private static final Object lock = new Object();

    public ARPALanguageModelFactory() {
        final Properties config = Configurator.getConfig("eu.monnetproject.translation.phrasal.lm");
        final Enumeration propNames = config.propertyNames();
        while (propNames.hasMoreElements()) {
            try {
                Language lang = Language.get(propNames.nextElement().toString());
                lmFiles.put(lang, config.getProperty(lang.toString()));
            } catch (LanguageCodeFormatException x) {
                Messages.componentLoadFail(ARPALanguageModelFactory.class,x);
            }
        }
    }

    @Override
    public LanguageModel getModel(Language language) {
        if (lmFiles.containsKey(language)) {
            try {
                synchronized (lock) {
                    if (lastLang != null && lastLang.equals(language)) {
                        return lastModel;
                    }
                    lastLang = language;
                    return lastModel = new ARPALanguageModel(lmFiles.get(language), language);
                }
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
        } else {
            return null;
        }
    }

    @Override
    public TrueCaser getTrueCaser(Language language) {
        if (lmFiles.containsKey(language)) {
            synchronized (lock) {
                final LanguageModelTrueCaser languageModelTrueCaser = new LanguageModelTrueCaser();
                languageModelTrueCaser.init(language, this);
                return languageModelTrueCaser;
            }
        } else {
            return null;
        }
    }
}
