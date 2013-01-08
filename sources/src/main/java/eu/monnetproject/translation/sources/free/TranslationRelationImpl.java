package eu.monnetproject.translation.sources.free;

import java.util.HashMap;

import eu.monnetproject.lang.Language;

public class TranslationRelationImpl implements TranslationRelation {

	private Language sourceLang;
    private Language targetLang;
    private static HashMap<String,TranslationRelation> instances = new HashMap<String, TranslationRelation>();
    
    /**
     * Get an instance of a translation relation. This is a singleton pattern
     * @param sourceLang The source language of the translation relation
     * @param targetLang The target language of the translation relation
     * @return The instance
     */
    public static TranslationRelation getInstance(Language sourceLang, Language targetLang) {
        String key = sourceLang + ">" + targetLang;
        if(instances.containsKey(key)) {
            return instances.get(key);
        }
        
        TranslationRelationImpl instance = new TranslationRelationImpl(sourceLang, targetLang);
        instances.put(key, instance);
        return instance;
    }

	private TranslationRelationImpl(Language src, Language trg) {
        this.sourceLang =src;
        this.targetLang =trg;
    }

    
	@Override
	public Language getSourceLang() {
		return sourceLang;
	}

	@Override
	public Language getTargetLang() {
		return targetLang;
	}

	@Override
	public String getName() {
		return sourceLang + "->" + targetLang;
	}

	
	
}
