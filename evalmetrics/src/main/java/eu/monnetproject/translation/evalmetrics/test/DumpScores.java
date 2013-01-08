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
package eu.monnetproject.translation.evalmetrics.test;

import eu.monnetproject.framework.services.Services;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.eval.TranslationEvaluator;
import eu.monnetproject.translation.eval.TranslationEvaluatorFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class DumpScores {

    private static void writeLetterPrecisions(File dataFile) throws IOException {
        final int N = 40;
        final int M = 6;
        final BufferedReader in = new BufferedReader(new FileReader(dataFile));
        final PrintWriter out = new PrintWriter(dumpMode == DumpMode.CSV ? "letterprecisions.csv" : "letterprecisions.dat");
        for (int i = 1; i <= M; i++) {
            out.print("WP" + i + ",WR" + i + ",");
        }
        for (int i = 1; i <= N; i++) {
            out.print("LP" + i + ",LR" + i + ",");
        }
        out.println("RANK");
        String s = null;
        READING:
        do {
            double[] manScrs = new double[5];
            for (int i = 0; i < 5; i++) {
                s = in.readLine();
                if (s == null) {
                    break READING;
                }
                if (s.matches("\\s*")) {
                    System.out.println("blank line!");
                    continue;
                }
                final String[] ss = s.split(" \\|\\|\\| ");
                assert (ss.length == 3);
                String translation = ss[0];
                String reference = ss[1];
                final double manualScore = Double.parseDouble(ss[2]);
                if (manualScore < 0) {
                    continue;
                }
                manScrs[i] = manualScore;

                
                for (int m = 1; m <= M; m++) {
                    final double[] wordPR = nGramPrecisionRecall(translation, reference, m);
                    out.print(wordPR[0] + "," + wordPR[1] + ",");
                }
                for (int n = 1; n <= N; n++) {
                    final double[] letterPR = nLetterPrecisionRecall(translation, reference, n);
                    out.print(letterPR[0] + "," + letterPR[1] + ",");
                }
                out.println(manualScore);
            }
        } while (s != null);
        out.close();
    }

     enum DumpMode {

        CSV,
        SVMRANK
    }
    public static final DumpMode dumpMode = DumpMode.valueOf(System.getProperty("dumpMode", "CSV"));

    private static void readScores(File dataFile, TranslationEvaluatorFactory evaluatorFactory, String evaluatorName, Language trgLang) throws IOException {
        final BufferedReader in = new BufferedReader(new FileReader(dataFile));
        final PrintWriter out = new PrintWriter(dumpMode == DumpMode.CSV ? "scores.csv" : "scores.dat");
        out.println("ESA,P1,R1,P2,R2,P3,R3,RANK");
        String s = null;
        final List<Double> autoScores = new ArrayList<Double>(), manualScores = new ArrayList<Double>();
        int linesRead = 0;
        int qid = 1;
        READING:
        do {
            double[] manScrs = new double[5];
            for (int i = 0; i < 5; i++) {
                s = in.readLine();
                if (s == null) {
                    break READING;
                }
                linesRead++;
                if (s.matches("\\s*")) {
                    System.out.println("blank line!");
                    continue;
                }
                final String[] ss = s.split(" \\|\\|\\| ");
                assert (ss.length == 3);
                String translation = ss[0];
                String reference = ss[1];
                final double manualScore = Double.parseDouble(ss[2]);
                if (manualScore < 0) {
                    continue;
                }
                manScrs[i] = manualScore;
                if (dumpMode == DumpMode.SVMRANK) {
                    out.print(((int) manScrs[i]) + " qid:" + qid + " ");
                }
                final TranslationEvaluator evaluator = evaluatorFactory.getEvaluator(evaluatorName, Collections.singletonList(Collections.singletonList((Translation) new MetricCorrelation.TranslationImpl(reference, trgLang))));
                final double autoScore = evaluator.score(Collections.singletonList((Translation) new MetricCorrelation.TranslationImpl(translation, trgLang)));
                if (dumpMode == DumpMode.SVMRANK) {
                    out.print("1:" + autoScore);
                } else {
                    out.print(autoScore + ",");
                }
                final double[] oneGrams = nGramPrecisionRecall(translation, reference, 1);
                final double[] twoGrams = nGramPrecisionRecall(translation, reference, 2);
                final double[] threeGrams = nGramPrecisionRecall(translation, reference, 3);
                if (dumpMode == DumpMode.CSV) {
                    out.print(oneGrams[0] + "," + oneGrams[1] + ",");
                    out.print(twoGrams[0] + "," + twoGrams[1] + ",");
                    out.print(threeGrams[0] + "," + threeGrams[1] + ",");
                    out.println(manualScore);
                } else {
                    out.print(" 2:" + oneGrams[0] + " 3:" + oneGrams[1]);
                    out.print(" 4:" + twoGrams[0] + " 5:" + twoGrams[1]);
                    out.println(" 6:" + threeGrams[0] + " 7:" + threeGrams[1]);
                }
            }
            qid++;
        } while (s != null);
        out.close();
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 3) {
            readScores(new File(args[0]), Services.getFactory(TranslationEvaluatorFactory.class), args[1], Language.get(args[2]));
        } else if (args.length == 1) {
            writeLetterPrecisions(new File(args[0]));
        } else {
            throw new RuntimeException("Require one argument: dataset");
        }
    }

    public static double[] nGramPrecisionRecall(String translation, String reference, int n) {
        final String[] transTks = translation.split("\\s+");
        final String[] refTks = reference.split("\\s+");

        if (refTks.length < n || transTks.length < n) {
            return new double[]{0.0, 0.0};
        }

        int nGramMatches = 0;

        for (int i = 0; i < transTks.length - n + 1; i++) {
            for (int j = 0; j < refTks.length - n + 1; j++) {
                if (transTks[i].equals(refTks[j])) {
                    NGRAM_MATCH:
                    {
                        for (int k = 1; k < n; k++) {
                            if (!transTks[i + k].equals(refTks[j + k])) {
                                break NGRAM_MATCH;
                            }
                        }
                        nGramMatches++;
                    }
                }
            }
        }
        return new double[]{(double) nGramMatches / (transTks.length - n + 1), (double) nGramMatches / (refTks.length - n + 1)};
    }

    public static double[] nLetterPrecisionRecall(String translation, String reference, int n) {

        if (reference.length() < n || translation.length() < n) {
            return new double[]{0.0, 0.0};
        }

        int nGramMatches = 0;

        for (int i = 0; i < translation.length() - n + 1; i++) {
            for (int j = 0; j < reference.length() - n + 1; j++) {
                if (translation.charAt(i) == reference.charAt(j)) {
                    NGRAM_MATCH:
                    {
                        for (int k = 1; k < n; k++) {
                            if (translation.charAt(i + k) != reference.charAt(j + k)) {
                                break NGRAM_MATCH;
                            }
                        }
                        nGramMatches++;
                    }
                }
            }
        }
        return new double[]{(double) nGramMatches / (translation.length() - n + 1), (double) nGramMatches / (reference.length() - n + 1)};
    }
}
