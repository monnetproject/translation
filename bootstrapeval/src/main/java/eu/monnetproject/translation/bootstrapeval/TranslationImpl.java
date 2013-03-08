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
package eu.monnetproject.translation.bootstrapeval;

import eu.monnetproject.lang.Language;
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
    private final StringLabel src, trg;
    private final URI entity;

    public TranslationImpl(String src, String trg, URI entity) {
        this.src = new StringLabel(src);
        this.trg = new StringLabel(trg);
        this.entity = entity;
    }
    
    @Override
    public Label getSourceLabel() {
        return src;
    }

    @Override
    public Label getTargetLabel() {
        return trg;
    }

    @Override
    public URI getEntity() {
        return entity;
    }

    @Override
    public double getScore() {
        return Double.NaN;
    }

    @Override
    public Collection<Feature> getFeatures() {
        return Collections.EMPTY_LIST;
    }

    private static class StringLabel implements Label {
        private final String l;

        public StringLabel(String l) {
            this.l = l;
        }
        
        
        @Override
        public String asString() {
            return l;
        }

        @Override
        public Language getLanguage() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
}
