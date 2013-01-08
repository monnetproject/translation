/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.sqlpt;

import eu.monnetproject.lang.Language;
import eu.monnetproject.config.Configurator;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.PhraseTable;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class SQLPhraseTableSourceTest {

    public SQLPhraseTableSourceTest() {
    }
    private static Connection conn;

    @BeforeClass
    public static void setUpClass() throws Exception {
        final Properties config = Configurator.getConfig("com.mysql.jdbc");
        if (config.containsKey("username") && config.containsKey("password") && config.containsKey("database")) {
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
            } catch (ClassNotFoundException x) {
                System.err.println("MySQL driver not available skipping all tests");
                return;
            }
            try {
System.out.println("connect to "+config.getProperty("database"));
                conn = DriverManager.getConnection("jdbc:mysql://localhost/" + config.getProperty("database") + "?user=" + config.getProperty("username") + "&password=" + config.getProperty("password"));
                File phraseTableFile = new File("src/test/resources/sample-models/phrase-model/phrase-table");
                String name = "test";
                SQLPhraseTableSource.createTableIfNotExists(conn, phraseTableFile, name, SQLPhraseTableSource.MYSQL);
            } catch (SQLException x) {
                System.err.println("Cannot connect to MySQL DB, skipping all tests");
                return;
            }
        } else {
            System.err.println("Config not present, skipping all tests");
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of createTableIfNotExists method, of class MySQLPhraseTableSource.
     */
    @Test
    public void testCreateTableIfNotExists() throws Exception {
        if (conn != null) {
            System.out.println("createTableIfNotExists");
            File phraseTableFile = new File("src/test/resources/sample-models/phrase-model/phrase-table");
            String dbtablename = "test";
            SQLPhraseTableSource.createTableIfNotExists(conn, phraseTableFile, dbtablename, SQLPhraseTableSource.MYSQL);
        }
    }

    /**
     * Test of candidates method, of class MySQLPhraseTableSource.
     */
    @Test
    public void testCandidates() throws SQLException {
        if (conn != null) {
            System.out.println("candidates");
            String dbtablename = "test";
            SQLPhraseTableSource instance = new SQLPhraseTableSource(conn, dbtablename, Language.GERMAN, Language.ENGLISH,null);
            String foreign = "Digital";
            PhraseTable result = instance.candidates(new ChunkImpl(foreign));
            int size = 0;
            for (Object pte : result) {
                size++;
            }
            assertEquals(4, size);
        }
    }

    /**
     * Test of featureCount method, of class MySQLPhraseTableSource.
     */
    @Test
    public void testFeatureCount() throws SQLException {
        if (conn != null) {
            System.out.println("featureCount");
            String dbtablename = "test";
            SQLPhraseTableSource instance = new SQLPhraseTableSource(conn, dbtablename, Language.GERMAN, Language.ENGLISH,null);
            int expResult = 1;
            int result = instance.featureCount();
            assertEquals(expResult, result);
        }
    }


    /**
     * Test of case fallback in candidates method, of class MySQLPhraseTableSource.
     */
    @Test
    public void testCandidatesFallbackCase() throws SQLException {
        if (conn != null) {
            System.out.println("candidatesFallbackCase");
            String dbtablename = "test";
            SQLPhraseTableSource instance = new SQLPhraseTableSource(conn, dbtablename, Language.GERMAN, Language.ENGLISH,null);
            String foreign = "Digital";
            PhraseTable result = instance.candidates(new ChunkImpl(foreign));
            int size = 0;
            if (!result.iterator().hasNext())
              fail("No results in phrase table \""+dbtablename+"\" for \""+foreign+"\"");
            for (Object r : result) {
                PhraseTableEntryImpl pte = (PhraseTableEntryImpl) r;
                TokenizedLabelImpl tl_translation = (TokenizedLabelImpl) pte.getTranslation();
                TokenizedLabelImpl tl_foreign = (TokenizedLabelImpl) pte.getForeign();
                size++;
                //System.out.println("final res: "+tl_translation.asString()+" for "+tl_foreign.asString());
                assertEquals(tl_foreign.asString(),foreign);
            }
        }
    }

    public List<String> readFile(String fn) throws IOException {
      List<String> list = new LinkedList<String>();
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fn), "UTF-8"));
      String line = "";
      while( (line = reader.readLine()) != null ) {
        list.add(line);
      }
      return list;
    }

    /**
     * Test of compound fallback for german in candidates method, of class MySQLPhraseTableSource.
     */
    @Test
    public void testCandidatesFallbackGermanCompound() throws SQLException, IOException {
        if (conn != null) {
            System.out.println("candidatesFallbackGermanCompound");
            String dbtablename = "pt_de_en";
            SQLPhraseTableSource instance = new SQLPhraseTableSource(conn, dbtablename, Language.GERMAN, Language.ENGLISH,null);
            List<String> foreigns = readFile("src/test/resources/sample-translations/compounds.de.txt");
            for(String foreign:foreigns) {
              System.out.println("translate: "+foreign);
              PhraseTable result = instance.candidates(new ChunkImpl(foreign));
              if (!result.iterator().hasNext())
                System.out.println("no translations found");
              //  fail("No results in phrase table \""+dbtablename+"\" for \""+foreign+"\"");
              for (Object r : result) {
                PhraseTableEntryImpl pte = (PhraseTableEntryImpl) r;
                Label tl_translation = (Label) pte.getTranslation();
                Label tl_foreign = (Label) pte.getForeign();
                System.out.println("final res: "+tl_translation.asString()+" for "+tl_foreign.asString());
              }
              System.out.println();
            }
        }
    }

    /**
     * Test of compound fallback for german in candidates method, of class MySQLPhraseTableSource.
     */
    @Test
    public void testCandidatesFallbackDutchCompound() throws SQLException, IOException {
        if (conn != null) {
            System.out.println("candidatesFallbackDutchCompound");
            String dbtablename = "pt_nl_en";
            SQLPhraseTableSource instance = new SQLPhraseTableSource(conn, dbtablename, Language.DUTCH, Language.ENGLISH,null);
            List<String> foreigns = readFile("src/test/resources/sample-translations/compounds.nl.txt");
            for(String foreign:foreigns) {
              System.out.println("translate: "+foreign);
              PhraseTable result = instance.candidates(new ChunkImpl(foreign));
              if (!result.iterator().hasNext())
                System.out.println("no translations found");
              //  fail("No results in phrase table \""+dbtablename+"\" for \""+foreign+"\"");
              for (Object r : result) {
                PhraseTableEntryImpl pte = (PhraseTableEntryImpl) r;
                Label tl_translation = (Label) pte.getTranslation();
                Label tl_foreign = (Label) pte.getForeign();
                System.out.println("final res: "+tl_translation.asString()+" for "+tl_foreign.asString());
              }
              System.out.println();
            }
        }
    }

}
