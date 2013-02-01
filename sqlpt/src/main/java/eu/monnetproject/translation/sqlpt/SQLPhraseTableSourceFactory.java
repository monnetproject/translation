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
import eu.monnetproject.config.Configurator;
import eu.monnetproject.translation.Decomposer;
import eu.monnetproject.translation.DecomposerFactory;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.TranslationSourceFactory;
import eu.monnetproject.translation.monitor.Messages;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author John McCrae
 */
public class SQLPhraseTableSourceFactory implements TranslationSourceFactory {

    //private final HashMap<LangPair, String> ptFiles = new HashMap<LangPair, String>();
    private final HashMap<LangPair, Integer> ptFeatures = new HashMap<LangPair, Integer>();
    private String server, database, username, password;
    private final Collection<DecomposerFactory> decomposerFactories;
    // private Connection conn;

    public SQLPhraseTableSourceFactory(Collection<DecomposerFactory> decomposerFactories) {
        this.decomposerFactories = decomposerFactories;
        final Properties dbConfig = Configurator.getConfig("com.mysql.jdbc");
        if (dbConfig.containsKey("username") && dbConfig.containsKey("password") && dbConfig.containsKey("database")) {
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
            } catch (ClassNotFoundException x) {
                Messages.componentLoadFail(this.getClass(),"MySQL driver not available");
                return;
            } catch (InstantiationException x) {
                Messages.componentLoadFail(this.getClass(),"MySQL driver not available");
                return;
            } catch (IllegalAccessException x) {
                Messages.componentLoadFail(this.getClass(),"MySQL driver not available");
                return;
            }
            this.server = dbConfig.containsKey("server") ? dbConfig.getProperty("server") : "localhost";
            this.database = dbConfig.getProperty("database");
            this.username = dbConfig.getProperty("username");
            this.password = dbConfig.getProperty("password");
            try {
                // Check we can connect
                checkForTables();
                Messages.info("Using MySQL database");
            } catch (SQLException x) {
                Messages.componentLoadFail(this.getClass(),x);
            }
        } else {
            Messages.componentLoadFail(this.getClass(),"No configuration");
            //throw new RuntimeException("dbConfig not found");
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://" + server + "/" + database + "?user=" + username + "&password=" + password + "&useUnicode=yes&characterEncoding=UTF-8");
    }

    private void checkForTables() throws SQLException {
        final Connection connection = connect();
        final DatabaseMetaData metaData = connection.getMetaData();
        final ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
        while (tables.next()) {
            final String name = tables.getString("TABLE_NAME");
            if (name.startsWith("pt_")) {
                final String[] ss = name.split("_");
                ptFeatures.put(new LangPair(Language.get(ss[1]), Language.get(ss[2])), 5);
            }
        }
        tables.close();
        final ResultSet views = metaData.getTables(null, null, "%", new String[]{"VIEW"});
        while (views.next()) {
            final String name = views.getString("TABLE_NAME");
            if (name.startsWith("pt_")) {
                final String[] ss = name.split("_");
                ptFeatures.put(new LangPair(Language.get(ss[1]), Language.get(ss[2])), 5);
            }
        }
        views.close();
        connection.close();
    }

    @Override
    public TranslationSource getSource(Language srcLang, Language trgLang) {
        if (server == null || database == null || username == null || password == null) {
            Messages.componentLoadFail(this.getClass(),"bad sql config");
            return null;
        }
        try {
            final Connection conn = connect();
            final LangPair lp = new LangPair(srcLang, trgLang);
            Decomposer decomposer = null;
            for(DecomposerFactory decomposerFactory : decomposerFactories) {
                try {
                    decomposer = decomposerFactory.makeDecomposer(srcLang.getLanguageOnly());
                    if(decomposer != null) {
                        break;
                    }
                } catch(Exception x) {
                    Messages.componentLoadFail(this.getClass(),x);
                }
            }
            if (ptFeatures.containsKey(lp)) {
                final String tableName = "pt_" + srcLang + "_" + trgLang;
                //SQLPhraseTableSource.createTableIfNotExists(conn, new File(ptFiles.get(lp)), tableName, SQLPhraseTableSource.MYSQL);
                return new SQLPhraseTableSource(conn, tableName, srcLang, trgLang,decomposer);
            } else {
                Messages.componentLoadFail(this.getClass(),"No phrase table from " + srcLang + " to " + trgLang);
                return null;
            }
        } catch (SQLException x) {
            Messages.componentLoadFail(this.getClass(),x);
            return null;
        }
    }
}
