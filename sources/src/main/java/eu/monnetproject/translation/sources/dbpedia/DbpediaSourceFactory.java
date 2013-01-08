package eu.monnetproject.translation.sources.dbpedia;

import java.util.Properties;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.TranslationSourceFactory;

public class DbpediaSourceFactory implements TranslationSourceFactory {

	@Override
	public TranslationSource getSource(Language srcLang, Language trgLang) {
		final Properties config = Configurator.getConfig("eu.monnetproject.translation.sources.dbpedia");
		if (!config.isEmpty()) {
			return new DbpediaSource(srcLang, trgLang);
		} else {
			return null;
		}
	}

}
