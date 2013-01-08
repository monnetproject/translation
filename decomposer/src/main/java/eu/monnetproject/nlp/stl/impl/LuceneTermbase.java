package eu.monnetproject.nlp.stl.impl;

import eu.monnetproject.nlp.stl.Termbase;
import eu.monnetproject.util.Logger;
import eu.monnetproject.util.Logging;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;

/**
 * A Lucene termbase implementation to lookup terms
 *
 * @author Tobias Wunner
 */
public class LuceneTermbase implements Termbase, Iterable<String> {

    private static final Logger log = Logging.getLogger(LuceneTermbase.class);
    private final String language;
    private final String lookupfield;
    private final IndexReader reader;
    private final IndexSearcher searcher;
    private final boolean lowerCaseSearch;

    public LuceneTermbase(String indexdir, String lookupfield, String language, boolean lowerCaseSearch) throws IOException {
        this.lookupfield = lookupfield;
        this.language = language;
        this.lowerCaseSearch = lowerCaseSearch;
        // init all docs searcher
        this.reader = IndexReader.open(FSDirectory.open(new File(indexdir)));
        this.searcher = new IndexSearcher(reader);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    reader.close();
                    searcher.close();
                } catch (Exception x) {
                    // Oh well... the program is shutting down anyway
                }
            }
        });
    }

    public LuceneTermbase(String indexdir, String language, boolean lowerCaseSearch) throws IOException {
        this(indexdir, "content", language, lowerCaseSearch);
    }

    public LuceneTermbase(String indexdir, String language) throws IOException {
        this(indexdir, "content", language, true);
    }

    @Override
    public boolean lookup(String term) {
        if (lowerCaseSearch) {
            term = term.toLowerCase();
        }
        TopScoreDocCollector collector = TopScoreDocCollector.create(1, true);
        try {
            searcher.search(new TermQuery(new Term(lookupfield, term)), collector);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ScoreDoc[] hits = collector.topDocs().scoreDocs;
        return (hits.length > 0);
    }

    @Override
    public Iterator<String> iterator() {
        try {
            reader.reopen();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new Iterator<String>() {
            private int docidx = -1;

            @Override
            public boolean hasNext() {
                return (docidx + 1) < size();
            }

            @Override
            public String next() {
                try {
                    docidx++;
                    return reader.document(docidx).get(lookupfield);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void remove() {
            }
        };
    }

    @Override
    public String getLanguage() {
        return this.language;
    }

    @Override
    public int size() {
        return reader.numDocs();
    }

    public static void main(String[] args) throws IOException {
        LuceneTermbase termbase = new LuceneTermbase("src/main/resources/index/de/", "en", true);
        int cnt = 0;
        System.out.println("size " + termbase.size());
        System.out.println("lookup Haus " + termbase.lookup("Haus"));
        System.out.println("lookup Hausboffele " + termbase.lookup("Hausboffele"));
        for (String term : termbase) {
            cnt++;
        }
        System.out.println(cnt);
        cnt = 0;
        for (String term : termbase) {
            if (cnt < 15) {
                System.out.println("i=" + cnt + " " + term);
            }
            cnt++;
        }
        System.out.println("lookup Haus " + termbase.lookup("Haus"));
    }
}
