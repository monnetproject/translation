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
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class MemoryLM extends AbstractLM {

    private final Object2ObjectMap<NGram, float[]> map;
    private final Language language;
    private int order;
    private final DoubleList mus = new DoubleArrayList(), sis = new DoubleArrayList();

    public MemoryLM(Language language, File model) throws IOException, ClassNotFoundException {
        this.language = language;
        this.map = new File(model.getPath() + ".mem").exists() ? loadLM(model) : buildLM(model);
        if (str2key.containsKey(UNK)) {
            unkCode = str2key.getInt(UNK);
        } else {
            unkCode = str2key.size() + 1;
        }
    }


    @Override
    protected double[] rawScore(int n, NGram key) {
        final float[] f = map.get(key);
        if (f == null) {
            return null;
        } else {
            double[] d = new double[f.length];
            for (int i = 0; i < d.length; i++) {
                d[i] = f[i];
            }
            return d;
        }
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
        return "MemoryLM";
    }

    @Override
    public void close() {
        
    }

    @Override
    protected double mean(int n) {
        return (n > 0 && n <= mus.size()) ? mus.get(n-1) : Double.NEGATIVE_INFINITY;
    }

    @Override
    protected double sd(int n) {
        return (n > 0 && n <= sis.size()) ? mus.get(n-1) : 0;
    }

    @SuppressWarnings({"unchecked", "unchecked"})
    private Object2ObjectMap<NGram, float[]> loadLM(File model) throws IOException, ClassNotFoundException {
        final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(model.getPath() + ".mem")));
        order = ois.readInt();
        str2key = (Object2IntMap<String>) ois.readObject();
        final Object2ObjectMap<NGram, float[]> map2 = (Object2ObjectMap<NGram, float[]>) ois.readObject();
        ois.close();
        return map2;
    }

    private Object2ObjectMap<NGram, float[]> buildLM(File model) throws IOException {
        final Object2ObjectMap<NGram, float[]> map2 = new Object2ObjectOpenHashMap<NGram, float[]>();
        final ARPAReader reader = new ARPAReader() {
            @Override
            protected void prepare(int order) throws IOException {
            }

            @Override
            protected void put(int[] ng, double[] scores) throws IOException {
                final float[] f = new float[scores.length];
                for (int i = 0; i < scores.length; i++) {
                    f[i] = (float) scores[i];
                }
                map2.put(new NGram(ng), f);
            }

            @Override
            protected void end(int order) throws IOException {
            }

            @Override
            protected void finished() throws IOException {
            }

            @Override
            protected void statistics(int n, double mu, double sd) throws IOException {
                mus.add(mu);
                sis.add(sd);
            }
            
            
        };
        reader.read(model);
        this.str2key = reader.str2key;
        this.order = reader.order;
        final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(model.getPath() + ".mem"));
        oos.writeInt(order);
        oos.writeObject(str2key);
        oos.writeObject(map2);
        oos.flush();
        oos.close();
        return map2;
    }
}
