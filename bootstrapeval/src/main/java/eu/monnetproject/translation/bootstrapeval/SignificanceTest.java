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
package eu.monnetproject.translation.bootstrapeval;

import eu.monnetproject.framework.services.Services;
import eu.monnetproject.label.LabelExtractor;
import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.lang.Language;
import eu.monnetproject.lemon.LemonModel;
import eu.monnetproject.lemon.LemonSerializer;
import eu.monnetproject.lemon.model.LexicalEntry;
import eu.monnetproject.lemon.model.LexicalSense;
import eu.monnetproject.lemon.model.Lexicon;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.ontology.OntologySerializer;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.eval.TranslationEvaluator;
import eu.monnetproject.translation.eval.TranslationEvaluatorFactory;
import java.io.FileReader;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 *
 * @author John McCrae
 */
public class SignificanceTest {

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("Usage:\n\tSignificanceTest refOnto lex1 lex2 srcLang trgLang");
            return;
        }
        final OntologySerializer ontoSerializer = Services.get(OntologySerializer.class);
        final LabelExtractor extractor = Services.get(LabelExtractorFactory.class).getExtractor(Collections.EMPTY_LIST, false, false);
        final LemonSerializer lemonSerializer = Services.get(LemonSerializer.class);
        final TranslationEvaluatorFactory tef = Services.get(TranslationEvaluatorFactory.class);

        final Ontology ontology = ontoSerializer.read(new FileReader(args[0]));
        final LemonModel model1 = lemonSerializer.read(new FileReader(args[1]));
        final LemonModel model2 = lemonSerializer.read(new FileReader(args[2]));
        final Language srcLang = Language.get(args[3]);
        final Language trgLang = Language.get(args[4]);

        final HashMap<URI, Collection<String>> refs = readRefs(ontology, extractor, trgLang);
        final HashMap<URI, Collection<String>> trans1 = readTrans(model1, trgLang);
        final HashMap<URI, Collection<String>> trans2 = readTrans(model2, trgLang);

        retainAll(refs, trans1, trans2);
        retainAll(trans1, refs, trans2);
        retainAll(trans2, refs, trans1);

        final int N = refs.size();
        if (N < 2) {
            System.err.println("Too few translations");
            return;
        }
        final int m = Math.min(100, N / 2);
        final int iters = 50;

        final URI[] entities = refs.keySet().toArray(new URI[N]);

        int[] system1better = null, system2better = null;
        String[] evaluatorNames = null;

        for (int i = 0; i < iters; i++) {
            int[] sample = sampleWithReplacement(m, N);
            final List<List<Translation>> refTranses = buildRefs(refs, entities, sample);
            final Collection<TranslationEvaluator> evaluators = tef.getEvaluators(refTranses);
            if (system1better == null) {
                system1better = new int[evaluators.size()];
                system2better = new int[evaluators.size()];
                evaluatorNames = new String[evaluators.size()];
                int evalIdx = 0;
                for (TranslationEvaluator translationEvaluator : evaluators) {
                    evaluatorNames[evalIdx++] = translationEvaluator.getName();
                }
            }
            final List<Translation> transes1 = buildTrans(trans1, entities, sample);
            final List<Translation> transes2 = buildTrans(trans2, entities, sample);

            int evalIdx = 0;
            for (TranslationEvaluator evaluator : evaluators) {
                final double score1 = evaluator.score(transes1);
                final double score2 = evaluator.score(transes2);
                if (score1 > score2) {
                    system1better[evalIdx++]++;
                } else if (score2 > score1) {
                    system2better[evalIdx++]++;
                }
            }
        }

        printResult(system1better, system2better, evaluatorNames, iters, args[1], args[2]);
    }

    private static HashMap<URI, Collection<String>> readRefs(Ontology ontology, LabelExtractor extractor, Language l) {
        final HashMap<URI, Collection<String>> refs = new HashMap<URI, Collection<String>>();
        for (Entity e : ontology.getEntities()) {
            final Map<Language, Collection<String>> labels = extractor.getLabels(e);
            if (labels.containsKey(l)) {
                refs.put(e.getURI(), labels.get(l));
            }
        }
        return refs;
    }

    private static HashMap<URI, Collection<String>> readTrans(LemonModel model1, Language l) {
        final HashMap<URI, Collection<String>> trans = new HashMap<URI, Collection<String>>();
        for (Lexicon lexicon : model1.getLexica()) {
            if (lexicon.getLanguage().equals(l.toString())) {
                for (LexicalEntry entry : lexicon.getEntrys()) {
                    final String label = entry.getCanonicalForm().getWrittenRep().value;
                    for (LexicalSense sense : entry.getSenses()) {
                        final URI ref = sense.getReference();
                        if (!trans.containsKey(ref)) {
                            trans.put(ref, new LinkedList<String>());
                        }
                        trans.get(ref).add(label);
                    }
                }
            }
        }
        return trans;
    }

    private static void retainAll(HashMap<URI, Collection<String>> refs, HashMap<URI, Collection<String>> trans1, HashMap<URI, Collection<String>> trans2) {
        final Iterator<Entry<URI, Collection<String>>> iter = refs.entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<URI, Collection<String>> e = iter.next();
            if (!trans1.containsKey(e.getKey()) || !trans2.containsKey(e.getKey())) {
                iter.remove();
            }
        }
    }
    private final static Random r = new Random();

    private static int[] sampleWithReplacement(int m, int N) {
        int[] samples = new int[m];
        for (int i = 0; i < m; i++) {
            samples[i] = r.nextInt(N);
        }
        return samples;
    }

    private static List<List<Translation>> buildRefs(HashMap<URI, Collection<String>> refs, URI[] entities, int[] sample) {
        final ArrayList<List<Translation>> result = new ArrayList<List<Translation>>(entities.length);
        for (int i = 0; i < sample.length; i++) {
            final LinkedList<Translation> t = new LinkedList<Translation>();
            for (String l : refs.get(entities[sample[i]])) {
                t.add(new TranslationImpl(null, l, entities[sample[i]]));
            }
            result.add(t);
        }
        return result;
    }

    private static List<Translation> buildTrans(HashMap<URI, Collection<String>> trans1, URI[] entities, int[] sample) {
        final ArrayList<Translation> result = new ArrayList<Translation>();
        for (int i = 0; i < sample.length; i++) {
            final Collection<String> t = trans1.get(entities[sample[i]]);
            int j = r.nextInt(t.size());
            final Iterator<String> iter = t.iterator();
            while (j > 0) {
                iter.next();
                j--;
            }
            result.add(new TranslationImpl(null, iter.next(), entities[sample[i]]));
        }
        return result;
    }

    private static String prettyStr(String s, int n) {
        final StringBuilder uriStr = new StringBuilder(s);
        if (uriStr.length() < n) {
            for (int i = uriStr.length(); i < n; i++) {
                uriStr.append(" ");
            }
            return uriStr.toString();
        } else {
            uriStr.replace(10, uriStr.length() - (n - 13), "...");
            return uriStr.toString();
        }
    }
    
    private static void printResult(int[] system1better, int[] system2better, String[] evaluatorNames, int iters, String file1Name, String file2Name) {
        final DecimalFormat df = new DecimalFormat("0.0000");
        System.out.print("                               ");
        for (String name : evaluatorNames) {
            System.out.print(name);
            System.out.print("\t");
        }
        System.out.println();
        System.out.print(prettyStr(file1Name,30)+ " ");
        for(int i = 0; i < system1better.length; i++) {
            if(system1better[i] != iters) {
                System.out.print(df.format((double)system1better[i] / (double)iters) + "\t");
            } else {
                System.out.print(">"+df.format((double)(iters-1)/(double)iters) + "\t");
            }
        }
        System.out.println();
        System.out.print(prettyStr(file2Name,30)+ " ");
        for(int i = 0; i < system2better.length; i++) {
            if(system2better[i] != iters) {
                System.out.print(df.format((double)system2better[i] / (double)iters) + "\t");
            } else {
                System.out.print(">"+df.format((double)(iters-1)/(double)iters) + "\t");
            }
        }
        System.out.println();
    }
}
