package eu.monnetproject.translation.sources.free;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.TranslationSource;


/**
 * @author J. Mauricio
 *
 */
public abstract class OnlineServiceHTTPTranslator implements TranslationSource {

	/** Returns a list of supported translations **/ 
	abstract protected Collection<LexicalRelation> getSupportedTranslations();

	/** Returns URL to web resource that will perform translation */
	abstract protected String getURL(String translation);

	/** Returns POST parameters to web site to do translation */
	abstract protected String getParams(Language srcLanguage, Language dstLanguage, String text);

	/** HTML Open Tag that contains translation */
	abstract protected String getStartSearchText();

	/** HTML Close Tag that contains translation */
	abstract protected String getEndSearchText();

	/** Unicode character set for input (i.e. UTF-8) */
	abstract protected String getInputCharSet();

	/** Unicode character set for output (i.e. UTF-8) */
	abstract protected String getOutputCharSet();

	/** Error message displayed by server when it is busy */
	abstract protected String getServerBusyError();


	/** Get's translation from either memory cache, otherwise from website if
	 *  not found in memory
	 * 
	 * @param label Text in Source Language to translate
	 * @return Collection<Translation> translated text
	 */
	public Set<String> translate(String srcText, Language srcLang, Language tgtLang) {
		Set<String> translations = new HashSet<String>();
		String translationResult = webTranslate(srcLang, tgtLang, srcText);
		translations.add(translationResult);
		return translations;
	}

	/** Translates text to destination language using online web
	 *  language translator
	 * 
	 * @param srcLocal Source locale to translate from
	 * @param dstLocale Destination locale to translate to
	 * @param text Text in srcLocale to translate
	 * @return Translated text or null if translation was not possible
	 */
	private final String webTranslate(Language srcLanguage, Language tgtLanguage, String text) {

		if((text==null) || (text.equals(""))) {
			return null;
		}

		// Input string is assumed to be english, so if destination is also
		// english, just return the input string
		if(tgtLanguage.equals(srcLanguage)) {
			return text;
		}

		// Manage the supported translations
		Collection<LexicalRelation> supportedTranslations = getSupportedTranslations();
		if (!supportedTranslations.contains(TranslationRelationImpl.getInstance(srcLanguage, tgtLanguage))) {
			return null;
		}

		// On server busy, retry will be true
		boolean retry;
		int maxRetry = 1;

		// String to hold translation
		String translation=null;

		do {
			retry = false;

			// HTTP Connection to Web Translator

			HttpURLConnection connection = null;

			// Returned translated document from Web Site
			String htmlDoc = "";

			try {
				// Get form parameters
				String params = getParams(srcLanguage, tgtLanguage, text);
				// Open connection to server
				URL server = new URL(getURL(srcLanguage.toString() + "2" + tgtLanguage.toString()));
				//URL server = new URL("http://translate.google.com/?text=Hallo+Welt!&langpair=de|fr&hl=en&ie=UTF-8&oe=UTF-8&submit=translate#");
				connection = (HttpURLConnection)server.openConnection();

				//	post the parameters
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setConnectTimeout(3000);
				byte[] parameterAsBytes = params.getBytes();

				// Some web services refuses connection from Java Client, so pretend we are IE
				connection.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.0.3705; .NET CLR 1.1.4322)");
				OutputStream wr =connection.getOutputStream(); 
				wr.write(parameterAsBytes);
				wr.flush();
				wr.close();

				// now let's get the results
				connection.connect();
				int responseCode = connection.getResponseCode();
				if(responseCode == 200) {
					InputStream in = connection.getInputStream();
					BufferedReader inReader = new BufferedReader(new InputStreamReader(in, getOutputCharSet()));
					String line;
					while((line=inReader.readLine())!=null) {
						htmlDoc += line + "\n";
					}
				} else {
					System.err.println("Invalid response code: " + responseCode +
							", " + this.getClass().getName());
				}
			}
			catch (IOException e) {
				htmlDoc = getServerBusyError();
			}
			catch (Exception e) {
				e.printStackTrace();
				htmlDoc = "";
			}
			finally {
				// Close connection
				if(connection != null) {
					connection.disconnect();
				}
			}


			// If HTML Document is empty, translation didn't work
			if(!htmlDoc.equals("")) {
				// Find the translated string in the HTML Document
				int startIndex = htmlDoc.indexOf(getStartSearchText());
				int endIndex = htmlDoc.indexOf(getEndSearchText());
				if((startIndex == -1) || (endIndex == -1)) {
					// Is the server busy?
					if((maxRetry > 0) && 
							(htmlDoc.indexOf(getServerBusyError()) != -1)) {
						retry = true;
						maxRetry--;
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {

						}
					} else {
						//new Exception("Index out of range, unknown error.").printStackTrace();
					}
				} else {
					translation = htmlDoc.substring(
							//htmlDoc.lastIndexOf(getStartSearchText()),
							htmlDoc.indexOf(getStartSearchText()), 
							htmlDoc.indexOf(getEndSearchText(), startIndex));
					translation = translation.substring(translation.indexOf(">") + 1);

					// Decode HTML Tag Codes  (ie. &lt; is "<")
					translation = HTMLDecoder.decode(translation);

					//LabelTokeniser tk = new LabelTokeniser();
					//List<Token> tokens = tk.tokenize(translation);

				}
			}
		} while(retry);

		return translation;
	}


//	public List<Pair<Token, Collection<Translation>>> translate(List<Token> tokens, Language sourceLang, Language targetLang) {
//
//		SortedMap<Token,Collection<Translation>> results = new TreeMap<Token,Collection<Translation>>();
//
//		//perform the translation
//		Iterator<Token> it = tokens.iterator();
//		while (it.hasNext()) {
//			Token token = it.next();
//
//			String label = token.getValue();
//			String translationResult = webTranslate(sourceLang, targetLang, label);
//
//			//source information
//			List<TranslationSource> source = new ArrayList<TranslationSource>();
//			source.add(new TranslationSourceImpl(getName()));
//
//
//			List<Translation> translations = new ArrayList<Translation>();
//			translations.add(new TranslationImpl(label, translationResult, sourceLang, targetLang, source));
//
//			results.put(token, translations);
//
//		}
//		return null; //results;
//
//	}
	public static void main(String[] args) {
	//	OnlineServiceHTTPTranslator  on = new OnlineServiceHTTPTranslator();
	}

}
