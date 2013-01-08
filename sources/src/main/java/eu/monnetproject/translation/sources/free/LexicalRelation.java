package eu.monnetproject.translation.sources.free;

/**
 * A lexical relation, such as hypernym, antonym etc.
 *
 * @author John McCrae
 */
public interface LexicalRelation {
    /**
     * Get the name of this relation
     */
    String getName();
    /**
     * Hypernymy. X is a hypernym of Y, if X is more generic than Y
     */
    public static final LexicalRelation HYPERNYM = new LexicalRelation() {

        public String getName() {
            return "hypernym";
        }
    };
    /**
     * Hyponymy. X is a hyponym of Y, if X is more specific than Y
     */
    public static final LexicalRelation HYPONYM = new LexicalRelation() {

        public String getName() {
            return "hyponym";
        }
    };
    /**
     * Synonymy. X and Y are synonyms, if they have the same meaning
     */
    public static final LexicalRelation SYNONYM = new LexicalRelation() {

        public String getName() {
            return "synonym";
        }
    };
    /**
     * Mernonymy. X is a meronym of Y, if X is a part of Y
     */
    public static final LexicalRelation MERONYM = new LexicalRelation() {

        public String getName() {
            return "meronym";
        }
    };
    /**
     * Holonymy. X is a holonym of Y, if X (partly) consists of Y
     */
    public static final LexicalRelation HOLONYM = new LexicalRelation() {

        public String getName() {
            return "holonym";
        }
    };
    /**
     * Antonymy. X is an anotnym of Y, if X and Y have opposite meanings
     */
    public static final LexicalRelation ANOTNYM = new LexicalRelation() {

        public String getName() {
            return "antonym";
        }
    };
}
