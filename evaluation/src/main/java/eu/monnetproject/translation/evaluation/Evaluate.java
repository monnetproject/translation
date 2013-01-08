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
package eu.monnetproject.translation.evaluation;

import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.ontology.OntologySerializer;
import eu.monnetproject.framework.services.Services;
import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.translation.OntologyTranslator;
import eu.monnetproject.translation.eval.TranslationEvaluatorFactory;
import eu.monnetproject.translation.evaluation.evaluate.MultiMonitorFactory;
import eu.monnetproject.translation.monitor.TranslationMonitor;
import eu.monnetproject.translation.monitor.TranslationMonitorFactory;
import eu.monnetproject.translation.monitor.Messages;
import java.io.File;
import java.util.Collection;

/**
 *
 * @author John McCrae
 */
public class Evaluate extends AbstractEvaluation {
    public Evaluate(String[] args, OntologySerializer ontoSerializer, TranslationEvaluatorFactory translationEvaluatorFactory, OntologyTranslator controller, LabelExtractorFactory lef, TranslationMonitorFactory monitorFactory) {
        super(args, ontoSerializer, translationEvaluatorFactory, controller, lef, monitorFactory);
    }

    public static void main(String[] args) throws Exception {
        Collection<TranslationMonitorFactory> monitorFactory = Services.getAll(TranslationMonitorFactory.class);
        final TranslationMonitorFactory monitor = new MultiMonitorFactory(monitorFactory);
        final Evaluate evaluate = new Evaluate(args, Services.get(OntologySerializer.class),
                Services.get(TranslationEvaluatorFactory.class),
                Services.get(OntologyTranslator.class),
                Services.get(LabelExtractorFactory.class),
                monitor);
        evaluate.exec();
    }

    @Override
    protected void checkUsage(CLIOpts opts) {
        if(!opts.verify("./evaluate")) {
            System.exit(-1);
        }
    }

    
    
    public void exec() throws Exception {
        final TranslationMonitor monitor = monitorFactory.getMonitor(runName, sourceLanguage, targetLanguage);
        monitor.start();
        controller.addMonitor(monitor);
        for (File ontologyFile : referenceFolder.listFiles()) {
            try {
                final PreparedOntology po = prepareOntologyFile(ontologyFile);
                if (po != null) {
                    doTranslation(po);
                    saveOntologyResult(po, monitor);
                } else {
                    //Messages.severe("Could not prepare ontology");
                    Messages.componentLoadFail(Ontology.class, "Could not prepare ontology");
                }
            } catch (Exception x) {
                //Messages.severe("Failed to process ontology " + ontologyFile.getName());
                Messages.componentLoadFail(Ontology.class,x);
            }
        }
        printFinalResult(monitor);
    }
}
