package eu.monnetproject.translation.sources.ewn.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import eu.monnetproject.lang.Language;


public class EuroWordnetAPI {

	/** Language code (ISO 639, alpha-2) for English */
	public static final String LANG_EN = "en";
	/** Language code (ISO 639, alpha-2) for Spanish */
	public static final String LANG_ES = "es";
	/** Language code (ISO 639, alpha-2) for Catalan */
	public static final String LANG_CA = "ca";
	/** Language code (ISO 639, alpha-2) for German */
	public static final String LANG_DE = "de";
	/** Language code (ISO 639, alpha-2) for Dutch */
	public static final String LANG_NL = "nl";
	/** Language code (ISO 639, alpha-2) for Estonian */
	public static final String LANG_ET = "et";
	/** Language code (ISO 639, alpha-2) for Czech */
	public static final String LANG_CS = "cs";
	/** Language code (ISO 639, alpha-2) for Italian */
	public static final String LANG_IT = "it";
	/** Language code (ISO 639, alpha-2) for French */
	public static final String LANG_FR = "fr";

	/**
	 * An array with all possible values for the part-of-speech,
	 * excluding the wildcard character "_"
	 */
	public static final String[] POS_VALUES = {"n", "v", "a", "b"};
	/** Wildcard character for the part-of-speech */
	public static final String POS_ANY = "_";
	/** Code for nouns */ 
	public static final String POS_NOUN = "n";
	/** Code for verbs */ 
	public static final String POS_VERB = "v";
	/** Code for adjectives */ 
	public static final String POS_ADJECTIVE = "a";
	/** Code for adverbs */ 
	public static final String POS_ADVERB = "b";


	/** Default part-of-speech for the multilingual API and all sub-lexicons */
	public static final String POS_DEFAULT = "_";//"n";

	/** The Hashmap containing the language specific lexicons */
	protected Hashtable<String, Lexicon> lexicon;

	/** The PoS variable used for most queries */
	private String partOfSpeech = POS_ANY;//POS_DEFAULT;


	protected Language languages[];

	/**
	 * Creates a multilingual EuroWordNet lexicon.
	 *
	 * @param languages an array of Strings containing the language shortcuts
	 *                  for the desired languages
	 * @param configFile a configuration file
	 * @param partOfSpeech the default part-of-speech for this lexicon
	 * @throws Exception 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws FileNotFoundException 
	 */
	public EuroWordnetAPI(Map<String,String> ewnPaths, Language languages[], String partOfSpeech) 
			throws FileNotFoundException, SecurityException, ClassNotFoundException, IOException, Exception {
		this(ewnPaths, languages);
		this.languages = languages;

		setPartOfSpeech(partOfSpeech);
	}

	/**
	 * Creates a multilingual EuroWordNet lexicon.
	 *
	 * @param languages an array of Strings containing the language shortcuts
	 *                  for the desired languages
	 * @param configFile a configuration file
	 * @throws Exception 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws FileNotFoundException 
	 */
	public EuroWordnetAPI(Map<String,String> ewnPaths, Language languages[]) 
			throws FileNotFoundException, SecurityException, ClassNotFoundException, IOException, Exception {

		//configFile = "conf/ewn.conf";
		lexicon = new Hashtable<String, Lexicon>();	
		this.languages = languages;
		String lang;

		// idea: remove languages as an argument, just get them from the
		//       configFile!!! would be much smarter ...
		// handle configfile only here, not deeper in the class hierarchy -
		// one file access is enough for initialization

		for (int i=0; i < languages.length; i++) {

			lang = languages[i].getIso639_1();


			if ( isValidLanguage(lang) ) {
				Lexicon lexicon2 = new Lexicon(ewnPaths, lang);
				lexicon.put(lang, lexicon2);

			} else {
				throw new LanguageNotAvailableException(lang);
			}
		}
	}


	/**
	 * Checks whether the given language has an available database 
	 *
	 * @param language the 2-character code for the language (lower case)
	 * @return true for a valid code, else false
	 */
	public boolean isValidLanguage(String language) {
		if (language.equals(LANG_EN) || language.equals(LANG_ES) || language.equals(LANG_DE)) {
			return true;
		} 
		return false;
	}    


	/**
	 * Set the part-of-speech for forthcoming lexical look-ups.
	 * 
	 * @param partOfSpeech "a" for adjective, "n" for noun, "v" for verb,
	 *                     EuroWordNet.POS_ANY for any
	 */
	public void setPartOfSpeech(String partOfSpeech) {
		this.partOfSpeech = partOfSpeech;

		Enumeration<String> keys = lexicon.keys();
		while (keys.hasMoreElements())
			((Lexicon) lexicon.get(keys.nextElement())).setPartOfSpeech(partOfSpeech);
	}

	/**
	 * Returns the lexicon object for a given language.
	 * 
	 * @param lang the ISO 639 language code (e.g. "en" for English)
	 * @throws LanguageNotAvailableException 
	 */
	public Lexicon lexicon(String lang) throws LanguageNotAvailableException {//throws LanguageNotAvailableException {
		Lexicon l = lexicon.get(lang);
		if (l == null) 
		    throw new LanguageNotAvailableException(lang);		
		return l;
	}

	/**
	 * Get word translations.
	 *
	 * @param word the word to be translated
	 * @param lang1 the language the word belongs to (source language)
	 * @param lang2 the target language
	 * @return A set of mappings between ILIs and corresponding synonyms in
	 *         the target language
	 */
	public Map<String, Set<String>> getTranslations(String word, String lang1, String lang2)
			throws Exception, LanguageNotAvailableException {

		Lexicon ewn1 = (Lexicon)lexicon.get(lang1);
		Lexicon ewn2 = (Lexicon)lexicon.get(lang2);

		if (ewn1 == null) {
			throw new LanguageNotAvailableException(lang1);
		}
		if (ewn2 == null) {
			throw new LanguageNotAvailableException(lang2);
		}        

		String ili = null;  // the synset ID
		Set<String> lexemes; 	     
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		Set<String> meanings = ewn1.getILIs(word);
		for(String ilI : meanings) {			
			lexemes = ewn2.getLexemesByILI(ilI);
			if (!lexemes.isEmpty()) 
				result.put(ili, lexemes);		 
		}
		return result;
	}

	public Map<String, Set<String>> getSynsets(String word, String srcLang, String trgLang)
			throws Exception, LanguageNotAvailableException {
		Lexicon srcEwn = lexicon.get(srcLang);
		Lexicon trgEwn = lexicon.get(trgLang);
		if (srcEwn == null) 
			throw new LanguageNotAvailableException(srcLang);		
		if (trgEwn == null) 
			throw new LanguageNotAvailableException(trgLang);
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		Set<String> itMeanings = srcEwn.getILIs(word);
		Set<String> lexemes;
		for(String ili : itMeanings) {
			lexemes = trgEwn.getLexemesByILI(ili);
			if (!lexemes.isEmpty()) {
				lexemes = srcEwn.getLexemesByILI(ili);
				result.put(ili, lexemes);
			}
		}
		return result;
	}

	public Language[] getValidLanguages() {
		return languages;
	}

}
