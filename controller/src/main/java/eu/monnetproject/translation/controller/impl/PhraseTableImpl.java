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
package eu.monnetproject.translation.controller.impl;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author John McCrae
 */
public class PhraseTableImpl extends HashMap<PhraseTableEntry,PhraseTableEntry> implements PhraseTable {

    private final Language foreignLanguage, translationLanguage;
    private final String tableName;
    private int longestForeignPhrase;
    private final List<String> features;

    public PhraseTableImpl(Language foreignLanguage, Language translationLanguage, String name, int longestForeignPhrase, List<String> features) {
        this.foreignLanguage = foreignLanguage;
        this.translationLanguage = translationLanguage;
        this.tableName = name;
        this.longestForeignPhrase = longestForeignPhrase;
        this.features = features;
    }

    public void addAll(PhraseTable phraseTable) {
        if (!this.foreignLanguage.equals(phraseTable.getForeignLanguage())
                || !this.translationLanguage.equals(phraseTable.getTranslationLanguage())) {
            throw new IllegalArgumentException("Phrase tables do not match language" + this.foreignLanguage + " -> " + this.translationLanguage + " vs " + phraseTable.getForeignLanguage() + " -> " + phraseTable.getTranslationLanguage());
        }
        for (PhraseTableEntry pte : phraseTable) {
            final StringHashPhraseTableEntry shpte = new StringHashPhraseTableEntry(pte, features, phraseTable.getName());
            if(super.containsKey(shpte)) {
                final PhraseTableEntry other = super.remove(shpte);
                final PhraseTableEntry pte2 = shpte.merge(shpte);
                super.put(pte2, pte2);
            } else {
                super.put(shpte,shpte);
            }
        }
        longestForeignPhrase = Math.max(this.longestForeignPhrase, phraseTable.getLongestForeignPhrase());
    }

    @Override
    public Iterator<PhraseTableEntry> iterator() {
        return super.keySet().iterator();
    }

    
    
    @Override
    public Language getForeignLanguage() {
        return foreignLanguage;
    }

    @Override
    public Language getTranslationLanguage() {
        return translationLanguage;
    }

    @Override
    public String getName() {
        return tableName;
    }

    @Override
    public int getLongestForeignPhrase() {
        return longestForeignPhrase;
    }

    public static final double phraseTableDefaultWt = Double.parseDouble(System.getProperty("eu.monnetproject.translation.wts.default","-7"));
        public static final String PHRASE_TABLE_SRC_PREFIX = "_src_";
    
    private static class StringHashPhraseTableEntry implements PhraseTableEntry {

        private final PhraseTableEntry c;
        private final List<String> featureNames;
        private final String tableName;

        public StringHashPhraseTableEntry(PhraseTableEntry c, List<String> featureNames, String tableName) {
            this.c = c;
            this.featureNames = featureNames;
            this.tableName = tableName;
        }



        @Override
        public Label getForeign() {
            return c.getForeign();
        }

        @Override
        public Label getTranslation() {
            return c.getTranslation();
        }

        @Override
        public double getApproxScore() {
            return c.getApproxScore();
        }
        
        

        @Override
        public Feature[] getFeatures() {
            final Feature[] feats = c.getFeatures();
            if(feats.length == featureNames.size()) {
                // Everything is there!
                return feats;
            } else {
                // Now we need to add a default score for all "missing" features
                final Feature[] newFeats = new Feature[featureNames.size()];
                System.arraycopy(feats, 0, newFeats, 0, feats.length);
                int n = feats.length;
                FEATURE_NAMES:
                for(String featureName : featureNames) {
                    for(int i = 0; i < n; i++) {
                        if(newFeats[i].name.equals(featureName)) {
                            continue FEATURE_NAMES;
                        }
                    }
                    if(featureName.equals(PHRASE_TABLE_SRC_PREFIX+tableName))  {
                        newFeats[n++] = new Feature(featureName, 1.0);
                    } else if(featureName.startsWith(PHRASE_TABLE_SRC_PREFIX)) {
                        newFeats[n++] = new Feature(featureName, 0.0);
                    } else {
                        newFeats[n++] = new Feature(featureName, phraseTableDefaultWt);
                    }
                }
                return newFeats;
            }
        }

        private String translation() {
            return c.getTranslation().asString();
        }
        
        private String foreign() {
            return c.getForeign().asString();
        }
        
        public PhraseTableEntry merge(PhraseTableEntry other) {
            return new MergedPHSTE(this, other);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 47 * hash + (this.translation() != null ? this.translation().hashCode() : 0);
            hash = 47 * hash + (this.foreign() != null ? this.foreign().hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StringHashPhraseTableEntry other = (StringHashPhraseTableEntry) obj;
            if ((this.translation() == null) ? (other.translation() != null) : !this.translation().equals(other.translation())) {
                return false;
            }
            if ((this.foreign() == null) ? (other.foreign() != null) : !this.foreign().equals(other.foreign())) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "StringHashPhraseTableEntry{" + "c=" + c + ", featureNames=" + featureNames + ", tableName=" + tableName + '}';
        }
        
        
    }
    
    private static class MergedPHSTE implements PhraseTableEntry {

        private final StringHashPhraseTableEntry c1;
        private final PhraseTableEntry c2;

        public MergedPHSTE(StringHashPhraseTableEntry c1, PhraseTableEntry c2) {
            this.c1 = c1;
            this.c2 = c2;
        }


        @Override
        public Label getForeign() {
            return c1.getForeign();
        }

        @Override
        public Label getTranslation() {
            return c1.getTranslation();
        }

        @Override
        public double getApproxScore() {
            return c1.getApproxScore() + c2.getApproxScore();
        }

        
        
        @Override
        public Feature[] getFeatures() {
            final Feature[] feats1 = c1.getFeatures();
            final Feature[] feats2 = c2.getFeatures();
            assert(feats1.length == feats2.length);
            final Feature[] newFeats = new Feature[feats1.length];
            boolean f = true;
            FEATS:
            for(int i = 0; i < feats1.length; i++) {
                for(int j = i; f || j != i; j = (j + 1) % feats2.length) {
                    if(feats1[i].name.equals(feats2[j].name)) {
                        newFeats[i] = feats1[i].score == phraseTableDefaultWt ?
                                ( feats2[j] ) :
                                ( feats2[j].score == phraseTableDefaultWt ? feats1[i] : 
                                  new Feature(feats1[i].name,feats1[i].score + feats2[j].score));
                        break;
                    }
                    f = false;
                }
            }
            return newFeats;
        }

        @Override
        public String toString() {
            return "MergedPHSTE{" + "c1=" + c1 + ", c2=" + c2 + '}';
        }
        
    }
}
