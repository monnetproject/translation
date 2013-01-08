package eu.monnetproject.translation.sources.dbpedia;

import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * 
 * @author tnguyen
 *
 */

public class ResultsParser {

	private Vector<String> resultList;
	private Document dom;
	private String urlRequest;

	public ResultsParser(String request) {
		resultList = new Vector<String>();
		this.urlRequest = request;
		parseXmlFile();
		if (dom!=null) 
			parseDocument();
	}

	public Vector<String> getResults() {
		return resultList;
	}

	private void parseXmlFile() {
		//get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();		
		// DONT CATCH EXCEPTIONS IF they kill the program anyways!!!
		try {			
			//Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();			
			//parse using builder to get DOM representation of the XML file
			dom = db.parse(urlRequest);	
		} 
		catch(Exception e) {
			System.out.print(e);
		}
	}

	private void parseDocument() {
		Element docEle = dom.getDocumentElement();
		NodeList nl = docEle.getElementsByTagName("Result");		
		if(nl != null && nl.getLength() > 0) {
			for(int i=0 ;i<nl.getLength();i++) {
				Element el = (Element)nl.item(i);
				String uri = getTextValue(el,"URI");				
				if (uri != null) {
					resultList.add(uri);
				}
			}
		}		
	}

	private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);		
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			if (el==null || el.getFirstChild()==null) return "";
			else return el.getFirstChild().getNodeValue();
		}
		return textVal;
	}

}


