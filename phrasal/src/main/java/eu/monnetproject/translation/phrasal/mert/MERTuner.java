///**********************************************************************************
// * Copyright (c) 2011, Monnet Project
// * All rights reserved.
// * 
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *     * Redistributions of source code must retain the above copyright
// *       notice, this list of conditions and the following disclaimer.
// *     * Redistributions in binary form must reproduce the above copyright
// *       notice, this list of conditions and the following disclaimer in the
// *       documentation and/or other materials provided with the distribution.
// *     * Neither the name of the Monnet Project nor the
// *       names of its contributors may be used to endorse or promote products
// *       derived from this software without specific prior written permission.
// * 
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// * DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
// * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// *********************************************************************************/
package eu.monnetproject.translation.phrasal.mert;

import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.IStrings;
import edu.stanford.nlp.mt.base.NBestListContainer;
import edu.stanford.nlp.mt.base.ScoredFeaturizedTranslation;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.base.SimpleSequence;
import edu.stanford.nlp.mt.metrics.EvaluationMetric;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import eu.monnetproject.lang.Language;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.translation.*;
import eu.monnetproject.translation.tune.Tuner;
import eu.monnetproject.translation.corpus.ParallelCorpus;
import eu.monnetproject.translation.corpus.ParallelDocument;
import eu.monnetproject.translation.corpus.SentencePair;
import eu.monnetproject.translation.eval.TranslationEvaluator;
import eu.monnetproject.translation.eval.TranslationEvaluatorFactory;
import eu.monnetproject.translation.phrasal.DecoderWeightsImpl;
import eu.monnetproject.translation.phrasal.FairlyGoodTokenizer;
import eu.monnetproject.translation.phrasal.StringLabel;
import eu.monnetproject.translation.phrasal.TranslationImpl;
import eu.monnetproject.translation.phrasal.corpus.TwoFileParallelCorpus;
import eu.monnetproject.translation.phrasal.pt.PhraseTableImpl;
import eu.monnetproject.translation.tune.EntityLabel;
import eu.monnetproject.translation.tune.EntityLabelList;
import eu.monnetproject.translation.tune.TranslatorSetup;
import eu.monnetproject.translation.monitor.Messages;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author John McCrae
 */
public class MERTuner implements Tuner {

    private final int nBest;

    public MERTuner() {
        this(100);
    }

    public MERTuner(int nBest) {
        this.nBest = nBest;
    }

    @Override
    public DecoderWeights tune(TranslatorSetup setup, ParallelCorpus corpus,
            TranslationEvaluatorFactory evaluatorFactory, String evaluatorName, int n, int options) {
        final TranslationEvaluator evaluator = evaluatorFactory.getEvaluator(evaluatorName, null);
        final List<List<Sequence<IString>>> references = getReferences(toIterable(corpus, false));
        EvaluationMetric<IString, String> metric = wrapEvaluator(evaluator, references);
        final Counter<String> initWts = convert(setup.weights());
        NBestListContainerImpl localNBestList = doDecoding(setup, setup.weights(), toELL(corpus));
        //log.info(""+metric.score(Collections.singletonList(localNBestList.nbestLists().get(0).get(0))));
        NBestListContainerImpl nBestList = copyNBestList(localNBestList);
        final LinkedList<Counter<String>> previousWeights = new LinkedList<Counter<String>>();
        previousWeights.add(initWts);
        DecoderWeights wts = setup.weights();
        for (int i = 0; i < n; i++) {
            final MERTImpl mert = new MERTImpl(metric, previousWeights, nBestList, localNBestList);
            final Counter<String> bestWeights = mert.getBestWeights();
            localNBestList = doDecoding(setup, convert(bestWeights), toELL(corpus));
            if (!append(nBestList, localNBestList)) {
                break;
            }
            previousWeights.add(bestWeights);
            wts = convert(bestWeights);
        }
        return wts;
    }

    @Override
    public DecoderWeights tune(TranslatorSetup setup, EntityLabelList corpus,
            TranslationEvaluatorFactory evaluatorFactory, String evaluatorName, int n, int options) {
        final TranslationEvaluator evaluator = evaluatorFactory.getEvaluator(evaluatorName, null);
        final List<List<Sequence<IString>>> references = getReferences(toIterable(corpus, false));
        EvaluationMetric<IString, String> metric = wrapEvaluator(evaluator, references);
        final Counter<String> initWts = convert(setup.weights());
        NBestListContainerImpl localNBestList = doDecoding(setup, setup.weights(), corpus);
        //log.info(""+metric.score(Collections.singletonList(localNBestList.nbestLists().get(0).get(0))));
        NBestListContainerImpl nBestList = copyNBestList(localNBestList);
        final LinkedList<Counter<String>> previousWeights = new LinkedList<Counter<String>>();
        previousWeights.add(initWts);
        DecoderWeights wts = setup.weights();
        for (int i = 0; i < n; i++) {
            final MERTImpl mert = new MERTImpl(metric, previousWeights, nBestList, localNBestList);
            final Counter<String> bestWeights = mert.getBestWeights();
            Messages.info("Next best: " + bestWeights);
            localNBestList = doDecoding(setup, convert(bestWeights), corpus);
            wts = convert(bestWeights);
            if (!append(nBestList, localNBestList)) {
                break;
            }
            previousWeights.add(bestWeights);
        }
        Messages.info("Final best: " + wts);
        return wts;
    }

    private Iterable<String> toIterable(final EntityLabelList list, final boolean src) {
        return new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                final Iterator<EntityLabel> iterator = list.iterator();
                return new Iterator<String>() {

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public String next() {
                        return src ? iterator.next().srcLabel : iterator.next().trgLabel;
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                    }
                };
            }
        };
    }

    private Iterable<String> toIterable(ParallelCorpus corpus, boolean src) {
        Messages.warning("Fix me I am very slow!");
        final LinkedList<String> iter = new LinkedList<String>();
        for (ParallelDocument doc : corpus) {
            for (SentencePair sp : doc) {
                if (src) {
                    iter.add(sp.getSourceSentence());
                } else {
                    iter.add(sp.getTargetSentence());
                }
            }
        }
        return iter;
    }

    private static class ELL extends LinkedList<EntityLabel> implements EntityLabelList { }
    
    private EntityLabelList toELL(ParallelCorpus corpus) {
        final ELL ell = new ELL();
        for (ParallelDocument doc : corpus) {
            for (SentencePair sp : doc) {
                ell.add(new EntityLabel(null, sp.getSourceSentence(), sp.getTargetSentence()));
            }
        }
        return ell;
    }
    
    private List<List<Sequence<IString>>> getReferences(Iterable<String> corpus) {
        final List<List<Sequence<IString>>> rv = new ArrayList<List<Sequence<IString>>>();
        for (String sp : corpus) {
            rv.add(Collections.singletonList((Sequence<IString>) new SimpleSequence<IString>(IStrings.toIStringArray(FairlyGoodTokenizer.split(sp)))));
        }
        return rv;
    }

    private EvaluationMetric<IString, String> wrapEvaluator(TranslationEvaluator evaluator, List<List<Sequence<IString>>> references) {
//        if (evaluator instanceof MetricWrapper) {
//            return ((MetricWrapper) evaluator).getMetric(references);
 //       } else {
            throw new UnsupportedOperationException("Not yet implemented: Use a Phrasal metric");
   //     }
    }

    private NBestListContainerImpl doDecoding(TranslatorSetup setup, DecoderWeights weights, EntityLabelList corpus) {
        final NBestListContainerImpl localNBest = new NBestListContainerImpl(new ArrayList<List<ScoredFeaturizedTranslation<IString, String>>>());
        final Decoder decoder = setup.decoder(weights);
        int i = 0;
        for (EntityLabel sp : corpus) {
            final ChunkList chunkList = setup.chunker(sp.entity).chunk(new StringLabel(sp.srcLabel, setup.sourceLanguage()));
            final PhraseTableImpl pt = new PhraseTableImpl(setup.sourceLanguage(), setup.targetLanguage(), "mert_table");
            for (Chunk chunk : chunkList) {
                for(TranslationSource source : setup.sources()) {
                    pt.addAll(source.candidates(chunk));
                }
            }

            // Step 3: Re-evaluating features semantically
            PhraseTable rerankedTable = pt;
            for (TranslationFeaturizer featurizer : setup.featurizers(sp.entity)) {
                try {
                    rerankedTable = featurizer.featurize(rerankedTable, sp.entity);
                } catch (Exception x) {
                }
            }

            final List<Translation> result = decoder.decode(Arrays.asList(FairlyGoodTokenizer.split(sp.srcLabel)), pt,setup.featureNames(), nBest);
            final ArrayList<ScoredFeaturizedTranslation<IString, String>> res2 = new ArrayList<ScoredFeaturizedTranslation<IString, String>>();
            for (Translation res : result) {
                if (res instanceof TranslationImpl) {
                    res2.add(((TranslationImpl) res).getTranslation());
                } else {
                    throw new UnsupportedOperationException("Translation result not Phrasal, wrapper needs to be implemented");
                }
            }
            localNBest.add(res2);
            i++;
        }
        return localNBest;
    }

    private NBestListContainerImpl copyNBestList(NBestListContainerImpl localNBestList) {
        return localNBestList.clone();
    }

    private boolean append(NBestListContainerImpl nBestList, NBestListContainer<IString, String> localNBestList) {
        return nBestList.addAll(localNBestList.nbestLists());
    }

    private Counter<String> convert(DecoderWeights wts) {
        final Counter<String> rv = new ClassicCounter<String>();
        for (Entry<String, Double> wt : wts.entrySet()) {
            rv.setCount(wt.getKey(), wt.getValue());
        }
        return rv;
    }

    private DecoderWeights convert(Counter<String> wts) {
        final DecoderWeightsImpl rv = new DecoderWeightsImpl();
        for (String k : wts.keySet()) {
            rv.put(k, wts.getCount(k));
        }
        return rv;
    }

//    public static void main(String[] args) throws Exception {
//        if (args.length != 5) {
//            System.err.println("Usage:\n\tMERTuner srcLangFile trgLangFile srcLang trgLang n");
//            System.exit(-1);
//        }
//        final TwoFileParallelCorpus corpus = new TwoFileParallelCorpus(new File(args[0]), new File(args[1]));
//        final MERTuner tuner = new MERTuner();
//        final DecoderWeights wts = tuner.tune(new DefaultTranslatorSetup(Language.get(args[2]), Language.get(args[3])), corpus, new MetricWrapperFactory(), "BLEU", Integer.parseInt(args[4]));
//        System.err.println("Final Weights: " + wts);
//        final PrintWriter out = new PrintWriter("eu.monnetproject.translation.phrasal.cfg");
//        for (Entry<String, Double> entry : wts.entrySet()) {
//            out.println(entry.getKey() + "=" + entry.getValue());
//        }
//        out.close();
//        System.err.println("Wrote weights to eu.monnetproject.translation.phrasal.cfg");
//    }
}
