package eu.monnetproject.nlp.stl;

//import eu.monnetproject.lang.Language;

/**
 * A factory for generating decomposer for the appropriate language
 *
 * @author Tobias Wunner
 */
public interface DecomposerFactory {
        /**
         * Create a decomposer for the given language
         * @param language The language as String language code, e.g. "en" for English or "de" for German
         * @throws Exception If the decomposer model does not exist or can't be found
         */
        Decomposer makeDecomposer(String lang) throws Exception;
}
