package eu.monnetproject.translation.sources.iate;

import eu.monnetproject.label.LabelExtractor;
import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.lang.Language;
import eu.monnetproject.lemon.LemonModel;
import eu.monnetproject.lemon.LemonModels;
import eu.monnetproject.lemon.LemonSerializer;
import eu.monnetproject.lemon.model.LexicalEntry;
import eu.monnetproject.lemon.model.Lexicon;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.translation.monitor.Messages;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author John McCrae
 */
public class SimpleLexicalizer {

    private final LabelExtractor labelExtractor;
    private final LemonModel model = LemonSerializer.newInstance().create();

    @SuppressWarnings("unchecked")
    public SimpleLexicalizer(LabelExtractorFactory lef) {
        this.labelExtractor = lef.getExtractor(Collections.EMPTY_LIST, false, false);
    }

    public Collection<Lexicon> lexicalize(Ontology ontology) {
        final HashSet<URI> processedPuns = new HashSet<URI>();
        final HashMap<Language, Lexicon> lexica = new HashMap<Language, Lexicon>();
        final HashMap<Language, Integer> counter = new HashMap<Language, Integer>();
        final HashMap<Language, Integer> dupes = new HashMap<Language, Integer>();
        for (Entity entity : ontology.getEntities()) {
            if (entity.getURI() != null) {
                if (processedPuns.contains(entity.getURI())) {
                    continue;
                }
                processedPuns.add(entity.getURI());
            }
            final Map<Language, Collection<String>> labels = labelExtractor.getLabels(entity);
            for (Map.Entry<Language, Collection<String>> label : labels.entrySet()) {
                final Language lang = label.getKey();
                if(!counter.containsKey(lang)) {
                    counter.put(lang,0);
                    dupes.put(lang, 0);
                }
                counter.put(lang,counter.get(lang) +1);
                if (lang.equals(LabelExtractor.NO_LANGUAGE)) {
                   // log.fine("Ignoring unlanged label \"" + label.getValue() + "\"");
                } else {
                    if (!lexica.containsKey(lang)) {
                        lexica.put(lang, model.addLexicon(mkURI(ontology, lang), lang.toString()));
                    }
                    if (label.getValue().size() > 1) {
                        //log.info(label.getValue().size() + " labels for entity " + entity.getURI());
                    }
                    for (String l : label.getValue()) {
                        final int oldLexiconSize = lexica.get(lang).getEntrys().size();
                        final LexicalEntry entry = LemonModels.addEntryToLexicon(lexica.get(lang), mkURI(ontology, lang, l), l, entity.getURI());
                        if(lexica.get(lang).getEntrys().size() == oldLexiconSize) {
                            dupes.put(lang,dupes.get(lang)+1);
                            //Messages.info("senses: " + entry.getSenses().size());
                        }
                    }
                }
            }
        }

        for(Language l : counter.keySet()) {
            Messages.info(l + " has [" + (counter.get(l) - dupes.get(l)) + "] " + counter.get(l) +" [unique] labels");
        }
        
        return lexica.values();
    }

    public Lexicon getBlankLexicon(Ontology ontology, Language lang) {
        return model.addLexicon(mkURI(ontology, lang), lang.toString());
    }

    private URI mkURI(Ontology ontology, Language lang) {
        final String ontoURIStr = ontology.getURI() == null ? "unknown:ontology#" : ontology.getURI().toString();
        final int fragPoint = ontoURIStr.lastIndexOf("#") > 0 ? ontoURIStr.lastIndexOf("#") : ontoURIStr.length();
        return URI.create(ontoURIStr.substring(0, fragPoint) + "#lexicon__" + lang);
    }

    private URI mkURI(Ontology ontology, Language lang, String l) {
        final String ontoURIStr = ontology.getURI() == null ? "unknown:ontology#" : ontology.getURI().toString();
        final int fragPoint = ontoURIStr.lastIndexOf("#") > 0 ? ontoURIStr.lastIndexOf("#") : ontoURIStr.length();
        try {
            return URI.create(ontoURIStr.substring(0, fragPoint) + "#lexicon__" + lang + "/" + URLEncoder.encode(l, "UTF-8"));
        } catch (UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
    }
}
