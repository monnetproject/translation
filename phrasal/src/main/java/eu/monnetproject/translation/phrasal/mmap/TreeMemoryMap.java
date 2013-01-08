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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.channels.FileChannel.MapMode.*;

/**
 * Map a file on disk using a tree.
 * 
 * @author John McCrae
 */
public class TreeMemoryMap implements MemoryMap<StableHashByteArray> {

    /** Can be used to check the file is a valid map */
    public static final int CHECK = 1775746669;
    private final FileChannel channel;
    private final int keySize;
    private final MappedByteBuffer[] buffers;
    private final int initialOffset;
    private long end = 0;
    private byte[] lastKey;
    // byte max value * size of long
    private static final int BLOCK = 2048;
    private static final int ENTRY = 16;
    private final byte[] BLANK = new byte[BLOCK];
    private boolean isClosed = false;

    public TreeMemoryMap(File file, int keySize) throws IOException {
        this.channel = new RandomAccessFile(file, "rw").getChannel();
        this.keySize = keySize;
        this.buffers = new MappedByteBuffer[keySize + 1];
        this.lastKey = new byte[keySize];
        this.initialOffset = 0;
    }

    public TreeMemoryMap(File file, int keySize, int initialOffset) throws IOException {
        this.channel = new RandomAccessFile(file, "rw").getChannel();
        this.keySize = keySize;
        this.buffers = new MappedByteBuffer[keySize + 1];
        this.lastKey = new byte[keySize];
        end = initialOffset;
        this.initialOffset = initialOffset;
    }

    // unsigned byte <
    private static boolean byteLeq(byte b1, byte b2) {
        return (((int) b1) & 0xff) < (((int) b2) & 0xff);
    }

    private static int abs(byte b1) {
        return b1 & 0xff;
    }

    /**
     * Add a value in the map
     * @param key The key, must be greater than the previous entered key. If it is equal to the last key the value will not be modified
     * @param idx The value to insert
     * @throws IOException If there was an exception reading the file
     * @throws IllegalArgumentException If the key is not valid, or the map is closed
     */
    @Override
    public void put(StableHashByteArray key, long idx) throws IOException {
        put(key.arr, idx);
    }

    public void put(byte[] key, long idx) throws IOException {
        if (key.length != keySize) {
            throw new IllegalArgumentException("Key size is not as specified in the constructor");
        }
        if (isClosed) {
            throw new AlreadyClosedException();
        }
        if (buffers[0] == null) {
            // initialize
            for (int i = 0; i < keySize; i++) {
                buffers[i] = channel.map(READ_WRITE, end, BLOCK);
                buffers[i].put(BLANK);
                buffers[i].position(8 * abs(key[i]));
                end += BLOCK;
                buffers[i].putLong(end);
            }
            buffers[keySize] = channel.map(READ_WRITE, end, ENTRY);
            buffers[keySize].putLong(idx);
            end += ENTRY;
        } else {
            for (int i = 0; i < keySize; i++) {
                if (key[i] == lastKey[i]) {
                    // same key just ignore
                    continue;
                    // key[i] == 0 is OK as it normally indicates no value 
                } else if (byteLeq(key[i], lastKey[i]) && key[i] != 0) {
                    throw new NonAscendingKeyException();
                    // putNonAscending(key, idx);
                    //return;
                } else {
                    // put the value of this new key
                    buffers[i].position(8 * abs(key[i]));
                    buffers[i].putLong(end);
                    // create new blocks
                    for (i = i + 1; i < keySize; i++) {
                        buffers[i] = channel.map(READ_WRITE, end, BLOCK);
                        buffers[i].put(BLANK);
                        buffers[i].position(8 * abs(key[i]));
                        end += BLOCK;
                        buffers[i].putLong(end);
                    }
                    // but the end in the current data section
                    buffers[keySize].putLong(idx);
                    // and start new data section
                    buffers[keySize] = channel.map(READ_WRITE, end, ENTRY);
                    buffers[keySize].putLong(idx);
                    end += ENTRY;
                }
            }
        }
        System.arraycopy(key, 0, lastKey, 0, keySize);
    }

    /**
     * Closes the map, essentially adding the second value to the last put entry
     * @param idx 
     */
    @Override
    public void close(long idx) {
        if (isClosed) {
            throw new AlreadyClosedException();
        }
        buffers[keySize].putLong(idx);
        isClosed = true;
    }

    /**
     * Get the range of the key
     * @param key The key
     * @return The range or null if the key is not in the map
     * @throws IOException If the file could not be read
     */
    public long[] get(byte[] key) throws IOException {
        if (key.length != keySize) {
            throw new IllegalArgumentException("Key size is not as specified in the constructor");
        }
        MappedByteBuffer buffer;
        long loc = initialOffset;
        for (int i = 0; i < keySize; i++) {
            buffer = channel.map(READ_ONLY, loc + 8 * abs(key[i]), 8);
            loc = buffer.getLong();
            if (loc == 0) {
                return null;
            }
        }
        buffer = channel.map(READ_ONLY, loc, ENTRY);
        long[] entry = new long[2];
        entry[0] = buffer.getLong();
        entry[1] = buffer.getLong();
        return entry;
    }
    
    @Override
    public long[] get(StableHashByteArray key) throws IOException {
        return get(key.arr);
    }
    
    

    /* private void putNonAscending(byte[] key, long idx) throws IOException {
    buffers[0].position(8 * abs(key[0]));
    long loc = buffers[0].getLong();
    MappedByteBuffer[] buffers2 = new MappedByteBuffer[keySize + 1];
    buffers2[0] = buffers[0];
    for (int i = 1; i < keySize; i++) {
    if (loc == 0) {
    buffers2[i - 1].position(8 * abs(key[0]));
    buffers2[i - 1].putLong(end);
    // create new blocks
    for (; i < keySize; i++) {
    buffers2[i] = channel.map(READ_WRITE, end, BLOCK);
    buffers2[i].put(BLANK);
    buffers2[i].position(8 * abs(key[i]));
    end += BLOCK;
    buffers2[i].putLong(end);
    }
    // but the end in the current data section
    buffers2[keySize].putLong(idx);
    // and start new data section
    buffers2[keySize] = channel.map(READ_WRITE, end, ENTRY);
    buffers2[keySize].putLong(idx);
    end += ENTRY;
    return;
    } else {
    buffers2[i] = channel.map(READ_WRITE, loc, BLOCK);
    buffers2[i].position(8 * abs(key[0]));
    loc = buffers2[i].getLong();
    }
    }
    if(loc == 0) {
    buffers2[keySize] = channel.map(READ_WRITE, end, ENTRY);
    buffers2[keySize].putLong(idx);
    end += ENTRY;
    } else {
    // no op
    }
    System.arraycopy(key, 0, lastKey, 0, keySize);
    }*/

    @Override
    public void dispose() throws IOException {
        channel.close();
    }
}
