/**********************************************************************************
 * Copyright (c) 2011, Monnet Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Monnet Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
package eu.monnetproject.translation.evaluation.evaluate;

import eu.monnetproject.translation.evaluation.Evaluate;
import eu.monnetproject.framework.launcher.Command;
import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.ontology.OntologySerializer;
import eu.monnetproject.translation.OntologyTranslator;
import eu.monnetproject.translation.eval.TranslationEvaluatorFactory;
import eu.monnetproject.translation.monitor.TranslationMonitorFactory;
import java.util.Collection;

/**
 *
 * @author John McCrae
 */
public class EvaluateCommand implements Command { 
    private final OntologySerializer ontoSerializer;
    private final TranslationEvaluatorFactory translationEvaluatorFactory;
    private final OntologyTranslator controller;
    private final LabelExtractorFactory lef;
    private final TranslationMonitorFactory monitorFactory;

    public EvaluateCommand(OntologySerializer ontoSerializer, TranslationEvaluatorFactory translationEvaluatorFactory, OntologyTranslator controller, LabelExtractorFactory lef, Collection<TranslationMonitorFactory> monitorFactory) {
        this.ontoSerializer = ontoSerializer;
        this.translationEvaluatorFactory = translationEvaluatorFactory;
        this.controller = controller;
        this.lef = lef;
        if(monitorFactory.iterator().hasNext()) {
            this.monitorFactory = monitorFactory.iterator().next();
        } else {
            this.monitorFactory = new EvaluationResultFactory();
        }
    }

    @Override
    public void execute(String[] args) throws Exception {
        new Evaluate(args, ontoSerializer, translationEvaluatorFactory, controller, lef, monitorFactory).exec();
    }
}
