/**********************************************************************************
 * Copyright (c) 2011, Monnet Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Monnet Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
package eu.monnetproject.translation.phrasal.mert;

import eu.monnetproject.lang.Language;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.ChunkList;
import eu.monnetproject.translation.Decoder;
import eu.monnetproject.translation.DecoderWeights;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.LanguageModel;
import eu.monnetproject.translation.TranslationFeaturizer;
import eu.monnetproject.translation.TranslationPhraseChunker;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.phrasal.ChunkImpl;
import eu.monnetproject.translation.phrasal.FairlyGoodTokenizer;
import eu.monnetproject.translation.phrasal.PhrasalDecoderFactory;
import eu.monnetproject.translation.phrasal.lm.ARPALanguageModelFactory;
import eu.monnetproject.translation.phrasal.pt.MemoryMappedPhraseTableSourceFactory;
import eu.monnetproject.translation.tune.TranslatorSetup;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class DefaultTranslatorSetup implements TranslatorSetup {

    private final Language srcLang, trgLang;
    private final TranslationPhraseChunker chunker;
    private final TranslationSource source;
    private final LanguageModel model;
    private final PhrasalDecoderFactory factory = new PhrasalDecoderFactory();

    private static final MemoryMappedPhraseTableSourceFactory mmpts = new MemoryMappedPhraseTableSourceFactory();
    
    public DefaultTranslatorSetup(Language srcLang, Language trgLang) {
        this.srcLang = srcLang;
        this.trgLang = trgLang;
        class ChunkListImpl extends LinkedList<Chunk> implements ChunkList { 

            public ChunkListImpl(Collection<String> c) {
                super();
                for(String s  : c) {
                    super.add(new ChunkImpl(s));
                }
            }
        
        }
        chunker = new TranslationPhraseChunker() {

            @Override
            public ChunkList chunk(Label label) {
                return new ChunkListImpl(Arrays.asList(FairlyGoodTokenizer.split(label.asString())));
            }
        };
        source = mmpts.getSource(srcLang, trgLang);
        model = new ARPALanguageModelFactory().getModel(trgLang);
    }

    @Override
    public TranslationPhraseChunker chunker(Entity entity) {
        return chunker;
    }

    public TranslationSource source() {
        return source;
    }

    @Override
    public Collection<TranslationSource> sources() {
        return Collections.singleton(source());
    }

    @Override
    public Collection<TranslationFeaturizer> featurizers(Entity entity) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Decoder decoder(DecoderWeights weights) {
        return factory.getDecoder(model, weights);
    }

    @Override
    public DecoderWeights weights() {
        return factory.getDefaultWeights();
    }

    @Override
    public LanguageModel languageModel() {
        return model;
    }

    @Override
    public Language sourceLanguage() {
        return srcLang;
    }

    @Override
    public Language targetLanguage() {
        return trgLang;
    }

    @Override
    public List<String> featureNames() {
        return Arrays.asList(source.featureNames());
    }
    
    
}
