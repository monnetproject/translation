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
package eu.monnetproject.translation.fidel;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Collection;
import static java.lang.Math.*;
import java.util.Random;

/**
 *
 * @author John McCrae
 */
public class FidelDecoderPerformance {

    private final static Random r = new Random();
    private final static Object2ObjectOpenHashMap<Phrase, Collection<PhraseTranslation>> pt = new Object2ObjectOpenHashMap<Phrase, Collection<PhraseTranslation>>();

    static {
        pt.put(new Phrase(new int[]{0}), Arrays.asList( // er
                new PhraseTranslation(new int[]{6}, new double[]{log10(0.474756), log10(0.427108), log10(0.128955), log10(0.144797), log10(2.718)}), // he
                new PhraseTranslation(new int[]{7}, new double[]{log10(0.0824588), log10(0.0631851), log10(0.370504), log10(0.354335), log10(2.718)}))); // it
        pt.put(new Phrase(new int[]{0, 1}), Arrays.asList( // er geht
                new PhraseTranslation(new int[]{7, 8}, new double[]{log10(5.91106e-06), log10(6.59882e-07), log10(0.278564), log10(0.089717), log10(2.718)}), // it is
                new PhraseTranslation(new int[]{7, 9}, new double[]{log10(0.0928548), log10(0.00613448), log10(0.278564), log10(0.00244537), log10(2.718)}))); // it goes
        pt.put(new Phrase(new int[]{1}), Arrays.asList( // geht
                new PhraseTranslation(new int[]{9}, new double[]{log10(0.00076391), log10(0.0009707), log10(0.0957447), log10(0.089717), log10(2.718)}), // goes
                new PhraseTranslation(new int[]{8}, new double[]{log10(0.195122), log10(0.0970874), log10(0.0106383), log10(0.0069013), log10(2.718)}))); // is
        pt.put(new Phrase(new int[]{2}), Arrays.asList( // ja
                new PhraseTranslation(new int[]{10}, new double[]{log10(0.688525), log10(0.596154), log10(0.0969977), log10(0.124), log10(2.718)}))); // yes
        pt.put(new Phrase(new int[]{2, 3}), Arrays.asList( // ja nicht
                new PhraseTranslation(new int[]{11}, new double[]{log10(1.15264e-05), log10(1.18081e-05), log10(0.557129), log10(0.521708), log10(2.718)}) // not
                ));
        pt.put(new Phrase(new int[]{3}), Arrays.asList( // nicht
                new PhraseTranslation(new int[]{11}, new double[]{log10(0.593731), log10(0.624766), log10(0.391964), log10(0.521708), log10(2.718)}), // not
                new PhraseTranslation(new int[]{12, 11}, new double[]{log10(0.518193), log10(0.53734), log10(0.0443482), log10(0.0226303), log10(2.718)})));
        pt.put(new Phrase(new int[]{4}), Arrays.asList( // nach
                new PhraseTranslation(new int[]{13}, new double[]{log10(0.623099), log10(0.678103), log10(0.0951965), log10(0.0844883), log10(2.718)}), // after
                new PhraseTranslation(new int[]{14}, new double[]{log10(0.00914233), log10(0.0189595), log10(0.0386796), log10(0.102775), log10(2.718)})));
        pt.put(new Phrase(new int[]{4, 5}), Arrays.asList( // nach hause
                new PhraseTranslation(new int[]{16}, new double[]{log10(0.0462963), log10(0.0122825), log10(0.625), log10(0.6), log10(2.718)})));
        pt.put(new Phrase(new int[]{5}), Arrays.asList( // hause
                new PhraseTranslation(new int[]{15}, new double[]{log10(0.0462963), log10(0.0122825), log10(0.625), log10(0.6), log10(2.718)})));
    }
    private static final Object2ObjectOpenHashMap<Phrase, double[]> lm = new Object2ObjectOpenHashMap<Phrase, double[]>();

    static {
        lm.put(new Phrase(new int[]{13}), new double[]{-3.048014, -0.5595744});
        lm.put(new Phrase(new int[]{12}), new double[]{-2.942701, -0.7269722});
        lm.put(new Phrase(new int[]{9}), new double[]{-3.485496, -0.8144186});
        lm.put(new Phrase(new int[]{6}), new double[]{-3.320179, -0.4907385});
        lm.put(new Phrase(new int[]{16}), new double[]{-3.894, -0.4098516});
        lm.put(new Phrase(new int[]{15}), new double[]{-4.069491, -0.1530379});
        lm.put(new Phrase(new int[]{8}), new double[]{-2.257655, -0.6430779});
        lm.put(new Phrase(new int[]{7}), new double[]{-2.622992, -0.5401071});
        lm.put(new Phrase(new int[]{11}), new double[]{-2.871661, -0.2037089});
        lm.put(new Phrase(new int[]{14}), new double[]{-2.203269, -0.6064922});
        lm.put(new Phrase(new int[]{10}), new double[]{-4.335011, -0.5965024});

        lm.put(new Phrase(new int[]{13, 6}), new double[]{-2.456458});
        lm.put(new Phrase(new int[]{13, 8}), new double[]{-3.731876});
        lm.put(new Phrase(new int[]{13, 7}), new double[]{-1.941688});
        lm.put(new Phrase(new int[]{13, 11}), new double[]{-3.731876});
        lm.put(new Phrase(new int[]{12, 13}), new double[]{-3.280287});
        lm.put(new Phrase(new int[]{12, 6}), new double[]{-2.15744});
        lm.put(new Phrase(new int[]{12, 8}), new double[]{-2.210782});
        lm.put(new Phrase(new int[]{12, 7}), new double[]{-1.685437});
        lm.put(new Phrase(new int[]{12, 11}), new double[]{-0.4060034});
        lm.put(new Phrase(new int[]{12, 14}), new double[]{-2.426915});
        lm.put(new Phrase(new int[]{9, 13}), new double[]{-3.661454});
        lm.put(new Phrase(new int[]{9, 16}), new double[]{-2.932294});
        lm.put(new Phrase(new int[]{9, 8}), new double[]{-3.661454});
        lm.put(new Phrase(new int[]{9, 7}), new double[]{-3.168323});
        lm.put(new Phrase(new int[]{9, 11}), new double[]{-3.168323});
        lm.put(new Phrase(new int[]{9, 14}), new double[]{-1.046571});
        lm.put(new Phrase(new int[]{6, 12}), new double[]{-2.046349});
        lm.put(new Phrase(new int[]{6, 9}), new double[]{-2.74139});
        lm.put(new Phrase(new int[]{6, 15}), new double[]{-4.246866});
        lm.put(new Phrase(new int[]{6, 8}), new double[]{-1.39645});
        lm.put(new Phrase(new int[]{6, 7}), new double[]{-4.246866});
        lm.put(new Phrase(new int[]{6, 11}), new double[]{-2.623379});
        lm.put(new Phrase(new int[]{6, 14}), new double[]{-3.753734});
        lm.put(new Phrase(new int[]{16, 13}), new double[]{-2.988746});
        lm.put(new Phrase(new int[]{16, 12}), new double[]{-2.988746});
        lm.put(new Phrase(new int[]{16, 15}), new double[]{-3.481877});
        lm.put(new Phrase(new int[]{16, 8}), new double[]{-1.976402});
        lm.put(new Phrase(new int[]{16, 7}), new double[]{-2.988746});
        lm.put(new Phrase(new int[]{16, 11}), new double[]{-2.988746});
        lm.put(new Phrase(new int[]{16, 14}), new double[]{-1.28136});
        lm.put(new Phrase(new int[]{15, 13}), new double[]{-2.974661});
        lm.put(new Phrase(new int[]{15, 12}), new double[]{-2.974661});
        lm.put(new Phrase(new int[]{15, 6}), new double[]{-3.703822});
        lm.put(new Phrase(new int[]{15, 8}), new double[]{-1.987634});
        lm.put(new Phrase(new int[]{15, 7}), new double[]{-3.703822});
        lm.put(new Phrase(new int[]{15, 11}), new double[]{-3.21069});
        lm.put(new Phrase(new int[]{15, 14}), new double[]{-2.198346});
        lm.put(new Phrase(new int[]{8, 13}), new double[]{-3.562478});
        lm.put(new Phrase(new int[]{8, 12}), new double[]{-4.242198});
        lm.put(new Phrase(new int[]{8, 9}), new double[]{-5.091981});
        lm.put(new Phrase(new int[]{8, 6}), new double[]{-3.769841});
        lm.put(new Phrase(new int[]{8, 16}), new double[]{-3.961626});
        lm.put(new Phrase(new int[]{8, 8}), new double[]{-4.398751});
        lm.put(new Phrase(new int[]{8, 7}), new double[]{-2.911189});
        lm.put(new Phrase(new int[]{8, 11}), new double[]{-1.577322});
        lm.put(new Phrase(new int[]{8, 14}), new double[]{-1.801822});
        lm.put(new Phrase(new int[]{8, 10}), new double[]{-3.961626});
        lm.put(new Phrase(new int[]{7, 13}), new double[]{-3.088698});
        lm.put(new Phrase(new int[]{7, 12}), new double[]{-2.247777});
        lm.put(new Phrase(new int[]{7, 9}), new double[]{-2.734638});
        lm.put(new Phrase(new int[]{7, 6}), new double[]{-4.273452});
        lm.put(new Phrase(new int[]{7, 16}), new double[]{-3.816251});
        lm.put(new Phrase(new int[]{7, 8}), new double[]{-1.335013});
        lm.put(new Phrase(new int[]{7, 7}), new double[]{-3.922493});
        lm.put(new Phrase(new int[]{7, 11}), new double[]{-2.750546});
        lm.put(new Phrase(new int[]{7, 14}), new double[]{-1.818335});
        lm.put(new Phrase(new int[]{11, 13}), new double[]{-1.121721});
        lm.put(new Phrase(new int[]{11, 12}), new double[]{-4.015676});
        lm.put(new Phrase(new int[]{11, 9}), new double[]{-4.954843});
        lm.put(new Phrase(new int[]{11, 6}), new double[]{-4.461711});
        lm.put(new Phrase(new int[]{11, 8}), new double[]{-3.768481});
        lm.put(new Phrase(new int[]{11, 7}), new double[]{-3.550728});
        lm.put(new Phrase(new int[]{11, 14}), new double[]{-1.549705});
        lm.put(new Phrase(new int[]{14, 13}), new double[]{-4.531987});
        lm.put(new Phrase(new int[]{14, 12}), new double[]{-5.381771});
        lm.put(new Phrase(new int[]{14, 16}), new double[]{-4.287304});
        lm.put(new Phrase(new int[]{14, 15}), new double[]{-4.287304});
        lm.put(new Phrase(new int[]{14, 8}), new double[]{-3.732888});
        lm.put(new Phrase(new int[]{14, 7}), new double[]{-2.673905});
        lm.put(new Phrase(new int[]{14, 11}), new double[]{-3.631014});
        lm.put(new Phrase(new int[]{14, 14}), new double[]{-4.531987});
        lm.put(new Phrase(new int[]{10, 13}), new double[]{-2.92767});
        lm.put(new Phrase(new int[]{10, 7}), new double[]{-2.198509});
        lm.put(new Phrase(new int[]{10, 14}), new double[]{-0.9798192});
    }

    public static void main(String[] args) throws Exception {
        System.in.read();
        for (int i = 0; i < 10000; i++) {
            int n = r.nextInt(4) + 2;
            int[] src = new int[n];
            for(int j = 0; j < n ; j++) {
                src[j] = r.nextInt(6);
            }
            Object2ObjectMap<Phrase, Collection<PhraseTranslation>> phraseTable = pt;
            Object2ObjectMap<Phrase, double[]> languageModel = lm;
            class IntegerLanguageModelImpl implements IntegerLanguageModel {

                public double[] get(Phrase phrase) {
                    return lm.get(phrase);
                }

                public int order() {
                    return 2;
                }

                public Int2ObjectMap<String> invWordMap() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                public Object2IntMap<String> wordMap() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
                
                
            }
            int lmN = 2;
            double[] weights = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
            int distiortionLimit = 3;
            int nBest = 1;
            FidelDecoder.decode(src, phraseTable, new IntegerLanguageModelImpl(), lmN, weights, distiortionLimit, nBest,1000,false);
            if(i % 100 == 0) {
                System.err.print(".");
            }
        }
    }
}
