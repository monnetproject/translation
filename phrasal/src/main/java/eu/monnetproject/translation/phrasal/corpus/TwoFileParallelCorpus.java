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
package eu.monnetproject.translation.phrasal.corpus;

import eu.monnetproject.translation.corpus.ParallelCorpus;
import eu.monnetproject.translation.corpus.ParallelDocument;
import eu.monnetproject.translation.corpus.SentencePair;
import eu.monnetproject.translation.monitor.Messages;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author John McCrae
 */
public class TwoFileParallelCorpus implements ParallelCorpus {
    private final File sourceFile, targetFile;

    public TwoFileParallelCorpus(File sourceFile, File targetFile) {
        this.sourceFile = sourceFile;
        this.targetFile = targetFile;
    }

    @Override
    public Iterator<ParallelDocument> iterator() {
        return Collections.singletonList((ParallelDocument)new ParallelDocument() {

            @Override
            public Iterator<SentencePair> iterator() {
                try {
                    return new TwoFileParallelCorpusIterator();
                    
                } catch(FileNotFoundException x) {
                    throw new RuntimeException(x);
                }
            }
        }).iterator();
    }
    
    

    private class TwoFileParallelCorpusIterator implements Iterator<SentencePair> {

        private final BufferedReader sourceReader, targetReader;
        private SentencePair next;

        public TwoFileParallelCorpusIterator() throws FileNotFoundException {
            sourceReader = new BufferedReader(new FileReader(sourceFile));
            targetReader = new BufferedReader(new FileReader(targetFile));
            readNext();
        }

        private void readNext() {
            try {
                final String srcLine = sourceReader.readLine();
                final String trgLine = targetReader.readLine();
                if (srcLine != null && trgLine != null) {
                    next = new SentencePair() {

                        @Override
                        public String getSourceSentence() {
                            return srcLine;
                        }

                        @Override
                        public String getTargetSentence() {
                            return trgLine;
                        }
                    };
                } else {
                    if(srcLine != null || trgLine != null) {
                        Messages.warning("Parallel files do not have same word count!");
                    }
                    next = null;
                }
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public SentencePair next() {
            final SentencePair next2 = next;
            if(next2 == null) {
                throw new NoSuchElementException();
            }
            readNext();
            return next2;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not mutable.");
        }
    }
}
