package eu.monnetproject.nlp.stl.impl;

import eu.monnetproject.nlp.stl.DecomposerFactory;
import eu.monnetproject.util.Logger;
import eu.monnetproject.util.Logging;
import eu.monnetproject.config.Configurator;
import eu.monnetproject.lang.Language;
import eu.monnetproject.nlp.stl.Termbase;
import eu.monnetproject.translation.Decomposer;
import java.io.File;
import java.util.Properties;

/**
 * A factory for generating minimum subterm decomposers for the appropriate
 * language
 *
 * @author Tobias Wunner
 */
public class MinimumSubtermDecomposerFactory implements DecomposerFactory, eu.monnetproject.translation.DecomposerFactory {

    private final Logger log = Logging.getLogger(this);
    private static final String LUCENE_INDEX_PROP = "termbase.lucene.index.";
    private static final String SIMPLE_INDEX_PROP = "termbase.simple.index.";

    public String getLuceneIndexDir(String lang) {
        final Properties config = Configurator.getConfig("eu.monnetproject.nlp.stl");
        if (config.containsKey(LUCENE_INDEX_PROP + lang)) {
            return config.getProperty(LUCENE_INDEX_PROP + lang);
        } else {
            return null;
        }
    }
    
    public String getSimpleIndexDir(String lang) {
        final Properties config = Configurator.getConfig("eu.monnetproject.nlp.stl");
        if (config.containsKey(SIMPLE_INDEX_PROP + lang)) {
            return config.getProperty(SIMPLE_INDEX_PROP + lang);
        } else {
            return null;
        }
    }

    @Override
    public MinimumSubtermDecomposer makeDecomposer(String lang) {
    	    System.err.println("makeDecomposer");
        String luceneIndexDir = getLuceneIndexDir(lang);
        String simpleIndexDir = getSimpleIndexDir(lang);
        if (luceneIndexDir != null) {
            if ((new File(luceneIndexDir)).exists()) {
                try {
                    LuceneTermbase termbase = new LuceneTermbase(luceneIndexDir, lang);
                    return new MinimumSubtermDecomposer(termbase);
                } catch (Exception x) {
                    Exception e = new Exception("No decomposer model for language " + lang + " available");
                    log.stackTrace(x);
                    Logging.stackTrace(log, e);
            return null;
                }
            } else {
                log.warning("Indexdir " + luceneIndexDir + " does not exist");
            return null;
            }
        } else if(simpleIndexDir != null) {
            if ((new File(simpleIndexDir)).exists()) {
                try {
                    final Termbase termbase = TermbaseImpl.fromFile(new File(simpleIndexDir), lang);
                    return new MinimumSubtermDecomposer(termbase);
                } catch (Exception x) {
                    Exception e = new Exception("No decomposer model for language " + lang + " available");
                    log.stackTrace(x);
                    Logging.stackTrace(log, e);
            return null;
                }
            } else {
                log.warning("Indexdir " + simpleIndexDir + " does not exist");
            return null;
            }
            
        } else {
            log.warning("Indexdir nullpointer excception");
            return null;
        }
    }

    @Override
    public Decomposer makeDecomposer(Language lang) throws Exception {
        return makeDecomposer(lang.toString());
    }
    
    
}
