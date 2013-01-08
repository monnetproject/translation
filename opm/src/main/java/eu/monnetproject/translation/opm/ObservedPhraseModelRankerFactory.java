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
package eu.monnetproject.translation.opm;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.label.LabelExtractor;
import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.lang.Language;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.translation.TranslationRanker;
import eu.monnetproject.translation.TranslationRankerFactory;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author John McCrae
 */
public class ObservedPhraseModelRankerFactory implements TranslationRankerFactory {
    private final LabelExtractor extractor;

    private final LabelExtractorFactory labelExtractorFactory;

    public ObservedPhraseModelRankerFactory(LabelExtractorFactory labelExtractorFactory) {
        this.labelExtractorFactory = labelExtractorFactory;
        this.extractor = labelExtractorFactory.getExtractor(Collections.EMPTY_LIST, false, false);
    }
    
    
    @Override
    public TranslationRanker getRanker(Ontology ontology, Language srcLang, Language trgLang) {
        
        final Properties config = Configurator.getConfig("eu.monnetproject.translation.opm");
        if (!config.isEmpty()) {
            return new ObservedPhraseModelRanker(extractor, ontology, srcLang, trgLang);
        } else {
            return null;
        }
    }

    @Override
    public TranslationRanker getRanker(Ontology ontology, Language srcLang, Language trgLang, Set<Language> extraLanguages) {
        return getRanker(ontology, srcLang, trgLang);
    }

}
