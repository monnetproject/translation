package eu.monnetproject.nlp.stl;
/**
 * A termbase to lookup terms
 * 
 * @author Tobias Wunner
 */
public interface Termbase/* extends Iterable<String> -- iterating through every term should be avoided */ {
    /**
     * lookup a term in the termbase
     */
    boolean lookup(String term);
    /**
     * the language of the termbase
     */
    String getLanguage();
    int size();
}
