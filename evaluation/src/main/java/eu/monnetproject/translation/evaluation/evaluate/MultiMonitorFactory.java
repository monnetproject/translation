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
package eu.monnetproject.translation.evaluation.evaluate;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.monitor.TranslationMonitor;
import eu.monnetproject.translation.monitor.TranslationMonitorFactory;
import eu.monnetproject.translation.monitor.Messages;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class MultiMonitorFactory implements TranslationMonitorFactory {

    private final LinkedList<TranslationMonitorFactory> factories;

    public MultiMonitorFactory(Collection<TranslationMonitorFactory> factories) {
        // This is bad... we only do it so that we can black-list bad factories, to avoid errors in aggregation
        this.factories = new LinkedList<TranslationMonitorFactory>(factories);
    }

    @Override
    public TranslationMonitor getMonitor(String runName, Language srcLang, Language trgLang) {
        final LinkedList<TranslationMonitor> monitors = new LinkedList<TranslationMonitor>();
        final Iterator<TranslationMonitorFactory> factoryIter = factories.iterator();
        while (factoryIter.hasNext()) {
            final TranslationMonitorFactory tmf = factoryIter.next();
            try {
                final TranslationMonitor monitor = tmf.getMonitor(runName, srcLang, trgLang);
                if (monitor == null) {
                    Messages.componentLoadFail(tmf.getClass(), "returned null");
                    factoryIter.remove();
                } else {
                    monitors.add(monitor);
                }
            } catch (Exception x) {
                    Messages.componentLoadFail(tmf.getClass(), x);
                factoryIter.remove();
            }
        }
        if (monitors.isEmpty()) {
            monitors.add(new EvaluationResult(srcLang, trgLang, runName));
        }
        return new MultiMonitor(monitors);
    }

    @Override
    public void aggregate(String aggregateRunName, List<TranslationMonitor> monitors) {
        final LinkedList<List<TranslationMonitor>> monitorLists = new LinkedList<List<TranslationMonitor>>();
        if (factories.isEmpty()) {
            monitorLists.add(new LinkedList<TranslationMonitor>());
        } else {
            for (TranslationMonitorFactory tmf : factories) {
                monitorLists.add(new LinkedList<TranslationMonitor>());
            }
        }
        for (TranslationMonitor monitor : monitors) {
            if (monitor instanceof MultiMonitor) {
                final Iterator<List<TranslationMonitor>> monitorListIterator = monitorLists.iterator();
                for (TranslationMonitor m : ((MultiMonitor) monitor).getMonitors()) {
                    monitorListIterator.next().add(m);
                }
            }
        }

        final Iterator<List<TranslationMonitor>> monitorListIterator = monitorLists.iterator();
        if (factories.isEmpty() && !monitorLists.isEmpty()) {
            new EvaluationResultFactory().aggregate(aggregateRunName, monitorListIterator.next());
        } else {
            for (TranslationMonitorFactory tmf : factories) {
                tmf.aggregate(aggregateRunName, monitorListIterator.next());
            }
        }
    }

    private static class MultiMonitor implements TranslationMonitor {

        private final Collection<TranslationMonitor> monitors;

        public MultiMonitor(Collection<TranslationMonitor> monitors) {
            this.monitors = monitors;
        }

        @Override
        public void recordTranslation(Translation translation) {
            for (TranslationMonitor monitor : monitors) {
                monitor.recordTranslation(translation);
            }
        }

        @Override
        public void recordOntologyScore(String ontologyID, String metricName, double metricValue, int ontologySize) {
            for (TranslationMonitor monitor : monitors) {
                monitor.recordOntologyScore(ontologyID, metricName, metricValue, ontologySize);
            }
        }

        @Override
        public void start() {
            for (TranslationMonitor monitor : monitors) {
                monitor.start();
            }
        }

        @Override
        public void end() {
            for (TranslationMonitor monitor : monitors) {
                monitor.end();
            }
        }

        @Override
        public void commit() throws Exception {
            for (TranslationMonitor monitor : monitors) {
                monitor.commit();
            }
        }

        public Collection<TranslationMonitor> getMonitors() {
            return monitors;
        }
    }
}
