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
package eu.monnetproject.translation.phrasal.pt;

import edu.stanford.nlp.mt.base.AbstractPhraseGenerator;
import edu.stanford.nlp.mt.base.DynamicIntegerArrayIndex;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.IStrings;
import edu.stanford.nlp.mt.base.IntegerArrayIndex;
import edu.stanford.nlp.mt.base.PhraseAlignment;
import edu.stanford.nlp.mt.base.PhraseTable;
import edu.stanford.nlp.mt.base.RawSequence;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.base.Sequences;
import edu.stanford.nlp.mt.base.SimpleSequence;
import edu.stanford.nlp.mt.base.TranslationOption;
import edu.stanford.nlp.mt.decoder.feat.IsolatedPhraseFeaturizer;
import edu.stanford.nlp.mt.decoder.util.Scorer;
import eu.monnetproject.translation.AlignedPhraseTableEntry;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.TokenizedLabel;
import eu.monnetproject.translation.monitor.Messages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author John McCrae
 */
public class WrappingPhraseTable<FV> extends AbstractPhraseGenerator<IString, FV> implements PhraseTable<IString> {

    public static final String FIVESCORE_PHI_t_f = "phi(t|f)";
    public static final String FIVESCORE_LEX_t_f = "lex(t|f)";
    public static final String FIVESCORE_PHI_f_t = "phi(f|t)";
    public static final String FIVESCORE_LEX_f_t = "lex(f|t)";
    public static final String ONESCORE_P_t_f = "p(t|f)";
    public static final String FIVESCORE_PHRASE_PENALTY = "phrasePenalty";
    public static final String[] CANONICAL_FIVESCORE_SCORE_TYPES = {
        FIVESCORE_PHI_t_f, FIVESCORE_LEX_t_f, FIVESCORE_PHI_f_t,
        FIVESCORE_LEX_f_t, FIVESCORE_PHRASE_PENALTY};
    public static final String[] CANONICAL_ONESCORE_SCORE_TYPES = {ONESCORE_P_t_f};
    public static final String DISABLED_SCORES_PROPERTY = "disableScores";
    public static final String DISABLED_SCORES = System.getProperty(DISABLED_SCORES_PROPERTY);
    public static final String CUSTOM_SCORES_PROPERTY = "customScores";
    public static final String CUSTOM_SCORES = System.getProperty(CUSTOM_SCORES_PROPERTY);
    //private eu.monnetproject.translation.PhraseTable<PhraseTableEntry> pt;
    private Map<String, List<PhraseTableEntry>> ptMap;
    private String ptName;
    private int ptLongestPhrase;
    static IntegerArrayIndex foreignIndex, translationIndex;
    List<String> scoreNames;
    static String[] customScores;

    static {
        List<String> l = new ArrayList<String>();
        // Custom score names:
        if (CUSTOM_SCORES != null) {
            for (String el : CUSTOM_SCORES.split(",")) {
                if (el.equals("phi_tf")) {
                    l.add(FIVESCORE_PHI_t_f);
                } else if (el.equals("phi_ft")) {
                    l.add(FIVESCORE_PHI_f_t);
                } else if (el.equals("lex_tf")) {
                    l.add(FIVESCORE_LEX_t_f);
                } else if (el.equals("lex_ft")) {
                    l.add(FIVESCORE_LEX_f_t);
                } else if (el.equals("p_tf")) {
                    l.add(ONESCORE_P_t_f);
                } else {
                    l.add(el);
                }
            }
            customScores = l.toArray(new String[l.size()]);
        }
        createIndex(false);
    }

    public WrappingPhraseTable(IsolatedPhraseFeaturizer<IString, FV> phraseFeaturizer, Scorer<FV> scorer) {
        super(phraseFeaturizer, scorer);
    }

    @Override
    public String getName() {
        //return pt.getName();
        return ptName;
    }

    public static void createIndex(boolean withGaps) {
        foreignIndex = new DynamicIntegerArrayIndex();
        translationIndex = new DynamicIntegerArrayIndex();
    }

    
    @Override
    public List<TranslationOption<IString>> getTranslationOptions(Sequence<IString> sequence) {
        if (ptMap == null) {
            throw new IllegalArgumentException("phrase table is not set");
        }
        RawSequence<IString> rawForeign = new RawSequence<IString>(sequence);
        List<TranslationOption<IString>> transOpts = new ArrayList<TranslationOption<IString>>();
        final String foreignStr = sequence.toString(" ");
        final List<PhraseTableEntry> ptSection = ptMap.get(foreignStr);
        if(ptSection == null) {
            return transOpts;
        }
        for (PhraseTableEntry pte : ptSection) {
            final Feature[] f = pte.getFeatures();
            for(int i = 0; i < f.length; i++) {
                if(Double.isInfinite(f[i].score) || Double.isNaN(f[i].score)) {
                    Messages.warning("Bad feature value score: " + f[i].name);
                }   
            }
          //  System.err.println(pte.getForeign().asString() + " -> " + pte.getTranslation().asString() + " " + Arrays.toString(pte.getFeatures()));
            final IntArrayTranslationOption intTransOpt = convert(pte);
            RawSequence<IString> translation = new RawSequence<IString>(
                    intTransOpt.translation, IString.identityIndex());
            transOpts.add(new TranslationOption<IString>(intTransOpt.id,
                    intTransOpt.scores, getScoreNames(pte), translation, rawForeign,
                    intTransOpt.alignment));
        }
        return transOpts;
    }

    @Override
    public int longestForeignPhrase() {
        
        return ptLongestPhrase;
    }

    @Override
    public void setCurrentSequence(Sequence<IString> foreign, List<Sequence<IString>> tranList) {
        // no op
    }

//    public String[] getScoreNames(int countScores) {
//        if (scoreNames != null) {
//            return scoreNames;
//        } else {
//            if (customScores != null) {
//                scoreNames = customScores;
//            } else if (countScores == 5) {
//                scoreNames = CANONICAL_FIVESCORE_SCORE_TYPES;
//            } else if (countScores == 1) {
//                scoreNames = CANONICAL_ONESCORE_SCORE_TYPES;
//            } else {
//                scoreNames = new String[countScores];
//                for (int i = 0; i < countScores; i++) {
//                    scoreNames[i] = String.format("%d.UnkTScore", i);
//                }
//            }
//
//            if (DISABLED_SCORES != null) {
//                System.err.println("Disabled features: " + DISABLED_SCORES);
//                for (String istr : DISABLED_SCORES.split(",")) {
//                    int i = Integer.parseInt(istr);
//                    if (i < scoreNames.length) {
//                        System.err.printf("Feature %s disabled.\n", scoreNames[i]);
//                        scoreNames[i] = null;
//                    }
//                }
//            }
//
//            return scoreNames;
//        }
//    }

    private IntArrayTranslationOption convert(Sequence<IString> foreignSequence,
            Sequence<IString> translationSequence, PhraseAlignment alignment,
            float[] scores) {
        int[] foreignInts = Sequences.toIntArray(foreignSequence);
        int[] translationInts = Sequences.toIntArray(translationSequence);
        int fIndex = foreignIndex.indexOf(foreignInts, true);
        int eIndex = translationIndex.indexOf(translationInts, true);
        int id = translationIndex.indexOf(new int[]{fIndex, eIndex}, true);

        return new IntArrayTranslationOption(id, translationIndex.get(eIndex), scores, alignment);
    }

    private String[] getScoreNames(PhraseTableEntry pte) {
        return scoreNames.toArray(new String[scoreNames.size()]);
    }
    
    private IntArrayTranslationOption convert(PhraseTableEntry pte) {
        Sequence<IString> foreignSequence = new SimpleSequence<IString>(IStrings.toIStringArray(getTokens(pte.getForeign())));
        Sequence<IString> translationSequence = new SimpleSequence<IString>(IStrings.toIStringArray(getTokens(pte.getTranslation())));
        float[] scores = new float[scoreNames.size()];
        if(scores.length != scoreNames.size()) {
            Messages.warning("Number of elements in phrase table not equal to feature name count");
        }
        for (int i = 0; i < scores.length; i++) {
            if(i < scoreNames.size() && pte.getFeatures()[i].name.equals(scoreNames.get(i))) {
                scores[i] = (float) pte.getFeatures()[i].score;
            } else {
                for(int j = 0; j < scoreNames.size(); j++) {
                    if(pte.getFeatures()[i].name.equals(scoreNames.get(j)))  {
                        scores[j] = (float) pte.getFeatures()[i].score;
                    }
                         
                }
            }
        }

        PhraseAlignment alignment = null;
        if (pte instanceof AlignedPhraseTableEntry && ((AlignedPhraseTableEntry) pte).getAlignments() instanceof PhraseAlignment) {
            alignment = (PhraseAlignment) ((AlignedPhraseTableEntry) pte).getAlignments();
        }
        return convert(foreignSequence, translationSequence, alignment, scores);
    }

    private List<String> getTokens(Label label) {
        if (label instanceof TokenizedLabel) {
            return ((TokenizedLabel) label).getTokens();
        } else {
            return Arrays.asList(label.asString().split("\\s+"));
        }
    }

    public void setPhraseTable(eu.monnetproject.translation.PhraseTable pt, List<String> featureNames) {
        ptMap = new HashMap<String, List<PhraseTableEntry>>();
        for(PhraseTableEntry pte : pt) {
            final String key = pte.getForeign().asString();
            if(!ptMap.containsKey(key)) {
                ptMap.put(key, new ArrayList<PhraseTableEntry>());
            }
            ptMap.get(key).add(pte);
        }
        ptName = pt.getName();
        ptLongestPhrase = pt.getLongestForeignPhrase();
        scoreNames = featureNames;
        //log.info("New lfp " + ptLongestPhrase);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new WrappingPhraseTable<FV>(phraseFeaturizer, scorer);
    }

    static class IntArrayTranslationOption implements
            Comparable<IntArrayTranslationOption> {

        final int[] translation;
        final float[] scores;
        final PhraseAlignment alignment;
        final int id;

        public IntArrayTranslationOption(int id, int[] translation, float[] scores,
                PhraseAlignment alignment) {
            this.id = id;
            this.translation = translation;
            this.scores = scores;
            this.alignment = alignment;
        }

        @Override
        public int compareTo(IntArrayTranslationOption o) {
            return (int) Math.signum(o.scores[0] - scores[0]);
        }
    }
}
