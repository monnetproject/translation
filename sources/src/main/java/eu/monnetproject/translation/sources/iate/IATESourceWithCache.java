package eu.monnetproject.translation.sources.iate;


import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.sources.cache.NRTCacheIndexer;
import eu.monnetproject.translation.sources.common.Pair;
import eu.monnetproject.translation.util.CLSim;

public class IATESourceWithCache implements TranslationSource {
	private final Language srcLang, trgLang;
	private Properties config;
	private NRTCacheIndexer cacheIndexer;
	private String[] contexts;
	private PrintWriter cacheLog;

	public IATESourceWithCache(Language srcLang, Language trgLang, Properties config) {
		this.srcLang = srcLang;
		this.trgLang = trgLang;
		this.config = config;
		cacheIndexer = new NRTCacheIndexer(this.config, srcLang, trgLang, false);
		contexts = config.getProperty("domains").split(";");
		if(config.containsKey("cacheLogPath")) 
			openLog(config.getProperty("cacheLogPath"));		
	}

	private void openLog(String filePath) {
		try {
			cacheLog = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)), true);	
		} catch (IOException e) {
			e.printStackTrace();
			cacheLog = null;
			return;
		}
	}

	@Override
	public String getName() {
		return "IATE";
	}

	@Override
	public void close() {		
		cacheLog.close();
		cacheIndexer.close();
	}

	@Override
	public PhraseTable candidates(Chunk chunk) {
		Set<String> translations = new HashSet<String>();
		Set<String> cacheResults = null;	
		for(String context : contexts) {	
			cacheResults = cacheIndexer.getTranslations(chunk.getSource(), "domain" + context.trim());			
			if(cacheResults == null) {
				List<Pair<String, String>> translationsWithContext = new ArrayList<Pair<String, String>>();
				Set<Pair<String, String>> translationsFromIateWS = getTranslations(chunk.getSource(), context);
				if(translationsFromIateWS!=null){
					translationsWithContext.addAll(translationsFromIateWS);
					if(translationsWithContext.size()==0) {
						cacheIndexer.cache(chunk.getSource(), "koitranslationnahihaiiskaiatepe", "domain" + "all", getName());
						if(cacheLog!=null)
							cacheLog.println(chunk.getSource().replace("\n", "").trim()+"\t::::\t"+"koitranslationnahihaiiskaiatepe".trim() + "\t:::::\t" + srcLang.getIso639_1() +"-"+trgLang.getIso639_1());																		
					}
					for(Pair<String, String> translationWithContext : translationsWithContext) {
						String translation = translationWithContext.getFirst();
						String retrievedContext = translationWithContext.getSecond();
						int retrievedContexNo = Integer.parseInt(retrievedContext);					
						if(retrievedContexNo/10 < 1) 
							retrievedContext = "0" + retrievedContexNo;					
						translations.add(translation);						
						if(retrievedContext.equalsIgnoreCase("00")) 
							continue;					
						cacheIndexer.cache(chunk.getSource(), translation.trim(), "domain" + retrievedContext.trim(), getName());
						if(cacheLog!=null)
							cacheLog.println(chunk.getSource().replace("\n", "").trim()+"\t::::\t"+translation.replace("\n", "").trim() + "\t:::::\t" + srcLang.getIso639_1() +"-"+trgLang.getIso639_1());																
					}		
				}
			} else {
				if(!(cacheResults.contains("koitranslationnahihaiiskaiatepe"))) 
					translations.addAll(cacheResults);			
			}
		}		
		return new PhraseTableImpl(translations, chunk.getSource(), srcLang, trgLang, getName());
	}


	public PhraseTable indexCandidates(Chunk chunk, CLSim clsim) {
		Set<String> translations = new HashSet<String>();
		Set<String> cacheResults = null;	
		for(String context : contexts) {	
			cacheResults = cacheIndexer.getTranslations(chunk.getSource().toLowerCase().trim(), "domain" + context.trim());			
			if(cacheResults == null) {
				List<Pair<String, String>> translationsWithContext = new ArrayList<Pair<String, String>>();
				Set<Pair<String, String>> translationsFromIateWS = getTranslations(chunk.getSource(), context);
				if(translationsFromIateWS!=null){
					translationsWithContext.addAll(translationsFromIateWS);
					if(translationsWithContext.size()==0) {
						//cacheIndexer.cache(chunk.getSource(), "koitranslationnahihaiiskaiatepe", "domain" + "all", getName());
						if(cacheLog!=null)
							cacheLog.println(chunk.getSource().replace("\n", "").trim()+"\t::::\t"+"koitranslationnahihaiiskaiatepe".trim() + 
									"\t::::\t" + srcLang.getIso639_1() +"-"+trgLang.getIso639_1() 
									+ "\t::::\t -1.0" + "\t::::\tdomain" + "all");																		
					}
				
					boolean atleastOneWritten = false;
					for(Pair<String, String> translationWithContext : translationsWithContext) {						
						String translation = translationWithContext.getFirst();
						String retrievedContext = translationWithContext.getSecond();
						int retrievedContexNo = Integer.parseInt(retrievedContext);					
						if(retrievedContexNo/10 < 1) 
							retrievedContext = "0" + retrievedContexNo;					
						translations.add(translation);						
						if(retrievedContext.equalsIgnoreCase("00")) 
							continue;					
						String text1 = chunk.getSource().replace("\n", "").trim();
						String text2 = translation.replace("\n", "").trim();
						double score = clsim.score(text1, srcLang, text2, trgLang);
						if(score>=0.0) {
							atleastOneWritten = true;
							cacheIndexer.cache(chunk.getSource(), translation.trim(), "domain" + retrievedContext.trim(), getName());
							if(cacheLog!=null)
								cacheLog.println(text1+"\t::::\t"+ text2 + "\t::::\t" + srcLang.getIso639_1() 
										+"-"+trgLang.getIso639_1() + "\t::::\t" + score + "\t::::\t" + 
										"domain" + retrievedContext.trim());													
						} 						
					}	
					if(!atleastOneWritten) {
						//cacheIndexer.cache(chunk.getSource(), "koitranslationnahihaiiskaiatepe", "domain" + "all", getName());
						if(cacheLog!=null)
							cacheLog.println(chunk.getSource().replace("\n", "").trim()+"\t::::\t"+
						"koitranslationnahihaiiskaiatepe".trim() + "\t::::\t" + srcLang.getIso639_1() +"-"+trgLang.getIso639_1() + 
						"\t::::\t -1.0" + "\t::::\tdomain" + "all");																						
					}
				}
			} else {
				if(!(cacheResults.contains("koitranslationnahihaiiskaiatepe"))) 
					translations.addAll(cacheResults);			
			}
		}		
		return new PhraseTableImpl(translations, chunk.getSource(), srcLang, trgLang, getName());
	}


	@Override
	public String[] featureNames() {	
		String[] featureNames = new String[1];
		featureNames[0] = "inIATE";
		return featureNames;
	}

	private Set<Pair<String, String>> getTranslations(String label, String domain){
		Set<Pair<String, String>> translationWithContext = new HashSet<Pair<String, String>>();
		try {
			URL u = new URL("http://iate.europa.eu/iatediff/services/WS");
			URLConnection uc = u.openConnection();
			HttpURLConnection connection = (HttpURLConnection) uc;

			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("SOAPAction", "search");
			OutputStream out = connection.getOutputStream();
			Writer wout = new OutputStreamWriter(out);

			wout.write("<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' xmlns:gen='http://generic.webservice.diffusion.iate.cdt'>\r\n");
			wout.write("<soapenv:Header/>\r\n");
			wout.write("<soapenv:Body>\r\n");
			wout.write("<gen:search>\r\n");
			wout.write("<gen:term>"+label+"</gen:term>\r\n");
			wout.write("<gen:src>"+srcLang+"</gen:src>\r\n");
			wout.write("<gen:trg>"+trgLang+"</gen:trg>\r\n");
			if(domain.equalsIgnoreCase("all"))
				wout.write("<gen:subject></gen:subject>\r\n");
			else 
				wout.write("<gen:subject>" + domain + "</gen:subject>\r\n");					
			wout.write("<gen:from>0</gen:from>\r\n");

			wout.write("<gen:to>50</gen:to>\r\n");
			wout.write("</gen:search>\r\n");
			wout.write("</soapenv:Body>\r\n");
			wout.write("</soapenv:Envelope>\r\n");
			wout.flush();
			wout.close();

			InputStream in = connection.getInputStream();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

			dbf.setValidating(false);
			dbf.setIgnoringComments(false);
			dbf.setIgnoringElementContentWhitespace(true);
			dbf.setNamespaceAware(true);

			DocumentBuilder db = null;

			db = dbf.newDocumentBuilder();

			db.setEntityResolver(new NullResolver());

			Document doc = db.parse(in);
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("soap:Body");
			Node node = list.item(0);
			NodeList list2 = node.getChildNodes();
			Node node2 = list2.item(0);
			if (node2.getNodeType()==Node.ELEMENT_NODE){
				Element element = (Element) node2;
				element.getAttribute("ns1:out");
				Document document = db.parse( new InputSource(  
						new StringReader( getTagValue("ns1:out",element ) ) ) ); 
				document.getDocumentElement().normalize();

				NodeList nl = document.getElementsByTagName("termEntry");

				for(int i=0; i<nl.getLength(); i++) {
					Node termEntry = nl.item(i);
					NodeList children = termEntry.getChildNodes();

					List<String> trgLabels = new ArrayList<String>();
					boolean addLabel = false;
					Element termEntryElement = (Element) termEntry;
					String context = getTagValue("descrip", termEntryElement);
					String[] contexts = context.split(",");
					Set<String> uniqContexts = new HashSet<String>();
					for(String string : contexts) {
						String contextCode;
						if(string.trim().length()<=1) 
							contextCode = string.trim().subSequence(0, 1).toString();
						else 
							contextCode = string.trim().subSequence(0, 2).toString();
						if(!uniqContexts.contains(contextCode)) {
							uniqContexts.add(contextCode);								
						}
					}

					for(int j=0;j<children.getLength();j++) {

						if(children.item(j).getNodeName().equalsIgnoreCase("langSet")) {
							Node langNode = children.item(j);
							Element elem = (Element) langNode;
							String lang = elem.getAttribute("xml:lang");

							if (lang.equals(srcLang.getIso639_1())){
								NodeList tigList = elem.getElementsByTagName("tig");
								for (int k=0;k<tigList.getLength();k++){
									Element elemTig = (Element) tigList.item(k);
									if (label.equalsIgnoreCase(getTagValue("term", elemTig)))
										addLabel = true;
								}
							} else if (lang.equals(trgLang.getIso639_1())){
								NodeList tigList = elem.getElementsByTagName("tig");
								for (int k=0;k<tigList.getLength();k++){
									Element elemTig = (Element) tigList.item(k);	
									trgLabels.add(getTagValue("term", elemTig));
								}
							} 
						}						
					}
					if (addLabel) {
						for (int l=0;l<trgLabels.size();l++)
							for(String thisContext : uniqContexts) {
								Pair<String, String> pair = new Pair<String, String>(trgLabels.get(l).toLowerCase().trim(), thisContext); 
								if(!translationWithContext.contains(pair)){
									translationWithContext.add(pair);
								}
							}
					}
				}
			}
			in.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;		
		} catch (ProtocolException e) {
			e.printStackTrace();
			return null;		
		} catch (IOException e) {
			e.printStackTrace();
			return null;		
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;		
		} catch (SAXException e) {
			e.printStackTrace();
			return null;		
		}		
		return translationWithContext;
	}

	public String getTagValue(String tag, Element elemento) {
		NodeList lista = elemento.getElementsByTagName(tag).item(0).getChildNodes();	 
		Node valor = (Node) lista.item(0);		 
		return valor.getNodeValue();		 
	}

	class NullResolver implements EntityResolver {
		public InputSource resolveEntity(String publicId, String systemId)
				throws SAXException, IOException {
			return new InputSource(new StringReader(""));
		}
	}

	public static void main(String[] args) throws IOException {
		String word = "event";
		String configFile = "load//eu.monnetproject.translation.sources.iate.cfg";
		InputStream is = new FileInputStream(configFile);
		Properties config =  new Properties();
		config.load(is);
		IATESourceWithCache iate = new IATESourceWithCache(Language.ENGLISH, Language.SPANISH, config);
		String allDomain = "all";
		String domains = "10;12;16;20;24;28;32";
		String[] split = domains.split(";");

		Set<Pair<String, String>> allTranslations = iate.getTranslations(word, allDomain);
		List<Pair<String,String>> domainTranslations = new ArrayList<Pair<String, String>>();

		for(String domain : split) { 
			if(domain !=null) 
				domainTranslations.addAll(iate.getTranslations(word, domain));					
		}		
		int i =0;
		for(Pair<String, String> translation : domainTranslations) {
			String text = translation.getFirst();
			String context = translation.getSecond();
			for(Pair<String, String> allT : allTranslations) {
				String text1 = allT.getFirst();
				String context1 = allT.getSecond();				
				if(text.equalsIgnoreCase(text1) && context.equalsIgnoreCase(context1)) {				
					System.out.println(text);
					System.out.println(context);
					i++;
				}
			} 
			System.out.println(i);
		}
	}

}




