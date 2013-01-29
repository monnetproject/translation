package eu.monnetproject.translation.sources.iate;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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

public class IATESource implements TranslationSource {
	private final Language srcLang, trgLang;
	private String[] contexts;

	public IATESource(Language srcLang, Language trgLang, Properties config) {
		this.srcLang = srcLang;
		this.trgLang = trgLang;
		contexts = config.getProperty("domains").split(";");
	}

	@Override
	public String getName() {
		return "IATE";
	}

	@Override
	public void close() {	
	}

	@Override
	public PhraseTable candidates(Chunk chunk) {
		Set<String> translations = new HashSet<String>();
		for(String context : contexts) 	{
			Set<String> translationsFromIATE = getTranslations(chunk.getSource(), context);
			if(translationsFromIATE!=null)
				translations.addAll(translationsFromIATE);
		}
		return new PhraseTableImpl(translations, chunk.getSource(), srcLang, trgLang, getName());
	}


	@Override
	public String[] featureNames() {
		return new String[] { };
	}

	private Set<String> getTranslations(String label, String domain){
		Set<String> translations = new HashSet<String>();
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
				//System.out.println(nl.getLength());

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
							translations.add(trgLabels.get(l).toLowerCase().trim());								
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
		return translations;
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
		IATESource iate = new IATESource(Language.ENGLISH, Language.SPANISH, config);
		//	String allDomain = "all";
		String domains = "10;12;16;20;24;28;32";
		String[] split = domains.split(";");

		//	Set<String> allTranslations = iate.getTranslations(word, allDomain);
		List<String> domainTranslations = new ArrayList<String>();

		for(String domain : split) { 
			if(domain !=null) 
				domainTranslations.addAll(iate.getTranslations(word, domain));					
		}		
		int i =0;
		for(String translation : domainTranslations) {
			System.out.println(translation);		
		} 
		System.out.println(i);
	}

}






