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
 ********************************************************************************
 */
package eu.monnetproject.translation.evaluation.evaluate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.evaluation.util.RestHelper;
import eu.monnetproject.translation.monitor.TranslationMonitor;

/**
*
* @author Miguel Angel Garcia
*
*/

public class SealsEvaluationResult implements TranslationMonitor {

	private final HashMap<String, Set<EvaluationResultEntry>> results = new HashMap<String, Set<EvaluationResultEntry>>();
    private final HashMap<String, Integer> resultsN = new HashMap<String, Integer>();
    private final ArrayList<Translation> translations = new ArrayList<Translation>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    final Language srcLang, trgLang;
    private final String runName;
    private long executionTimeTotal;
    private final String resourceLocator;
    private final String author;
    private final String description;
    
    public SealsEvaluationResult(Language srcLang, Language trgLang, String runName, String resourceLocator, String author, String description) {
        this.srcLang = srcLang;
        this.trgLang = trgLang;
        this.runName = runName;
        this.resourceLocator = resourceLocator;
        this.author = author;
        this.description = description;
    }
	
	@Override
	public void recordTranslation(Translation translation) {
		translations.add(translation);
		
	}

	@Override
	public void recordOntologyScore(String ontologyID, String metricName,
			double metricValue, int ontologySize) {
		addResult(ontologyID, ontologySize, metricName, metricValue);
		
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
	public void commit() throws Exception {
		if (!results.isEmpty())
			this.storeServer();
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
	
	public void addResult(String id, int labelCount, String metric, double score) {
        if (!results.containsKey(id)) {
            results.put(id, new TreeSet<EvaluationResultEntry>());
            resultsN.put(id, labelCount);
        }
        results.get(id).add(new EvaluationResultEntry(metric, score));
    }

    public void aggregateFolds(Collection<SealsEvaluationResult> foldResults, Language srcLang, Language trgLang) {
        for(SealsEvaluationResult result : foldResults) {
            this.executionTimeTotal += result.executionTimeTotal;
            final Iterator<String> keySetIterator = result.results.keySet().iterator();
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
                    keySetIterator.remove();
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
    
    public String storeServer() {
    	final StringBuilder sb = new StringBuilder();        
        String rawResultsId = UUID.randomUUID().toString();        
        Map<String, String> ontoIds = new HashMap<String, String>();
        sb.append("<rdf:RDF\n");
        sb.append("    xmlns:monnetmetadata=\"http://www.seals-project.eu/ontologies/MonnetMetadata.owl#\"\n");
        sb.append("    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n");
        sb.append("    xmlns:seals=\"http://www.seals-project.eu/ontologies/SEALSMetadata.owl#\"\n");
        sb.append("    xmlns:dc=\"http://purl.org/dc/terms/\"\n");
        sb.append("    xmlns:monnet=\"http://www.seals-project.eu/ontologies/MonnetSuite.owl#\"\n");
        sb.append("    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\" >\n");
        sb.append("<rdf:Description rdf:about=\"http://www.monnet-project.eu/rawresult#").append(rawResultsId).append("\">\n");
        sb.append("    <seals:hasSourceLanguage>").append(srcLang).append("</seals:hasSourceLanguage>\n");
        sb.append("    <seals:hasTargetLanguage>").append(trgLang).append("</seals:hasTargetLanguage>\n");
        sb.append("    <monnetmetadata:TimeExecution>").append(executionTimeTotal).append("</monnetmetadata:TimeExecution>\n");
        sb.append("    <dc:identifier>").append(rawResultsId).append("</dc:identifier>\n");
        for (String id:results.keySet()){
        	String ontoId = UUID.randomUUID().toString();
        	ontoIds.put(id, ontoId);
        	sb.append("    <monnetmetadata:hasRawOntologyItem rdf:resource=\"http://www.monnet-project.eu/rawontologyitem#").append(ontoId).append("\"/>\n");
        }
        sb.append("</rdf:Description>\n");
        for (String id:results.keySet()){
        	String ontoId = ontoIds.get(id);
        	sb.append("<rdf:Description rdf:about=\"http://www.monnet-project.eu/rawontologyitem#").append(ontoId).append("\">\n");
        	Map<String, String> evalResIds = new HashMap<String, String>();
        	sb.append("    <monnetmetadata:OntologyID>").append(id).append("</monnetmetadata:OntologyID>\n");
        	sb.append("    <monnetmetadata:LabelCount>").append(resultsN.get(id)).append("</monnetmetadata:LabelCount>\n");
        	for(EvaluationResultEntry ere : results.get(id)){
        		String ereId = UUID.randomUUID().toString();
        		evalResIds.put(ere.getMetricName(), ereId);
        		sb.append("    <monnetmetadata:hasMetricItem rdf:resource=\"http://www.monnet-project.eu/metricitem#").append(ereId).append("\"/>\n");
        	}
        	sb.append("</rdf:Description>\n");
        	for (EvaluationResultEntry ere : results.get(id)){
        		String ereId = evalResIds.get(ere.getMetricName());  
        		sb.append("<rdf:Description rdf:about=\"http://www.monnet-project.eu/metricitem#").append(ereId).append("\">\n");
        		sb.append("    <monnetmetadata:hasEvaluationMetricName>").append(ere.getMetricName()).append("</monnetmetadata:hasEvaluationMetricName>\n");
        		sb.append("    <monnetmetadata:hasEvaluationMetricValue>").append(ere.getScore()).append("</monnetmetadata:hasEvaluationMetricValue>\n");
        		sb.append("</rdf:Description>\n");
        	}
        }
        sb.append("</rdf:RDF>");
        String metadataFile = toCreateMetadataFile(rawResultsId);
        System.out.println(metadataFile);
        String rawResults = sb.toString();
        System.out.println(rawResults);
        String path = System.getProperty("java.io.tmpdir");
    	String fileSeparator = System.getProperty("file.separator");
    	if (!path.endsWith(fileSeparator))
    		path = path + fileSeparator;
        try {
        	
			BufferedWriter out = new BufferedWriter(new FileWriter(path+rawResultsId+".rdf"));
			out.write(rawResults);
			out.close();	
			File zipFile = new File(path+"Metadata.zip");
			if (zipFile.exists())
				zipFile.delete();
			
			ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile));
			InputStream is = new FileInputStream(path+rawResultsId+".rdf");
			ZipEntry zipEntry = new ZipEntry("Metadata.rdf"); 
			
			System.out.println("FileMetadata:"+path+rawResultsId+".rdf");
			System.out.println("FileZip:"+zipFile.getAbsolutePath());
			
			zip.putNextEntry(zipEntry); 			
			byte[] buffer = new byte[2048];
			int byteCount;
		
			while (-1 != (byteCount = is.read(buffer))) {
			zip.write(buffer, 0, byteCount);
			}
			zip.closeEntry();
			is.close(); 
			zip.close();
			File file = new File("metadata.rdf");
			file.delete();
			
			MultiPart multiPart =
					new FormDataMultiPart().
						bodyPart(new FormDataBodyPart("metadata",
													  metadataFile,
													  new MediaType("application","rdf+xml"))).
						bodyPart(new FileDataBodyPart("data",
													  zipFile,
													  new MediaType("application","zip")));
					
					try {
						ClientResponse response=
							RestHelper.
								//getResource("http://localhost:8090/rrs-web/").
								getResource(resourceLocator).
									path("results").
									type(MediaType.MULTIPART_FORM_DATA_TYPE).
									accept("application/rdf+xml").
									post(ClientResponse.class,multiPart);
						
					try {
						Thread.sleep(5000);
					} catch (InterruptedException ex) {

					}
						
						Status status = response.getClientResponseStatus();
						String body = response.getEntity(String.class);
						String message = "";
								
						if (status.equals(Status.CREATED)){
							message="SUCCESS ["+status+ "("+status.getStatusCode()+"]: "+//RestHelper.getFactory().getLastConnection.responseMessage}.\n"+
						    "Version published at "+response.getHeaders().get((Object)"Content-Location")+"\\n"+
							"Persistent test data version metadata:\n"+body;
							System.out.println(message);
//							deleteFile(file);
//							deleteFile(dataFilename);						
						}
						
					} catch (UniformInterfaceException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();

					} catch (ClientHandlerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();

					} 
		}
		catch (IOException e)
		{
			System.out.println("Exception ");		
		} finally {
			File newFile = new File(path+"Metadata.zip");
			if (newFile.exists())
				newFile.delete();
			File rdfFile = new File(path+rawResultsId+".rdf");
			if (rdfFile.exists())
				rdfFile.delete();
		}
        
        return sb.toString();
    }
    
    public String toCreateMetadataFile(String id){
    	final StringBuilder sb = new StringBuilder();
    	sb.append("<rdf:RDF\n");
    	sb.append("    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n");
    	sb.append("    xmlns:seals=\"http://www.seals-project.eu/ontologies/SEALSMetadata.owl#\"\n");
    	sb.append("    xmlns:dc=\"http://purl.org/dc/terms/\"\n");
    	sb.append("    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\">\n");
    	sb.append("  <rdf:Description rdf:about=\"http://www.monnet-project.eu/result#").append(id).append("4cf66441-c470-488e-8e1e-06b347ce9383\">\n");
    	sb.append("    <dc:identifier>").append(id).append("</dc:identifier>\n");
    	sb.append("    <dc:creator>").append(author).append("</dc:creator>\n");
    	sb.append("    <dc:description>").append(description).append("</dc:description>\n");
    	sb.append("    <seals:hasTestDataCategory>Ontology localization</seals:hasTestDataCategory>\n");
    	sb.append("    <seals:hasName>Evaluation translation</seals:hasName>\n");
    	sb.append("    <rdf:type rdf:resource=\"http://www.seals-project.eu/ontologies/SEALSMetadata.owl#RawResult\"/>\n");
    	sb.append("  </rdf:Description>\n");
    	sb.append("</rdf:RDF>\n");
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
