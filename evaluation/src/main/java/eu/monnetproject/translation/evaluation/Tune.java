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
package eu.monnetproject.translation.evaluation;

import eu.monnetproject.framework.services.Services;
import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.lemon.model.Lexicon;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.ontology.OntologySerializer;
import eu.monnetproject.translation.DecoderWeights;
import eu.monnetproject.translation.OntologyTranslator;
import eu.monnetproject.translation.eval.TranslationEvaluatorFactory;
import eu.monnetproject.translation.evaluation.evaluate.EvaluationResultFactory;
import eu.monnetproject.translation.evaluation.evaluate.LexiconParallelCorpus;
import eu.monnetproject.translation.monitor.TranslationMonitorFactory;
import eu.monnetproject.translation.monitor.Messages;
import eu.monnetproject.translation.tune.TranslatorSetup;
import eu.monnetproject.translation.tune.Tuner;
import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author John McCrae
 */
public class Tune extends AbstractEvaluation {
    protected int numIters;
    protected String metricName;
    protected File wtsFile;
    private final Tuner tuner;

    public Tune(String[] args, OntologySerializer ontoSerializer, TranslationEvaluatorFactory translationEvaluatorFactory, OntologyTranslator controller, LabelExtractorFactory lef, Tuner tuner, TranslationMonitorFactory monitorFactory) {
        super(args, ontoSerializer, translationEvaluatorFactory, controller, lef, monitorFactory);
        this.tuner = tuner;
    }

    @Override
    protected void checkUsage(CLIOpts opts) {
        this.metricName = opts.string("metricName", "The name of the metric to optimize");
        this.numIters = opts.intValue("numIters", "The number of iterations of tuning");
        this.wtsFile = opts.woFile("wtsFile", "The file to write the weights to");
        if(!opts.verify("./tune")) {
            System.exit(-1);
        }
    }

    public void exec() throws Exception {
        final PreparedOntologyList pol = new PreparedOntologyList();
        for (File ontologyFile : referenceFolder.listFiles()) {
            try {
                final PreparedOntology po = prepareOntologyFile(ontologyFile);
                if (po != null) {
                    //doTranslation(po);
                    pol.add(po);
                } else {
                    Messages.severe("Could not prepare ontology");
                }

            } catch (Exception x) {
                Messages.severe("Failed to process ontology " + ontologyFile.getName());
                Messages.componentLoadFail(Ontology.class,x);
            }
        }
        final TranslatorSetup setup = controller.setup(sourceLanguage, targetLanguage, null, null);
        
        final DecoderWeights weights = doTune(setup, pol, tuner, evaluatorFactory);
        
        final PrintWriter wtsOut = new PrintWriter(wtsFile);
        for (Map.Entry<String, Double> e : weights.entrySet()) {
            wtsOut.println(e.getKey().replaceAll(":", "\\\\:")+"="+e.getValue());
            System.out.println(e.getKey().replaceAll(":", "\\\\:")+"="+e.getValue());
        }
        wtsOut.flush();
        wtsOut.close();
    }

    protected DecoderWeights doTune(TranslatorSetup setup, PreparedOntologyList pol, Tuner tuner, TranslationEvaluatorFactory tef) {
        // Source Lexicon is for tuning
        final DecoderWeights weights = tuner.tune(setup, new LexiconParallelCorpus(pol.sourceLexica, pol.referenceLexica, pol.ontologies), tef, metricName, numIters, translationOptions);

        Messages.info("===================================");
        Messages.info("== Tuning Complete               ==");
        Messages.info("== weights: " + weights);
        Messages.info("===================================");

        controller.updateWeights(setup.sourceLanguage(), setup.targetLanguage(), weights);
        
        return weights;
    }

    protected static class PreparedOntologyList {

        private final List<Collection<Lexicon>> allSourceLexica = new LinkedList<Collection<Lexicon>>();
        private final List<Lexicon> sourceLexica = new LinkedList<Lexicon>();
        private final List<Lexicon> targetLexica = new LinkedList<Lexicon>();
        private final List<Lexicon> referenceLexica = new LinkedList<Lexicon>();
        private final List<Ontology> ontologies = new LinkedList<Ontology>();
        private final List<String> fileNames = new LinkedList<String>();

        public void add(PreparedOntology po) {
            allSourceLexica.add(po.sourceLexica);
            sourceLexica.add(po.sourceLexicon);
            referenceLexica.add(po.referenceLexicon);
            targetLexica.add(po.targetLexicon);
            ontologies.add(po.ontology);
            fileNames.add(po.fileName);
        }

        public List<PreparedOntology> asList() {
            final Iterator<Collection<Lexicon>> iter1 = allSourceLexica.iterator();
            final Iterator<Lexicon> iter2 = sourceLexica.iterator();
            final Iterator<Lexicon> iter3 = targetLexica.iterator();
            final Iterator<Lexicon> iter4 = referenceLexica.iterator();
            final Iterator<Ontology> iter5 = ontologies.iterator();
            final Iterator<String> iter6 = fileNames.iterator();
            final LinkedList<PreparedOntology> list = new LinkedList<PreparedOntology>();
            while (iter1.hasNext()) {
                list.add(new PreparedOntology(iter1.next(), iter2.next(), iter3.next(), iter4.next(), iter5.next(), iter6.next()));
            }
            return list;
        }
    }

    public static void main(String[] args) throws Exception {
        Collection<TranslationMonitorFactory> monitorFactory = Services.getAll(TranslationMonitorFactory.class);
        final TranslationMonitorFactory monitor;
        if (monitorFactory.iterator().hasNext()) {
            monitor = monitorFactory.iterator().next();
        } else {
            monitor = new EvaluationResultFactory();
        }
        new Tune(args,
                Services.get(OntologySerializer.class),
                Services.get(TranslationEvaluatorFactory.class),
                Services.get(OntologyTranslator.class),
                Services.get(LabelExtractorFactory.class),
                Services.get(Tuner.class),
                monitor).exec();
    }
}
