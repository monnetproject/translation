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

import eu.monnetproject.translation.phrasal.TokenizedLabelImpl;
import edu.stanford.nlp.mt.base.PhraseAlignment;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.phrasal.mmap.MemoryMap;
import eu.monnetproject.translation.phrasal.mmap.PhraseTableMapper;
import eu.monnetproject.translation.phrasal.pt.cache.Cache;
import eu.monnetproject.translation.monitor.Messages;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 * @author John McCrae
 */
public abstract class MemoryMappedPhraseTableSource<Key> implements TranslationSource {

    private final String name;
    //   private final FileChannel srcTable;
    private final File tableFile;
    //private final TreeMemoryMap tmm;
    private final MemoryMap<Key> tmm;
    private final int longestPhrase, featureCount;
    private final Language srcLang, trgLang;
    //private final Cache<StableHashByteArray, PhraseTableEntry> cache;
    private final Cache<Key, PhraseTableEntry> cache;
    private static final int LINE_BYTES = 1;

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
    
    public MemoryMappedPhraseTableSource(Cache<Key, PhraseTableEntry> cache, String filename, int featureCount, Language srcLang, Language trgLang, MemoryMap<Key> tmm) throws IOException {
        this.cache = cache;
        this.tableFile = new File(filename);
        if (!tableFile.exists()) {
            throw new IOException("Could not locate phrase table at " + tableFile.getPath());
        }
        name = String.format("FlatPhraseTable(%s)", tableFile.getName());
        File mapFile = new File(filename + ".map");
        if (!mapFile.exists()) {
            throw new RuntimeException("Something has gone wrong... child class should have created the map!");
        }
        // this.srcTable = new FileInputStream(tableFile).getChannel();
        //this.tmm = new TreeMemoryMap(mapFile, PhraseTableMapper.KEY, PhraseTableMapper.OFFSET);
        this.tmm = tmm;
        final FileChannel channel = new FileInputStream(mapFile).getChannel();
        final MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0, PhraseTableMapper.OFFSET);
        check(map.getInt());
        this.longestPhrase = map.getInt();
        this.featureCount = featureCount;
        if (longestPhrase < 1 || longestPhrase > 20) {
            throw new IllegalArgumentException("Unexpected phrase length: this is likely caused by the map being invalid");
        }
        this.srcLang = srcLang;
        this.trgLang = trgLang;
    }

    protected abstract void check(int check) throws IOException;

    protected abstract Key key(String label);

    @Override
    public PhraseTable candidates(Chunk label) {
        try {
            String src = label.getSource();
            if(src.split("\\s+").length > longestPhrase) {
                return new PhraseTableImpl(srcLang, trgLang, name, longestPhrase);
            }
            //final byte[] key = new byte[PhraseTableMapper.KEY];
            //final byte[] phrBytes = src.getBytes();
            //System.arraycopy(phrBytes, 0, key, 0, Math.min(PhraseTableMapper.KEY, phrBytes.length));
            //final List<PhraseTableEntry> cached = cache.get(new StableHashByteArray(key));
            final List<PhraseTableEntry> cached = cache.get(key(src));
            if (cached != null) {
                //log.info("in  cache: " + label.getSource());
                final PhraseTableImpl phraseTable = new PhraseTableImpl(srcLang, trgLang, name, longestPhrase);
                for (PhraseTableEntry pte : cached) {
                    if (pte != null && pte.getForeign().asString().equals(src)) {
                        phraseTable.add(pte);
                    } else {
                        Messages.warning(pte.getForeign().asString() + " != " + src);
                    }
                }
                return phraseTable;
            } else {
                //log.info("not cache: " + label.getSource());
                return candidatesFromDisk(src, key(src), cached);
            }
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    private PhraseTable candidatesFromDisk(String src, final Key key, final List<PhraseTableEntry> cached) throws IOException {
        final List<PhraseTableEntry> newCached = new ArrayList<PhraseTableEntry>();
        final long[] range = tmm.get(key);
        if (range == null) {
            //log.warning("key not found: " + src);
            cache.put(key,newCached);
            return new PhraseTableImpl(srcLang, trgLang, name, longestPhrase);
        } else {
            final long size = range[1] - range[0];
            final FileInputStream fis = new FileInputStream(tableFile);
            fis.skip(range[0]);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            long read = 0;

            final PhraseTableImpl phraseTable = new PhraseTableImpl(srcLang, trgLang, name, longestPhrase);
            while ((line = reader.readLine()) != null) {
                read += line.getBytes().length + LINE_BYTES;
                if (read > size) {
                    break;
                }
                final PhraseTableEntry pte = read(line);
                if (pte != null && pte.getForeign().asString().equals(src)) {
                    phraseTable.add(pte);
                } else {
                    Messages.warning(pte.getForeign().asString() + " != " + src);
                }
                newCached.add(pte);
            }
            cache.put(key, newCached);
            return phraseTable;
        }
    }

    private PhraseTableEntry read(String line) {

        StringTokenizer toker = new StringTokenizer(line);
        StringBuilder src = new StringBuilder();
        List<String> foreignTokenList = new LinkedList<String>();
        do {
            String token = toker.nextToken();
            if ("|||".equals(token)) {
                break;
            }
            foreignTokenList.add(token);
            src.append(token).append(" ");
        } while (toker.hasMoreTokens());

        if (!toker.hasMoreTokens()) {
            throw new RuntimeException(String.format(
                    "Additional fields expected (line %s)", line));
        }

        List<String> translationTokenList = new LinkedList<String>();

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
        
        List<String> scoreList = new LinkedList<String>();
        do {
            String  token = toker.nextToken();
            if("|||".equals(token)) {
                break;
            }
            scoreList.add(token);
        } while(toker.hasMoreTokens());
        
        double[] scores;
        try {
            scores = stringProbListToFloatProbArray(scoreList, true);
        } catch (NumberFormatException e) {
            throw new RuntimeException(String.format(
                    "Error on line: '%s' not a list of numbers", scoreList));
        }
        scoreList = new LinkedList<String>();
        
        Collection<String> constilationList = new LinkedList<String>();
        boolean first = true;
        while(toker.hasMoreTokens()) {
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
        final TokenizedLabelImpl foreign = new TokenizedLabelImpl(foreignTokenList, srcLang);
        final TokenizedLabelImpl translation = new TokenizedLabelImpl(translationTokenList, trgLang);

        String constilationBStr = constilationB.toString();
        if (constilationBStr.equals("")) {
            return new PhraseTableEntryImpl(foreign, translation, toFeatures(scores), null);
        } else {
            return new PhraseTableEntryImpl(foreign, translation, toFeatures(scores), PhraseAlignment.getPhraseAlignment(constilationBStr));
        }
    }

    private static Feature[] toFeatures(double[] scores) {
        if(scores.length == 1) {
            return new Feature[] { new Feature(CANONICAL_ONESCORE_SCORE_TYPES[0],scores[0]) };
        } else if(scores.length == 5) {
            return new Feature[] { new Feature(CANONICAL_FIVESCORE_SCORE_TYPES[0],scores[0]),
            new Feature(CANONICAL_FIVESCORE_SCORE_TYPES[1],scores[1]),
            new Feature(CANONICAL_FIVESCORE_SCORE_TYPES[2],scores[2]),
            new Feature(CANONICAL_FIVESCORE_SCORE_TYPES[3],scores[3]),
            new Feature(CANONICAL_FIVESCORE_SCORE_TYPES[4],scores[4]),
            };
        } else {
            throw new RuntimeException("Bad number of weights in phrase table");
        }
    }
    
    private static double[] stringProbListToFloatProbArray(List<String> sList, boolean doLog) {
        double[] fArray = new double[sList.size()];
        int i = 0;
        for (String s : sList) {
            double f = Double.parseDouble(s);
            if (f != f) {
                throw new RuntimeException(String.format(
                        "Bad phrase table. %s parses as (float) %f", s, f));
            }
            fArray[i++] = doLog ? (float) Math.log(f) : f;
        }
        return fArray;
    }

//    @Override
    public int featureCount() {
        return featureCount;
    }

    @Override
    public String[] featureNames() {
        return featureCount == 1 ? CANONICAL_ONESCORE_SCORE_TYPES : CANONICAL_FIVESCORE_SCORE_TYPES ;
    }
    
    

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void close() {
        try {
            tmm.dispose();
        } catch(IOException x) {
            x.printStackTrace();
        }
    }
}
