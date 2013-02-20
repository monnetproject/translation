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

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.monitor.Messages;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMaps;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Scanner;

/**
 *
 * @author John McCrae
 */
public class PagedLM extends AbstractLM {

    private final Object2DoubleMap<NGram> salients = Object2DoubleMaps.synchronize(new Object2DoubleOpenHashMap<NGram>());
    private final Language language;
    private int order;
    private Int2ObjectMap<double[]> unigrams;
    private DirectDataMMap map;
    private double[] mus, sds;

    public PagedLM(Language language, File model) throws IOException, ClassNotFoundException {
        this.language = language;
        Messages.info("Using " + model.getPath() + " for language " + language);
        this.map = new File(model.getPath() + ".static").exists() ? loadLM(model) : buildLM(model);
        if (str2key.containsKey(UNK)) {
            unkCode = str2key.getInt(UNK);
        } else {
            unkCode = str2key.size() + 1;
        }
//        try {
//            final Scanner in = new Scanner(new File("salience.uniq"));
//            while (in.hasNextLine()) {
//                final String line = in.nextLine();
//                final double score = Double.parseDouble(line.substring(0, line.indexOf(" ")));
//                final String ngram = line.substring(line.indexOf(" ") + 1, line.length()).trim();
//                if (score > 0) {
//                    salients.put(super.toKey(Arrays.asList(ngram.split(" "))), score);
//                }
//            }
//
//        } catch (Exception x) {
//            x.printStackTrace();
//        }
    }

    @Override
    public Language getLanguage() {
        return language;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public String getName() {
        return "PageMapDBLM";
    }

    @Override
    public void close() {
        try {
            Messages.info("Raw score calls: " + rawScoreCalls);
            map.close();
        } catch (Exception x) {
            Messages.cleanupFailure(x);
        }
    }

    @Override
    protected double mean(int n) {
        if (n > 0 && n <= mus.length) {
            return mus[n - 1];
        } else {
            return -Double.NEGATIVE_INFINITY;
        }
    }

    @Override
    protected double sd(int n) {
        if (n > 0 && n <= sds.length) {
            return sds[n - 1];
        } else {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private DirectDataMMap loadLM(File file) throws IOException, ClassNotFoundException {

        final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(file.getPath() + ".static")));
        order = ois.readInt();
        mus = (double[]) ois.readObject();
        sds = (double[]) ois.readObject();
        unigrams = (Int2ObjectMap<double[]>) ois.readObject();
        str2key = (Object2IntMap<String>) ois.readObject();
        ois.close();
        return new DirectDataMMap(file);
    }

    private DirectDataMMap buildLM(File model) throws IOException {
        final DirectDataMMapBuilder builder = new DirectDataMMapBuilder(model);
        final DoubleArrayList mus = new DoubleArrayList(), sds = new DoubleArrayList();
        final ARPAReader reader = new ARPAReader() {
            @Override
            protected void prepare(int i) {
                if (i == 1) {
                    // Reduced load factor for speed
                    unigrams = new Int2ObjectOpenHashMap<double[]>(50000, 0.4f);
                }

            }

            @Override
            protected void put(int[] ng, double[] scores) throws IOException {
                if (ng.length == 1) {
                    unigrams.put(ng[0], scores);
                } else {
                    builder.accept(ng, scores);
                }
            }

            @Override
            protected void end(int order) {
            }

            @Override
            protected void finished() throws IOException {
                builder.close();
            }

            @Override
            protected void statistics(int n, double mu, double sd) throws IOException {
                mus.add(mu);
                sds.add(sd);
            }
        };
        reader.read(model);
        this.order = reader.order;
        this.str2key = reader.str2key;

        final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(model.getPath() + ".static")));
        oos.writeInt(order);
        oos.writeObject(mus.toDoubleArray());
        oos.writeObject(sds.toDoubleArray());
        oos.writeObject(unigrams);
        oos.writeObject(str2key);
        oos.close();
        // We could use the builder to create the record map, instead of re-reading
        // it from disk
        return new DirectDataMMap(model, builder.dataLocMap);
    }
    private int rawScoreCalls = 0;

    @Override
    protected double[] rawScore(int n, NGram key) {
//        final double[] rs = rawScore2(n, key);
//        if (rs != null && salients.containsKey(key)) {
//            return rs.length == 2 ? new double[]{rs[0] + 2.0 * salients.getDouble(key), rs[1]} : new double[]{rs[0] + 2.0 * salients.getDouble(key)};
//        } else {
//            return rs;
//        }
//    }
//
//    protected double[] rawScore2(int n, NGram key) {
        if (n == 1) {
            return unigrams.get(key.data(0));
        } else {
            //synchronized (this) {
            rawScoreCalls++;
            return map.rawScore(key.data());
            //}
        }
    }
}
