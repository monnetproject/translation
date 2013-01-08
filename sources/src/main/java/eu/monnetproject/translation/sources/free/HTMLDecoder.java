package eu.monnetproject.translation.sources.free;

import java.util.logging.Logger;

/** Translates the codes in an HTMLString to it's value (ie. "&lt;" becomes "<") 
 *  This class should be replaced with an XML parser...
 */
public class HTMLDecoder {
	private static final Logger logger = 
		Logger.getLogger(HTMLDecoder.class.getName());
	/** Decodes HTML String (ie. "&lt;" becomes "<")
	 * 
	 * @param htmlString html string
	 * @return decoded values
	 */
	public final static String decode(String htmlString) {
		logger.entering(HTMLDecoder.class.getName(), "decode", htmlString);
		if(htmlString!=null) {
			htmlString = htmlString.replaceAll("&lt;", "<");
			htmlString = htmlString.replaceAll("&gt;", ">");
			htmlString = htmlString.replaceAll("&quot;", "\"");
			htmlString = htmlString.replaceAll("&amp;", "&");
			htmlString = htmlString.replaceAll("&lsquo;", "�");
	
			htmlString = htmlString.replaceAll("&#39;", "'");
		}		
		logger.exiting(HTMLDecoder.class.getName(), "decode", htmlString);
		return htmlString;
	}

}
