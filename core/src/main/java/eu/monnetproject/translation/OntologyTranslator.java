package eu.monnetproject.translation;

import eu.monnetproject.lang.Language;
import eu.monnetproject.lemon.model.Lexicon;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.translation.monitor.TranslationMonitor;
import eu.monnetproject.translation.tune.TranslatorSetup;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

/**
 * A translator of ontologies
 * 
 * @author John McCrae
 */
public interface OntologyTranslator {
    
    public static int ESTIMATE_CONFIDENCE = 1;
    public static int DECODE_FAST = 2;
    public static int NO_SEMANTIC_PROCESSING = 4;

	/**
	 * Translate all the lexical entries of the sourceLexicon and add them to the targetLexicon
	 * Source and target languages are taken from the source and target lexicons.
	 * 
	 * @param ontology The ontology containing all entities to be translated
	 * @param sourceLexicon The collection of source lexica to find source labels from.
	 * @param targetLexicon The lexicon were translations should be written to
	 * @param scope The set of URIs in the ontology to be translated, null or an empty list should be interpreted as translate all
	 * @param namePrefix The base of URIs for lexical entries inserted into the target lexicon
         * @param nBest The maximum number of translations per entity to return
	 */
	public void translate(Ontology ontology, 
                Collection<Lexicon> sourceLexicon, 
                Lexicon targetLexicon, 
                Collection<URI> scope, 
                String namePrefix, 
                int nBest);
        
        /**
	 * Translate all the lexical entries of the sourceLexicon and add them to the targetLexicon
	 * Source and target languages are taken from the source and target lexicons.
	 * 
	 * @param ontology The ontology containing all entities to be translated
	 * @param sourceLexicon The collection of source lexica to find source labels from.
	 * @param targetLexicon The lexicon were translations should be written to
	 * @param scope The set of URIs in the ontology to be translated, null or an empty list should be interpreted as translate all
	 * @param namePrefix The base of URIs for lexical entries inserted into the target lexicon
         * @param nBest The maximum number of translations per entity to return
         * @param options Include confidence estimation, speed ups (small beam) and semantic_processing
	 */
	public void translate(Ontology ontology, 
                Collection<Lexicon> sourceLexicon, 
                Lexicon targetLexicon, 
                Collection<URI> scope, 
                String namePrefix, 
                int nBest,
                int options);
        
        /**
         * Get the set up the translator is using internally for a particular translation
         * setting.
         * @param sourceLanguage The source language
         * @param targetLanguage The target language
         * @param extraLangs The pivot languages
         * @param weights The weights on the decoder
         * @return The setup object
         */
        public TranslatorSetup setup(final Language sourceLanguage, final Language targetLanguage, 
                final Set<Language> extraLangs, final DecoderWeights weights);
        
        /**
         * Update the weights the decoder is using
         * @param sourceLanguage  The source language to update weights for
         * @param targetLanguage  The target language to update weights for
         * @param weights The weights
         * @throws UnsupportedOperationException 
         */
        public void updateWeights(final Language sourceLanguage, final Language targetLanguage, DecoderWeights weights) throws UnsupportedOperationException;
        
        /**
         * Add a monitor for this translation process
         * @param monitor The monitor to add
         */
        public void addMonitor(TranslationMonitor monitor);
}
