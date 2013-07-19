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
package eu.monnetproject.translation.bootstrapeval;

import eu.monnetproject.framework.services.Services;
import eu.monnetproject.label.LabelExtractor;
import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.lang.Language;
import eu.monnetproject.ontology.AnnotationProperty;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.ontology.Ontology;
import eu.monnetproject.ontology.OntologyFactory;
import eu.monnetproject.ontology.OntologySerializer;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author John McCrae
 */
public class SplitOntology {
    public static void main(String[] args) throws Exception {
        if(args.length < 2) {
            System.err.println("Usage:\n\t SplitOntology ontoFile lang1 lang2");
        }
        final OntologySerializer ontoSerializer = Services.get(OntologySerializer.class);
        final Ontology ontology = ontoSerializer.read(new FileReader(args[0]));
        final Set<Language> targetLangs = new HashSet<Language>();
        for(int i = 1; i < args.length; i++) {
            targetLangs.add(Language.get(args[i]));
        }
        final LabelExtractorFactory lef = Services.get(LabelExtractorFactory.class);
        final LabelExtractor extractor = lef.getExtractor(Collections.EMPTY_LIST, false, false);
        final Map<Language,OntologySerializer> serializers = new HashMap<Language, OntologySerializer>();
        final Map<Language,Ontology> targetOntologies = new HashMap<Language, Ontology>();
        for(Language targetLang : targetLangs) {
            final OntologySerializer serializer = Services.get(OntologySerializer.class);
            targetOntologies.put(targetLang, serializer.create(ontology.getURI() != null ? ontology.getURI() : new File(args[0]).toURI()));
            serializers.put(targetLang, serializer);
        }
        
        for(Entity entity : ontology.getEntities()) {
            final Map<Language, Collection<String>> labels = extractor.getLabels(entity);
            for(Map.Entry<Language,Collection<String>> labelEntry : labels.entrySet()) {
                if(targetLangs.contains(labelEntry.getKey())) {
                    final Ontology targetOntology = targetOntologies.get(labelEntry.getKey());
                    final OntologyFactory ontoFactory = targetOntology.getFactory();
                    final AnnotationProperty rdfsLabel = ontoFactory.makeAnnotationProperty(URI.create("http://www.w3.org/2000/01/rdf-schema#label"));
                    final Entity newEntity;
                    if(entity instanceof eu.monnetproject.ontology.Class) {
                        newEntity = ontoFactory.makeClass(entity.getURI());
                        targetOntology.addClass((eu.monnetproject.ontology.Class)newEntity);
                    } else if(entity instanceof eu.monnetproject.ontology.ObjectProperty) {
                        newEntity = ontoFactory.makeObjectProperty(entity.getURI());
                        targetOntology.addObjectProperty((eu.monnetproject.ontology.ObjectProperty)newEntity);
                    } else if(entity instanceof eu.monnetproject.ontology.DatatypeProperty) {
                        newEntity = ontoFactory.makeDatatypeProperty(entity.getURI());
                        targetOntology.addDatatypeProperty((eu.monnetproject.ontology.DatatypeProperty)newEntity);
                    } else if(entity instanceof eu.monnetproject.ontology.Individual) {
                        newEntity = ontoFactory.makeIndividual(entity.getURI());
                        targetOntology.addIndividual((eu.monnetproject.ontology.Individual)newEntity);
                    } else if(entity instanceof eu.monnetproject.ontology.AnnotationProperty) {
                        newEntity = ontoFactory.makeAnnotationProperty(entity.getURI());
                        targetOntology.addAnnotationProperty((eu.monnetproject.ontology.AnnotationProperty)newEntity);
                    } else {
                        throw new RuntimeException("Entity not a class, property, individual or annotation!");
                    }
                    for(String label : labelEntry.getValue()) {
                        newEntity.addAnnotation(rdfsLabel, ontoFactory.makeLiteralWithLanguage(label, labelEntry.getKey().toString()));
                    }
                }   
            }
        }
        for(Map.Entry<Language,Ontology> toEntry : targetOntologies.entrySet()) {
            final PrintWriter out = new PrintWriter(new File(args[0]+"."+toEntry.getKey()));
            serializers.get(toEntry.getKey()).write(toEntry.getValue(), out);
            out.flush();
            out.close();
        }
    }
}
