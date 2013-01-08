package eu.monnetproject.translation.evaluation.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.sun.jersey.client.urlconnection.HttpURLConnectionFactory;

public class ControlledHttpURLConnectionFactory implements HttpURLConnectionFactory{

	private HttpURLConnection last;
	
	public ControlledHttpURLConnectionFactory() {
	}
	
	@Override
	public HttpURLConnection getHttpURLConnection(final URL url) throws IOException {
		last=(HttpURLConnection)url.openConnection();
		return last;
	}
	
	public HttpURLConnection getLastConnection() {
		return last;
	}
}
