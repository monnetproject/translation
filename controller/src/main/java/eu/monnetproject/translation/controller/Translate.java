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
 ********************************************************************************
 */
package eu.monnetproject.translation.controller;

import eu.monnetproject.lang.Language;
import eu.monnetproject.lemon.model.Lexicon;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.ontology.OntologySerializer;
import eu.monnetproject.framework.services.Services;
import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.lemon.LemonSerializer;
import eu.monnetproject.translation.*;
import eu.monnetproject.translation.controller.impl.SimpleLexicalizer;
import eu.monnetproject.translation.controller.impl.TranslationController;
import eu.monnetproject.translation.monitor.Job;
import eu.monnetproject.translation.monitor.MessageHandler;
import eu.monnetproject.translation.monitor.Messages;
import eu.monnetproject.translation.monitor.Messages.Message;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class Translate {

    public static void main(String[] args) throws Exception {
        final List<String> argsList = new LinkedList<String>(Arrays.asList(args));
        String namePrefix = null;
        int nBest = 1;
        String[] scopeStrs = new String[]{};
        boolean verbose = false;
        boolean fast = false;
        for (int i = 0; i < argsList.size(); i++) {
            if (argsList.get(i).equals("-namePrefix")) {
                namePrefix = argsList.get(i + 1);
                argsList.remove(i);
                argsList.remove(i);
                i--;
            } else if (argsList.get(i).equals("-nBest")) {
                nBest = Integer.parseInt(argsList.get(i + 1));
                argsList.remove(i);
                argsList.remove(i);
                i--;
            } else if (argsList.get(i).equals("-scope")) {
                scopeStrs = argsList.get(i).split(",");
                for (int j = 0; j < scopeStrs.length; j++) {
                    scopeStrs[j] = scopeStrs[j].trim();
                }
                argsList.remove(i);
                argsList.remove(i);
                i--;
            } else if(argsList.get(i).equals("-fast")) {
                fast = true;
                argsList.remove(i);
            } else if (argsList.get(i).equals("-verbose")) {
                verbose = true;
                argsList.remove(i);
            }
        }
        final OntologySerializer ontoSerializer = Services.get(OntologySerializer.class);
        final LemonSerializer lemonSerializer = Services.get(LemonSerializer.class);
        if (argsList.size() != 2 && argsList.size() != 3) {
            System.err.println("Usage:\n\ttranslate [-namePrefix prefix] [-nBest n] [-scope uri1,uri2,uri3...] [-fast] ontologyFileOrUrl targetLanguage [ontologyOut]");
            System.exit(-1);
        }
        final String ontologyFileOrUrl = argsList.get(0);
        Reader ontologyReader;
        if (ontologyFileOrUrl.startsWith("http:") || ontologyFileOrUrl.startsWith("https:") || ontologyFileOrUrl.startsWith("ftp:")
                || ontologyFileOrUrl.startsWith("file:")) {
            ontologyReader = new InputStreamReader(new URL(ontologyFileOrUrl).openStream());
        } else {
            if (!new File(ontologyFileOrUrl).exists()) {
                throw new IllegalArgumentException(ontologyFileOrUrl + " does not exist");
            } else {
                System.err.println("Reading " + ontologyFileOrUrl);
            }
            ontologyReader = new FileReader(ontologyFileOrUrl);
        }
        final Writer out = argsList.size() == 3 ?
                new PrintWriter(argsList.get(2)) :
                new OutputStreamWriter(System.out);
        final Ontology ontology = ontoSerializer.read(ontologyReader);

        Messages.addHandler(new DefaultMessageHandler());
        
        final TranslationController controller = new TranslationController(Services.getAll(LanguageModelFactory.class),
                Services.get(DecoderFactory.class),
                Services.getAll(TranslationPhraseChunkerFactory.class),
                Services.getAll(TranslationSourceFactory.class),
                Services.getAll(TranslationFeaturizerFactory.class),
                Services.getFactory(TokenizerFactory.class),
                Services.getAll(TranslationConfidenceFactory.class));
        final SimpleLexicalizer lexicalizer = new SimpleLexicalizer(Services.get(LabelExtractorFactory.class));
        final Collection<Lexicon> sourceLexica = lexicalizer.lexicalize(ontology);
        final Language targetLanguage = Language.get(argsList.get(1));
        final Lexicon targetLexicon = lexicalizer.getBlankLexicon(ontology, targetLanguage);
        final ArrayList<URI> scopes = new ArrayList<URI>();
        for (String scopeStr : scopeStrs) {
            final URI scope = URI.create(scopeStr);
            if (scope == null) {
                throw new IllegalArgumentException(scopeStr + " is not a valid URI");
            }
            scopes.add(scope);
        }

        System.err.printf("Translating %d entities using %d lexica\n", ontology.getEntities().size(), sourceLexica.size());
        System.err.print("Source languages: ");
        for (Lexicon sourceLexicon : sourceLexica) {
            System.err.print(sourceLexicon.getLanguage() + " ");
        }
        System.err.println();

        controller.setVerbose(verbose);

        controller.translate(ontology, sourceLexica, targetLexicon, scopes, namePrefix, nBest, fast ? OntologyTranslator.DECODE_FAST : 0);

        System.err.println("Writing model");
        lemonSerializer.write(targetLexicon.getModel(), out);
        out.flush();
        out.close();
        System.err.println("Done");
    }
}
