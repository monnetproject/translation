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
package eu.monnetproject.translation.phrasal.lm;

import edu.stanford.nlp.mt.base.FixedLengthIntegerArrayRawIndex;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.IntegerArrayRawIndex;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.LanguageModel;
import eu.monnetproject.translation.monitor.Messages;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

/**
 * (Modified from main Phrasal code)
 *
 * @author John McCrae
 */
public class ARPALanguageModel implements LanguageModel {

    private final Language language;
    static boolean verbose = false;
    protected final String name;
    public static final String START_TOKEN = "<s>";
    public static final String END_TOKEN = "</s>";
    public static final String UNK_TOKEN = "<unk>";

    @Override
    public String getName() {
        return name;
    }

    protected static String readLineNonNull(LineNumberReader reader)
            throws IOException {
        String inline = reader.readLine();
        if (inline == null) {
            throw new RuntimeException(String.format("premature end of file"));
        }
        return inline;
    }
    protected IntegerArrayRawIndex[] tables;
    private float[][] probs;
    private float[][] bows;
    protected static final int MAX_GRAM = 10; // highest order ngram possible
    protected static final float LOAD_MULTIPLIER = (float) 1.7;
    //protected static final WeakHashMap<String, ARPALanguageModel> lmStore = new WeakHashMap<String, ARPALanguageModel>();

    public ARPALanguageModel(String filename, Language language) throws IOException {
        this.language = language;
        name = String.format("APRA(%s)", filename);
        init(filename);
    }

    protected final void init(String filename) throws IOException {
        File f = new File(filename);

        System.gc();
        Runtime rt = Runtime.getRuntime();
        long preLMLoadMemUsed = rt.totalMemory() - rt.freeMemory();
        long startTimeMillis = System.currentTimeMillis();

        if(!f.exists()) {
            throw new IOException(f.getPath() + " does not exist");
        }
        
        if(!f.canRead()) {
            throw new IOException(f.getPath() + " was not readable");
        }
        
        LineNumberReader reader = (filename.endsWith(".gz") ? new LineNumberReader(
                new InputStreamReader(
                new GZIPInputStream(new FileInputStream(filename))))
                : new LineNumberReader(new FileReader(f)));

        // skip everything until the line that begins with '\data\'
        while (!readLineNonNull(reader).startsWith("\\data\\")) {
        }

        // read in ngram counts
        int[] ngramCounts = new int[MAX_GRAM];
        String inline;
        int maxOrder = 0;
        while ((inline = readLineNonNull(reader)).startsWith("ngram")) {
            inline = inline.replaceFirst("ngram\\s+", "");
            String[] fields = inline.split("=");
            int ngramOrder = Integer.parseInt(fields[0]);
            if (ngramOrder > MAX_GRAM) {
                throw new RuntimeException(String.format("Max n-gram order: %d\n",
                        MAX_GRAM));
            }
            ngramCounts[ngramOrder - 1] = Integer.parseInt(fields[1].replaceFirst(
                    "[^0-9].*$", ""));
            if (maxOrder < ngramOrder) {
                maxOrder = ngramOrder;
            }
        }

        tables = new FixedLengthIntegerArrayRawIndex[maxOrder];
        probs = new float[maxOrder][];
        bows = new float[maxOrder - 1][];
        for (int i = 0; i < maxOrder; i++) {
            int tableSz = Integer.highestOneBit((int) (ngramCounts[i] * LOAD_MULTIPLIER)) << 1;
            tables[i] = new FixedLengthIntegerArrayRawIndex(i + 1,
                    Integer.numberOfTrailingZeros(tableSz));
            probs[i] = new float[tableSz];
            if (i + 1 < maxOrder) {
                bows[i] = new float[tableSz];
            }
        }

        float log10LogConstant = (float) Math.log(10);

        // read in the n-gram tables one by one
        for (int order = 0; order < maxOrder; order++) {
            Messages.info(String.format("Reading %d %d-grams...", probs[order].length,
                    order + 1));
            String nextOrderHeader = String.format("\\%d-grams:", order + 1);
            IString[] ngram = new IString[order + 1];
            int[] ngramInts = new int[order + 1];

            // skip all material upto the next n-gram table header
            while (!readLineNonNull(reader).startsWith(nextOrderHeader)) {
            }

            // read in table
            while (!(inline = readLineNonNull(reader)).equals("")) {
                // during profiling, 'split' turned out to be a bottle neck
                // and using StringTokenizer is about twice as fast
                StringTokenizer tok = new StringTokenizer(inline);
                float prob = Float.parseFloat(tok.nextToken()) * log10LogConstant;

                for (int i = 0; i <= order; i++) {
                    ngram[i] = new IString(tok.nextToken());
                    ngramInts[i] = ngram[i].getId();
                }

                float bow = (tok.hasMoreElements() ? Float.parseFloat(tok.nextToken())
                        * log10LogConstant : Float.NaN);
                int index = tables[order].insertIntoIndex(ngramInts);
                probs[order][index] = prob;
                if (order < bows.length) {
                    bows[order][index] = bow;
                }
            }
        }

        System.gc();

        // print some status information
        long postLMLoadMemUsed = rt.totalMemory() - rt.freeMemory();
        long loadTimeMillis = System.currentTimeMillis() - startTimeMillis;
        Messages.info(String.format(
                "Done loading arpa lm: %s (order: %d) (mem used: %d MiB time: %.3f s)",
                filename, maxOrder, (postLMLoadMemUsed - preLMLoadMemUsed)
                / (1024 * 1024), loadTimeMillis / 1000.0));
        reader.close();
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     *
     * From CMU language model headers:
     * ------------------------------------------------------------------
     *
     * This file is in the ARPA-standard format introduced by Doug Paul.
     *
     * p(wd3|wd1,wd2)= if(trigram exists) p_3(wd1,wd2,wd3) else if(bigram w1,w2
     * exists) bo_wt_2(w1,w2)*p(wd3|wd2) else p(wd3|w2)
     *
     * p(wd2|wd1)= if(bigram exists) p_2(wd1,wd2) else bo_wt_1(wd1)*p_1(wd2)
     *
     */
    protected double scoreR(List<String> sequence) {
        int[] ngramInts = toIntArray(sequence);
        int index;

        index = tables[ngramInts.length - 1].getIndex(ngramInts);
        if (index >= 0) { // found a match
            double p = probs[ngramInts.length - 1][index];
            if (verbose) {
                System.err.printf("scoreR: seq: %s logp: %f\n", sequence.toString(), p);
            }
            return p;
        }
        if (ngramInts.length == 1) {
            return Double.NEGATIVE_INFINITY; // OOV
        }
        //List<String> prefix = sequence.subList(0, ngramInts.length - 1);
        //int[] prefixInts = toIntArray(prefix);
        int[] prefixInts = Arrays.copyOfRange(ngramInts, 0, ngramInts.length - 1);
        index = tables[prefixInts.length - 1].getIndex(prefixInts);
        double bow = 0;
        if (index >= 0) {
            bow = bows[prefixInts.length - 1][index];
        }
        if (Double.isNaN(bow)) {
            bow = 0.0; // treat NaNs as bow that are not found at all
        }
        double p = bow + scoreR(sequence.subList(1, ngramInts.length));
        if (verbose) {
            System.err.printf("scoreR: seq: %s logp: %f [%f] bow: %f\n",
                    sequence.toString(), p, p / Math.log(10), bow);
        }
        return p;
    }

    /**
     * Determines whether we are computing p( <s> | <s> ... ) or p( w_n=</s> |
     * w_n-1=</s> ..), in which case log-probability is zero. This function is
     * only useful if the translation hypothesis contains explicit <s> and </s>,
     * and always returns false otherwise.
     */
    boolean isBoundaryWord(List<String> sequence) {
        if (sequence.size() == 2 && sequence.get(0).equals(START_TOKEN)
                && sequence.get(1).equals(START_TOKEN)) {
            return true;
        }
        if (sequence.size() > 1) {
            int last = sequence.size() - 1;
            String endTok = END_TOKEN;
            if (sequence.get(last).equals(endTok)
                    && sequence.get(last - 1).equals(endTok)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public double score(List<String> sequence) {
        if (isBoundaryWord(sequence)) {
            return 0.0;
        }
        List<String> ngram;
        int sequenceSz = sequence.size();
        int maxOrder = (probs.length < sequenceSz ? probs.length : sequenceSz);

        if (sequenceSz == maxOrder) {
            ngram = sequence;
        } else {
            ngram = sequence.subList(sequenceSz - maxOrder, sequenceSz);
        }
        return scoreR(ngram);
    }

    @Override
    public int getOrder() {
        return probs.length;
    }

    @Override
    public boolean isRelevantPrefix(List<String> prefix) {
        if (prefix.size() > probs.length - 1) {
            return false;
        }
        int[] prefixInts = toIntArray(prefix);
        int index = tables[prefixInts.length - 1].getIndex(prefixInts);
        if (index < 0) {
            return false;
        }
        double bow = bows[prefixInts.length - 1][index];
        return !Double.isNaN(bow);
    }

    @Override
    public Language getLanguage() {
        return language;
    }

    public static int stringToId(String s) {
        return new IString(s).getId();
    }

    public static synchronized int[] toIntArray(List<String> prefix) {
        final int[] ints = new int[prefix.size()];
        for (int i = 0; i < prefix.size(); i++) {
            ints[i] = stringToId(prefix.get(i));
        }
        return ints;
    }

    @Override
    public void close() {
    }

    @Override
    public int quartile(List<String> tokens) {
        return 0;
    }
    
    
    
}
