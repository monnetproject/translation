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
package eu.monnetproject.translation.langmodel;

import eu.monnetproject.translation.langmodel.DirectDataHashMap.Data;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author John McCrae
 */
public class DirectDataMMapBuilder {

    private final DataOutputStream dosMap, dosData;
    private DirectDataHashMap map;
    private int w, n;
    private long pos;
    final Map<DirectDataMMap.RecordID, long[]> dataLocMap = new HashMap<DirectDataMMap.RecordID, long[]> ();
    private File dataFile;
    
    public DirectDataMMapBuilder(File base) throws IOException {
        this.dosMap = new DataOutputStream(new FileOutputStream(new File(base.getPath()+ ".map")));
        dataFile = new File(base.getPath() + ".data");
        this.dosData = new DataOutputStream(new FileOutputStream(dataFile));
        w = -1;
        n = 0;
    }

    public void accept(int[] ngram, double[] probs) throws IOException {
        assert (ngram[0] != 0); // This is empty record
        if (ngram[0] != w) {
            if (map != null) {
                dosMap.writeInt(w);
                dosMap.writeInt(n);
                if(dataLocMap.containsKey(new DirectDataMMap.RecordID(n, w))) {
                    throw new IllegalArgumentException("LM out of sequence");
                }
                long begin = pos;
                dosMap.writeLong(pos);
                final Data data = map.getData();
                assert (data.k.length == data.v.length);
                for (int i = 0; i < data.k.length; i++) {
                    if (data.k[i] == null) {
                        for (int j = 0; j < n; j++) {
                            dosData.writeInt(0);
                        }
                        for (int j = 0; j < 2; j++) {
                            dosData.writeDouble(0.0);
                        }
                    } else {
                        assert (data.k[i].length == n);

                        for (int j = 0; j < n; j++) {
                            dosData.writeInt(data.k[i][j]);
                        }
                        dosData.writeDouble(data.v[i][0]);
                        if (data.v[i].length >= 2) {
                            dosData.writeDouble(data.v[i][1]);
                        } else {
                            dosData.writeDouble(0);
                        }
                    }
                }
                pos += data.k.length * 4 * n + data.k.length * 8 * 2;
                dosMap.writeLong(pos);
                dataLocMap.put(new DirectDataMMap.RecordID(n, w), new long[] { begin, pos });
            }
            map = new DirectDataHashMap();
            w = ngram[0];
            n = ngram.length;
        }
        map.put(ngram, probs);
    }

    public void close() throws IOException {
        if (map != null) {
            dosMap.writeInt(w);
            dosMap.writeInt(n);
            long begin = pos;
            dosMap.writeLong(pos);
            final Data data = map.getData();
            assert (data.k.length == data.v.length);
            for (int i = 0; i < data.k.length; i++) {
                if (data.k[i] == null) {
                    for (int j = 0; j < n; j++) {
                        dosData.writeInt(0);
                    }
                    for (int j = 0; j < 2; j++) {
                        dosData.writeDouble(0.0);
                    }
                } else {
                    assert (data.k[i].length == n);

                    for (int j = 0; j < n; j++) {
                        dosData.writeInt(data.k[i][j]);
                    }
                    dosData.writeDouble(data.v[i][0]);
                    if (data.v[i].length >= 2) {
                        dosData.writeDouble(data.v[i][1]);
                    } else {
                        dosData.writeDouble(0);
                    }
                }
            }
            pos += data.k.length * 4 * n + data.k.length * 8 * 2;
            dosMap.writeLong(pos);
            dataLocMap.put(new DirectDataMMap.RecordID(n, w), new long[] { begin, pos });
        }
        dosMap.flush();
        dosData.flush();
        dosMap.close();
        dosData.close();
        assert(pos == dataFile.length());
    }
}
