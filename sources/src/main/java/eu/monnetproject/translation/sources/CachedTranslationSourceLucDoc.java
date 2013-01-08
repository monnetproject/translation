package eu.monnetproject.translation.sources;


import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import eu.monnetproject.lang.Language;

public class CachedTranslationSourceLucDoc {
	private static String fieldNameLang = "lang";

	private static String fieldNameLang1 = "lang";
	private static String fieldNameLang2 = "lang";	
	private static String fieldNameTranslationSource = "translationSource";
	private static String fieldNameContext = "context";	
	
	private String fieldValueLang1 = null;
	private String fieldValueLang2 = null;
	private String fieldValueTranslationSource = null;
	private String fieldValueContext = null;

	private Document cacheLucDoc = new Document();
	
	public void addField(Language lang1, Language lang2) {		
		fieldNameLang1 = fieldNameLang1 + lang1.getIso639_1();
		fieldNameLang2 = fieldNameLang2 + lang2.getIso639_1();		
	}
	
	public void addTranslation(String lang1String, String lang2String, String translationSource, String context) {
		fieldValueLang1 = lang1String;
		fieldValueLang2 = lang2String;
		fieldValueTranslationSource = translationSource;
		fieldValueContext = context;
		Field lang1Field = new Field(fieldNameLang1, fieldValueLang1, Field.Store.YES, Field.Index.NOT_ANALYZED);
		Field lang2Field = new Field(fieldNameLang2, fieldValueLang2, Field.Store.YES, Field.Index.NOT_ANALYZED);
		Field translationSourceField = new Field(fieldNameTranslationSource, fieldValueTranslationSource, Field.Store.YES, Field.Index.NOT_ANALYZED);
		Field contextField = new Field(fieldNameContext, fieldValueContext, Field.Store.YES, Field.Index.ANALYZED);
		cacheLucDoc.add(lang1Field);
		cacheLucDoc.add(lang2Field);
		cacheLucDoc.add(translationSourceField);
		cacheLucDoc.add(contextField);
	}

	public static String getFieldNameLang(Language lang) {
		return fieldNameLang + lang.getIso639_1();
	}

	public static String getFieldNameTranslationSource() {
		return fieldNameTranslationSource;
	}
	
	public static String getFieldNameContext() {
		return fieldNameContext;
	}	

	public Document getLucDoc() {
		return cacheLucDoc;
	}	

}
