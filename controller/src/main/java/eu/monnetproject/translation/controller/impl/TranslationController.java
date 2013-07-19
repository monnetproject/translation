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
package eu.monnetproject.translation.controller.impl;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.lang.Script;
import eu.monnetproject.lemon.model.LexicalEntry;
import eu.monnetproject.lemon.model.Lexicon;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.translation.Decoder;
import eu.monnetproject.translation.DecoderFactory;
import eu.monnetproject.translation.DecoderWeights;
import eu.monnetproject.translation.LanguageModel;
import eu.monnetproject.translation.LanguageModelFactory;
import eu.monnetproject.translation.OntologyTranslator;
import eu.monnetproject.translation.TranslationFeaturizer;
import eu.monnetproject.translation.TranslationFeaturizerFactory;
import eu.monnetproject.translation.TranslationPhraseChunker;
import eu.monnetproject.translation.TranslationPhraseChunkerFactory;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.TranslationSourceFactory;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import eu.monnetproject.lemon.model.LexicalSense;
import eu.monnetproject.ontology.AnnotationProperty;
import eu.monnetproject.ontology.DatatypeProperty;
import eu.monnetproject.ontology.Individual;
import eu.monnetproject.ontology.ObjectProperty;
import eu.monnetproject.translation.*;
import eu.monnetproject.translation.monitor.TranslationMonitor;
import eu.monnetproject.translation.monitor.Messages;
import eu.monnetproject.translation.monitor.Job;
import eu.monnetproject.translation.tune.TranslatorSetup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;

/**
 *
 * @author John McCrae
 */
public class TranslationController implements OntologyTranslator {

    private final Collection<LanguageModelFactory> languageModelFactories;
    private final HashMap<Language, LanguageModel> languageModels = new HashMap<Language, LanguageModel>();
    private final DecoderFactory decoderFactory;
    private final HashMap<LanguagePair, Decoder> decoders = new HashMap<LanguagePair, Decoder>();
    private final Iterable<TranslationPhraseChunkerFactory> chunkerFactories;
    private final Iterable<TranslationSourceFactory> sourceFactories;
    private final HashMap<LanguagePair, List<TranslationSource>> sources = new HashMap<LanguagePair, List<TranslationSource>>();
    private final Iterable<TranslationFeaturizerFactory> featurizerFactories;
    private final TokenizerFactory tokenizerFactory;
    private final LinkedList<TranslationMonitor> monitors = new LinkedList<TranslationMonitor>();
    private final Iterable<TranslationConfidenceFactory> confidenceFactory;
    private final HashMap<LanguagePair, DecoderWeights> weights = new HashMap<LanguagePair, DecoderWeights>();
    private boolean verbose = false;
    private final boolean untrainedPairs = Boolean.parseBoolean(System.getProperty("eu.monnetproject.translation.controller.untrainedPairs", "true"));

    public TranslationController(Collection<LanguageModelFactory> languageModelFactories, DecoderFactory decoderFactory, Iterable<TranslationPhraseChunkerFactory> chunkerFactories, Iterable<TranslationSourceFactory> sourceFactories, Iterable<TranslationFeaturizerFactory> featurizerFactories, TokenizerFactory tokenizerFactory, Iterable<TranslationConfidenceFactory> confidences) {
        this.languageModelFactories = languageModelFactories;
        this.decoderFactory = decoderFactory;
        this.chunkerFactories = chunkerFactories;
        this.sourceFactories = sourceFactories;
        this.featurizerFactories = featurizerFactories;
        this.tokenizerFactory = tokenizerFactory;
        this.confidenceFactory = confidences;
    }

    @Override
    public void addMonitor(TranslationMonitor monitor) {
        monitors.add(monitor);
    }
    private final int numThreads = Integer.parseInt(System.getProperty("eu.monnetproject.translation.controller.threads", "10"));

    @Override
    public void translate(Ontology ontology,
            Collection<Lexicon> sourceLexicons,
            Lexicon targetLexicon,
            Collection<URI> scope,
            String namePrefix,
            int nBest) {
        translate(ontology, sourceLexicons, targetLexicon, scope, namePrefix, nBest, 0);
    }

    @Override
    public void translate(Ontology ontology,
            Collection<Lexicon> sourceLexicons,
            Lexicon targetLexicon,
            Collection<URI> scope,
            String namePrefix,
            int nBest,
            int options) {
        Messages.info("Using " + numThreads + " threads");
        // Find suitable services
        final Language targetLanguage = Language.get(targetLexicon.getLanguage());
        TrueCaser trueCaser = null;
        for (LanguageModelFactory languageModelFactory : languageModelFactories) {
            trueCaser = languageModelFactory.getTrueCaser(targetLanguage.getLanguageOnly());
            if (trueCaser != null) {
                break;
            }
        }
        boolean confidenceEstimation = (options & OntologyTranslator.ESTIMATE_CONFIDENCE) != 0;
        boolean decodeFast = (options & OntologyTranslator.DECODE_FAST) != 0;

        Set<Language> availLangs = new HashSet<Language>();
        for (Lexicon sourceLexicon : sourceLexicons) {
            availLangs.add(Language.get(sourceLexicon.getLanguage()));
        }

        final TranslationConfidence confidence;
        if (confidenceEstimation && confidenceFactory.iterator().hasNext()) {
            confidence = confidenceFactory.iterator().next().getConfidence();
        } else {
            if (confidenceEstimation) {
                System.err.println("Confidence estimation failed no estimator available");
            }
            confidence = null;
        }


        for (Lexicon sourceLexicon : sourceLexicons) {
            final Job job = Messages.beginTranslation(sourceLexicon.getEntrys().size());
            //final Collection<Entity> entities = ontology.getEntities();
            final ThreadPoolExecutor threadPool = numThreads == 1 ? null : new ThreadPoolExecutor(numThreads - 1, numThreads - 1, 10, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
            if (threadPool != null) {
                threadPool.setRejectedExecutionHandler(new Wait()/*new ThreadPoolExecutor.CallerRunsPolicy()*/);
            }
            final Language sourceLanguage = Language.get(sourceLexicon.getLanguage());

            final Script[] knownScriptsForLanguage = Script.getKnownScriptsForLanguage(sourceLanguage);
            final Script sourceScript =
                    (knownScriptsForLanguage != null && knownScriptsForLanguage.length > 0)
                    ? knownScriptsForLanguage[0]
                    : Script.LATIN;
            final Tokenizer tokenizer = tokenizerFactory.getTokenizer(sourceScript);
            if (tokenizer == null) {
                Messages.warning("Skipping translations from " + sourceLanguage + " as no tokenizer available");
                continue;
            }
            // Train ontology + language specific resources
            final List<TranslationPhraseChunker> chunkers = getChunkers(chunkerFactories,
                    ontology, sourceLanguage);
            if (chunkers.isEmpty()) {
                Messages.warning("Skipping translations from " + sourceLanguage + " as no chunker available");
                continue;
            }
            final List<TranslationSource> translationSources = getSources(sourceFactories, sourceLanguage, targetLanguage);
            if (translationSources.isEmpty()) {
                Messages.warning("Skipping translation from " + sourceLanguage + " to " + targetLanguage + " as no sources available");
                continue;
            }
            final Set<Language> extraLangs = new HashSet<Language>(availLangs);
            extraLangs.remove(sourceLanguage);
            extraLangs.remove(targetLanguage);
            final List<TranslationFeaturizer> featurizers = getFeaturizers(ontology, sourceLanguage, targetLanguage, extraLangs);
            final List<String> features = prepFeatures(translationSources, Collections.singleton((Collection<TranslationFeaturizer>) featurizers));
            final LanguageModel languageModel = getLanguageModel(targetLanguage);
            final Decoder decoder = getDecoder(new LanguagePair(sourceLanguage, targetLanguage), languageModel);

            //log.info("Translating " + sourceLexicon.getEntrys().size() + " entries");
            for (LexicalEntry entry : sourceLexicon.getEntrys()) {
                Messages.translationStart(entry.getURI());
                if (entry.getSenses() == null) {
                    Messages.translationFail(entry.getURI(), "entry has no senses");
                }
                for (LexicalSense sense : entry.getSenses()) {
                    if (sense.getReference() == null) {
                        Messages.warning("Sense with null reference for " + entry.getURI());
                        continue;
                    }
                    Entity entity = getBestEntity(ontology.getEntities(sense.getReference()), scope);
                    if (entity == null) {
                        Messages.warning(sense.getReference()+ " not found in ontology, assuming class");
                        entity = ontology.getFactory().makeClass(sense.getReference());
                    }
                    final TranslationThread thread = new TranslationThread(entity, entry, nBest, sourceLanguage, targetLexicon, namePrefix, chunkers, translationSources, featurizers, decoder, tokenizer, verbose, monitors, confidence, decodeFast, trueCaser, features, job);

                    // Dispatch translations
                    if (threadPool != null) {
                        threadPool.execute(thread);
                    } else {
                        thread.run();
                    }
                }
            }
            Messages.info("Cleaning up");
            // Await the threads
            try {
                if (threadPool != null) {
                    Messages.info("Awaiting translation threads");
                    threadPool.shutdown();
                    threadPool.awaitTermination(5, TimeUnit.MINUTES);
                }
            } catch (InterruptedException x) {
                throw new RuntimeException("The thread pool was interrupted or timed out");
            }
            Messages.info("Closing sources");
            for (TranslationSource source : translationSources) {
                try {
                    source.close();
                } catch (Exception x) {
                    Messages.cleanupFailure(x);
                }
            }
            Messages.info("Closing language model");
            try {
                languageModel.close();
            } catch (Exception x) {
                Messages.cleanupFailure(x);
            }
            Messages.info("Closing featurizers");
            try {
                closeFeaturizers(featurizers);
            } catch (Exception x) {
                Messages.cleanupFailure(x);
            }
            final LanguagePair lp = new LanguagePair(sourceLanguage, targetLanguage);
            if (sources.get(lp) == translationSources) {
                sources.remove(lp);
            }
            Messages.info("Translation complete");
        }
    }

    private List<TranslationPhraseChunker> getChunkers(Iterable<TranslationPhraseChunkerFactory> chunkerFactories, Ontology ontology, Language src) {
        List<TranslationPhraseChunker> chunkers = new LinkedList<TranslationPhraseChunker>();
        for (TranslationPhraseChunkerFactory tpcf : chunkerFactories) {
            final TranslationPhraseChunker phraseChunker = tpcf.getPhraseChunker(ontology, src);
            if (phraseChunker != null) {
                chunkers.add(phraseChunker);
            }
        }
        return chunkers;
    }

    private List<TranslationFeaturizer> getFeaturizers(Ontology ontology, Language src, Language trg, Set<Language> extraLangs) {
        List<TranslationFeaturizer> featurizers = new LinkedList<TranslationFeaturizer>();
        for (TranslationFeaturizerFactory tff : featurizerFactories) {
            final TranslationFeaturizer featurizer = extraLangs == null
                    ? tff.getFeaturizer(ontology, src, trg)
                    : tff.getFeaturizer(ontology, src, trg, extraLangs);
            if (featurizer != null) {
                featurizers.add(featurizer);
            }
        }
        return featurizers;
    }

    private void closeFeaturizers(List<TranslationFeaturizer> translationFeaturizers) {
        for (TranslationFeaturizer featurizer : translationFeaturizers) {
            featurizer.close();
        }
    }

    private LanguageModel getLanguageModel(Language language) {
        if (!languageModels.containsKey(language)) {
            LanguageModel languageModel = null;
            for (LanguageModelFactory languageModelFactory : languageModelFactories) {
                languageModel = languageModelFactory.getModel(language);
                if (languageModel != null) {
                    break;
                }
            }
            if (languageModel != null) {
                languageModels.put(language, languageModel);
            } else {
                throw new IllegalArgumentException("Could not load model for language " + language);
            }
        }
        return languageModels.get(language);
    }

    private Decoder getDecoder(LanguagePair language, LanguageModel languageModel) {
        if (!decoders.containsKey(language)) {
            if (languageModel == null) {
                throw new IllegalArgumentException("Could not load model for language " + language);
            }
            final Decoder decoder = decoderFactory.getDecoder(languageModel, getDecoderWeights(language));
            decoders.put(language, decoder);
        }
        return decoders.get(language);
    }

    private DecoderWeights getDecoderWeights(LanguagePair lp) {
        if (weights.containsKey(lp)) {
            return weights.get(lp);
        } else {
            final Properties config = Configurator.getConfig("eu.monnetproject.translation.wts." + lp.l1 + "-" + lp.l2);
            if (config.isEmpty()) {
                if (untrainedPairs) {
                    return decoderFactory.getDefaultWeights();
                } else {
                    throw new IllegalArgumentException("No weights for model to translate from " + lp.l1 + "-" + lp.l2);
                }
            } else {
                final DecoderWeightsImpl wts = new DecoderWeightsImpl(config);
                weights.put(lp, wts);
                return wts;
            }
        }
    }

    /**
     * Set the weights to be used in the next run
     */
    @Override
    public void updateWeights(final Language srcLang, final Language trgLang, final DecoderWeights weights) {
        final LanguagePair lp = new LanguagePair(srcLang, trgLang);
        decoders.remove(lp);
        this.weights.put(lp, weights);
    }

//    private int featureCount(List<TranslationSource> sources) {
//        int featCt = -1;
//        for (TranslationSource source : sources) {
//            if (featCt != -1 && source.featureCount() != featCt) {
//                throw new RuntimeException("Cannot yet handle sources with different feature counts");
//            } else {
//                featCt = source.featureCount();
//            }
//        }
//        return featCt;
//    }
    private List<TranslationSource> getSources(Iterable<TranslationSourceFactory> sourceFactories, Language sourceLanguage, Language targetLanguage) {
        final LanguagePair lp = new LanguagePair(sourceLanguage, targetLanguage);
        if (!sources.containsKey(lp)) {
            List<TranslationSource> sources2 = new LinkedList<TranslationSource>();
            for (TranslationSourceFactory tsf : sourceFactories) {
                final TranslationSource source = tsf.getSource(sourceLanguage, targetLanguage);
                if (source != null) {
                    sources2.add(source);
                }
            }
            sources.put(lp, sources2);
        }
        return sources.get(lp);
    }

    private Entity getBestEntity(Collection<Entity> entities, Collection<URI> scope) {
        Entity bestEntity = null;
        for (Entity entity : entities) {
            if (entity.getURI() == null
                    || (scope != null
                    && !scope.isEmpty()
                    && !scope.contains(entity.getURI()))) {
                continue;
            }
            if (entity instanceof eu.monnetproject.ontology.Class) {
                return entity;
            } else if (entity instanceof ObjectProperty) {
                bestEntity = entity;
            } else if (entity instanceof DatatypeProperty && (bestEntity == null || !(bestEntity instanceof ObjectProperty))) {
                bestEntity = entity;
            } else if (entity instanceof AnnotationProperty && (bestEntity == null || (!(bestEntity instanceof ObjectProperty) && !(bestEntity instanceof DatatypeProperty)))) {
                bestEntity = entity;
            } else if (entity instanceof Individual && (bestEntity == null || (!(bestEntity instanceof ObjectProperty) && !(bestEntity instanceof DatatypeProperty) && !(bestEntity instanceof AnnotationProperty)))) {
                bestEntity = entity;
            } else if (bestEntity == null) {
                bestEntity = entity;
            }
        }
        return bestEntity;
    }

    @Override
    public TranslatorSetup setup(final Language sourceLanguage, final Language targetLanguage,
            final Set<Language> extraLangs, final DecoderWeights weights) {
        return new TranslatorSetup() {
            private final HashMap<Ontology, TranslationPhraseChunker> tpcs = new HashMap<Ontology, TranslationPhraseChunker>();
            private final HashMap<Ontology, Collection<TranslationFeaturizer>> tfs = new HashMap<Ontology, Collection<TranslationFeaturizer>>();

            @Override
            public TranslationPhraseChunker chunker(Entity entity) {
                final Ontology ontology = entity.getOntology();
                if (!tpcs.containsKey(ontology)) {
                    final TranslationPhraseChunker chunker = getChunkers(chunkerFactories, entity.getOntology(), sourceLanguage).iterator().next();
                    if (chunker != null) {
                        tpcs.put(ontology, chunker);
                        return chunker;
                    } else {
                        return null;
                    }
                } else {
                    return tpcs.get(ontology);
                }
            }

            @Override
            public Collection<TranslationSource> sources() {
                return getSources(sourceFactories, sourceLanguage, targetLanguage);
            }

            @Override
            public Collection<TranslationFeaturizer> featurizers(Entity entity) {
                final Ontology ontology = entity.getOntology();
                if (!tfs.containsKey(ontology)) {
                    final Collection<TranslationFeaturizer> featurizer = getFeaturizers(ontology, sourceLanguage, targetLanguage, extraLangs);
                    if (featurizer != null) {
                        tfs.put(ontology, featurizer);
                        return featurizer;
                    } else {
                        return null;
                    }
                } else {
                    return tfs.get(ontology);
                }
            }

            @Override
            public Decoder decoder(DecoderWeights weights) {
                return decoderFactory.getDecoder(languageModel(), weights == null ? weights() : weights);
            }

            @Override
            public DecoderWeights weights() {
                if (weights != null) {
                    return weights;
                } else {
                    return decoderFactory.getDefaultWeights();
                }
            }

            @Override
            public LanguageModel languageModel() {
                return getLanguageModel(targetLanguage);
            }

            @Override
            public Language sourceLanguage() {
                return sourceLanguage;
            }

            @Override
            public Language targetLanguage() {
                return targetLanguage;
            }

            @Override
            public List<String> featureNames() {
                return prepFeatures(sources(), tfs.values());
            }
        };
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private List<String> prepFeatures(Collection<TranslationSource> translationSources, Collection<Collection<TranslationFeaturizer>> featurizerss) {
        final List<String> features = new ArrayList<String>();
        for (TranslationSource source : translationSources) {
            final String[] featureNames = source.featureNames();
            if (featureNames.length == 0) {
                final String name = source.getName();
                features.add(name);
            } else {
                for (String feature : featureNames) {
                    if (!features.contains(feature)) {
                        features.add(feature);
                    }
                }
            }
        }
        for (Collection<TranslationFeaturizer> featurizers : featurizerss) {
            for (TranslationFeaturizer featurizer : featurizers) {
                features.addAll(featurizer.extraFeatureNames());
            }
        }
        return features;
    }

    private static class LanguagePair {

        private final Language l1;
        private final Language l2;

        public LanguagePair(Language l1, Language l2) {
            this.l1 = l1;
            this.l2 = l2;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LanguagePair other = (LanguagePair) obj;
            if (this.l1 != other.l1 && (this.l1 == null || !this.l1.equals(other.l1))) {
                return false;
            }
            if (this.l2 != other.l2 && (this.l2 == null || !this.l2.equals(other.l2))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (this.l1 != null ? this.l1.hashCode() : 0);
            hash = 29 * hash + (this.l2 != null ? this.l2.hashCode() : 0);
            return hash;
        }
    }

    public class DecoderWeightsImpl extends HashMap<String, Double> implements DecoderWeights {

        private static final long serialVersionUID = -8575226431160097127L;

        public DecoderWeightsImpl(Properties config) {
            for (Object key : config.keySet()) {
                String keyStr = key.toString();
                String value = config.getProperty(key.toString());
                put(keyStr, Double.parseDouble(value));
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("{");
            for (Map.Entry<String, Double> entry : entrySet()) {
                sb.append(" ").append(entry.getKey()).append(" -> ").append(entry.getValue()).append(",");
            }
            return sb.deleteCharAt(sb.length() - 1).append(" }").toString();
        }
    }

    private static class Wait implements RejectedExecutionHandler {

        private final HashSet<Runnable> waiting = new HashSet<Runnable>();

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (waiting.contains(r)) {
                throw new RejectedExecutionException();
            }
            while (true) {
                waiting.add(r);
                try {
                    synchronized(this) {
                        this.wait(200);
                    }
                } catch (InterruptedException x) {
                }
                try {
                    if(executor.getActiveCount() < executor.getPoolSize()) {
                        executor.submit(r);
                        waiting.remove(r);
                        return;
                    }
                } catch (RejectedExecutionException ree) {
                }
            }
        }
    }
}
