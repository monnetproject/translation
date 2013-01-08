/**
 * ********************************************************************************
 * Copyright (c) 2011, Monnet Project All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. * Neither the name of the Monnet Project nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *******************************************************************************
 */
package eu.monnetproject.translation.evaluation;

import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.lang.Language;
import eu.monnetproject.lemon.model.LexicalEntry;
import eu.monnetproject.lemon.model.LexicalSense;
import eu.monnetproject.lemon.model.Lexicon;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.ontology.OntologySerializer;
import eu.monnetproject.translation.OntologyTranslator;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.eval.TranslationEvaluator;
import eu.monnetproject.translation.eval.TranslationEvaluatorFactory;
import eu.monnetproject.translation.evaluation.evaluate.SimpleLexicalizer;
import eu.monnetproject.translation.evaluation.evaluate.TranslationImpl;
import eu.monnetproject.translation.monitor.TranslationMonitor;
import eu.monnetproject.translation.monitor.TranslationMonitorFactory;
import eu.monnetproject.translation.monitor.Messages;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author John McCrae
 */
public abstract class AbstractEvaluation {
    protected final String namePrefix;
    protected final int nBest;
    protected final String[] scopeStrs;
    protected final String runName;
    protected final int firstFold;
    protected final OntologySerializer ontoSerializer;
    protected final Language sourceLanguage, targetLanguage;
    protected final List<Language> extraLangs;
    protected final TranslationEvaluatorFactory evaluatorFactory;
    protected final int translationOptions;
    protected TranslationMonitorFactory monitorFactory;
    protected final Long startTime;
    protected final List<URI> scopes;
    protected final File referenceFolder;
    protected final OntologyTranslator controller;
    protected final LabelExtractorFactory lef;

    public AbstractEvaluation(String[] args, OntologySerializer ontoSerializer,
            TranslationEvaluatorFactory translationEvaluatorFactory,
            OntologyTranslator controller,
            LabelExtractorFactory lef,
            TranslationMonitorFactory monitorFactory) {
        this.controller = controller;
        this.lef = lef;
        
        final CLIOpts opts = new CLIOpts(args);

        //args = "tmp en es 2 5 nist".split("\\s+");
        //final List<String> argsList = new LinkedList<String>(Arrays.asList(args));
        this.namePrefix = opts.string("namePrefix", null, "The prefix name space to use for URIs created");
        this.nBest = opts.nonNegIntValue("nBest", 1, "The number of translations to generate");
        final String scopeOpts = opts.string("scope", null,"The scopes to translate");
        if(scopeOpts != null) {
            scopeStrs = scopeOpts.split(",");
            for (int j = 0; j < scopeStrs.length; j++) {
                    scopeStrs[j] = scopeStrs[j].trim();
            }
        } else {
            scopeStrs = new String[0];
        }
        this.runName = opts.string("runName", null, "The name of the run, as saved in results file");
        this.firstFold = opts.nonNegIntValue("firstFold", 0, "The fold to begin with (folded evaluation only)");
        this.translationOptions = (opts.flag("confidence", "Estimate confidence scores") ? OntologyTranslator.ESTIMATE_CONFIDENCE : 0 ) |
                (opts.flag("fast", "Decode less accurately but faster") ? OntologyTranslator.DECODE_FAST : 0) |
                (opts.flag("nosem", "Do not apply semantic processing") ? OntologyTranslator.NO_SEMANTIC_PROCESSING : 0);
        if(opts.flag("color", "Colorize output")) {
            messageHandler.setPretty(true);
        } else if(opts.flag("nocolor", "Do not colorize output")) {
            messageHandler.setPretty(false);
        }
        
        this.evaluatorFactory = translationEvaluatorFactory;
        this.ontoSerializer = ontoSerializer;
        this.referenceFolder = opts.directory("referenceOntologies", "The folder containing all reference ontologies");
        
        final String langOpts = opts.string("sourceLanguages", "The languages to use a source, as a comma-separated list of ISO-639 codes, e.g., 'en,de'");
        final String[] langs = langOpts == null ? null : langOpts.split(",");
        this.sourceLanguage = langs == null ? null : Language.get(langs[0]);
        if (langs != null && langs.length > 1) {
            this.extraLangs = new LinkedList<Language>();
            for (int i = 1; i < langs.length; i++) {
                extraLangs.add(Language.get(langs[i]));
            }
        } else {
            extraLangs = null;
        }
        this.targetLanguage = opts.language("targetLanguage", "The target language to translate into");

        this.monitorFactory = monitorFactory;
        checkUsage(opts);
        
        this.startTime = System.currentTimeMillis();

        this.scopes = new ArrayList<URI>();
        for (String scopeStr : scopeStrs) {
            final URI scope = URI.create(scopeStr);
            if (scope == null) {
                throw new IllegalArgumentException(scopeStr + " is not a valid URI");
            }
            scopes.add(scope);
        }
    }

    private static final DefaultMessageHandler messageHandler;
    
    static {
    	    Messages.addHandler(messageHandler = new DefaultMessageHandler());
    }
    
    protected abstract void checkUsage(CLIOpts opts);

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

    protected void saveOntologyResult(PreparedOntology po, TranslationMonitor monitor) {
        final LinkedList<Translation> translations = new LinkedList<Translation>();
        final LinkedList<List<Translation>> references = new LinkedList<List<Translation>>();

        Messages.info("Saving ontology");
        Messages.info("Refs:" + po.referenceLexicon.getEntrys().size());

        for (LexicalEntry targetEntry : po.targetLexicon.getEntrys()) {
            try {
                for (LexicalSense targetSense : targetEntry.getSenses()) {
                    final Set<LexicalEntry> sourceEntries = new HashSet<LexicalEntry>();
                    for (LexicalEntry sourceEntry : po.sourceLexicon.getEntrys()) {
                        for (LexicalSense sourceSense : sourceEntry.getSenses()) {
                            if (sourceSense.getReference().equals(targetSense.getReference())) {
                                sourceEntries.add(sourceEntry);
                            }
                        }
                    }
                    final Set<LexicalEntry> referenceEntries = new HashSet<LexicalEntry>();
                    for (LexicalEntry referenceEntry : po.referenceLexicon.getEntrys()) {
                        for (LexicalSense referenceSense : referenceEntry.getSenses()) {
                            if (referenceSense.getReference().equals(targetSense.getReference())) {
                                referenceEntries.add(referenceEntry);
                            }
                        }
                    }
                    for (LexicalEntry sourceEntry : sourceEntries) {
                        final LinkedList<Translation> refList = new LinkedList<Translation>();
                        for (LexicalEntry referenceEntry : referenceEntries) {
                            refList.add(new TranslationImpl(sourceEntry, referenceEntry));
                        }
                        final TranslationImpl t = new TranslationImpl(sourceEntry, targetEntry);
                        if (refList.isEmpty()) {
                            Messages.warning("No references for " + t.getEntity());
                        } else {
                            translations.add(t);
                            references.add(refList);
                        }
                    }
                }
            } catch (Exception x) {
                Messages.translationFail(URI.create("unknown:///entity"), x);
            }
        }
        Messages.info("Found " + translations.size() + " translations");

        final Collection<TranslationEvaluator> evaluators = evaluatorFactory.getEvaluators(references);
        for (TranslationEvaluator evaluator : evaluators) {
            double score = evaluator.score(translations);
            // evaluationResult.addResult(po.fileName, translations.size(), evaluator.getName(), score);
            monitor.recordOntologyScore(po.fileName, evaluator.getName(), score, translations.size());
        }

        System.gc();
        //final long totalMemory = Runtime.getRuntime().totalMemory();
        //final long max = Runtime.getRuntime().maxMemory();
        //System.err.println("Using " + (totalMemory / 0x100000) + "MB/" + (max / 0x100000) + "MB");
    }

    protected void doTranslation(PreparedOntology po) {
        if (extraLangs == null) {
            controller.translate(po.ontology, Collections.singletonList(po.sourceLexicon), po.targetLexicon, scopes, namePrefix, nBest, translationOptions);
        } else {
            final LinkedList<Lexicon> sourceLexicon = new LinkedList<Lexicon>();
            sourceLexicon.add(po.sourceLexicon);
            for (Lexicon lexicon : po.sourceLexica) {
                if (extraLangs.contains(Language.get(lexicon.getLanguage()))) {
                    sourceLexicon.add(lexicon);
                }
            }
            controller.translate(po.ontology, sourceLexicon, po.targetLexicon, scopes, namePrefix, nBest, translationOptions);
        }
    }

    protected void printFinalResult(TranslationMonitor monitor) throws Exception {
        monitor.end();
        monitor.commit();
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
