/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.controller.webservice;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author tobwun
 */
public class TestRESTTranslationService {
    
	@Test public void testNothing() {}
	
   // @Test
    public void TestService() throws FileNotFoundException, UnsupportedEncodingException, IOException {
        
        // service url
        String MONNET_SERVICE = "http://monnet01.sindice.net:8888/translate";
        
        // read params: ontology as string
        Scanner scanner = new Scanner(new File("data/finance.small.rdf"));
        String ontologyAsString = "";
        while (scanner.hasNextLine()) {
            ontologyAsString = ontologyAsString + scanner.nextLine() + "\n";
        }
        
        // http post with params
        HttpPost httpPost = new HttpPost(MONNET_SERVICE);
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("ontology", ontologyAsString));
        nameValuePairs.add(new BasicNameValuePair("n-best", "5"));
        nameValuePairs.add(new BasicNameValuePair("target-language", "de"));
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        
        // do post and get result
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(httpPost);
        InputStream is = response.getEntity().getContent();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = in.readLine()) != null) {
            System.out.println(line);
        }
        
        // 1 - test not null
        //Assert.assertNotNull(line);
        
        // 2 - test valid html
        // TODO:
        
        // 3 - test valid results
        // TODO:
        
    }
}
