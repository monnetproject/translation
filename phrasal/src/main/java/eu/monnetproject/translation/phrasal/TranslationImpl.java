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
package eu.monnetproject.translation.phrasal;

import edu.stanford.nlp.mt.base.FeatureValue;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.RichTranslation;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.Translation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author John McCrae
 */
public class TranslationImpl implements Translation {

    private final Language srcLang, trgLang;
    private final RichTranslation<IString, String> translation;

    public TranslationImpl(RichTranslation<IString, String> translation, Language srcLang, Language trgLang) {
        this.srcLang = srcLang;
        this.trgLang = trgLang;
        this.translation = translation;
    }

    @Override
    public Label getSourceLabel() {
        return new TokenizedLabelImpl(translation.foreign, srcLang);
    }

    @Override
    public Label getTargetLabel() {
        return new TokenizedLabelImpl(translation.translation, trgLang);
    }

    @Override
    public URI getEntity() {
        return null;
    }

    @Override
    public double getScore() {
        return translation.score;
    }

    @Override
    public Collection<Feature> getFeatures() {
        ArrayList<Feature> features = new ArrayList<Feature>(translation.features.size());
        for (FeatureValue<String> fv : translation.features) {
            features.add(new Feature(fv.name,fv.value));
        }
        return features;
    }

    public RichTranslation<IString, String> getTranslation() {
        return translation;
    }

    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TranslationImpl other = (TranslationImpl) obj;
        if (this.srcLang != other.srcLang && (this.srcLang == null || !this.srcLang.equals(other.srcLang))) {
            return false;
        }
        if (this.trgLang != other.trgLang && (this.trgLang == null || !this.trgLang.equals(other.trgLang))) {
            return false;
        }
        if (this.translation != other.translation && (this.translation == null || !this.translation.equals(other.translation))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + (this.srcLang != null ? this.srcLang.hashCode() : 0);
        hash = 23 * hash + (this.trgLang != null ? this.trgLang.hashCode() : 0);
        hash = 23 * hash + (this.translation != null ? this.translation.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "TranslationImpl{" + "srcLang=" + srcLang + ", trgLang=" + trgLang + ", translation=" + translation + '}';
    }
    
}
