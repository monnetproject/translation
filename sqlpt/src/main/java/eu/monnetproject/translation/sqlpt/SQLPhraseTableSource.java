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
package eu.monnetproject.translation.sqlpt;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.Decomposer;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.monitor.Messages;
import java.io.File;
import java.util.List;
import java.util.LinkedList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author John McCrae, Tobias Wunner
 */
public class SQLPhraseTableSource implements TranslationSource {

    private static final int KBEST = 10;
    private final Connection conn;
    private final String tableName;
    //private final PreparedStatement select;
    private final Language srcLang, trgLang;
    // Careful! May be null
    private final Decomposer decomposer;
    // Well it turns out a PreparedStatement can only have one result set open at a time
    // So we make it thread-local so it works for multi-threaded decoding
    private final ThreadLocal<PreparedStatement> tlSelect = new ThreadLocal<PreparedStatement>() {
        @Override
        protected PreparedStatement initialValue() {
            try {
                return conn.prepareStatement("select * from " + tableName + " where forin=?");
            } catch (SQLException x) {
                throw new RuntimeException(x);
            }
        }
    };
    /**
     * The dbType for MySQL databases
     */
    public static final String MYSQL = "mysql";
    /**
     * The dbType for Virtuoso databases
     */
    public static final String VIRTUOSO = "virtuoso";

    public SQLPhraseTableSource(Connection conn, String tableName, Language srcLang, Language trgLang, Decomposer decomposer) throws SQLException {
        this.conn = conn;
        this.tableName = tableName;
        // this.select = conn.prepareStatement("select * from " + tableName + " where forin=?");
        this.srcLang = srcLang;
        this.trgLang = trgLang;
        this.decomposer = decomposer;
    }

    @Override
    public String getName() {
        return "SQL:" + tableName;
    }

    public static void createTableIfNotExists(Connection conn, File phraseTableFile, String name, String dbType) throws SQLException {
        final Statement stat = conn.createStatement();
        try {
            stat.executeQuery("select * from " + name + " limit 1");
            //return;
        } catch (SQLException x) {
            if (dbType.equals(MYSQL)) {
                if (x.getErrorCode() == 1146) {
                    Messages.info("MySQL table " + name + " does not exist... creating, may take a long time");
                    stat.execute("create table " + name + " ( forin varchar(255), translation varchar(255), scores varchar(255), alignment varchar(255), extra varchar(255) );");
                    stat.execute("create index " + name + "_idx on " + name + " (forin,translation) using btree;");
                    stat.execute("load data local infile \"" + phraseTableFile.getPath() + "\" replace into table " + name + " fields terminated by '||| ' (forin, translation, scores, alignment, extra);");
                } else {
                    throw x;
                }
            } else if (dbType.equals(VIRTUOSO)) {
                if (x.getErrorCode() == 74) {
                    Messages.info("Virtuos table " + name + " does not exist... creating, may take a long time");
                    stat.execute("create table " + name + " ( forin varchar(255), translation varchar(255), scores varchar(255), alignment varchar(255), extra varchar(255) )");
                    stat.execute("create index " + name + "_idx on " + name + " (forin,translation)");
                    stat.execute("create procedure pharaoah_load(in file varchar) { declare sess any ; sess := file_open(file) ; csv_load(sess,0,null,'DB.DBA." + name + "',2,vector('csv-delimiter','||| ')); }");
                    stat.execute("select pharoah_load('" + phraseTableFile.getPath() + "')");
                } else {
                    throw x;
                }
            }
        } finally {
            if (stat != null) {
                try {
                    stat.close();
                } catch (Exception x) {
                }
            }
        }
    }
    public static final String FIVESCORE_PHI_t_f = "phi(t|f)";
    public static final String FIVESCORE_LEX_t_f = "lex(t|f)";
    public static final String FIVESCORE_PHI_f_t = "phi(f|t)";
    public static final String FIVESCORE_LEX_f_t = "lex(f|t)";
    public static final String ONESCORE_P_t_f = "p(t|f)";
    public static final String FIVESCORE_PHRASE_PENALTY = "phrasePenalty";
    public static final String[] CANONICAL_FIVESCORE_SCORE_TYPES = {
        FIVESCORE_PHI_t_f, FIVESCORE_LEX_t_f, FIVESCORE_PHI_f_t,
        FIVESCORE_LEX_f_t, FIVESCORE_PHRASE_PENALTY};
    public static final String[] CANONICAL_ONESCORE_SCORE_TYPES = {ONESCORE_P_t_f};

    private static Feature[] toFeatures(double[] scores) {
        //if (scores.length == 1) {
        //return new Feature[]{new Feature(CANONICAL_ONESCORE_SCORE_TYPES[0], scores[0])};
        //} else 
        if (scores.length == 5) {
            return new Feature[]{new Feature(CANONICAL_FIVESCORE_SCORE_TYPES[0], scores[0]),
                        new Feature(CANONICAL_FIVESCORE_SCORE_TYPES[1], scores[1]),
                        new Feature(CANONICAL_FIVESCORE_SCORE_TYPES[2], scores[2]),
                        new Feature(CANONICAL_FIVESCORE_SCORE_TYPES[3], scores[3]),
                        new Feature(CANONICAL_FIVESCORE_SCORE_TYPES[4], scores[4]),};
        } else {
            throw new RuntimeException("Bad number of weights in phrase table");
        }
    }

    @Override
    public String[] featureNames() {
        return CANONICAL_FIVESCORE_SCORE_TYPES;
    }

    private static String trimBegin(String s) {
        if (s.length() > 0 && Character.isWhitespace(s.charAt(0))) {
            return s.substring(1);
        } else {
            return s;
        }
    }

    private PhraseTableImpl candidatesDefault(Chunk label) {
        ResultSet rs = null;
        try {
            final String foreign = label.getSource();
            final PreparedStatement select = tlSelect.get();
            select.setString(1, foreign);
            rs = select.executeQuery();
            PhraseTableImpl caseMatchingResults = new PhraseTableImpl(srcLang, trgLang, tableName);
            PhraseTableImpl caseNonmatchingResults = new PhraseTableImpl(srcLang, trgLang, tableName);

            
            // do
            while (rs.next()) {
                // check if case was available and if current foreign is a case match
                String foreign_res = rs.getString("forin").trim();
                final boolean caseMatch = foreign_res.equals(label.getSource());

                if (!caseMatch && !caseMatchingResults.isEmpty()) {
                    continue;
                }

                final String translation = trimBegin(rs.getString("translation").trim());//new String(rs.getBytes("translation"),"UTF-8");
                final String[] scoresStrs = trimBegin(rs.getString("scores").trim()).split("\\s+");
                final double[] scores = new double[scoresStrs.length];
                for (int i = 0; i < scoresStrs.length; i++) {
                    try {
                        scores[i] = Math.log(Double.parseDouble(scoresStrs[i]));
                    } catch (NumberFormatException x) {
                        System.err.println("Failed for " + scoresStrs[i] + "(" + rs.getString("scores") + ") forin=" + label.getSource());
                        throw x;
                    }
                }
                final String alignmentStr = rs.getString("alignment");
                final Object alignment = (alignmentStr.matches("\\s*") || alignmentStr.contains("|")) ? null : rs.getString("alignment");

                final PhraseTableEntryImpl newPTE = new PhraseTableEntryImpl(
                        new TokenizedLabelImpl(Arrays.asList(FairlyGoodTokenizer.split(foreign)), srcLang),
                        new TokenizedLabelImpl(Arrays.asList(translation.split("\\s+")), trgLang),
                        toFeatures(scores), alignment);
                if (caseMatch) {
                    caseMatchingResults.add(newPTE);
                } else {
                    caseNonmatchingResults.add(newPTE);
                }
            }
            return caseMatchingResults.isEmpty() ? caseNonmatchingResults : caseMatchingResults;
        } catch (SQLException x) {
            throw new RuntimeException(x);
            //} catch(UnsupportedEncodingException x) {
            //  throw new RuntimeException("Argh no UTF-8... panic!");
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
        }
    }

    private PhraseTableEntry concatPhraseTableEntries(PhraseTableEntry e1, PhraseTableEntry e2, String srcLabel) {
        // sum values of features
        double[] newScores = new double[e1.getFeatures().length];
        Feature[] f1 = e1.getFeatures();
        Feature[] f2 = e2.getFeatures();
        for (int i = 0; i < e1.getFeatures().length; i++) {
            newScores[i] = f1[i].score + f2[i].score;
        }
        Feature[] newFeatures = toFeatures(newScores);
        // combine foreign and translation labels
        //Label newForeign = new StringLabel(e1.getForeign().asString() + "" + e2.getForeign().asString(), srcLang);
        Label newTranslation = new StringLabel(e1.getTranslation().asString() + " " + e2.getTranslation().asString(), trgLang);
        return new PhraseTableEntryImpl(new StringLabel(srcLabel, srcLang), newTranslation, newFeatures, null);
    }

    public PhraseTableTreeSetImpl combineKBestSearch(List<PhraseTable> compounds, int k, String srcLabel) {
        PhraseTableTreeSetImpl pt = new PhraseTableTreeSetImpl(this.srcLang, this.trgLang, this.tableName);
        // add 1st element
        for (Object entry : compounds.get(0)) {
            pt.add((PhraseTableEntry) entry);
        }
        // combine/translate/sort iterative
        for (int i = 1; i < compounds.size(); i = i + 1) {
            //List<List<String>> compoundsalli = new LinkedList<List<String>>();
            PhraseTableTreeSetImpl tmp = new PhraseTableTreeSetImpl(this.srcLang, this.trgLang, this.tableName);
            // append i-th element
            for (Object o : compounds.get(i)) {
                PhraseTableEntry entryappend = (PhraseTableEntry) o;
                for (PhraseTableEntry pte : pt) {
                    // copy entryappend to pti
                    PhraseTableEntry newentry = concatPhraseTableEntries(pte, entryappend,srcLabel);
                    tmp.add(newentry);
                }
            }
            // truncate k best elements
            while (tmp.size() > k) {
                tmp.remove(tmp.first());
            }
            pt = tmp;
        }
        return pt;
    }

    public PhraseTableImpl combinePhraseTables(List<PhraseTableImpl> pts, String srcLabel) {
        if (pts.isEmpty()) {
            return new PhraseTableImpl(srcLang, trgLang, tableName);
        } else if (pts.size() == 1) {
            return pts.get(0);
        } else {
            PhraseTable t = pts.get(0);
            pts.remove(0);
            PhraseTable t2 = combinePhraseTables(pts,srcLabel);
            PhraseTableImpl rval = new PhraseTableImpl(srcLang, trgLang, tableName);
            for (Object e1 : t) {
                for (Object e2 : t2) {
                    rval.add(concatPhraseTableEntries((PhraseTableEntry) e1, (PhraseTableEntry) e2,srcLabel));
                }
            }
            return rval;
        }
    }

    @Override
    public PhraseTable candidates(Chunk label) {
        final PhraseTableImpl defaultPt = candidatesDefault(label);
        if (defaultPt.isEmpty()) {
            if (decomposer != null && !label.getSource().contains(" ")) {
                final String srcLabel = label.getSource();
                final HashMap<String, PhraseTableImpl> ptiCache = new HashMap<String, PhraseTableImpl>();
                // compounding
                COMPOUNDS:
                for (List<String> compounds : decomposer.decomposeRanked(srcLabel)) {
                    if (!compounds.isEmpty()) {
                        // some stuff ..
                        List<String> compounds2 = compounds;
                        if (srcLang.getLanguageOnly().equals(Language.GERMAN)) {
                            compounds2 = new LinkedList<String>();
                            for (String compound : compounds) {
                                compound = compound.replaceAll("gs$", "g");
                                compound = compound.replaceAll("ts$", "t");
                                compounds2.add(compound);
                            }
                        }
                        // translate compounds	
                       // System.out.println("translate compounds: " + compounds2);
                        List<PhraseTable> pts = new LinkedList<PhraseTable>();
                        for (String compoundi : compounds2) {
                            if (compoundi.matches("\\s*")) {
                                continue;
                            }
                            final PhraseTableImpl pti;
                            if(ptiCache.containsKey(compoundi)) {
                                pti = ptiCache.get(compoundi);
                            } else {
                                pti = candidatesDefault(new ChunkImpl(compoundi));
                                ptiCache.put(compoundi, pti);
                            }
                            if (pti.isEmpty()) {
                                //System.out.println("Error: no translations for \"" + compoundi + "\"");
                                //break;
                                continue COMPOUNDS;
                            }
                            pts.add(pti);
                        }
                        // combine k best
                        PhraseTableTreeSetImpl fallbackGerman = combineKBestSearch(pts, KBEST,srcLabel);
                        if (!fallbackGerman.isEmpty()) {
                            return fallbackGerman;
                        }
                    }
                }
            }
            return defaultPt;
        } else {
            return defaultPt;
        }
    }
    private int featureCount = -1;

//    @Override
    public int featureCount() {
        if (featureCount >= 0) {
            return featureCount;
        }
        try {
            Statement stat = conn.createStatement();
            try {
                final ResultSet rs = stat.executeQuery("select * from " + tableName + " limit 2");
                rs.next();
                return featureCount = rs.getString("scores").split("\\s+").length;
            } catch (SQLException x) {
                throw new RuntimeException(x);
            } finally {
                if (stat != null) {
                    try {
                        stat.close();
                    } catch (Exception x) {
                        throw new RuntimeException(x);
                    }
                }
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException x) {
            Messages.severe("Failed to close SQL connection:" + x.getMessage());
        }
    }
}
