package eu.monnetproject.translation.sources.ewn.api;


import eu.monnetproject.lang.Language;


/**
 * This exception is thrown when an API user tries to initialize a
 * a lexicon for language that is not supported by the system, or if
 * a user tries to retrieve information from a lexical database that
 * does not exist (at run-time).
 *
 * @author Mauricio Espinoza
 * 
 */

@SuppressWarnings("serial")
public class LanguageNotAvailableException extends Exception {

    /**
     * Creates an exception of this type.
     */
    LanguageNotAvailableException() {
    }

    /**
     * Creates an exception of type LanguageNotAvailableException
     * 
     * @param lang the ISO 639 code for the language that is not available to
     *        the system
     */
    public LanguageNotAvailableException(String code) {
    	super("The language " + Language.getByIso639_1(code) + " is not available");
    }
}