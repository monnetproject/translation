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
 * *******************************************************************************
 */
package eu.monnetproject.translation.opm;

import eu.monnetproject.label.LabelExtractor;
import eu.monnetproject.lang.Language;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.TranslationRanker;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author John McCrae
 */
public class ObservedPhraseModelRanker implements TranslationRanker {

    private final LabelExtractor extractor;
    private final Ontology ontology;
    private final Language srcLang, trgLang;
    private final Map<Entity, Collection<String>> srcLabels = new HashMap<Entity, Collection<String>>(),
            trgLabels = new HashMap<Entity, Collection<String>>();

    public ObservedPhraseModelRanker(LabelExtractor extractor, Ontology ontology, Language srcLang, Language trgLang) {
        this.extractor = extractor;
        this.ontology = ontology;
        this.srcLang = srcLang;
        this.trgLang = trgLang;
        for (Entity entity : ontology.getEntities()) {
            final Map<Language, Collection<String>> labels = extractor.getLabels(entity);
            if (labels.containsKey(srcLang) && labels.containsKey(trgLang)) {
                srcLabels.put(entity, labels.get(srcLang));
                trgLabels.put(entity, labels.get(trgLang));
            }
        }
    }

    @Override
    public double score(PhraseTableEntry entry, Entity entity) {
        if (!srcLabels.containsKey(entity)) {
            return 0;
        }
        assert (trgLabels.containsKey(entity));
        final String srcLabel = entry.getForeign().asString();
        final String trgLabel = entry.getTranslation().asString();
        SRC_CHECK:
        {
            for (String srcCandidate : srcLabels.get(entity)) {
                if (srcLabel.contains(srcCandidate)) {
                    break SRC_CHECK;
                }
            }
            return 0;
        }
        for (String trgCandidate : trgLabels.get(entity)) {
            if (trgLabel.contains(trgCandidate)) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public String getName() {
        return "OPM";
    }

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
}
