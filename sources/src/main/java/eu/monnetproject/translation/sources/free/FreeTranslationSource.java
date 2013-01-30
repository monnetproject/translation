package eu.monnetproject.translation.sources.free;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.sources.cache.NRTCacheIndexer;
import eu.monnetproject.translation.sources.iate.PhraseTableImpl;



/** FreeTranslation.com Translator Service.  Uses FreeTranslation's web translator
 *  to translate text at run-time.  Translations are stored in cache based in Lucene index
 */
public class FreeTranslationSource extends OnlineServiceHTTPTranslator {

	private NRTCacheIndexer cacheIndexer;
	static List<LexicalRelation> supportedRelations = new LinkedList<LexicalRelation>();
	public Collection<LexicalRelation> getSupportedTranslations() {
		if(supportedRelations.isEmpty()) {
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.ENGLISH, Language.CHINESE));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.ENGLISH, Language.DUTCH));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.ENGLISH, Language.FRENCH));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.ENGLISH, Language.GERMAN));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.ENGLISH, Language.ITALIAN));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.ENGLISH, Language.PORTUGUESE));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.ENGLISH, Language.RUSSIAN));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.ENGLISH, Language.SPANISH));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.FRENCH, Language.ENGLISH));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.GERMAN, Language.ENGLISH));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.ITALIAN, Language.ENGLISH));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.PORTUGUESE, Language.ENGLISH));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.RUSSIAN, Language.ENGLISH));
			supportedRelations.add(TranslationRelationImpl.getInstance(Language.SPANISH, Language.ENGLISH));
		}
		return supportedRelations;
	}

	private Language srcLang;
	private Language trgLang;
	private Properties config;

	public FreeTranslationSource(Language srcLang, Language trgLang, Properties config) {
		this.srcLang = srcLang;
		this.trgLang = trgLang;
		this.config = config;
		cacheIndexer = new NRTCacheIndexer(this.config, srcLang, trgLang, false);
	}

	/** Character encoding required by provider for input */
	protected String getInputCharSet() {
		String retVal = "windows-1252";
		return retVal;
	}

	/** Character encoding used by provider for output */
	protected String getOutputCharSet() {
		String retVal = "windows-1252";
		return retVal;
	}

	/** Returns URL to web translator */
	protected String getURL(String translation) {
		String retVal;
		if(translation.equals("en2ru") ||
				translation.equals("ru2en") ||
				translation.equals("en2zh") ||
				translation.equals("en2zh_TW") ||
				translation.equals("en2zt")) {
			retVal = "http://ets6.freetranslation.com";
		} else {
			retVal = "http://ets.freetranslation.com";			
		}

		return retVal;
	}

	public Set<String> translate(String srcText) {
		Set<String> cacheResults = cacheIndexer.getTranslations(srcText);
		if(cacheResults == null) {
			Set<String> translations = translate(srcText, srcLang, trgLang);
			for(String translation : translations) 
				cacheIndexer.cache(srcText, translation, "", getName());
			return translations;
		} 
		return cacheResults;
	}

	/** Returns parameters passed to web translator */
	protected String getParams(Language sLanguage, Language tLanguage, String text)	{
		String retVal = null;
		//Map languages = getSupportedLanguages();
		String srcLanguage = sLanguage.getName();
		String dstLanguage = tLanguage.getName();
		String language = srcLanguage + "/" + dstLanguage;

		try {
			retVal = URLEncoder.encode( "sequence", getInputCharSet()) + "=" + URLEncoder.encode( "core", getInputCharSet())  + "&" +
					URLEncoder.encode( "mode", getInputCharSet()) + "=" + URLEncoder.encode( "html", getInputCharSet()) + "&" +
					URLEncoder.encode( "template", getInputCharSet()) + "=" + URLEncoder.encode("results_en-us.htm", getInputCharSet()) + "&" +
					URLEncoder.encode( "language", getInputCharSet()) + "=" + URLEncoder.encode( language, getInputCharSet()) + "&" + 
					URLEncoder.encode( "srctext", getInputCharSet()) + "=" + URLEncoder.encode( text, getInputCharSet());
		} catch (UnsupportedEncodingException e) {
			System.err.println(e);
		}
		return retVal;
	}

	/** Returns tag name where translation can be found. */
	protected String getStartSearchText() {
		String retVal = "<textarea name=\"dsttext\"";
		return retVal;
	}

	/** Text that indicates the end of the translation */
	protected String getEndSearchText() {
		String retVal = "</textarea>";
		return retVal;
	}

	/** Text that indicates server is busy */
	protected String getServerBusyError() {
		String retVal = "Server doesn't return server busy error";
		return retVal;
	}	


	@Override
	public PhraseTable candidates(Chunk label) {
		Set<String> translations = translate(label.getSource());
		return new PhraseTableImpl(translations, label.getSource(), srcLang, trgLang, getName());
	}

	@Override
	public String getName() {
		return "FreeTranslation";
	}

//	@Override
	public int featureCount() {
		return 0;
	}

    @Override
    public String[] featureNames() {
        return new String[] { };
    }
        
        

	public static void main(String[] args) {
		Language srcLang = Language.ENGLISH;
		Language trgLang = Language.SPANISH;
		Properties config = Configurator.getConfig("eu.monnetproject.translation.sources.freetranslation");
		FreeTranslationSource free = new FreeTranslationSource(srcLang, trgLang, config);

		Collection<String> translations = free.translate("free assets");
		for(String translation : translations) {
			System.out.println(translation);
		}
		free.close();
	}

	@Override
	public void close() {
		cacheIndexer.close();	
	}


}
