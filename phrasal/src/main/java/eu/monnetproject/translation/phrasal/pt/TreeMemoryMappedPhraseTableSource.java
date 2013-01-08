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
package eu.monnetproject.translation.phrasal.pt;

import eu.monnetproject.translation.phrasal.mmap.StableHashByteArray;
import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.phrasal.mmap.PhraseTableMapper;
import eu.monnetproject.translation.phrasal.mmap.TreeMemoryMap;
import eu.monnetproject.translation.phrasal.mmap.TreeMemoryMap;
import eu.monnetproject.translation.phrasal.pt.cache.Cache;
import eu.monnetproject.translation.monitor.Messages;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author John McCrae
 */
public class TreeMemoryMappedPhraseTableSource extends MemoryMappedPhraseTableSource<StableHashByteArray> {

    public TreeMemoryMappedPhraseTableSource(Cache<StableHashByteArray, PhraseTableEntry> cache, String filename, int featureCount, Language srcLang, Language trgLang) throws IOException {
        super(cache, filename, featureCount, srcLang, trgLang, makeMap(filename));
    }

    private static TreeMemoryMap makeMap(String filename) throws IOException {
        File mapFile = new File(filename + ".map");
        if (!mapFile.exists()) {
            File tableFile = new File(filename);
            if (!tableFile.exists()) {
                throw new IOException("Could not locate phrase table at " + tableFile.getPath());
            }
            Messages.warning("Phrase table map does not exist... creating... this may take a very long time");
            new PhraseTableMapper().mapPhraseTable(tableFile, mapFile,"tree");
        }
        return new TreeMemoryMap(mapFile, PhraseTableMapper.OFFSET);
    }

    @Override
    protected StableHashByteArray key(String label) {
        final byte[] key = new byte[PhraseTableMapper.KEY];
        final byte[] phrBytes = label.getBytes();
        System.arraycopy(phrBytes, 0, key, 0, Math.min(PhraseTableMapper.KEY, phrBytes.length));
        return new StableHashByteArray(key);
    }

    @Override
    protected void check(int check) throws IOException {
        if(check != TreeMemoryMap.CHECK)
            throw new IOException("Invalid file");
    }
    
    
}
