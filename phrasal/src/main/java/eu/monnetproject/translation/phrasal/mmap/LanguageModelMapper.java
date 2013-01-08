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
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author John McCrae
 */
public class LanguageModelMapper {

    public static final int KEY = 4;

    public void mapLanguageModel(File languageModel, File mapFile) throws IOException {
        final CountingLineReader reader = new CountingLineReader(new FileInputStream(languageModel));
        CountingLineReader.Line s;
        while ((s = reader.readLine()) != null) {
            if (s.line.startsWith("\\data\\")) {
                break;
            }
            if (!s.line.matches("\\s*")) {
                throw new IllegalArgumentException("Bad line: " + s.line);
            }
        }

        List<Integer> nGramSizes = new LinkedList<Integer>();

        while ((s = reader.readLine()) != null) {
            if (s.line.matches("\\s*")) {
                break;
            }
            final Matcher m = Pattern.compile("ngram \\d+=(\\d+)").matcher(s.line);
            if (!m.matches()) {
                throw new IllegalArgumentException("Bad line: " + s.line);
            }
            nGramSizes.add(Integer.parseInt(m.group(1)));
        }
        final TreeMemoryMap tmm = new TreeMemoryMap(mapFile, KEY, 4 * nGramSizes.size() + 4);

        CountingLineReader.Line s2 = null;

        for (int i = 0; i < nGramSizes.size(); i++) {
            Messages.info((i + 1) + "-grams");
            while ((s = reader.readLine()) != null) {
                if (s.line.matches("\\s*")) {
                    continue;
                } else if (!s.line.matches("\\\\\\d+-grams.*")) {
                    throw new IllegalArgumentException("Bad line: " + s.line);
                } else {
                    break;
                }
            }

            Messages.info("Mapping...");
            while ((s = reader.readLine()) != null) {
                if (s.line.matches("\\s*")) {
                    break;
                }
                final String[] parts = s.line.split("\\t");
                if (parts.length < 2) {
                    throw new IllegalArgumentException("Bad line: " + s.line);
                }

                byte[] key = makeKey(parts[1], i);
                tmm.put(key, s.start);
                s2 = s;
            }
        }
        tmm.close(s2.end);

        final MappedByteBuffer header = new RandomAccessFile(mapFile, "rw").getChannel().map(MapMode.READ_WRITE, 0, 4 * nGramSizes.size() + 4);
        header.putInt(nGramSizes.size());
        for (int nGramSize : nGramSizes) {
            header.putInt(nGramSize);
        }
    }

    public static byte[] makeKey(final String phr, int i) {
        final byte[] key = new byte[KEY];
        final byte[] phrBytes = phr.getBytes();
        System.arraycopy(phrBytes, 0, key, 0, Math.min(phrBytes.length, KEY));
        final BigInteger bi = new BigInteger(key);
        System.arraycopy(bi.shiftRight(3).toByteArray(), 0, key, 0, KEY);
        key[0] = (byte) (((int) key[0] & 0x1f) + (i << 5));
        return key;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage:\n\tLanguageModelMapper lmFile mapFile");
            System.exit(-1);
        }
        final LanguageModelMapper lmm = new LanguageModelMapper();
        lmm.mapLanguageModel(new File(args[0]), new File(args[1]));
    }
}
