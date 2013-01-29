package eu.monnetproject.translation.sources.iate;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import eu.monnetproject.config.Configurator;
import eu.monnetproject.framework.services.Services;
import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.lang.Language;
import eu.monnetproject.lang.LanguageCodeFormatException;
import eu.monnetproject.lang.Script;
import eu.monnetproject.lemon.model.LexicalEntry;
import eu.monnetproject.lemon.model.LexicalForm;
import eu.monnetproject.lemon.model.LexicalSense;
import eu.monnetproject.lemon.model.Lexicon;
import eu.monnetproject.lemon.model.Text;
import eu.monnetproject.ontology.AnnotationProperty;
import eu.monnetproject.ontology.DatatypeProperty;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.ontology.Individual;
import eu.monnetproject.ontology.ObjectProperty;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.ontology.OntologySerializer;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.Tokenizer;
import eu.monnetproject.translation.TokenizerFactory;
import eu.monnetproject.translation.TranslationPhraseChunker;
import eu.monnetproject.translation.TranslationPhraseChunkerFactory;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.monitor.Messages;



public class IATEIndexer {

	private final OntologySerializer ontoSerializer;
	private final LabelExtractorFactory lef;
	private Language sourceLanguage, targetLanguage;
	private TokenizerFactory tokenizerFactory;
	private final Iterable<TranslationPhraseChunkerFactory> chunkerFactories;
	private TranslationSource translationSource = null;
	private final String[] scopeStrs;
	private final List<URI> scopes;


	public IATEIndexer(OntologySerializer ontoSerializer, LabelExtractorFactory lef, TokenizerFactory tokenizerFactory, Iterable<TranslationPhraseChunkerFactory> chunkerFactories,  Language sourceLanguage, Language targetLanguage){
		this.ontoSerializer = ontoSerializer;		
		this.lef = lef;
		this.tokenizerFactory = tokenizerFactory;
		this.sourceLanguage = sourceLanguage;
		this.targetLanguage = targetLanguage;	
		this.chunkerFactories = chunkerFactories;
		scopes = new ArrayList<URI>();
		scopeStrs = new String[0];			
	}


	public static void main(String[] args) throws IOException {	
		final Properties config = Configurator.getConfig("eu.monnetproject.translation.sources.iate.indexer");
		File referenceFolder = new File(config.getProperty("ontologiesFolder"));		
		IATEIndexer indexer = new IATEIndexer(Services.get(OntologySerializer.class),
				Services.get(LabelExtractorFactory.class), Services.get(TokenizerFactory.class), Services.getAll(TranslationPhraseChunkerFactory.class), Language.ENGLISH, Language.DUTCH);
		indexer.sourceLanguage = Language.getByIso639_1(config.getProperty("sourceLanguage"));
		indexer.targetLanguage = Language.getByIso639_1(config.getProperty("targetLanguage"));
		Boolean use = Boolean.parseBoolean(config.getProperty("use"));		
		for (String scopeStr : indexer.scopeStrs) {
			final URI scope = URI.create(scopeStr);
			if (scope == null) {
				throw new IllegalArgumentException(scopeStr + " is not a valid URI");
			}
			indexer.scopes.add(scope);
		}
		if (use) {
			indexer.translationSource = new IATESourceWithCache(indexer.sourceLanguage, indexer.targetLanguage, config);
		} else {
			return;
		}		
		for (File ontologyFile : referenceFolder.listFiles()) {
			final PreparedOntology po = indexer.prepareOntologyFile(ontologyFile);
			if (po != null) 
				indexer.doIndexing(po.ontology, Collections.singletonList(po.sourceLexicon), indexer.scopes);				
		}
		indexer.translationSource.close();
	}

	private List<TranslationPhraseChunker> getChunkers(Iterable<TranslationPhraseChunkerFactory> chunkerFactories, Ontology ontology, Language src) {
		List<TranslationPhraseChunker> chunkers = new LinkedList<TranslationPhraseChunker>();
		for (TranslationPhraseChunkerFactory tpcf : chunkerFactories) {
			final TranslationPhraseChunker phraseChunker = tpcf.getPhraseChunker(ontology, src);
			if (phraseChunker != null) {
				chunkers.add(phraseChunker);
			}
		}
		return chunkers;
	}

	protected void doIndexing(Ontology ontology, Collection<Lexicon> sourceLexicons,  Collection<URI> scope) {
		for(Lexicon sourceLexicon : sourceLexicons) {
			final Language sourceLanguage = Language.get(sourceLexicon.getLanguage());
			final Script[] knownScriptsForLanguage = Script.getKnownScriptsForLanguage(sourceLanguage);
			final Script sourceScript = (knownScriptsForLanguage != null && knownScriptsForLanguage.length > 0)? knownScriptsForLanguage[0] : Script.LATIN;
			final Tokenizer tokenizer = tokenizerFactory.getTokenizer(sourceScript);
			if (tokenizer == null) {
				Messages.warning("Skipping translations from " + sourceLanguage + " as no tokenizer available");
				continue;
			}
			final List<TranslationPhraseChunker> chunkers = getChunkers(chunkerFactories,
					ontology, sourceLanguage);
			if (chunkers.isEmpty()) {
				Messages.warning("Skipping translations from " + sourceLanguage + " as no chunker available");
				continue;
			}

			for (LexicalEntry entry : sourceLexicon.getEntrys()) {
				if (entry.getSenses() == null) {
					Messages.translationFail(entry.getURI(), "entry has no senses");
				}
				for (LexicalSense sense : entry.getSenses()) {
					if (sense.getReference() == null) {
						Messages.warning("Sense with null reference for " + entry.getURI());
						continue;
					}
					final Entity entity = getBestEntity(ontology.getEntities(sense.getReference()), scope);
					if (entity == null) {
						Messages.warning("Sense for " + entry.getURI() + " (" + sense.getReference() + ") not found in ontology ");
						continue;
					}
					final Label srcLabel = getLabel(entry, tokenizer);
					final ChunkListImpl chunkList = new ChunkListImpl();
					for (TranslationPhraseChunker chunker : chunkers) {
						chunkList.addAll(chunker.chunk(srcLabel));
					}
					for(Chunk chunk : chunkList)
						translationSource.candidates(chunk);														
				}
			}
		}
	}


	private Label getLabel(LexicalEntry entry, Tokenizer tokenizer) {
		final LexicalForm canonicalForm = entry.getCanonicalForm();
		if (canonicalForm != null) {
			final Text writtenRep = canonicalForm.getWrittenRep();
			if (writtenRep != null) {
				try {
					return new SimpleLabel(writtenRep.value, Language.get(writtenRep.language), tokenizer);
				} catch (LanguageCodeFormatException x) {
				}
			}
		}
		return null;
	}

	private Entity getBestEntity(Collection<Entity> entities, Collection<URI> scope) {
		Entity bestEntity = null;
		for (Entity entity : entities) {
			if (entity.getURI() == null
					|| (scope != null
					&& !scope.isEmpty()
					&& !scope.contains(entity.getURI()))) {
				continue;
			}
			if (entity instanceof eu.monnetproject.ontology.Class) {
				return entity;
			} else if (entity instanceof ObjectProperty) {
				bestEntity = entity;
			} else if (entity instanceof DatatypeProperty && (bestEntity == null || !(bestEntity instanceof ObjectProperty))) {
				bestEntity = entity;
			} else if (entity instanceof AnnotationProperty && (bestEntity == null || (!(bestEntity instanceof ObjectProperty) && !(bestEntity instanceof DatatypeProperty)))) {
				bestEntity = entity;
			} else if (entity instanceof Individual && (bestEntity == null || (!(bestEntity instanceof ObjectProperty) && !(bestEntity instanceof DatatypeProperty) && !(bestEntity instanceof AnnotationProperty)))) {
				bestEntity = entity;
			} else if (bestEntity == null) {
				bestEntity = entity;
			}
		}
		return bestEntity;
	}


	protected PreparedOntology prepareOntologyFile(File ontologyFile) throws IOException {
		if (!ontologyFile.getName().endsWith("rdf") && !ontologyFile.getName().endsWith("owl")
				&& !ontologyFile.getName().endsWith("ttl") && !ontologyFile.getName().endsWith("xml")
				&& !ontologyFile.getName().endsWith("nt")) {
			Messages.warning("Skipping " + ontologyFile.getName());
			return null;
		}
		Messages.info("Reading " + ontologyFile);
		final Reader ontologyReader = new FileReader(ontologyFile);
		final Ontology ontology = ontoSerializer.read(ontologyReader, ontologyFile.toURI());
		final SimpleLexicalizer lexicalizer = new SimpleLexicalizer(lef);
		final Collection<Lexicon> sourceLexica = lexicalizer.lexicalize(ontology);
		Lexicon sourceLexicon = null;
		Lexicon referenceLexicon = null;
		for (Lexicon lexicon : sourceLexica) {
			if (Language.get(lexicon.getLanguage()).equals(sourceLanguage)) {
				sourceLexicon = lexicon;
			} else if (Language.get(lexicon.getLanguage()).equals(targetLanguage)) {
				referenceLexicon = lexicon;
			}
		}
		if (sourceLexicon == null || referenceLexicon == null) {
			Messages.warning("No source lexicon created or no references available");
			return null;
		}
		final Lexicon targetLexicon = lexicalizer.getBlankLexicon(ontology, targetLanguage);

		Messages.info("Translating "+ontology.getEntities().size()+ " entities");
		return new PreparedOntology(sourceLexica, sourceLexicon, targetLexicon, referenceLexicon, ontology, ontologyFile.getName());
	}


	protected static class PreparedOntology {

		public final Collection<Lexicon> sourceLexica;
		public final Lexicon sourceLexicon;
		public final Lexicon targetLexicon;
		public final Lexicon referenceLexicon;
		public final Ontology ontology;
		public final String fileName;

		public PreparedOntology(Collection<Lexicon> sourceLexica, Lexicon sourceLexicon, Lexicon targetLexicon, Lexicon referenceLexicon, Ontology ontology, String fileName) {
			this.sourceLexica = sourceLexica;
			this.sourceLexicon = sourceLexicon;
			this.targetLexicon = targetLexicon;
			this.referenceLexicon = referenceLexicon;
			this.ontology = ontology;
			this.fileName = fileName;
		}
	}


}
