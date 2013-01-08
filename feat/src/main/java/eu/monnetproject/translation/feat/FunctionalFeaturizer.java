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
package eu.monnetproject.translation.feat;

import eu.monnetproject.lang.Language;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.translation.*;
import eu.monnetproject.translation.monitor.Messages;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author John McCrae
 */
public abstract class FunctionalFeaturizer implements TranslationFeaturizer {

    private final List<TranslationRanker> rankers;

    public FunctionalFeaturizer(Ontology ontology, Language srcLang, Language trgLang, Set<Language> pivotLangs, Iterable<TranslationRankerFactory> factories) {
        rankers = new LinkedList<TranslationRanker>();
        for (TranslationRankerFactory factory : factories) {
            final TranslationRanker ranker = pivotLangs == null ? factory.getRanker(ontology, srcLang, trgLang) : factory.getRanker(ontology, srcLang, trgLang, pivotLangs);
            if (ranker != null) {
                rankers.add(ranker);
            }
        }
    }

    @Override
    public PhraseTable featurize(PhraseTable candidates, Entity entity) {
        Messages.info("Using " + rankers.size() + " rankers");
        return new RescoringPhraseTable(entity, candidates);
    }
    
    @Override
    public void close() {
    	Messages.info("Closing rankers");        
    	for(TranslationRanker ranker: rankers) {
    		ranker.close();
    	}
    }
    
    @Override
    public List<String> extraFeatureNames() {
    	final List<String> efn = new LinkedList<String>();
    	for(TranslationRanker ranker : rankers) {
    		efn.add(ranker.getName());
    	}
    	return efn;
    }
    

    private static class PhraseTableEntryImpl implements PhraseTableEntry {

        private final Label foreign, translation;
        private final Feature[] scores;

        public PhraseTableEntryImpl(Label foreign, Label translation, Feature[] scores) {
            this.foreign = foreign;
            this.translation = translation;
            this.scores = scores;
        }

        @Override
        public Label getForeign() {
            return foreign;
        }

        @Override
        public Label getTranslation() {
            return translation;
        }

        @Override
        public Feature[] getFeatures() {
            return scores;
        }

        @Override
        public String toString() {
            return "PhraseTableEntryImpl{" + "foreign=" + foreign + ", translation=" + translation + ", scores=" + Arrays.toString(scores) + '}';
        }
        
        
    }

    protected abstract Feature[] rescore(Feature[] scores, double factor, String rankerName);

    private class RescoringPhraseTable implements PhraseTable {

        private final Entity entity;
        private final PhraseTable base;

        public RescoringPhraseTable(Entity entity, PhraseTable base) {
            this.entity = entity;
            this.base = base;
        }

        @Override
        public Language getForeignLanguage() {
            return base.getForeignLanguage();
        }

        @Override
        public Language getTranslationLanguage() {
            return base.getTranslationLanguage();
        }

        @Override
        public String getName() {
            return base.getName();
        }

        @Override
        public int getLongestForeignPhrase() {
            return base.getLongestForeignPhrase();
        }

        @Override
        public Iterator iterator() {
            final Iterator<PhraseTableEntry> baseIter = base.iterator();
            return new Iterator() {

                @Override
                public boolean hasNext() {
                    return baseIter.hasNext();
                }

                @Override
                public Object next() {
                    final PhraseTableEntry next = baseIter.next();
                    Feature[] scores = next.getFeatures();
                    for (TranslationRanker ranker : rankers) {
                        double factor = ranker.score(next, entity);
                        scores = rescore(scores, factor, ranker.getName());
                    }
                    return new PhraseTableEntryImpl(next.getForeign(), next.getTranslation(), scores);
                }

                @Override
                public void remove() {
                    baseIter.remove();
                }
            };
        }

        
        
    }
}