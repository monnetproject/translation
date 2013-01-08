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

import eu.monnetproject.translation.monitor.Messages;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

/**
 * Create a map of an alphabetically sorted pseudo-Pharaoh phrase table
 * 
 * @author John McCrae
 */
public class PhraseTableMapper {

    public static final int KEY = 4;
    public static final int OFFSET = 8;
    
    public void mapPhraseTable(File phraseTable, File mapFile) throws IOException {
        mapPhraseTable(phraseTable,mapFile,"trie");
    }
    
    public void mapPhraseTable(File phraseTable, File mapFile, String method) throws IOException {
        final CountingLineReader reader = new CountingLineReader(new FileInputStream(phraseTable));
        MemoryMap map;
        if(method.equals("tree")) {
            map = new TreeMemoryMap(mapFile, KEY, OFFSET);
        } else if(method.equals("trie")){
            map = new TrieMemoryMap(mapFile, OFFSET);
        } else {
            throw new IllegalArgumentException("Unknown method " + method);
        }
        int longestPhrase = 0;
        CountingLineReader.Line s, s2 = null;
        Object lastKey = "";
        int i = 0;
        while ((s = reader.readLine()) != null) {
            if(++i % 1000000 == 0) {
                System.err.print(".");
            }
            int idx = s.line.indexOf(" |||");
            if (idx > 0) {
                final String ePhr = s.line.substring(0, idx);
                if(!ePhr.trim().equals(ePhr)) {
                    throw new RuntimeException("Untrimmed string");
                }
                Object key;
                if(method.equals("tree")) {
                    final byte[] key2 = new byte[KEY];
                    final byte[] ePhrBytes = ePhr.getBytes();
                    System.arraycopy(ePhrBytes, 0, key2, 0, Math.min(ePhrBytes.length, KEY));
                    key = new StableHashByteArray(key2);
                } else {
                    key = ePhr.trim() + " ";
                }
                if(!lastKey.equals(key)) {
                    try {
                        map.put(key, s.start);
                    } catch(NonAscendingKeyException x) {
                        Messages.warning("Not ascending " + key);
                    }
                }
                
                int phraseLength = ePhr.split("\\s+").length;
                if(phraseLength > longestPhrase)
                    longestPhrase = phraseLength;
                s2 = s;
                lastKey = key;
            }
        }
        map.close(s2.end);
        final MappedByteBuffer map1 = new RandomAccessFile(mapFile, "rw").getChannel().map(MapMode.READ_WRITE, 0, OFFSET);
        if(method.equals("tree")) {
            map1.putInt(TreeMemoryMap.CHECK);
        } else {
            map1.putInt(TrieMemoryMap.CHECK);
        }
        map1.putInt(longestPhrase);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 2) {
            System.err.println("Usage:\n\t PhraseTableMapper phrase-table [method]");
            System.exit(-1);
        }
        final PhraseTableMapper ptm = new PhraseTableMapper();
        if(args.length == 1) {
            ptm.mapPhraseTable(new File(args[0]), new File(args[0] + ".map"));
        } else {
            ptm.mapPhraseTable(new File(args[0]), new File(args[0] + ".map"),args[1]);
        }
    }
}
