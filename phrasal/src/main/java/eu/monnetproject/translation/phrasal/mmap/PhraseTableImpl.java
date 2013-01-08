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
package eu.monnetproject.translation.phrasal.mmap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author John McCrae
 */
public class PhraseTableImpl {

    private final FileChannel srcTable;
    private final TreeMemoryMap tmm;

    public PhraseTableImpl(FileInputStream srcTable, File mapFile) throws IOException, ClassNotFoundException {
        this.srcTable = srcTable.getChannel();
        this.tmm = new TreeMemoryMap(mapFile, PhraseTableMapper.KEY, PhraseTableMapper.OFFSET);
    }
    
    public List<PhraseTableEntry> getEntriesBySrc(String src) {
        try {
            final byte[] key = new byte[PhraseTableMapper.KEY];
            final byte[] phrBytes = src.getBytes();
            System.arraycopy(phrBytes, 0, key, 0, Math.min(PhraseTableMapper.KEY, phrBytes.length));
            final long[] range = tmm.get(key);
            if (range == null) {
                return Collections.EMPTY_LIST;
            } else {
                final long size = range[1] - range[0];
                final MappedByteBuffer map = srcTable.map(FileChannel.MapMode.READ_ONLY, range[0], size);
                byte[] buf = new byte[(int) size];
                map.get(buf);
                String s = new String(buf);
                String ss[] = s.split(System.getProperty("line.separator"));
                LinkedList<PhraseTableEntry> rval = new LinkedList<PhraseTableEntry>();
                for (String s2 : ss) {
                    final PhraseTableEntry pte = PhraseTableEntry.fromLine(s2);
                    if (pte.getSrc().equals(src)) {
                        rval.add(pte);
                    }
                }
                return rval;
            }
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    public List<PhraseTableEntry> getRelevantEntries(List<String> tokens) {
        LinkedList<PhraseTableEntry> phraseTable = new LinkedList<PhraseTableEntry>();
        ListIterator<String> iter1 = tokens.listIterator();
        while (iter1.hasNext()) {
            ListIterator<String> iter2 = tokens.listIterator(iter1.nextIndex() + 1);
            StringBuilder subtokens = new StringBuilder();
            subtokens.append(iter1.next()).append(" ");
            do {
                List<PhraseTableEntry> entriesBySrc = getEntriesBySrc(subtokens.toString().trim());
                phraseTable.addAll(entriesBySrc);
                if (iter2.hasNext()) {
                    subtokens.append(iter2.next()).append(" ");
                }
            } while (iter2.hasNext());
            phraseTable.addAll(getEntriesBySrc(subtokens.toString().trim()));
        }
        return phraseTable;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage:\n\tPhraseTableImpl phrase-table map-file key");
            System.exit(-1);
        }

        String[] words = new String[args.length - 2];
        System.arraycopy(args, 2, words, 0, args.length - 2);
        List<PhraseTableEntry> entriesBySrc = new PhraseTableImpl(new FileInputStream(args[0]), new File(args[1])).getRelevantEntries(Arrays.asList(words));
        for (PhraseTableEntry entry : entriesBySrc) {
            System.err.println(entry.toString());
        }


    }
}
