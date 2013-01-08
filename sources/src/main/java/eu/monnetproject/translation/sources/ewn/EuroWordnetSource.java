package eu.monnetproject.translation.sources.ewn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.monnetproject.lang.Language;
//import eu.monnetproject.mrd.LexicalRelation;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.sources.ewn.api.EuroWordnetAPI;
import eu.monnetproject.translation.sources.iate.PhraseTableImpl;
import eu.monnetproject.translation.monitor.Messages;


public class EuroWordnetSource implements TranslationSource {

	//private EWNAPI ewnAPI; 
	private Language srcLang, trgLang;	

	//private final Logger log = Logging.getLogger(this);

	private static EuroWordnetAPI ewn;

	//Available languages in EuroWordnet
	private static Language[] languages = {Language.ENGLISH,
		Language.GERMAN,
		Language.SPANISH};

	private static Map<String, String> paths = new HashMap<String, String>();

	//private static List<LexicalRelation> supportedRelations = new LinkedList<LexicalRelation>();

	public static final String resPrefix = "/ewn/";

	public EuroWordnetSource(Language srcLang, Language trgLang) {	
		this.srcLang = srcLang;
		this.trgLang = trgLang;
		
		try {
			if (ewn == null) {
				init();
				ewn = new EuroWordnetAPI(paths, languages);
			}
		} catch (Exception e) {
			Messages.severe("Error connection to EWN");
		}
		start();
		
	}

	public void start() {
		//log.warning("Activating EuroWordNet");
	//	Parameters.setParameters(Configurator.getConfig("load/eu.monnetproject.translation.sources.EWN"));
	}

	private void init() {
		InputStream stream = null;
		for (int i=0; i<languages.length; i++) {
			String code = languages[i].getIso639_1();
			stream = EuroWordnetSource.class.getResourceAsStream(resPrefix + "ewn_" + code + ".db");
			if(stream == null) {
				throw new IllegalArgumentException("Could not locate " + resPrefix + "ewn_" + code + ".db");
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
			}
		}
	}

//	@Override
	public int featureCount() {
		return 0;
	}

    @Override
    public String[] featureNames() {
        return new String[] { };
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
			e.printStackTrace();
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
		EuroWordnetSource ewnSource = new EuroWordnetSource(Language.ENGLISH, Language.SPANISH);
		List<String> translations = ewnSource.getTranslations("sport");
		for(String candidate : translations) {
			System.out.println(candidate);
		}		
	}	
}
