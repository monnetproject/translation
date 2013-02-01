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
package eu.monnetproject.translation.quality;

import eu.monnetproject.framework.services.Services;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.LanguageModel;
import eu.monnetproject.translation.LanguageModelFactory;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.TokenizedLabel;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.TranslationSourceFactory;
import eu.monnetproject.translation.util.NGramCarousel;
import java.io.*;
import java.net.URI;
import java.util.*;

/**
 * Based on the 17 Baseline features suggested by Lucia Specia for the WMT-12
 * shared task on Quality Estimation.
 *
 * @author John McCrae
 */
public class BaselineFeatures {

    private final LanguageModelFactory lmFactory;
    private final TranslationSourceFactory sourceFactory;

    public BaselineFeatures(LanguageModelFactory lmFactory, TranslationSourceFactory sourceFactory) {
        this.lmFactory = lmFactory;
        this.sourceFactory = sourceFactory;
    }
    private final WeakHashMap<Language, LanguageModel> models = new WeakHashMap<Language, LanguageModel>();

    private synchronized LanguageModel getModel(Language lang) {
        if (models.containsKey(lang)) {
            return models.get(lang);
        } else {
            final LanguageModel model = lmFactory.getModel(lang);
            models.put(lang, model);
            return model;
        }
    }
    
    private final WeakHashMap<String, TranslationSource> sources = new WeakHashMap<String, TranslationSource>();

    private synchronized TranslationSource getSource(Language srcLang, Language trgLang) {
        final String key = srcLang + "=>" + trgLang;
        if (sources.containsKey(key)) {
            return sources.get(key);
        } else {
            System.err.println("Loading translation source for quality estimation");
            final TranslationSource source = sourceFactory.getSource(srcLang, trgLang);
            System.err.println("Done loading translation source");
            sources.put(key, source);
            return source;
        }
    }

    public static double countTksInSrc(Translation translation) {
        if (translation.getSourceLabel() instanceof TokenizedLabel) {
            return ((TokenizedLabel) translation.getSourceLabel()).getTokens().size();
        } else {
            return translation.getSourceLabel().asString().split("\\s+").length;
        }
    }

    public static double countTksInTrg(Translation translation) {
        if (translation.getTargetLabel() instanceof TokenizedLabel) {
            return ((TokenizedLabel) translation.getTargetLabel()).getTokens().size();
        } else {
            return translation.getTargetLabel().asString().split("\\s+").length;
        }
    }

    public static double aveSrcTkLen(Translation translation) {
        double length = 0.0;
        int n = 0;
        if (translation.getTargetLabel() instanceof TokenizedLabel) {
            for (String token : ((TokenizedLabel) translation.getSourceLabel()).getTokens()) {
                length += token.length();
                n++;
            }
        } else {
            for (String token : translation.getSourceLabel().asString().split("\\s+")) {
                length += token.length();
                n++;
            }
        }
        return length / n;
    }
    static public final double MOSES_LM_UNKNOWN_WORD_SCORE = -100;

    public double lmProb(LanguageModel model, List<String> tokens) {

        double lmSumScore = 0.0;
        for (int pos = 0; pos < tokens.size(); pos++) {
            final int seqStart = Math.max(0, pos - model.getOrder() + 1);
            final List<String> ngram = new ArrayList<String>(tokens.subList(seqStart, pos + 1));
            final ListIterator<String> ngramIter = ngram.listIterator();
            while (ngramIter.hasNext()) {
                ngramIter.set(ngramIter.next().toLowerCase());
            }
            double ngramScore = model.score(ngram);
            if (ngramScore == Double.NEGATIVE_INFINITY || ngramScore != ngramScore) {
                lmSumScore += MOSES_LM_UNKNOWN_WORD_SCORE;
                continue;
            }
            lmSumScore += ngramScore;
        }
        return lmSumScore;
    }

    public double sourceLMProb(Translation translation) {
        final LanguageModel model = getModel(translation.getSourceLabel().getLanguage());
        if (model == null) {
            return 0;
        }
        return lmProb(model, getTokens(translation.getSourceLabel()));
    }

    public double targetLMProb(Translation translation) {
        final LanguageModel model = getModel(translation.getTargetLabel().getLanguage());
        if (model == null) {
            return 0;
        }
        return lmProb(model, getTokens(translation.getTargetLabel()));
    }

    public static double aveOccurencesInTarget(Translation translation) {
        final HashMap<String, Integer> occMap = new HashMap<String, Integer>();
        final Label label = translation.getTargetLabel();
        if (label instanceof TokenizedLabel) {
            for (String token : ((TokenizedLabel) label).getTokens()) {
                if (occMap.containsKey(token)) {
                    occMap.put(token, 1 + occMap.get(token));
                } else {
                    occMap.put(token, 1);
                }
            }
        } else {
            for (String token : label.asString().split("\\s+")) {
                if (occMap.containsKey(token)) {
                    occMap.put(token, 1 + occMap.get(token));
                } else {
                    occMap.put(token, 1);
                }
            }
        }
        double aveCount = 0.0;
        for (int i : occMap.values()) {
            aveCount += i;
        }
        return aveCount / occMap.size();
    }

    private static List<String> getTokens(Label label) {
        if (label instanceof TokenizedLabel) {
            return ((TokenizedLabel) label).getTokens();
        } else {
            return Arrays.asList(label.asString().split("\\s+"));
        }
    }

    // Run at minProb = 0.2 and minProb = 0.01
    public double aveTranslationCount(Translation translation, double minProb) {
        final TranslationSource source = getSource(translation.getSourceLabel().getLanguage(), translation.getTargetLabel().getLanguage());
        if (source == null) {
            return 0.0;
        }
        final double p = Math.log(minProb);
        final List<String> tokens = getTokens(translation.getSourceLabel());
        int transCt = 0;
        for (String token : tokens) {
            final PhraseTable candidates = source.candidates(new ChunkImpl(token));
            for (PhraseTableEntry entry : candidates) {
                if (entry.getFeatures()[2].score >= p) {
                    transCt++;
                }
            }
        }
        return (double) transCt / tokens.size();
    }

    // Run at n =1,2,3
    public double[] percentNGramsInTopBotQuartile(Translation translation, int n) {
        //final LMInfo lmInfo = getLMInfo(translation.getSourceLabel().getLanguage());
        final LanguageModel nGramSource = getModel(translation.getSourceLabel().getLanguage());
        if (nGramSource == null) {
            return new double[]{0.0, 0.0};
        }
        final NGramCarousel<String> carousel = new NGramCarousel<String>(n, String.class);
        final List<String> tokens = getTokens(translation.getSourceLabel());
        int botCount = 0;
        int topCount = 0;
        for (String s : tokens) {
            final String[] ngram = carousel.next(s);
            if (ngram == null) {
                continue;
            }
            final int quartile = nGramSource.quartile(Arrays.asList(ngram));
            if (quartile == 0 || quartile == 1) {
                botCount++;
            } else if (quartile == 4) {
                topCount++;
            }
//            if (!lmInfo.maps[n - 1].containsKey(sb.toString())) {
//                continue;
//            }
//            double score = lmInfo.maps[n - 1].get(sb.toString());
//            if (score >= lmInfo.quartile[n - 1][2]) {
//                topCount++;
//            } else if (score < lmInfo.quartile[n - 1][0]) {
//                botCount++;
//            }
        }
        return new double[]{
                    (double) topCount / (tokens.size() - n + 1),
                    (double) botCount / (tokens.size() - n + 1)
                };
    }

    public double percentUnigramsInLM(Translation translation) {
        final LanguageModel nGramSource = getModel(translation.getSourceLabel().getLanguage());
        if (nGramSource == null) {
            return 1.0;
        }
        final List<String> tokens = getTokens(translation.getSourceLabel());
        int count = 0;
        for (String s : tokens) {
            if (Double.isInfinite(nGramSource.score(Arrays.asList(s)))) {
                count++;
            }
        }
        return (double) (tokens.size() - count) / tokens.size();
    }

    public static double countPunctuationInSource(Translation translation) {
        return countPunctuation(translation.getSourceLabel());
    }

    public static double countPunctuationInTarget(Translation translation) {
        return countPunctuation(translation.getTargetLabel());
    }
    private static final String puncClass = "[!-/:-@\\[-`\\{-~\u2000-\u206f]+";

    private static double countPunctuation(Label label) {
        int count = 0;
        if (label instanceof TokenizedLabel) {
            for (String token : ((TokenizedLabel) label).getTokens()) {
                if (token.matches(puncClass)) {
                    count++;
                }
            }
        } else {
            for (String token : label.asString().split("\\b")) {
                if (token.trim().matches(puncClass)) {
                    count++;
                }
            }
        }
        return count;


    }

//    private static final class LMInfo {
//
//        final double[][] quartile;
//        final Map<String, Double>[] maps;
//
//        @SuppressWarnings("unchecked")
//        public LMInfo(ARPALMReader lmData) {
//            quartile = new double[3][];
//            maps = new Map[3];
//            for (int n = 1; n <= 3; n++) {
//                quartile[n - 1] = lmData.getQuartiles(n);
//                maps[n - 1] = lmData.asMap(n);
//            }
//        }
//    }
    private static final class ChunkImpl implements Chunk {

        private final String source;

        public ChunkImpl(String source) {
            this.source = source;
        }

        @Override
        public String getSource() {
            return source;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ChunkImpl other = (ChunkImpl) obj;
            if ((this.source == null) ? (other.source != null) : !this.source.equals(other.source)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 17 * hash + (this.source != null ? this.source.hashCode() : 0);
            return hash;
        }
    }

    public double[] allFeatures(Translation translation) {
        final double[] features = new double[17];
        features[0] = countTksInSrc(translation);
        features[1] = countTksInTrg(translation);
        features[2] = aveSrcTkLen(translation);
        features[3] = sourceLMProb(translation);
        features[4] = targetLMProb(translation);
        features[5] = aveOccurencesInTarget(translation);
        features[6] = aveTranslationCount(translation, 0.2);
        features[7] = aveTranslationCount(translation, 0.01);
        final double[] oneGramScores = percentNGramsInTopBotQuartile(translation, 1);
        features[8] = oneGramScores[0];
        features[9] = oneGramScores[1];
        final double[] twoGramScores = percentNGramsInTopBotQuartile(translation, 2);
        features[10] = twoGramScores[0];
        features[11] = twoGramScores[1];
        final double[] threeGramScores = percentNGramsInTopBotQuartile(translation, 3);
        features[12] = threeGramScores[0];
        features[13] = threeGramScores[1];
        features[14] = percentUnigramsInLM(translation);
        features[15] = countPunctuationInSource(translation);
        features[16] = countPunctuationInTarget(translation);
        return features;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            throw new IllegalArgumentException("Usage: src trg srcLang trgLang annotations out.csv");
        }
        final Language srcLang = Language.get(args[2]);
        final Language trgLang = Language.get(args[3]);
        final BufferedReader src = new BufferedReader(new FileReader(args[0]));
        final BufferedReader trg = new BufferedReader(new FileReader(args[1]));
        final BufferedReader annotations = new BufferedReader(new FileReader(args[4]));
        final PrintWriter out = new PrintWriter(args[5]);
        final BaselineFeatures features = new BaselineFeatures(Services.get(LanguageModelFactory.class), Services.get(TranslationSourceFactory.class));
        out.println(
                "TKS_S,TKS_T,TKLEN_S,LM_S,LM_T,OCC_T,TC_20,TC_1,TOP1G,BOT1G,TOP2G,BOT2G,TOP3G,BOT3G,UNK,PUNC_S,PUNC_T,C");
        while (true) {
            final String srcTranslation = src.readLine();
            final String trgTranslation = trg.readLine();
            class TranslationImpl implements Translation {

                @Override
                public Label getSourceLabel() {
                    return new Label() {

                        @Override
                        public String asString() {
                            return srcTranslation;
                        }

                        @Override
                        public Language getLanguage() {
                            return srcLang;
                        }
                    };
                }

                @Override
                public Label getTargetLabel() {
                    return new Label() {

                        @Override
                        public String asString() {
                            return trgTranslation;
                        }

                        @Override
                        public Language getLanguage() {
                            return trgLang;
                        }
                    };
                }

                @Override
                public URI getEntity() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public double getScore() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public Collection<Feature> getFeatures() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            }
            final Translation translation = new TranslationImpl();
            final String annoLine = annotations.readLine();
            if (srcTranslation == null || trgTranslation == null || annoLine == null) {
                break;
            }
            out.print(countTksInSrc(translation));
            out.print(",");
            out.print(countTksInTrg(translation));
            out.print(",");
            out.print(aveSrcTkLen(translation));
            out.print(",");
            out.print(features.sourceLMProb(translation));
            out.print(",");
            out.print(features.targetLMProb(translation));
            out.print(",");
            out.print(aveOccurencesInTarget(translation));
            out.print(",");
            out.print(features.aveTranslationCount(translation, 0.2));
            out.print(",");
            out.print(features.aveTranslationCount(translation, 0.01));
            out.print(",");
            final double[] oneGramScores = features.percentNGramsInTopBotQuartile(translation, 1);
            out.print(oneGramScores[0]);
            out.print(",");
            out.print(oneGramScores[1]);
            out.print(",");
            final double[] twoGramScores = features.percentNGramsInTopBotQuartile(translation, 2);
            out.print(twoGramScores[0]);
            out.print(",");
            out.print(twoGramScores[1]);
            out.print(",");
            final double[] threeGramScores = features.percentNGramsInTopBotQuartile(translation, 3);
            out.print(threeGramScores[0]);
            out.print(",");
            out.print(threeGramScores[1]);
            out.print(",");
            out.print(features.percentUnigramsInLM(translation));
            out.print(",");
            out.print(countPunctuationInSource(translation));
            out.print(",");
            out.print(countPunctuationInTarget(translation));
            out.print(",");
            out.println(annoLine);
            System.err.print(".");
        }

        out.flush();

        out.close();

        System.err.println(
                "Done");
    }
}
