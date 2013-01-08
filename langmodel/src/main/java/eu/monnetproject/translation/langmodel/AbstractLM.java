/**
 * *******************************************************************************
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
package eu.monnetproject.translation.langmodel;

import eu.monnetproject.translation.monitor.Messages;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author John McCrae
 */
public abstract class AbstractLM implements LanguageModelAndTrueCaser {

    protected final String UNK = "<UNK>";
    protected Object2IntMap<String> str2key = new Object2IntOpenHashMap<String>();
    protected int unkCode;

    @Override
    public boolean isRelevantPrefix(List<String> tokens) {
        int n = tokens.size();
        if (n == 0) {
            return true;
        }
        if (n >= getOrder()) {
            return false;
        }
        NGram key = toKey(tokens);
        final double[] p = rawScore(n, key);
        return p != null && p.length > 1 && !Double.isNaN(p[1]);
    }

    @Override
    public double score(List<String> tokens) {
        int n = tokens.size();
        if (n == 0) {
            return Double.NaN;
        }
        final int order = getOrder();
        if (n > order) {
            tokens = tokens.subList(tokens.size() - order, tokens.size());
            n = order;
        }
        if (tokens.isEmpty()) {
            return Double.NaN;
        }
        NGram key = toKey(tokens);

        final double[] p1 = rawScore(n, key);
        if (p1 != null) {
            return p1[0];
        } else {
            double score = 0.0;
            while (n > 1) {
                NGram left = key.dropRight();
                final double[] p2 = rawScore(n - 1, left);
                if (p2 != null && p2.length == 2) {
                    score += Double.isNaN(p2[1]) ? 0.0 : p2[1];
                }
                key = key.dropLeft();
                n--;
                final double[] p3 = rawScore(n, key);
                if (p3 != null) {
                    return score + p3[0];
                }
            }
            return Double.NEGATIVE_INFINITY;
        }
    }

    @Override
    public String[] trueCase(String[] tokens, int id) {
        final String[] trueCased = Arrays.copyOf(tokens, tokens.length);
        final int[] iTrueCased = new int[tokens.length];
        int n = getOrder();

        for (int i = 0; i < tokens.length; i++) {
            final int[] ng = new int[i < n ? i + 1 : n];
            //final NGram nGram = new NGram(ng);
            if (i > 0 && n > 0) {
                System.arraycopy(iTrueCased, i < n ? 0 : i - n, ng, 0, i < n ? i : n - 1);
            }
            // lower cased
            final String lc = trueCased[i].toLowerCase();
            int lcId = toKey(lc, false);
            final double lcScore;
            if (lcId > 0) {
                ng[i < n ? i : n - 1] = lcId;
                lcScore = scoreNGram(new NGram(ng));
            } else {
                lcScore = Double.NEGATIVE_INFINITY;
            }

            // upper cased     
            final String uc = trueCased[i].toUpperCase();
            int ucId = toKey(uc, false);
            final double ucScore;
            if (ucId > 0) {
                ng[i < n ? i : n - 1] = ucId;
                ucScore = scoreNGram(new NGram(ng));
            } else {
                ucScore = Double.NEGATIVE_INFINITY;
            }

            // upper first
            final String uf = trueCased[i].substring(0, 1).toUpperCase() + trueCased[i].substring(1);
            int ufId = toKey(uf, false);
            final double ufScore;
            if (ufId > 0) {
                ng[i < n ? i : n - 1] = ufId;
                ufScore = scoreNGram(new NGram(ng));
            } else {
                ufScore = Double.NEGATIVE_INFINITY;
            }
            if (ufScore > ucScore && ufScore > lcScore) {
                trueCased[i] = uf;
                iTrueCased[i] = ufId;
            } else if (ucScore > lcScore && ucScore > ufScore) {
                trueCased[i] = uc;
                iTrueCased[i] = ucId;
            } else if (lcScore > ucScore && lcScore > ucScore) {
                trueCased[i] = lc;
                iTrueCased[i] = lcId;
            } else {
                iTrueCased[i] = toKey(trueCased[i], true);
            }
        }

        return trueCased;
    }
    private final double ONE_SD = 0.6744898;

    @Override
    public int quartile(List<String> tokens) {
        int n = tokens.size();
        if (n == 0) {
            return 0;
        }
        final int order = getOrder();
        if (n > order) {
            tokens = tokens.subList(tokens.size() - order, tokens.size());
            n = order;
        }
        if (tokens.isEmpty()) {
            return 0;
        }
        NGram key = toKey(tokens);
        final double mu = mean(n);
        final double si = sd(n);
        final double[] ps = rawScore(n, key);
        double p = Double.NEGATIVE_INFINITY;
        if (ps != null) {
            p = ps[0];
        } else {
            double score = 0.0;
            while (n > 1) {
                NGram left = key.dropRight();
                final double[] p2 = rawScore(n - 1, left);
                if (p2 != null && p2.length == 2) {
                    score += Double.isNaN(p2[1]) ? 0.0 : p2[1];
                }
                key = key.dropLeft();
                n--;
                final double[] p3 = rawScore(n, key);
                if (p3 != null) {
                    p = score + p3[0];
                    break;
                }
            }
        }
        final double ep = Math.exp(p);
        if (ep < mu - si * ONE_SD) {
            return 1;
        } else if (ep < mu) {
            return 2;
        } else if (ep < mu + si * ONE_SD) {
            return 3;
        } else {
            return 4;
        }
    }

    protected abstract double mean(int n);

    protected abstract double sd(int n);

    protected NGram toKey(List<String> tokens) {
        final int[] ng = new int[tokens.size()];
        int i = 0;
        for (String token : tokens) {
            if (str2key.containsKey(token)) {
                ng[i++] = str2key.getInt(token);
            } else {
                ng[i++] = unkCode;
            }
        }
        return new NGram(ng);
    }
    
    protected int toKey(String token, boolean useUnk) {
        if (str2key.containsKey(token)) {
            return str2key.getInt(token);
        } else {
            return useUnk ? unkCode : -1;
        }
    }

    protected abstract double[] rawScore(int n, NGram key);

    private double scoreNGram(final NGram nGram) {
        NGram ng2 = nGram;
        while (ng2.length() > 0) {
            final double[] score = rawScore(ng2.length(), ng2);
            if (score != null) {
                return score[0];
            } else {
                ng2 = ng2.dropLeft();
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    protected static class NGram implements Serializable/*, Comparable<NGram>*/ {

        private static final long serialVersionUID = -8900687174528824990L;
        public static final NGram EMPTY_NGRAM = new NGram(new int[0]);
        private int[] data;
        private int offset, length;
        private int hashCode;

        public NGram(int[] data) {
            this.data = data;
            this.offset = 0;
            this.length = data.length;

            hashCode = length;
            for (int i = 0; i < length; i++) {
                hashCode = 31 * hashCode + data[i + offset];
            }
        }

        private NGram(int[] data, int offset, int length) {
            this.data = data;
            this.offset = offset;
            this.length = length;

            hashCode = length;
            for (int i = 0; i < length; i++) {
                hashCode = 31 * hashCode + data[i + offset];
            }
        }

        public int[] data() {
            if (offset == 0 && data.length == length) {
                return data;
            } else {
                data = Arrays.copyOfRange(data, offset, offset + length);
                offset = 0;
                length = data.length;
                return data;
            }
        }

        public int data(int i) {
            return data[offset + i];
        }

        public int length() {
            return length;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            NGram other = (NGram) obj;
            if (obj == this) {
                return true;
            }
            if (length != other.length) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (data[i + offset] != other.data[i + other.offset]) {
                    return false;
                }
            }
            return true;
        }

        public NGram dropLeft() {
            if (length == 0) {
                throw new IllegalArgumentException();
            } else if (length == 1) {
                return EMPTY_NGRAM;
            } else {
                return new NGram(data, offset + 1, length - 1);
            }
        }

        public NGram dropRight() {
            return new NGram(data, offset, length - 1);
        }
    }

    protected abstract static class ARPAReader {

        public Object2IntMap<String> str2key;
        int order;

        private String expectLine(Scanner scanner, String lineRegex) throws IOException {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                if (line.contains(lineRegex)) {
                    return line;
                }
            }
            throw new IOException("Expected " + lineRegex + " but got EOF");
        }

        public void read(File model) throws IOException {
            final Scanner scanner;
            if(model.getName().endsWith(".gz")) {
                scanner = new Scanner(new GZIPInputStream(new FileInputStream(model)));
            } else {
                scanner = new Scanner(model);
            }
            // Reduced load factor for speed
            str2key = new Object2IntOpenHashMap<String>(50000,0.4f);

            expectLine(scanner, "\\data\\");
            final IntList ngramSizes = new IntArrayList();
            order = 0;
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                if (!line.matches("\\s*")) {
                    order++;
                    final String ngramOeq = "ngram " + order + "=";
                    if (line.startsWith(ngramOeq)) {
                        final String sizeStr = line.substring(ngramOeq.length());
                        if (sizeStr.equals("?")) {
                            ngramSizes.add(-1);
                        } else {
                            ngramSizes.add(Integer.parseInt(sizeStr));
                        }
                    }
                } else {
                    break;
                }
            }

            int W = 1;

            for (int i = 1; i <= order; i++) {
                double mu = 0.0;
                double si = 0.0;
                double n = 0;
                expectLine(scanner, "\\" + i + "-grams:");

                prepare(i);
                int read = 0;
                LINES:
                while (scanner.hasNextLine()) {
                    final String line = scanner.nextLine();
                    if (line.matches("\\s*")) {
                        break;
                    }
                    if (++read % 10000 == 0) {
                        System.err.print(".");
                    }
                    final String[] elems = line.split("\\t");
                    if (elems.length != 2 && elems.length != 3) {
                        throw new IOException("Bad line: " + line + "[" + read + "]");
                    }
                    final double p = Double.parseDouble(elems[0]);
                    if (i == 1) {
                        put(new int[]{W}, elems.length == 3
                                ? new double[]{p, Double.parseDouble(elems[2])}
                                : new double[]{p});
                        str2key.put(elems[1], W++);
                    } else {
                        int[] ng = new int[i];
                        String[] words = elems[1].split("\\s+");
                        if (words.length != i) {
                            Messages.severe("Element " + elems[1] + " was under " + order + "-grams but does not have that many elements");
                            continue;
                        }
                        for (int j = 0; j < i; j++) {
                            if (!str2key.containsKey(words[j])) {
                                Messages.severe(words[j] + " first seen in higher order grams");
                                continue LINES;
                            }
                            ng[j] = str2key.get(words[j]);
                        }
                        put(ng, elems.length == 3
                                ? new double[]{p, Double.parseDouble(elems[2])}
                                : new double[]{p});
                    }
                    final double ep = Math.exp(p);
                    mu += ep;
                    si += ep * ep;
                    n++;
                }
                System.err.println();
                if (read != ngramSizes.get(i - 1) && ngramSizes.get(i - 1) >= 0) {
                    Messages.warning("Read " + read + " " + i + "-grams, but expected " + ngramSizes.get(i - 1));
                } else {
                    Messages.info("Read " + read + " " + i + "-grams");
                }
                statistics(i, mu / n, Math.sqrt((si - mu * 2) / (n - 1)));
                end(i);
            }
            expectLine(scanner, "\\end\\");
            finished();
        }

        protected abstract void prepare(int order) throws IOException;

        protected abstract void put(int[] ng, double[] scores) throws IOException;

        protected abstract void statistics(int n, double mu, double sd) throws IOException;

        protected abstract void end(int order) throws IOException;

        protected abstract void finished() throws IOException;
    }
}
