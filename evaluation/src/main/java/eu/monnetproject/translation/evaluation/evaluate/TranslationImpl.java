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
package eu.monnetproject.translation.evaluation.evaluate;

import eu.monnetproject.lang.Language;
import eu.monnetproject.lemon.model.LexicalEntry;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.Translation;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author John McCrae
 */
public class TranslationImpl implements Translation {

    private final LexicalEntry sourceEntry, targetEntry;

    public TranslationImpl(LexicalEntry sourceEntry, LexicalEntry targetEntry) {
        this.sourceEntry = sourceEntry;
        this.targetEntry = targetEntry;
    }

    @Override
    public double getScore() {
        return 1.0;
    }

    @Override
    public Collection<Feature> getFeatures() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Label getSourceLabel() {
        return new Label() {

            @Override
            public String asString() {
                return sourceEntry.getCanonicalForm().getWrittenRep().value;
            }

            @Override
            public Language getLanguage() {
                return Language.get(sourceEntry.getCanonicalForm().getWrittenRep().language);
            }

            @Override
            public String toString() {
                return asString();
            }
            
            
        };
    }

    @Override
    public Label getTargetLabel() {
        return new Label() {

            @Override
            public String asString() {
                return targetEntry.getCanonicalForm().getWrittenRep().value;
            }

            @Override
            public Language getLanguage() {
                return Language.get(targetEntry.getCanonicalForm().getWrittenRep().language);
            }

            @Override
            public String toString() {
                return asString();
            }
        };
    }

    @Override
    public URI getEntity() {
        return sourceEntry.getSenses().iterator().next().getReference();
    }
    
    
}
