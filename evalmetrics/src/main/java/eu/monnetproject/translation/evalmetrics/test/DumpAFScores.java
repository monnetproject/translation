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
package eu.monnetproject.translation.evalmetrics.test;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.framework.services.Services;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.LanguageModel;
import eu.monnetproject.translation.LanguageModelFactory;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.eval.TranslationEvaluator;
import eu.monnetproject.translation.eval.TranslationEvaluatorFactory;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;

/**
 *
 * @author John McCrae
 */
public class DumpAFScores {

    private static void writeLetterPrecisions(File dataFile, String outFile) throws IOException {
        final int N = 40;
        final int M = 6;
        final BufferedReader in = new BufferedReader(new FileReader(dataFile));
        final PrintWriter out = new PrintWriter(outFile);
        out.print("CLESA,");
        for (int i = 1; i <= M; i++) {
            out.print("WP" + i + ",WR" + i + ",");
        }
        for (int i = 1; i <= N; i++) {
            out.print("LP" + i + ",LR" + i + ",");
        }
        final TranslationEvaluatorFactory evaluatorFactory = Services.getFactory(TranslationEvaluatorFactory.class);
        //Configurator.setConfig("eu.monnetproject.translation.phrasal.lm", "en", "/home/jmccrae/projects/en-es/model.en.lm");
        //final LanguageModelFactory languageModelFactory = Services.getFactory(LanguageModelFactory.class);
        //final LanguageModel model = languageModelFactory.getModel(Language.ENGLISH);

        out.println("FLUENCY,ADEQUACY");
        String s;
        while ((s = in.readLine()) != null) {
            final String[] ss = s.split(" \\|\\|\\| ");
            assert (ss.length == 4);
            String translation = ss[0];
            String reference = ss[1];
            final double manualScore1 = Double.parseDouble(ss[2]);
            final double manualScore2 = Double.parseDouble(ss[3]);

            final Language trgLang = Language.ENGLISH;

              final TranslationEvaluator evaluator = evaluatorFactory.getEvaluator("CLESA", Collections.singletonList(Collections.singletonList((Translation) new MetricCorrelation.TranslationImpl(reference, trgLang))));
            final double autoScore = evaluator.score(Collections.singletonList((Translation) new MetricCorrelation.TranslationImpl(translation, trgLang)));
            //final double autoScore = Math.abs(model.score(Arrays.asList(translation.split("\\s+"))) - model.score(Arrays.asList(reference.split("\\s+"))));

            if (Double.isInfinite(autoScore) || Double.isNaN(autoScore)) {
                out.print("0,");
            } else {
                out.print(autoScore + ",");
            }

            for (int m = 1; m <= M; m++) {
                final double[] wordPR = DumpScores.nGramPrecisionRecall(translation, reference, m);
                out.print(wordPR[0] + "," + wordPR[1] + ",");
            }
            for (int n = 1; n <= N; n++) {
                final double[] letterPR = DumpScores.nLetterPrecisionRecall(translation, reference, n);
                out.print(letterPR[0] + "," + letterPR[1] + ",");
            }
            out.print(manualScore1 + ",");
            out.println(manualScore2);
        }
        out.close();
    }

    public static void main(String[] args) throws Exception {
        writeLetterPrecisions(new File(args[0]), args[1]);
    }
}
