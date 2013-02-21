package eu.monnetproject.translation.sources.ewn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.monitor.Messages;
import eu.monnetproject.translation.sources.ewn.api.EuroWordnetAPI;


public class EuroWordnetSource implements TranslationSource {
	private Language srcLang, trgLang;	
	private static EuroWordnetAPI ewn;
	private Properties config;

	//Available languages in EuroWordnet
	private static Language[] languages = {Language.ENGLISH,
		Language.GERMAN,
		Language.SPANISH};

	private static Map<String, String> paths = new HashMap<String, String>();

	//	public static final String resPrefix = "/ewn/";

	public EuroWordnetSource(Language srcLang, Language trgLang, Properties config) {	
		this.srcLang = srcLang;
		this.trgLang = trgLang;
		this.config = config;
		try {
			if (ewn == null) {
				init();
				ewn = new EuroWordnetAPI(paths, languages);
			}
		} catch (Exception e) {
			Messages.severe("Error connection to EWN");
		}
	}

	private void init() {
		String ewnDBPath = config.getProperty("ewnDBPath");
		InputStream stream = null;
		for (int i=0; i<languages.length; i++) {
			String code = languages[i].getIso639_1();
			String resourcePath = ewnDBPath + File.separator + "ewn_" + code + ".db"; 
			//stream = EuroWordnetSource.class.getResourceAsStream(resourcePath);			
			File file = new File(resourcePath);
			try {
				stream = new FileInputStream(file);
			} catch (FileNotFoundException e1) {
				Messages.warning("Problem in loading EuroWordNet DataBase, may be missing in the given path " + resourcePath);								
			}
			try {
				final File tempFile = File.createTempFile("ewn_" + code ,".db");
				final OutputStream out = new FileOutputStream(tempFile);
				byte buf[] = new byte[1024];
				int len;
				while ((len = stream.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				out.close();
				stream.close();
				tempFile.deleteOnExit();
				paths.put(code, tempFile.getAbsolutePath());
			}
			catch (IOException e){
				Messages.warning("Error in closing the EuroWordNet database");				
			}
		}
	}

	@Override
	public String[] featureNames() {	
		String[] featureNames = new String[1];
		featureNames[0] = "inEWN";
		return featureNames;
	}

	@Override
	public PhraseTable candidates(Chunk label) {
		List<String> translations = getTranslations(label.getSource());
		return new PhraseTableImpl(translations, label.getSource(), srcLang, trgLang, getName());
	}


	private List<String> getTranslations(String label) {
		List<String> candidates = new ArrayList<String>();
		try {    		
			String sourceLanguage = srcLang.getIso639_1();
			String targetLanguage = trgLang.getIso639_1();			
			Map<String, Set<String>> translations = ewn.getTranslations(label, sourceLanguage, targetLanguage);			
			for (Set<String> candidateTranslations : translations.values()) 				
				for (String translation : candidateTranslations) 
					candidates.add(translation);		
		} catch (Exception e) {
			Messages.warning("Error in getting translations from EuroWordNet");
			//e.printStackTrace();
		}		
		return candidates;
	}	

	@Override
	public String getName() {
		return "EWN";
	}
	
	@Override
	public void close() {
	}

	public static void main(String[] args) {
		final Properties config = Configurator.getConfig("eu.monnetproject.translation.sources.EWN");

		EuroWordnetSource ewnSource = new EuroWordnetSource(Language.SPANISH, Language.ENGLISH, config);
		List<String> translations = ewnSource.getTranslations("sport");
		for(String candidate : translations) {
			System.out.println(candidate);
		}		
	}	


}
