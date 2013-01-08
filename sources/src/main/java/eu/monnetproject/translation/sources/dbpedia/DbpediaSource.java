package eu.monnetproject.translation.sources.dbpedia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.hp.hpl.jena.rdf.model.RDFNode;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.sources.common.Parameters;
import eu.monnetproject.translation.sources.iate.PhraseTableImpl;
import eu.monnetproject.translation.monitor.Messages;
import eu.monnetproject.translation.TranslationSource;

public class DbpediaSource  implements TranslationSource {

	//private static final String DBPEDIA_ENDPOINT =  "http://dbpedia.org/sparql";

	private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	private static final String DBONT = "http://dbpedia.org/ontology/";

	private static final String LOOKUP_BEGIN = "http://lookup.dbpedia.org/api/search.asmx/KeywordSearch?QueryString=";
	private static final String LOOKUP_END = "&QueryClass=&MaxHits=50";

	private Language srcLang;
	private Language trgLang;

	private String _request = "";

	public DbpediaSource(Language srcLang, Language trgLang){
		this.srcLang = srcLang;
		this.trgLang = trgLang;
		start();
	}	

	public void start() {
		Parameters.setParameters(Configurator.getConfig("eu.monnetproject.translation.sources.dbpedia"));
	}

	private List<String> getTranslations(String label) {
		List<String> candidateTranslations = new ArrayList<String>();		
		List<String> endpoints = getRemoteSPARQLEndpointsFromPool();
		for (String endpoint : endpoints) {	    		    	
			List<String> urls = new ArrayList<String>();	
			if (srcLang == Language.ENGLISH)
				urls = getURLsFromEnglishLabel(label);
			if (urls.size()==0)
				urls = getURLsFromLabel(label, srcLang.getIso639_1(), endpoint);				
			for (int i=0; i<urls.size(); i++) {
				String l = getLabel(urls.get(i),trgLang.getIso639_1(), endpoint);
				if (l!="" && !candidateTranslations.contains(l) ) {
					candidateTranslations.add(l);
				}
			}
		}
		return candidateTranslations;
	}


	private List<String> getURLsFromLabel(String term, String lang, String endpoint) {
		String queryString;
		List<String> urls = new ArrayList<String>();
		//	Vector<RDFNode> queryResult = new Vector<RDFNode>();			
		queryString = 
				"PREFIX rdfs: <" + RDFS + "> " +
						"SELECT ?x " +
						"WHERE { ?x rdfs:label ?label. " +
						" FILTER ( ?label = \"" + term + "\"@" + lang  + ")"+
						"      }";
		List<RDFNode> queryResult = QuerySparqlEndpoint.executeQuery(endpoint,queryString);
		for (int i=0; i<queryResult.size(); i++){
			urls.add(queryResult.get(i).toString());
		}
		return urls;
	}

	private String getLabel(String url, String lang, String endpoint) {
		String queryString;
		List<RDFNode> queryResult = new ArrayList<RDFNode>();
		String redirect = getRedirect(url, endpoint);

		//		if redirection exists, the url is change 
		if (redirect != ""){
			url = redirect;
		}
		queryString = 
				"PREFIX rdfs: <" + RDFS + "> " +
						"SELECT ?x " +
						"WHERE {" +
						"      <" + url + "> rdfs:label ?x. " +
						" FILTER langMatches( lang(?x), '" + lang + "')" +
						"      }";


		queryResult = QuerySparqlEndpoint.executeQuery(endpoint,queryString);

		if (!queryResult.isEmpty()){ 
			RDFNode nodo = queryResult.get(0);			
			return nodo.asNode().getLiteral().getLexicalForm();

		}
		else return "";
	}


	private String setRequest(String label) {
		_request = LOOKUP_BEGIN + label.replace(" ", "%20") + LOOKUP_END;		
		return _request;
	}


	public Vector<String> getURLsFromEnglishLabel(String label) {
		ResultsParser rp = new ResultsParser(setRequest(label));
		Vector<String> rl = rp.getResults();
		return rl;
	}


	private String getRedirect(String url, String endpoint) {
		String queryString;
		List<RDFNode> queryResult = new ArrayList<RDFNode>();
		queryString = 
				"PREFIX dbpedia-owl: <" + DBONT + "> " +
						"PREFIX rdfs: <" + RDFS + "> " +
						"SELECT ?x " +
						"WHERE {" +
						" <" +  url + "> dbpedia-owl:wikiPageRedirects ?x . "  +
						"      }";
		queryResult = QuerySparqlEndpoint.executeQuery(endpoint,queryString);

		if (!queryResult.isEmpty()) 
			return queryResult.get(0).toString();	
		else 
			return "";
	}




	private List<String> getRemoteSPARQLEndpointsFromPool(){
		List<String> uris = new ArrayList<String>();
		FileReader fileReader;
		File fileIN = new File(Parameters.RemoteSPARQLEndpointsFile);
		BufferedReader buffFileIN = null;
		try {
			fileReader = new FileReader(fileIN);
			buffFileIN = new BufferedReader(fileReader);
			String uri = null;
			while((uri=buffFileIN.readLine())!=null) {
				if (uri!=null && !uri.startsWith("#")){
//					log.info("ADDED REMOTE SPARQL ENDPOINT " + uri);
					uris.add(uri);	
				}				
			}
			buffFileIN.close();			
		} catch (IOException e) {
			Messages.severe(e.getMessage());
		} 
		return uris;
	}	


	public static void main(String[] args) {
		DbpediaSource db = new DbpediaSource(Language.ENGLISH, Language.SPANISH);
		List<String> translations = db.getTranslations("Spain");
		for(String translation : translations) {
			System.out.println(translation);
		}
	}

//	@Override
	public int featureCount() {
		return 0;
	}

    @Override
    public String[] featureNames() {
        return new String[] { };
    }
        
        

	@Override
	public PhraseTable candidates(Chunk label) {
		List<String> translations = getTranslations(label.getSource());
		return new PhraseTableImpl(translations, label.getSource(), srcLang, trgLang, getName());
	}

	@Override
	public String getName() {
		return "DBpedia";
	}

	@Override
	public void close() {
	}	

}
