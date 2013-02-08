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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
//import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author John McCrae
 */
public class DirectDataMMap {

    public static final int CACHE_SIZE = 4096;
    private final Map<RecordID, long[]> dataLocMap;
    // Must query dataLocMap.contains before cache !
    //private final LoadingCache<RecordID, byte[]> cache;
    private final LoadingCache<RecordID, ByteBuffer> cache;
    private final FileChannel fileChannel;
    // private long mapped = 0;

    public DirectDataMMap(File base) throws IOException {
        this.dataLocMap = readMap(base);
        fileChannel = new FileInputStream(new File(base.getPath() + ".data")).getChannel();
        cache = makeCache();
    }

    DirectDataMMap(File base, Map<RecordID, long[]> dataLocMap) throws IOException {
        this.dataLocMap = dataLocMap;
        fileChannel = new FileInputStream(new File(base.getPath() + ".data")).getChannel();
        cache = makeCache();
    }

    private Map<RecordID, long[]> readMap(File base) throws IOException {
        final HashMap<RecordID, long[]> map = new HashMap<RecordID, long[]>();
        final DataInputStream dis = new DataInputStream(new FileInputStream(new File(base.getPath() + ".map")));
        while (dis.available() > 0) {
            try {
                int key = dis.readInt();
                int n = dis.readInt();
                long start = dis.readLong();
                long end = dis.readLong();
                map.put(new RecordID(n, key), new long[]{start, end});
            } catch (EOFException x) {
                break;
            }
        }
        return map;
    }

    public double[] rawScore(int[] key) {
        final RecordID rid = new RecordID(key.length, key[0]);
        final long[] l = dataLocMap.get(rid);
        if (l == null) {
            return null;
        }
        final int rs = recordSize(key.length);
        int mask = (int) (l[1] - l[0] - 1) / rs;
        try {
            //final byte[] data;
            final ByteBuffer data;
            //synchronized (this) {
            data = cache.get(rid);
            //}
            return get(data, key, mask, rs);
        } catch (ExecutionException x) {
            throw new RuntimeException(x);
        }
    }

    private int recordSize(int n) {
        return 4 * n + 16;
    }

    private double read(byte[] data, int o) {
        long l = 0;
        for (int i = 0; i < 7; i++) {
            long l2 = data[o + i] & 0xff;
            l |= l2;
            l = l << 8;
        }
        l |= data[o + 7];
        return Double.longBitsToDouble(l);
    }

    private int validateKey(ByteBuffer data, int pos, int[] k, int arrHash) {
        if (data.get() == 0 && data.get() == 0 && data.get() == 0 && data.get() == 0) {
            return -1;
        } else {
            data.position(pos);
        }
        for (int i = 0; i < k.length; i++) {
            if (data.get() != ((k[i] & 0xff000000) >>> 24)) {
                return 0;
            }
            if (data.get() != ((k[i] & 0xff0000) >>> 16)) {
                return 0;
            }
            if (data.get() != ((k[i] & 0xff00) >>> 8)) {
                return 0;
            }
            if (data.get() != ((k[i] & 0xff))) {
                return 0;
            }
        }
        return 1;
    }
//        if (data[pos + 3] == 0 && data[pos + 2] == 0 && data[pos + 1] == 0 && data[pos] == 0) {
//            return -1;
//        }
//        for (int i = 0; i < k.length; i++) {
//            
//            if ((k[i] & 0xff) != (data[pos + 3] & 0xff)) {
//                return 0;
//            }
//            if (((k[i] & 0xff00) >>> 8) != (data[pos + 2] & 0xff)) {
//                return 0;
//            }
//            if (((k[i] & 0xff0000) >>> 16) != (data[pos + 1] & 0xff)) {
//                return 0;
//            }
//            if (((k[i] & 0xff000000) >>> 24) != (data[pos] & 0xff)) {
//                return 0;
//            }
//            pos += 4;
//        }
//        return 1;
//    }

    private double[] get(ByteBuffer data, int[] k, int mask, int rs) {
        synchronized (data) { // Other threads can change the position of the buffer :(
            int arrHash = MurmurHash.hash32(k);
            int pos = (arrHash & mask) * rs;
            data.position(pos);
            int keyVal = validateKey(data, pos, k, arrHash);
            pos += 4 * k.length;
            while (keyVal >= 0) {
                if (keyVal > 0) {
                    double p = data.getDouble();//read(data, pos);
                    double a = data.getDouble();//read(data, pos + 8);
                    if (a != 0.0) {
                        return new double[]{p, a};
                    } else {
                        return new double[]{p};
                    }
                }
                arrHash++;
                pos = (arrHash & mask) * rs;
                data.position(pos);
                keyVal = validateKey(data, pos, k, arrHash);
                pos += 4 * k.length;
            }
        }
        return null;
    }

    public void close() {
        cache.invalidateAll();
    }

    //private LoadingCache<RecordID, byte[]> makeCache() {
    private LoadingCache<RecordID, ByteBuffer> makeCache() {
        return CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).softValues().build(new CacheLoader<RecordID, ByteBuffer>() {
            @Override
            public ByteBuffer load(RecordID k) throws Exception {
                final long[] startEnd = dataLocMap.get(k);
                final int size = (int) (startEnd[1] - startEnd[0]);
                //byte[] data = new byte[size];
                try {
                    final MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_ONLY, startEnd[0], startEnd[1] - startEnd[0]);
                    //map.get(data);
                    //return data;
                    return map;
                } catch (IOException x) {
                    System.err.println("page size: " + size);
                    x.printStackTrace();
                    return ByteBuffer.allocate(size);
                }
            }
        });
    }

    static class RecordID {

        final public int n;
        final public int w;

        public RecordID(int n, int w) {
            this.n = n;
            this.w = w;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + this.n;
            hash = 29 * hash + this.w;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RecordID other = (RecordID) obj;
            if (this.n != other.n) {
                return false;
            }
            if (this.w != other.w) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "(w=" + w + ",n=" + n + ")";
        }
    }
}
