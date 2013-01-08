package eu.monnetproject.translation.sources.free;

import eu.monnetproject.lang.Language;

/**
 * The lexical relation indicating a translation
 *
 * @author John McCrae
 */
public interface TranslationRelation extends LexicalRelation {
    
    /**
     * Get the source language
     * @return The source language
     */
    public Language getSourceLang();

    /**
     * Get the target language
     * @return The target language
     */
    public Language getTargetLang();
}
