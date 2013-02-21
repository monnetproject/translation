package eu.monnetproject.translation.sources.ewn;


import java.util.Properties;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.TranslationSourceFactory;
import eu.monnetproject.translation.monitor.Messages;

public class EuroWordnetFactory implements TranslationSourceFactory {

	@Override
	public TranslationSource getSource(Language srcLang, Language trgLang) {
		final Properties config = Configurator.getConfig("eu.monnetproject.translation.sources.EWN");
		Boolean use = false;
		if(config.containsKey("use")) 
			use = Boolean.parseBoolean(config.getProperty("use"));
		if (use) {
			try {
				TranslationSource source = null; 
				source = new EuroWordnetSource(srcLang, trgLang, config);
				Messages.info("EuroWordNet Source available");
				return source;
			} catch(Exception e) {
				e.printStackTrace();
				Messages.warning("EuroWordNet Factory returned null");				
				return null;
			}			
		} else {
			return null;
		}
	}
	
}