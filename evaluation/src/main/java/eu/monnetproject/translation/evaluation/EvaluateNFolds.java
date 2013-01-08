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
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
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
package eu.monnetproject.translation.evaluation;

import eu.monnetproject.lemon.model.Lexicon;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.ontology.OntologySerializer;
import eu.monnetproject.framework.services.Services;
import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.translation.DecoderWeights;
import eu.monnetproject.translation.OntologyTranslator;
import eu.monnetproject.translation.eval.TranslationEvaluatorFactory;
import eu.monnetproject.translation.evaluation.evaluate.EvaluationResultFactory;
import eu.monnetproject.translation.evaluation.evaluate.FoldLexicon;
import eu.monnetproject.translation.evaluation.evaluate.LexiconParallelCorpus;
import eu.monnetproject.translation.evaluation.evaluate.MultiMonitorFactory;
import eu.monnetproject.translation.monitor.TranslationMonitor;
import eu.monnetproject.translation.monitor.TranslationMonitorFactory;
import eu.monnetproject.translation.monitor.Messages;
import eu.monnetproject.translation.tune.TranslatorSetup;
import eu.monnetproject.translation.tune.Tuner;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class EvaluateNFolds extends AbstractEvaluation {

    protected int numFolds, numIters;
    protected String metricName;
    private final Tuner tuner;

    public EvaluateNFolds(String[] args, OntologySerializer ontoSerializer, TranslationEvaluatorFactory translationEvaluatorFactory, OntologyTranslator controller, LabelExtractorFactory lef, Tuner tuner, TranslationMonitorFactory monitorFactory) {
        super(args, ontoSerializer, translationEvaluatorFactory, controller, lef, monitorFactory);
        this.tuner = tuner;
    }

    @Override
    protected void checkUsage(CLIOpts opts) {
        this.numFolds = opts.intValue("numFolds", "The number of folds to use");
        this.numIters = opts.intValue("numIters", "The number of iterations to use when tuning");
        this.metricName = opts.string("metricName", "The name of the metric to optimize");
        if(!opts.verify("./evaluate-nfolds")) {
            System.exit(-1);
        }
    }

    public void exec() throws Exception {
        long lastStartTime = startTime;
        //final List<EvaluationResult> evaluationResults = new LinkedList<EvaluationResult>();
        Collection<TranslationMonitorFactory> monitorFactories = Services.getAll(TranslationMonitorFactory.class);
        final TranslationMonitorFactory monitorFactory = new MultiMonitorFactory(monitorFactories);
        final List<TranslationMonitor> translationMonitors = new LinkedList<TranslationMonitor>();
        final TranslatorSetup setup = controller.setup(sourceLanguage, targetLanguage, null, null);
        for (int i = 0; i < numFolds; i++) {
            if (i + 1 < firstFold) {
                continue;
            }
            System.err.println("===== FOLD " + (i + 1) + " =====================================");
            final TranslationMonitor monitor = monitorFactory.getMonitor(runName+"_fold"+(i+1), sourceLanguage, targetLanguage);
            monitor.start();
            controller.addMonitor(monitor);
            PreparedOntologyList pol = new PreparedOntologyList();
            for (File ontologyFile : referenceFolder.listFiles()) {
                try {
                final PreparedOntology po = prepareOntologyFile(ontologyFile);

                if (po != null) {
                    Messages.info("Source lexicon size: " + po.sourceLexicon.getEntrys().size());
                    final Lexicon foldLexicon = FoldLexicon.fold(po.sourceLexicon, ((double) i) * 1.0 / (double) numFolds, ((double) i + 1) * 1.0 / (double) numFolds);
                    Messages.info("Fold size: " + foldLexicon.getEntrys().size());
                    Messages.info("Test size: " + po.sourceLexicon.getEntrys().size());
                    final FoldedPreparedOntology foldedPO = new FoldedPreparedOntology(foldLexicon, po);
                    pol.add(foldedPO);
                }
                } catch(Exception x) {
                Messages.severe("Failed to process ontology " + ontologyFile.getName());
                Messages.componentLoadFail(Ontology.class,x);
                }
            }

            doTune(setup,pol, tuner, evaluatorFactory);

            for (PreparedOntology po : pol.asList()) {
                doTranslation(po);
                saveOntologyResult(po,monitor);
            }
            long newStartTime = System.currentTimeMillis();
            //evaluationResult.setExecutionTimeTotal(newStartTime - lastStartTime);
            monitor.end();
            monitor.commit();
            lastStartTime = newStartTime;
            translationMonitors.add(monitor);
            //printResult(evaluationResult, runName == null ? null : (runName + "_fold_" + i), sourceLanguage, targetLanguage);
            //evaluationResults.add(evaluationResult);
            //evaluationResult = new EvaluationResult(sourceLanguage, targetLanguage);
        }

        //evaluationResult = new EvaluationResult(sourceLanguage, targetLanguage);
        //evaluationResult.aggregateFolds(evaluationResults, sourceLanguage, targetLanguage);
        //printFinalResult();
        monitorFactory.aggregate(runName, translationMonitors);
    }

    protected void doTune(TranslatorSetup setup, PreparedOntologyList pol, Tuner tuner, TranslationEvaluatorFactory tef) {
        // Source Lexicon is for tuning
        final DecoderWeights weights = tuner.tune(setup, new LexiconParallelCorpus(pol.foldLexica, pol.referenceLexica, pol.ontologies), tef, metricName, numIters, translationOptions);

        Messages.info("===================================");
        Messages.info("== Tuning Complete               ==");
        Messages.info("== weights: " + weights);
        Messages.info("===================================");

        controller.updateWeights(setup.sourceLanguage(),setup.targetLanguage(),weights);
    }

    protected static class FoldedPreparedOntology extends PreparedOntology {

        public final Lexicon foldLexicon;

        public FoldedPreparedOntology(Lexicon foldLexicon, PreparedOntology po) {
            super(po.sourceLexica, po.sourceLexicon, po.targetLexicon, po.referenceLexicon, po.ontology,po.fileName);
            this.foldLexicon = foldLexicon;
        }
    }

    protected static class PreparedOntologyList {

        private final List<Lexicon> foldLexica = new LinkedList<Lexicon>();
        private final List<Collection<Lexicon>> allSourceLexica = new LinkedList<Collection<Lexicon>>();
        private final List<Lexicon> sourceLexica = new LinkedList<Lexicon>();
        private final List<Lexicon> targetLexica = new LinkedList<Lexicon>();
        private final List<Lexicon> referenceLexica = new LinkedList<Lexicon>();
        private final List<Ontology> ontologies = new LinkedList<Ontology>();
        private final List<String> fileNames = new LinkedList<String>();

        public void add(FoldedPreparedOntology po) {
            allSourceLexica.add(po.sourceLexica);
            sourceLexica.add(po.sourceLexicon);
            referenceLexica.add(po.referenceLexicon);
            targetLexica.add(po.targetLexicon);
            ontologies.add(po.ontology);
            foldLexica.add(po.foldLexicon);
            fileNames.add(po.fileName);
        }

        public List<FoldedPreparedOntology> asList() {
            final Iterator<Collection<Lexicon>> iter1 = allSourceLexica.iterator();
            final Iterator<Lexicon> iter2 = sourceLexica.iterator();
            final Iterator<Lexicon> iter3 = targetLexica.iterator();
            final Iterator<Lexicon> iter4 = referenceLexica.iterator();
            final Iterator<Ontology> iter5 = ontologies.iterator();
            final Iterator<String> iter6 = fileNames.iterator();
            final Iterator<Lexicon> iter0 = foldLexica.iterator();
            final LinkedList<FoldedPreparedOntology> list = new LinkedList<FoldedPreparedOntology>();
            while (iter1.hasNext()) {
                list.add(new FoldedPreparedOntology(iter0.next(), new PreparedOntology(iter1.next(), iter2.next(), iter3.next(), iter4.next(), iter5.next(),iter6.next())));
            }
            return list;
        }
    }

    public static void main(String[] args) throws Exception {
        Collection<TranslationMonitorFactory> monitorFactory = Services.getAll(TranslationMonitorFactory.class);
        final TranslationMonitorFactory monitor;
        if(monitorFactory.iterator().hasNext()) {
            monitor = monitorFactory.iterator().next();
        } else {
            monitor = new EvaluationResultFactory();
        }
        new EvaluateNFolds(args,
                Services.get(OntologySerializer.class),
                Services.get(TranslationEvaluatorFactory.class),
                Services.get(OntologyTranslator.class),
                Services.get(LabelExtractorFactory.class),
                Services.get(Tuner.class),
                monitor).exec();
    }
//
//    public static void notmain(String[] args) throws Exception {
//        //args = "tmp en es 2 5 nist".split("\\s+");
//        final List<String> argsList = new LinkedList(Arrays.asList(args));
//        String namePrefix = null;
//        int nBest = 1;
//        String[] scopeStrs = new String[]{};
//        String runName = null;
//        int firstFold = 0;
//        for (int i = 0; i < argsList.size() - 1; i++) {
//            if (argsList.get(i).equals("-namePrefix")) {
//                namePrefix = argsList.get(i + 1);
//                argsList.remove(i);
//                argsList.remove(i);
//                i--;
//            } else if (argsList.get(i).equals("-nBest")) {
//                nBest = Integer.parseInt(argsList.get(i + 1));
//                argsList.remove(i);
//                argsList.remove(i);
//                i--;
//            } else if (argsList.get(i).equals("-scope")) {
//                scopeStrs = argsList.get(i).split(",");
//                for (int j = 0; j < scopeStrs.length; j++) {
//                    scopeStrs[j] = scopeStrs[j].trim();
//                }
//                argsList.remove(i);
//                argsList.remove(i);
//                i--;
//            } else if (argsList.get(i).equals("-runName")) {
//                runName = argsList.get(i + 1);
//                argsList.remove(i);
//                argsList.remove(i);
//                i--;
//            } else if (argsList.get(i).equals("-firstFold")) {
//                firstFold = Integer.parseInt(argsList.get(i + 1));
//                argsList.remove(i);
//                argsList.remove(i);
//                i--;
//            }
//        }
//        final TranslationEvaluatorFactory evaluatorFactory = Services.get(TranslationEvaluatorFactory.class);
//        final OntologySerializer ontoSerializer = Services.get(OntologySerializer.class);
//        if (argsList.size() != 6) {
//            System.err.println("Usage:\n\tEvaluateNFolds [-namePrefix prefix] [-nBest n] [-scope uri1,uri2,uri3...] [-runName name] referenceFolder srcLang trgLang folds tuneIters metricName");
//            System.exit(-1);
//        }
//        final Language sourceLanguage = Language.get(argsList.get(1));
//        final Language targetLanguage = Language.get(argsList.get(2));
//        final String metricName = argsList.get(5);
//
//        final File referenceFolder = new File(argsList.get(0));
//        if (!referenceFolder.isDirectory()) {
//            throw new IllegalArgumentException(referenceFolder.getPath() + " is not a directory");
//        }
//        //final EvaluationResult evaluationResult = new EvaluationResult(sourceLanguage, targetLanguage);
//        final List<EvaluationResult> evaluationResults = new ArrayList<EvaluationResult>();
//        for (int i = 0; i < numFolds; i++) {
//            evaluationResults.add(new EvaluationResult(sourceLanguage, targetLanguage));
//        }
//
//        if (runName != null) {
//            Logging.setLoggerFactory(new LoggerFactory() {
//
//                @Override
//                public Logger getLogger(Class c) {
//                    return new EvaluateLogger(c.getName());
//                }
//            });
//        }
//        final long startTime = System.currentTimeMillis();
//
//        //System.out.println("Press enter to start");
//        //System.in.read();
//
//        final TranslationController controller = new TranslationController(Services.get(LanguageModelFactory.class),
//                Services.get(DecoderFactory.class),
//                Services.getAll(TranslationPhraseChunkerFactory.class),
//                Services.getAll(TranslationSourceFactory.class),
//                Services.getAll(TranslationFeaturizerFactory.class),
//                Services.getFactory(TokenizerFactory.class));
//        final SimpleLexicalizer lexicalizer = new SimpleLexicalizer(Services.get(LabelExtractorFactory.class));
//        for (int i = 0; i < numFolds; i++) {
//            log.info("Fold number: " + i);
//            if (i < firstFold) {
//                log.info("Skipped!");
//                continue;
//            }
//
//            final List<Lexicon> foldLexica = new LinkedList<Lexicon>();
//            final List<Lexicon> sourceLexica = new LinkedList<Lexicon>();
//            final List<Lexicon> referenceLexica = new LinkedList<Lexicon>();
//            final List<Ontology> ontologies = new LinkedList<Ontology>();
//            for (File ontologyFile : referenceFolder.listFiles()) {
//                try {
//                    if (!ontologyFile.getName().endsWith("rdf") && !ontologyFile.getName().endsWith("owl")
//                            && !ontologyFile.getName().endsWith("ttl") && !ontologyFile.getName().endsWith("xml")) {
//                        System.err.println("Skipping " + ontologyFile.getName());
//                        continue;
//                    }
//                    System.err.println("Reading " + ontologyFile);
//                    final Reader ontologyReader = new FileReader(ontologyFile);
//                    final Ontology ontology = ontoSerializer.read(ontologyReader, ontologyFile.toURI());
//                    final Collection<Lexicon> sourceLexica2 = lexicalizer.lexicalize(ontology);
//                    List<Lexicon> sourceLexiconAsList = null;
//                    Lexicon referenceLexicon = null;
//                    for (Lexicon lexicon : sourceLexica2) {
//                        if (Language.get(lexicon.getLanguage()).equals(sourceLanguage)) {
//                            sourceLexiconAsList = Collections.singletonList(lexicon);
//                        } else if (Language.get(lexicon.getLanguage()).equals(targetLanguage)) {
//                            referenceLexicon = lexicon;
//                        }
//                    }
//                    if (sourceLexiconAsList == null || referenceLexicon == null) {
//                        continue;
//                    }
//                    final Lexicon foldLexicon = FoldLexicon.fold(sourceLexiconAsList.get(0), ((double) i) * 1.0 / (double) numFolds, ((double) i + 1) * 1.0 / (double) numFolds);
//
//                    System.err.println("Size:" + foldLexicon.getEntrys().size() + " vs " + sourceLexiconAsList.get(0).getEntrys().size());
//
//                    sourceLexica.add(sourceLexiconAsList.get(0));
//                    referenceLexica.add(referenceLexicon);
//                    foldLexica.add(foldLexicon);
//                    ontologies.add(ontology);
//
//                } catch (Exception x) {
//                    log.severe("Could not process ontology " + ontologyFile);
//                    log.stackTrace(x);
//                }
//            }
//
//            final Tuner tuner = Services.get(Tuner.class);
//            final TranslationEvaluatorFactory tef = Services.getFactory(TranslationEvaluatorFactory.class);
//
//            // Source Lexicon is for tuning
//            final DecoderWeights weights = tuner.tune(controller.setup(sourceLanguage, targetLanguage, null, null), new LexiconParallelCorpus(sourceLexica, referenceLexica, ontologies), tef, metricName, numIters);
//
//            log.info("===================================");
//            log.info("== Tuning Complete               ==");
//            log.info("== weights: " + weights);
//            log.info("===================================");
//
//            controller.setWeights(weights);
//
//            // Fold lexica is for evaluation
//            for (int j = 0; j < foldLexica.size(); j++) {
//                final List<Lexicon> foldLexiconAsList = Collections.singletonList(foldLexica.get(j));
//                final Ontology ontology = ontologies.get(j);
//                final Lexicon targetLexicon = lexicalizer.getBlankLexicon(ontology, targetLanguage);
//                final ArrayList<URI> scopes = new ArrayList<URI>();
//                for (String scopeStr : scopeStrs) {
//                    final URI scope = URI.create(scopeStr);
//                    if (scope == null) {
//                        throw new IllegalArgumentException(scopeStr + " is not a valid URI");
//                    }
//                    scopes.add(scope);
//                }
//                controller.translate(ontology, foldLexiconAsList, targetLexicon, scopes, namePrefix, nBest);
//
//                System.err.printf("Translated %d entries\n", targetLexicon.getEntrys().size());
//
//                final LinkedList<Translation> translations = new LinkedList<Translation>();
//                final LinkedList<List<Translation>> references = new LinkedList<List<Translation>>();
//
//                final Lexicon referenceLexicon = referenceLexica.get(j);
//                for (LexicalEntry targetEntry : targetLexicon.getEntrys()) {
//                    try {
//                        final Lexicon foldLexicon = foldLexiconAsList.get(0);
//                        final List<LexicalEntry> sourceEntries = LemonModels.getEntryByReference(foldLexicon, targetEntry.getSenses().iterator().next().getReference());
//                        final List<LexicalEntry> referenceEntries = LemonModels.getEntryByReference(referenceLexicon, targetEntry.getSenses().iterator().next().getReference());
//                        for (LexicalEntry sourceEntry : sourceEntries) {
//                            final LinkedList<Translation> refList = new LinkedList<Translation>();
//                            for (LexicalEntry referenceEntry : referenceEntries) {
//                                refList.add(new TranslationImpl(sourceEntry, referenceEntry));
//                            }
//                            final TranslationImpl t = new TranslationImpl(sourceEntry, targetEntry);
//                            if (refList.isEmpty()) {
//                                log.warning("No references for " + t.getEntity());
//                            } else {
//                                translations.add(t);
//                                references.add(refList);
//                            }
//                        }
//                    } catch (Exception x) {
//                    }
//                }
//                final Collection<TranslationEvaluator> evaluators = evaluatorFactory.getEvaluators(references);
//                for (TranslationEvaluator evaluator : evaluators) {
//                    double score = evaluator.score(translations);
//                    evaluationResults.get(i).addResult(ontology, translations.size(), evaluator.getName(), score);
//                }
//
//                System.gc();
//                final long totalMemory = Runtime.getRuntime().totalMemory();
//                final long max = Runtime.getRuntime().maxMemory();
//                System.err.println("Using " + (totalMemory / 0x100000) + "MB/" + (max / 0x100000) + "MB");
//            }
//
//            evaluationResults.get(i).setExecutionTimeTotal(System.currentTimeMillis() - startTime);
//
//            printResult(evaluationResults.get(i), runName == null ? null : (runName + "_fold_" + i), sourceLanguage, targetLanguage);
//
//        }
//        final EvaluationResult aggResult = new EvaluationResult(sourceLanguage, targetLanguage);
//        aggResult.aggregateFolds(evaluationResults, sourceLanguage, targetLanguage);
//        printResult(aggResult, runName, sourceLanguage, targetLanguage);
//
//        System.exit(0);
//    }
//
//    private static void printResult(EvaluationResult evaluationResult, String runName, Language sourceLanguage, Language targetLanguage) throws IOException {
//
//        final File results = new File("results");
//        if (!results.exists()) {
//            results.mkdir();
//        }
//        if (runName != null) {
//            final String resultsFileName = "results" + System.getProperty("file.separator") + runName + "_" + sourceLanguage + "_" + targetLanguage + "_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm").format(new Date()) + ".xml";
//            final PrintWriter xmlFile = new PrintWriter(resultsFileName);
//            xmlFile.println(evaluationResult.toXML());
//            xmlFile.close();
//            System.err.println("Saving results to " + resultsFileName);
//        }
//        System.out.println(evaluationResult.toString());
//
//    }
}
