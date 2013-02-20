/**
 * *******************************************************************************
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
package eu.monnetproject.translation.evalmetrics;

import eu.monnetproject.framework.services.Services;
import eu.monnetproject.label.LabelExtractor;
import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.lang.Language;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.ontology.OntologySerializer;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.eval.TranslationEvaluator;
import eu.monnetproject.translation.phrasal.eval.MetricWrapperFactory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Evaluate a SGM (as for example coming out of Moses)
 *
 * @author John McCrae
 */
public class EvaluateSGM {

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("Usage: mvn exec:java -Dexec.mainClass=" + EvaluateSGM.class.getCanonicalName() + " -Dexec.args=\"srcLang trgLang src.sgm trg.sgm ontology.rdf\"");
            return;
        }
        final Language srcLang = Language.get(args[0]);
        final Language trgLang = Language.get(args[1]);
        final File srcSGMfile = new File(args[2]);
        final File trgSGMfile = new File(args[3]);
        final File ontologyFile = new File(args[4]);

        final String[] srcArr = readSGM(srcSGMfile);
        final String[] trgArr = readSGM(trgSGMfile);
        if (srcArr.length != trgArr.length) {
            System.err.println("SGM files have different number of segments");
            return;
        }
        final HashMap<String, String> translations = new HashMap<String, String>();
        for (int i = 0; i < srcArr.length; i++) {
            translations.put(srcArr[i], trgArr[i]);
        }
        final OntologySerializer ontoSerializer = Services.get(OntologySerializer.class);
        final Ontology ontology = ontoSerializer.read(new FileReader(ontologyFile));
        final LabelExtractorFactory lef = Services.get(LabelExtractorFactory.class);
        final LabelExtractor extractor = lef.getExtractor(Collections.EMPTY_LIST, false, false);
        final LinkedList<List<Translation>> refs = new LinkedList<List<Translation>>();
        final LinkedList<Translation> transes = new LinkedList<Translation>();
        for (Entity entity : ontology.getEntities()) {
            final Collection<String> srcLabels = extractor.getLabels(entity).get(srcLang);
            if (srcLabels != null) {
                for (String srcLabel2 : srcLabels) {
                    final String srcLabel = srcLabel2.trim();
                    if(!translations.containsKey(srcLabel)) {
                        System.err.println("No translation for " + srcLabel + "#");
                        continue;
                    }
                    transes.add(new TranslationImpl(srcLabel, translations.get(srcLabel), srcLang, trgLang, entity.getURI()));
                    final LinkedList<Translation> ref = new LinkedList<Translation>();
                    final Collection<String> trgLabels = extractor.getLabels(entity).get(trgLang);
                    if (trgLabels != null && !trgLabels.isEmpty()) {
                        for (String trgLabel2 : trgLabels) {
                            final String trgLabel = trgLabel2.trim();
                            ref.add(new TranslationImpl(srcLabel, trgLabel, srcLang, trgLang, entity.getURI()));
                        }
                        refs.add(ref);
                    } else {
                        System.err.println("No target language labels for " + entity.getURI());
                    }
                }
            } else {
                System.err.println("No source language labels for " + entity.getURI());
            }
        }
        final Collection<TranslationEvaluator> evaluators = new MetricWrapperFactory().getEvaluators(refs);
        for(TranslationEvaluator evaluator : evaluators) {
            System.out.println(evaluator.getName() + ": " + evaluator.score(transes));
        }
    }

    private static String[] readSGM(final File sgmFile) throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document doc = db.parse(sgmFile);
        final NodeList segs = doc.getElementsByTagName("seg");
        final String[] labels = new String[segs.getLength()];
        for (int i = 0; i < segs.getLength(); i++) {
            labels[i] = segs.item(i).getTextContent().replaceAll("^\\s*", "").replaceAll("\\s*$", "");
        }
        return labels;
    }

    private static class TranslationImpl implements Translation {

        private final String srcLabel, trgLabel;
        private final Language srcLang, trgLang;
        private final URI entity;

        public TranslationImpl(String srcLabel, String trgLabel, Language srcLang, Language trgLang, URI entity) {
            this.srcLabel = srcLabel;
            this.trgLabel = trgLabel;
            this.srcLang = srcLang;
            this.trgLang = trgLang;
            this.entity = entity;
        }


        @Override
        public Label getSourceLabel() {
            return new Label() {
                @Override
                public String asString() {
                    return srcLabel;
                }

                @Override
                public Language getLanguage() {
                    return srcLang;
                }
            };
        }

        @Override
        public Label getTargetLabel() {
            return new Label() {
                @Override
                public String asString() {
                    return trgLabel;
                }

                @Override
                public Language getLanguage() {
                    return trgLang;
                }
            };
        }

        @Override
        public URI getEntity() {
            return entity;
        }

        @Override
        public double getScore() {
            return 0.0;
        }

        @Override
        public Collection<Feature> getFeatures() {
            return new LinkedList<Feature>();
        }
    }
}
