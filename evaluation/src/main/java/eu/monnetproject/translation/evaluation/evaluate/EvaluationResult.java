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

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.monitor.TranslationMonitor;
import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author John McCrae
 */
public class EvaluationResult implements TranslationMonitor {

    private final HashMap<String, Set<EvaluationResultEntry>> results = new HashMap<String, Set<EvaluationResultEntry>>();
    private final HashMap<String, Integer> resultsN = new HashMap<String, Integer>();
    private final ArrayList<Translation> translations = new ArrayList<Translation>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    final Language srcLang, trgLang;
    private final String runName;
    private long executionTimeTotal;

    public EvaluationResult(Language srcLang, Language trgLang, String runName) {
        this.srcLang = srcLang;
        this.trgLang = trgLang;
        this.runName = runName;
    }

    @Override
    public void commit() throws Exception {
        final File results = new File("results");
        if (!results.exists()) {
            results.mkdir();
        }
        if (runName != null) {
            final String resultsFileName = "results" + System.getProperty("file.separator") + runName + "_" + srcLang + "_" + trgLang + "_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm").format(new Date()) + ".xml";
            final PrintWriter xmlFile = new PrintWriter(resultsFileName);
            xmlFile.println(this.toXML());
            xmlFile.close();
            System.err.println("Saving results to " + resultsFileName);
        }
        System.out.println(this.toString());
    }
    
    @Override
    public void start() {
        executionTimeTotal = System.currentTimeMillis();
    }

    @Override
    public void end() {
        executionTimeTotal = System.currentTimeMillis() - executionTimeTotal;
    }

    @Override
    public void recordOntologyScore(String ontologyID, String metricName, double metricValue, int ontologySize) {
        addResult(ontologyID, ontologySize, metricName, metricValue);
    }

    @Override
    public void recordTranslation(Translation translation) {
        translations.add(translation);
    }
    
    public void addResult(String id, int labelCount, String metric, double score) {
        if (!results.containsKey(id)) {
            results.put(id, new TreeSet<EvaluationResultEntry>());
            resultsN.put(id, labelCount);
        }
        results.get(id).add(new EvaluationResultEntry(metric, score));
    }

    public void aggregateFolds(Collection<EvaluationResult> foldResults, Language srcLang, Language trgLang) {
        for(EvaluationResult result : foldResults) {
            this.executionTimeTotal += result.executionTimeTotal;
            final Iterator<String> keySetIterator = result.results.keySet().iterator();
            //for(URI err : result.results.keySet()) {
            while(keySetIterator.hasNext()) {
                final String err = keySetIterator.next();
                if(!this.results.containsKey(err)) {
                    this.results.put(err, result.results.get(err));
                    this.resultsN.put(err, result.resultsN.get(err));
                } else {
                    final int aggN = this.resultsN.remove(err);
                    final int newN = result.resultsN.remove(err);
                    final Set<EvaluationResultEntry> oldR = this.results.remove(err);
                    final Set<EvaluationResultEntry> newR = result.results.get(err);
                    keySetIterator.remove();//newR = result.results.remove(err);
                    if(oldR.size() != newR.size()) {
                        throw new IllegalArgumentException("Size of results for a row not equal!");
                    }
                    final TreeSet<EvaluationResultEntry> newResults = new TreeSet<EvaluationResultEntry>();
                    final Iterator<EvaluationResultEntry> oldIter = oldR.iterator();
                    final Iterator<EvaluationResultEntry> newIter = newR.iterator();
                    while(oldIter.hasNext() && newIter.hasNext()) {
                        final EvaluationResultEntry oldERE = oldIter.next();
                        final EvaluationResultEntry newERE = newIter.next();
                        newResults.add(new EvaluationResultEntry(oldERE.metricName, (oldERE.score * aggN + newERE.score * newN) / (aggN + newN)));
                    }
                    results.put(err, newResults);
                    resultsN.put(err, aggN+newN);
                }
                
            }
        }
    }
    
    public void setExecutionTimeTotal(long ExecutionTimeTotal) {
        this.executionTimeTotal = ExecutionTimeTotal;
    }

    private String escapeXMLLiteral(String s) {
        return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;");
    }
    
    public String toXML() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<results time=\"").append(sdf.format(new Date())).append("\" sourceLanguage=\"").append(srcLang).append("\" targetLanguage=\"").append(trgLang).append("\" executionTime=\"").append(executionTimeTotal).append("\">\n");
        for (String id : results.keySet()) {
            sb.append("\t<ontology id=\"").append(id).append("\" labelCount=\"").append(resultsN.get(id)).append("\">\n");
            for (EvaluationResultEntry ere : results.get(id)) {
                sb.append("\t\t<result metric=\"").append(ere.metricName).append("\" score=\"").append(ere.score).append("\"/>\n");
            }
            sb.append("\t</ontology>\n");
        }
        sb.append("\t<summary labelCount=\"").append(total()).append("\">\n");
        final Map<String, Double> averages = averages();
        for(String metricName : averages.keySet()) {
            sb.append("\t\t<result metric=\"").append(metricName).append("\" score=\"").append(averages.get(metricName)).append("\"/>\n");
        }
        sb.append("\t</summary>\n");
        sb.append("\t<translations>\n");
        for(Translation translation : translations) {
            sb.append("\t\t<translation");
            if(translation.getEntity() != null) {
                sb.append(" of=\"").append(translation.getEntity().toString()).append("\"");
            }
            sb.append(">\n");
            sb.append("\t\t\t<src>").append(escapeXMLLiteral(translation.getSourceLabel().asString())).append("</src>\n");
            sb.append("\t\t\t<trg>").append(escapeXMLLiteral(translation.getTargetLabel().asString())).append("</trg>\n");
            sb.append("\t\t</translation>\n");
        }
        sb.append("\t</translations>\n");
        //EvaluateLogger.writeXML(sb, 1);
        sb.append("</results>\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final DecimalFormat df = new DecimalFormat("0.0000");
        if (results.isEmpty()) {
            return "No results";
        } else {
            final Set<EvaluationResultEntry> eres = results.values().iterator().next();
            sb.append("                               ");
            for (EvaluationResultEntry ere : eres) {
                sb.append(ere.metricName).append("\t");
            }
            sb.append("N\n");
        }
        for (String err : results.keySet()) {
            sb.append(prettyStr(err, 30)).append(" ");
            for (EvaluationResultEntry ere : results.get(err)) {
                sb.append(df.format(ere.score)).append("\t");
            }
            sb.append(resultsN.get(err));
            sb.append("\n");
        }
        final Map<String, Double> averages = averages();
        sb.append("TOTAL                          ");
        for(String metricName : averages.keySet()) {
            sb.append(df.format(averages.get(metricName))).append("\t");
        }
        sb.append(total());
        sb.append("\n");
        return sb.toString();
    }

    private int total() {
        int n = 0;
        for (String err : results.keySet()) {
            n += resultsN.get(err);
        }
        return n;
    }

    private Map<String,Double> averages() {
        if (results.isEmpty()) {
            return Collections.EMPTY_MAP;
        } else {
            final Set<EvaluationResultEntry> eres = results.values().iterator().next();
            Map<String,Double> averages = new TreeMap<String, Double>();
            int n = 0;
            for (String err : results.keySet()) {
                for (EvaluationResultEntry ere : results.get(err)) {
                    if(!averages.containsKey(ere.getMetricName())) {
                        averages.put(ere.getMetricName(), ere.getScore() * resultsN.get(err));
                    } else {
                        averages.put(ere.getMetricName(),averages.get(ere.getMetricName())+ere.getScore() * resultsN.get(err));
                    }
                }
                n += resultsN.get(err);
            }
            for(String metricName : averages.keySet()) {
                averages.put(metricName, averages.get(metricName) / n);
            }
            return averages;
        }
    }

    private static String prettyURI(URI uri, int n) {
        return prettyStr(uri == null ? "null" : uri.toString(),n);
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

    public class EvaluationResultEntry implements Comparable<EvaluationResultEntry> {

        private final String metricName;
        private final double score;

        public EvaluationResultEntry(String metricName, double score) {
            this.metricName = metricName;
            this.score = score;
        }

        public String getMetricName() {
            return metricName;
        }

        public double getScore() {
            return score;
        }

        @Override
        public int compareTo(EvaluationResultEntry o) {
            return metricName.compareTo(o.metricName);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final EvaluationResultEntry other = (EvaluationResultEntry) obj;
            if ((this.metricName == null) ? (other.metricName != null) : !this.metricName.equals(other.metricName)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 83 * hash + (this.metricName != null ? this.metricName.hashCode() : 0);
            return hash;
        }
    }
}
