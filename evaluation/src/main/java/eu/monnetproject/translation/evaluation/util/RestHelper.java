package eu.monnetproject.translation.evaluation.util;

import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.tidy.Tidy;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.urlconnection.HttpURLConnectionFactory;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;

public class RestHelper {
	
	public String tidy(String content) {
		String body=content;
		if(!content.startsWith("<!DOCTYPE")) {
			body="<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\\n\\" + body;
		}
	
		StringWriter writer = new StringWriter();
		Tidy tidy = new Tidy();
		tidy.setQuiet(true);
		tidy.setTidyMark(false);
		tidy.setXHTML(true);
		tidy.setWraplen(0);
		tidy.setWrapAttVals(false);
		tidy.setIndentContent(true);
		tidy.setSpaces(4);
		tidy.parse(new StringReader(body),writer);		

		String sb = writer.getBuffer().toString();
	
		return sb;
	}
	
	public String getFailure(String content) {
		String body=tidy(content);
		String output = "";

		GPathResult html;
		try {
			XmlSlurper slurper = new XmlSlurper();
			slurper.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			Map<String, String> params = new HashMap<String, String>();
			params.put("xhtml:", "http://www.w3.org/1999/xhtml");
			html = slurper.parseText(body).declareNamespace(null);
			output = html.text().trim();
		} catch (SAXNotRecognizedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	}
	
	static ControlledHttpURLConnectionFactory factory=new ControlledHttpURLConnectionFactory();

	public static HttpURLConnectionFactory getFactory() {
		return factory;
	}

	public static WebResource getResource(String location) {

		return new Client(new URLConnectionClientHandler(getFactory())).resource(location);
	}

}