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
import eu.monnetproject.translation.phrasal.mmap.PhraseTableMapper;
import eu.monnetproject.translation.phrasal.mmap.TreeMemoryMap;
import eu.monnetproject.translation.monitor.Messages;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 * @author John McCrae
 */
public class MemoryMappedPhraseTable<FV> extends AbstractPhraseGenerator<IString, FV> implements PhraseTable<IString> {

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
    private final String name;
    private final FileChannel srcTable;
    private final TreeMemoryMap tmm;
    private final int longestPhrase;
    static IntegerArrayIndex foreignIndex, translationIndex;
    String[] scoreNames;
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

    public MemoryMappedPhraseTable(IsolatedPhraseFeaturizer<IString, FV> phraseFeaturizer,
            Scorer<FV> scorer, String filename) throws IOException {
        this(phraseFeaturizer, scorer, filename, true);
    }

    public MemoryMappedPhraseTable(IsolatedPhraseFeaturizer<IString, FV> phraseFeaturizer,
            Scorer<FV> scorer, String filename, boolean doLog) throws IOException {
        super(phraseFeaturizer, scorer);
        File tableFile = new File(filename);
        name = String.format("FlatPhraseTable(%s)", tableFile.getName());
        File mapFile = new File(filename + ".map");
        if (!mapFile.exists()) {
            throw new IOException("Could not locate map file at" + mapFile.getName());
        }
        this.srcTable = new FileInputStream(tableFile).getChannel();
        this.tmm = new TreeMemoryMap(mapFile, PhraseTableMapper.KEY, PhraseTableMapper.OFFSET);
        this.longestPhrase = new FileInputStream(mapFile).getChannel().map(FileChannel.MapMode.READ_ONLY, 0, 4).getInt();
        Messages.info("Longest phrase: " + longestPhrase + " (if this is a silly number the map is likely invalid)");
    }

    @Override
    public String getName() {
        return name;
    }

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

    private IntArrayTranslationOption convert(String line, String srcKey, boolean doLog) {
        StringTokenizer toker = new StringTokenizer(line);
        StringBuilder src = new StringBuilder();
        Collection<String> foreignTokenList = new LinkedList<String>();
        do {
            String token = toker.nextToken();
            if ("|||".equals(token)) {
                break;
            }
            foreignTokenList.add(token);
            src.append(token).append(" ");
        } while (toker.hasMoreTokens());

        if (!src.toString().trim().equals(srcKey)) {
            return null;
        }

        if (!toker.hasMoreTokens()) {
            throw new RuntimeException(String.format(
                    "Additional fields expected (line %s)", line));
        }

        Collection<String> translationTokenList = new LinkedList<String>();

        do {
            String token = toker.nextToken();
            if ("|||".equals(token)) {
                break;
            }
            translationTokenList.add(token);
        } while (toker.hasMoreTokens());

        if (!toker.hasMoreTokens()) {
            throw new RuntimeException(String.format(
                    "Additional fields expected (line %s)", line));
        }
        Collection<String> constilationList = new LinkedList<String>();
        List<String> scoreList = new LinkedList<String>();
        boolean first = true;
        do {
            String token = toker.nextToken();
            if (token.startsWith("|||")) {
                constilationList.addAll(scoreList);
                scoreList = new LinkedList<String>();
                first = false;
                continue;
            }
            if (!first) {
                scoreList.add(token);
            }
        } while (toker.hasMoreTokens());

        IString[] foreignTokens = IStrings.toIStringArray(foreignTokenList);
        IString[] translationTokens = IStrings.toIStringArray(translationTokenList);

        Sequence<IString> foreign = new SimpleSequence<IString>(true,
                foreignTokens);
        Sequence<IString> translation = new SimpleSequence<IString>(true,
                translationTokens);
        float[] scores;
        try {
            scores = stringProbListToFloatProbArray(scoreList, doLog);
        } catch (NumberFormatException e) {
            throw new RuntimeException(String.format(
                    "Error on line: '%s' not a list of numbers", scoreList));
        }

        StringBuilder constilationB = new StringBuilder();
        {
            int idx = -1;
            for (String t : constilationList) {
                idx++;
                if (idx > 0) {
                    constilationB.append(";");
                }
                constilationB.append(t);
            }
        }

        String constilationBStr = constilationB.toString();
        if (constilationBStr.equals("")) {
            return convert(foreign, translation, null, scores);
        } else {
            return convert(foreign, translation,
                    PhraseAlignment.getPhraseAlignment(constilationBStr), scores);
        }
    }

    private static float[] stringProbListToFloatProbArray(List<String> sList, boolean doLog) {
        float[] fArray = new float[sList.size()];
        int i = 0;
        for (String s : sList) {
            float f = Float.parseFloat(s);
            if (f != f) {
                throw new RuntimeException(String.format(
                        "Bad phrase table. %s parses as (float) %f", s, f));
            }
            fArray[i++] = doLog ? (float) Math.log(f) : f;
        }
        return fArray;
    }

    @Override
    public List<TranslationOption<IString>> getTranslationOptions(Sequence<IString> sequence) {
        try {
            RawSequence<IString> rawForeign = new RawSequence<IString>(sequence);
            String src = sequence.toString(" ");
            final byte[] key = new byte[PhraseTableMapper.KEY];
            final byte[] phrBytes = src.getBytes();
            System.arraycopy(phrBytes, 0, key, 0, Math.min(PhraseTableMapper.KEY, phrBytes.length));
            final long[] range = tmm.get(key);
            if (range == null) {
                return Collections.EMPTY_LIST;
            } else {
                final long size = range[1] - range[0];
                final MappedByteBuffer map = srcTable.map(FileChannel.MapMode.READ_ONLY, range[0], size);
                byte[] buf = new byte[(int) size];
                map.get(buf);
                String s = new String(buf);
                String ss[] = s.split("\n");

                List<TranslationOption<IString>> transOpts = new ArrayList<TranslationOption<IString>>();
                for (String line : ss) {
                    final IntArrayTranslationOption intTransOpt = convert(line, src, false);
                    if (intTransOpt == null) {
                        continue;
                    }
                    RawSequence<IString> translation = new RawSequence<IString>(
                            intTransOpt.translation, IString.identityIndex());
                    transOpts.add(new TranslationOption<IString>(intTransOpt.id,
                            intTransOpt.scores, getScoreNames(intTransOpt.scores.length), translation, rawForeign,
                            intTransOpt.alignment));
                }
                return transOpts;
            }
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public int longestForeignPhrase() {
        return longestPhrase;
    }

    @Override
    public void setCurrentSequence(Sequence<IString> foreign, List<Sequence<IString>> tranList) {
        // no op
    }

    public static void createIndex(boolean withGaps) {
        foreignIndex = new DynamicIntegerArrayIndex();
        translationIndex = new DynamicIntegerArrayIndex();
    }

    public String[] getScoreNames(int countScores) {
        if (scoreNames != null) {
            return scoreNames;
        } else {

            String[] scoreNames;
            if (customScores != null) {
                scoreNames = customScores;
            } else if (countScores == 5) {
                scoreNames = CANONICAL_FIVESCORE_SCORE_TYPES;
            } else if (countScores == 1) {
                scoreNames = CANONICAL_ONESCORE_SCORE_TYPES;
            } else {
                scoreNames = new String[countScores];
                for (int i = 0; i < countScores; i++) {
                    scoreNames[i] = String.format("%d.UnkTScore", i);
                }
            }

            if (DISABLED_SCORES != null) {
                System.err.println("Disabled features: " + DISABLED_SCORES);
                for (String istr : DISABLED_SCORES.split(",")) {
                    int i = Integer.parseInt(istr);
                    if (i < scoreNames.length) {
                        System.err.printf("Feature %s disabled.\n", scoreNames[i]);
                        scoreNames[i] = null;
                    }
                }
            }

            return scoreNames;
        }
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
