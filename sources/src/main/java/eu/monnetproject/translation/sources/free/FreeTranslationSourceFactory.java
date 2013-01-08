package eu.monnetproject.translation.sources.free;

import java.util.Properties;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.TranslationSourceFactory;

public class FreeTranslationSourceFactory implements TranslationSourceFactory {

	@Override
	public TranslationSource getSource(Language srcLang, Language trgLang) {
		final Properties config = Configurator.getConfig("eu.monnetproject.translation.sources.freetranslation");
		if (!config.isEmpty()) {
			return new FreeTranslationSource(srcLang, trgLang, config);
		} else {
			return null;
		}
	}
	

}
