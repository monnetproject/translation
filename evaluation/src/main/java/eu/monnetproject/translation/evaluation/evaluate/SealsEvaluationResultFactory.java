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
package eu.monnetproject.translation.evaluation.evaluate;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.monitor.TranslationMonitor;
import eu.monnetproject.translation.monitor.TranslationMonitorFactory;

/**
 *
 * @author Miguel Angel Garcia
 * 
*/
public class SealsEvaluationResultFactory implements TranslationMonitorFactory {

    @Override
    public TranslationMonitor getMonitor(String runName, Language srcLang,
            Language trgLang) {
        Properties props = Configurator.getConfig("eu.monnetproject.seals");
        if (props.getProperty("resourceLocator") != null) {
            String resourceLocator = props.getProperty("resourceLocator");
            String author = props.getProperty("author");
            String description = props.getProperty("description");
            return new SealsEvaluationResult(srcLang, trgLang, runName, resourceLocator, author, description);
        } else {
            return null;
        }

    }

    @Override
    public void aggregate(String aggregateRunName,
            List<TranslationMonitor> monitors) {
        if (monitors.isEmpty()) {
            throw new RuntimeException("Aggregating over no monitors");
        }
        final TranslationMonitor monitor = monitors.get(0);
        final Language srcLang, trgLang;
        if (monitor instanceof SealsEvaluationResult) {
            srcLang = ((SealsEvaluationResult) monitor).srcLang;
            trgLang = ((SealsEvaluationResult) monitor).trgLang;
        } else {
            throw new RuntimeException("Aggregating over some other monitor... not supported");
        }
        final List<SealsEvaluationResult> results = new ArrayList<SealsEvaluationResult>(monitors.size());
        for (TranslationMonitor mon : monitors) {
            if (mon instanceof SealsEvaluationResult) {
                results.add((SealsEvaluationResult) mon);
            } else {
                throw new RuntimeException("Aggregating over some other monitor... not supported");

            }
        }
        Properties props = Configurator.getConfig("eu.monnetproject.seals");
        String resourceLocator = "http://localhost:8090/rrs-web/";
        String author = "unknonw";
        String description = "Generic description";
        if (props.getProperty("resourceLocator") != null) {
            resourceLocator = props.getProperty("resourceLocator");
            author = props.getProperty("author");
            description = props.getProperty("description");
        }
        final SealsEvaluationResult evaluationResult = new SealsEvaluationResult(srcLang, trgLang, aggregateRunName, resourceLocator, author, description);
        evaluationResult.aggregateFolds(results, srcLang, trgLang);
        try {
            evaluationResult.commit();
        } catch (Exception x) {
            throw new RuntimeException(x);
        }

    }
}
