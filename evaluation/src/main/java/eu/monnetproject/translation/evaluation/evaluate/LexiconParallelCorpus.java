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

import eu.monnetproject.lemon.model.LexicalEntry;
import eu.monnetproject.lemon.model.LexicalSense;
import eu.monnetproject.lemon.model.Lexicon;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.translation.tune.EntityLabel;
import eu.monnetproject.translation.tune.EntityLabelList;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author John McCrae
 */
public class LexiconParallelCorpus extends HashSet<EntityLabel> implements EntityLabelList {
    
    public LexiconParallelCorpus(Collection<Lexicon> sourceLexica, Collection<Lexicon> targetLexica, Collection<Ontology> ontologies) {
        final HashMap<URI, String> src = new HashMap<URI, String>();
        for (Lexicon sourceLexicon : sourceLexica) {
            for (LexicalEntry entry : sourceLexicon.getEntrys()) {
                for (LexicalSense sense : entry.getSenses()) {
                    src.put(sense.getReference(), entry.getCanonicalForm().getWrittenRep().value);
                }
            }
        }
        final HashMap<URI, String> trg = new HashMap<URI, String>();
        for (Lexicon targetLexicon : targetLexica) {
            for (LexicalEntry entry : targetLexicon.getEntrys()) {
                for (LexicalSense sense : entry.getSenses()) {
                    trg.put(sense.getReference(), entry.getCanonicalForm().getWrittenRep().value);
                }
            }
        }
        //final HashMap<URI, Entity> entities = new HashMap<URI, Entity>();
        for (URI uri : src.keySet()) {
            if (trg.containsKey(uri)) {
                for(Ontology ontology : ontologies) {
                    final Collection<Entity> entities2 = ontology.getEntities(uri);
                    if(!entities2.isEmpty()) {
                        this.add(new EntityLabel(entities2.iterator().next(), src.get(uri), trg.get(uri)));
                        break;
                    }
                }
            }
        }
    }

}
