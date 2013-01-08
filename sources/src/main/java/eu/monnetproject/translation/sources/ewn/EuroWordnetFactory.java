package eu.monnetproject.translation.sources.ewn;


import java.util.Properties;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.TranslationSourceFactory;

public class EuroWordnetFactory implements TranslationSourceFactory {

	@Override
	public TranslationSource getSource(Language srcLang, Language trgLang) {
		final Properties config = Configurator.getConfig("eu.monnetproject.translation.sources.EWN");
		if (!config.isEmpty()) {
			return new EuroWordnetSource(srcLang, trgLang);
		} else {
			return null;
		}
	}

}
