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
package eu.monnetproject.translation.phrasal.jmert;

import eu.monnetproject.lang.Language;
import eu.monnetproject.lang.Script;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.translation.*;
import eu.monnetproject.translation.corpus.ParallelCorpus;
import eu.monnetproject.translation.corpus.ParallelDocument;
import eu.monnetproject.translation.corpus.SentencePair;
import eu.monnetproject.translation.eval.TranslationEvaluator;
import eu.monnetproject.translation.eval.TranslationEvaluatorFactory;
import eu.monnetproject.translation.tune.EntityLabel;
import eu.monnetproject.translation.tune.EntityLabelList;
import eu.monnetproject.translation.tune.TranslatorSetup;
import eu.monnetproject.translation.tune.Tuner;
import eu.monnetproject.translation.monitor.Messages;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;

/**
 *
 * @author John McCrae
 */
public class JMert implements Tuner {

    public enum Method {
        DUMMY,
        MERT,
        OLS,
        SVM
    };
    private final int nBest = 100;
    private final Optimizer optimizer;
    private final Method method = Method.valueOf(System.getProperty("jmert.method","SVM"));
    private final Tokenizer tokenizer;
    
    
    public JMert(TokenizerFactory tokenizerFactory) {
        //this.optimizer = optimizer == null ? new OLSOptimizer() : optimizer;
        //this.optimizer = new OLSOptimizer();
        this.tokenizer = tokenizerFactory.getTokenizer(Script.LATIN);
        switch(method) {
            case DUMMY: 
                this.optimizer = new DummyOptimizer();
                break;
            case MERT:
                this.optimizer = new JMertOptimizer();
                break;
            case OLS:
                this.optimizer = new OLSOptimizer();
                break;
            case SVM:
                this.optimizer = new SVMRankOptimizer();
                break;
            default:
                throw new RuntimeException();
        }
    }

    private Feature[] features2array(Collection<String> featureNames, Collection<Feature> features) {
        final Feature[] feats = new Feature[featureNames.size()];
        int k = 0;
        for (String featureName : featureNames) {
            for (Feature feature : features) {
                if (feature.name.equals(featureName)) {
                    if (feats[k] == null) {
                        feats[k] = feature;
                    } else {
                        feats[k] = new Feature(feats[k].name, feats[k].score + feature.score);
                    }
                }
            }
            if (feats[k] == null) {
                feats[k] = new Feature(featureName, 0.0);
            }
            k++;
        }
        return feats;
    }

    private void writeTranslations(String file, List<Collection<JMertTranslation>> jMertTranslations) {
        try {
            final PrintWriter out = new PrintWriter(file);
            final JMertTranslation trans = jMertTranslations.get(0).iterator().next();
            for (int k = 0; k < trans.features.length; k++) {
                out.print(trans.features[k].name + ",");
            }
            out.println("score,idx");
            for (int i = 0; i < jMertTranslations.size(); i++) {
                for (JMertTranslation jmt : jMertTranslations.get(i)) {
                    out.println(jmt.toCSV() + "," + i);
                }
            }
            out.close();
        } catch (Exception x) {
            x.printStackTrace();
        }

    }

    private static class ELL extends LinkedList<EntityLabel> implements EntityLabelList {
    }

    private ELL toELL(ParallelCorpus corpus) {
        final ELL ell = new ELL();
        for (ParallelDocument doc : corpus) {
            for (SentencePair sp : doc) {
                ell.add(new EntityLabel(null, sp.getSourceSentence(), sp.getTargetSentence()));
            }
        }
        return ell;
    }

    @Override
    public DecoderWeights tune(TranslatorSetup setup, ParallelCorpus corpus, TranslationEvaluatorFactory evaluatorFactory, String evaluatorName, int n, int options) {
        final ELL ell = toELL(corpus);
        return tune(setup, ell, evaluatorFactory, evaluatorName, n, options);

    }

    private boolean isInteresting(Collection<JMertTranslation> transes) {
        for (JMertTranslation trans : transes) {
            if (trans.score != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DecoderWeights tune(TranslatorSetup setup, EntityLabelList ell, TranslationEvaluatorFactory evaluatorFactory, String evaluatorName, int n, int options) {
        final TranslationEvaluator evaluator = evaluatorFactory.getEvaluator(evaluatorName, getReferences(ell));
        final DecoderWeights weights = new DecoderWeightsImpl(setup.weights());
        final Set<String> featureNames = weights.keySet();
        final List<Collection<JMertTranslation>> jMertTranslations = new ArrayList<Collection<JMertTranslation>>();

        for (int i = 0; i < n; i++) {
            Set<String> used = new HashSet<String>();
            final Decoder decoder = setup.decoder(weights);
            boolean changed = false;
            int j = 0;
            for (EntityLabel el : ell) {
                final List<Translation> translations = doDecoding(setup, decoder, el, options);
                if (j >= jMertTranslations.size()) {
                    jMertTranslations.add(new HashSet<JMertTranslation>());
                }
                final Collection<JMertTranslation> jMertTranses = jMertTranslations.get(j);
                for (Translation translation : translations) {
                    if(translation.getFeatures().isEmpty()) {
                        Messages.warning("Empty features for translation");
                    }
                    for (Feature f : translation.getFeatures()) {
                        if (!featureNames.contains(f.name)) {
                            Messages.severe("No initial value for feature " + f.name);
                        }
                        used.add(f.name);
                    }
                    changed = jMertTranses.add(new JMertTranslation(features2array(featureNames, translation.getFeatures()), evaluator.score(Collections.singletonList(translation)))) || changed;
                }
                j++;
            }
            Messages.info("Finished decoding");
            final HashSet<String> unused = new HashSet<String>(featureNames);
            unused.removeAll(used);
            for (String fName : unused) {
                Messages.severe("Unused feature " + fName);
            }
            if (changed) {

                if (featureNames == null || featureNames.isEmpty()) {
                    Messages.warning("No features to optimize");
                    return weights;
                }

                Feature[] featureArray = new Feature[featureNames.size()];
                int k = 0;
                for (String featureName : featureNames) {
                    if (!weights.containsKey(featureName)) {
                        throw new IllegalArgumentException("Feature " + featureName + " does not have a default weight!");
                    }
                    featureArray[k++] = new Feature(featureName, weights.get(featureName));
                }
                final LinkedList<Collection<JMertTranslation>> interestingTranslation = new LinkedList<Collection<JMertTranslation>>(jMertTranslations);
                final Iterator<Collection<JMertTranslation>> interestingIterator = interestingTranslation.iterator();
                while (interestingIterator.hasNext()) {
                    if (!isInteresting(interestingIterator.next())) {
                        interestingIterator.remove();
                    }
                }
                //writeTranslations("iter" + i + ".csv", interestingTranslation);
                final double[] newWts = optimizer.optimizeFeatures(interestingTranslation, featureArray, 100, unused);
                k = 0;
                for (String featureName : featureNames) {
                    weights.put(featureName, newWts[k++]);
                }
                Messages.info("<<< IMPROVED: " + weights + " >>>");

            } else {
                Messages.info("Not changed");
                break;
            }
        }
        return weights;
    }

    private List<Translation> doDecoding(TranslatorSetup setup, Decoder decoder, EntityLabel el, int options) {
        final ChunkList chunkList = setup.chunker(el.entity).chunk(tokenizer.tokenize(el.srcLabel));
        final PhraseTableImpl pt = new PhraseTableImpl(setup.sourceLanguage(), setup.targetLanguage(), "mert_table");
        for (Chunk chunk : chunkList) {
            for (TranslationSource source : setup.sources()) {
                pt.addAll(source.candidates(chunk));
            }
        }

        PhraseTable rerankedTable = pt;
        for (TranslationFeaturizer featurizer : setup.featurizers(el.entity)) {
            try {
                rerankedTable = featurizer.featurize(rerankedTable, el.entity);
            } catch (Exception x) {
            }
        }
        final List<Translation> decoded = (options & OntologyTranslator.DECODE_FAST) == 0 ?
                decoder.decode(Arrays.asList(el.srcLabel.split("\\s+")), rerankedTable, setup.featureNames(), nBest) :
                decoder.decodeFast(Arrays.asList(el.srcLabel.split("\\s+")), rerankedTable, setup.featureNames(), nBest);
        return decoded;
    }

    private List<List<Translation>> getReferences(EntityLabelList corpus) {
        final List<List<Translation>> rv = new ArrayList<List<Translation>>();
        for (EntityLabel el : corpus) {
            rv.add(Collections.singletonList((Translation) new TranslationImpl(el)));
        }
        return rv;
    }

    private static class TranslationImpl implements Translation {

        private final String trgLabel;
        private final String srcLabel;
        private final Entity entity;

        public TranslationImpl(EntityLabel el) {
            this.srcLabel = el.srcLabel;
            this.trgLabel = el.trgLabel;
            this.entity = el.entity;
        }

        @Override
        public Label getSourceLabel() {
            return new Label() {

                @Override
                public String asString() {
                    return srcLabel;
                }

                @Override
                public Language getLanguage() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            };
        }

        @Override
        public Label getTargetLabel() {
            return new Label() {

                @Override
                public String asString() {
                    return trgLabel;
                }

                @Override
                public Language getLanguage() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            };
        }

        @Override
        public URI getEntity() {
            return entity.getURI();
        }

        @Override
        public double getScore() {
            return Double.NaN;
        }

        @Override
        public Collection<Feature> getFeatures() {
            return Collections.EMPTY_LIST;
        }
    }
}
