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
import java.util.LinkedList;
import java.util.ListIterator;

/**
 *
 * @author John McCrae
 */
public class TrieMemoryMap implements MemoryMap<CharSequence> {

    /** Can be used to check the file is a valid map */
    public final static int CHECK = -10664893;
    // The channel where data is stored
    private final FileChannel channel;
    // The buffers used by the last key
    private final LinkedList<MappedByteBuffer> buffers;
    // The characters of the last key
    private final LinkedList<Character> lastKey;
    // The gap to leave at the start of the file for other data
    private final int initialOffset;
    // The current end of the written data
    private long offset;
    // Size of an entry, 1 char and 3  longs = 2 + 3 * 8 = 26
    private static final int ENTRY_SIZE = 26;
    // Size of a data block, 2 longs = 2 * 8
    private static final int DATA_SIZE = 16;

    public TrieMemoryMap(File file) throws IOException {
        this.channel = new RandomAccessFile(file, "rw").getChannel();
        this.buffers = new LinkedList<MappedByteBuffer>();
        this.lastKey = new LinkedList<Character>();
        this.initialOffset = 0;
        this.offset = initialOffset;
    }

    public TrieMemoryMap(File file, int initialOffset) throws IOException {
        this.channel = new RandomAccessFile(file, "rw").getChannel();
        this.buffers = new LinkedList<MappedByteBuffer>();
        this.lastKey = new LinkedList<Character>();
        this.initialOffset = initialOffset;
        this.offset = initialOffset;
    }
    private boolean isClosed = false;

    @Override
    public void close(long idx) throws IOException {
        // Set the flag
        if (isClosed) {
            throw new AlreadyClosedException();
        }
        isClosed = true;
        // Insert final entry
        insert(lastKeySeq, lastIdx, idx);
    }
    
    @Override
    public void dispose() throws IOException {
        channel.close();
    }

    @Override
    public long[] get(CharSequence key) throws IOException {
        long loc = initialOffset;
        if (key.length() == 0) {
            throw new IllegalArgumentException("Cannot index empty string");
        }
        BufferEntry buffer = null;
        for (int i = 0; i < key.length(); i++) {
            // Read the next buffer
            buffer = read(loc);
            // Traverse nexts until we find the correct character
            while (buffer.character < key.charAt(i)) {
                if (buffer.next == 0) {
                    return null;
                }
                buffer = read(buffer.next);
                if (buffer.character == 0) {
                    return null;
                }
            }
            if (buffer.character != key.charAt(i)) {
                return null;
            }
            // If we are not finished and can't descend the key is too long
            if (buffer.descend == 0 && i + 1 < key.length()) {
                return null;
            }
            loc = buffer.descend;
        }
        // If the entry has no data the key is too short
        if (buffer.data == 0) {
            return null;
        }
        // Read the data from the buffer
        long[] rval = new long[2];
        final MappedByteBuffer map = channel.map(READ_ONLY, buffer.data, DATA_SIZE);
        rval[0] = map.getLong();
        rval[1] = map.getLong();
        return rval;
    }
    private long lastIdx;
    private CharSequence lastKeySeq;
    
    @Override
    public void put(CharSequence key, long idx) throws IOException {
        if (isClosed) {
            throw new AlreadyClosedException();
        }
        if (key.length() == 0) {
            throw new IllegalArgumentException("Cannot index empty string");
        }
        // We insert the previous key using this index
        if (lastKeySeq != null) {
            insert(lastKeySeq, lastIdx, idx);
        }
        lastIdx = idx;
        lastKeySeq = key;
    }

    private BufferEntry read(long loc) throws IOException {
        final MappedByteBuffer buffer = channel.map(READ_ONLY, loc, ENTRY_SIZE);
        return new BufferEntry(buffer.getChar(), buffer.getLong(), buffer.getLong(), buffer.getLong());
    }

    private BufferEntry read(MappedByteBuffer buffer) throws IOException {
        buffer.position(0);
        return new BufferEntry(buffer.getChar(), buffer.getLong(), buffer.getLong(), buffer.getLong());
    }

    private void write(long l, BufferEntry entry) throws IOException {
        write(channel.map(READ_WRITE, l, ENTRY_SIZE), entry);
    }

    private void write(MappedByteBuffer buffer, BufferEntry entry) throws IOException {
        buffer.position(0);
        buffer.putChar(entry.character);
        buffer.putLong(entry.descend);
        buffer.putLong(entry.next);
        buffer.putLong(entry.data);
    }

    private MappedByteBuffer append(BufferEntry entry) throws IOException {
        final MappedByteBuffer buffer = channel.map(READ_WRITE, offset, ENTRY_SIZE);
        write(buffer, entry);
        offset += ENTRY_SIZE;
        return buffer;
    }

    private MappedByteBuffer appendData(long s, long e) throws IOException {
        final MappedByteBuffer buffer = channel.map(READ_WRITE, offset, DATA_SIZE);
        buffer.putLong(s);
        buffer.putLong(e);
        offset += DATA_SIZE;
        return buffer;
    }

    private void insert(CharSequence key, long lastIdx, long idx) throws IOException {
        ListIterator<MappedByteBuffer> bufferIter = buffers.listIterator();
        ListIterator<Character> charIter = lastKey.listIterator();
        int i = 0;
        // First we attempt to use the existing buffers
        while (bufferIter.hasNext()) {
            final MappedByteBuffer buffer = bufferIter.next();
            final char c = charIter.next();

            // Check for non-ascending
            if (c > key.charAt(i)) {
                insertNonAscending(key, lastIdx, idx);
                return;
                // If we differ in place i
            } else if (c < key.charAt(i)) {
                final BufferEntry b = read(buffer);
                if(b.next != 0) {
                    throw new RuntimeException();
                }
                // Update the last buffer's next pointer
                write(buffer, b.next(offset));
                // Add the new entry
                BufferEntry newEntry = new BufferEntry(key.charAt(i), i + 1 == key.length() ? 0 : offset + ENTRY_SIZE, 0, i + 1 == key.length() ? offset + ENTRY_SIZE : 0);
                bufferIter.set(append(newEntry));
                charIter.set(key.charAt(i));
                // Remove all stored buffers after this point
                while (bufferIter.hasNext()) {
                    bufferIter.next();
                    charIter.next();
                    bufferIter.remove();
                    charIter.remove();
                }
                // Add any new buffers
                for (i = i + 1; i < key.length(); i++) {
                    newEntry = new BufferEntry(key.charAt(i), i + 1 == key.length() ? 0 : offset + ENTRY_SIZE, 0, i + 1 == key.length() ? offset + ENTRY_SIZE : 0);
                    buffers.add(append(newEntry));
                    lastKey.add(key.charAt(i));
                }
                appendData(lastIdx, idx);
                return;
            }
            i++;


            // If we are at the end of the key, we insert the data
            if (i >= key.length()) {
                final BufferEntry entry = read(buffer);
                if (entry.data != 0) {
                    throw new IllegalArgumentException("Duplicate key: " + key);
                }
                write(buffer, entry.data(offset));
                appendData(lastIdx, idx);
                // Remove all stored buffers after this point
                while (bufferIter.hasNext()) {
                    bufferIter.next();
                    charIter.next();
                    bufferIter.remove();
                    charIter.remove();
                }
                return;
            }
        }
        if (buffers.isEmpty()) {
            // Initial key creation
            for (i = 0; i < key.length(); i++) {
                BufferEntry newEntry = new BufferEntry(key.charAt(i), i + 1 == key.length() ? 0 : offset + ENTRY_SIZE, 0, i + 1 == key.length() ? offset + ENTRY_SIZE : 0);
                buffers.add(append(newEntry));
                lastKey.add(key.charAt(i));
            }
            appendData(lastIdx, idx);
        } else {
            // In case the new key is longer than the previous
            // Add a new child
            final MappedByteBuffer last = buffers.getLast();
            final BufferEntry entry = read(last);
            if (entry.descend != 0) {

                insertNonAscending(key, lastIdx, idx);
                return;
            }
            write(last, entry.descend(offset));
            // Write extra keys
            for (; i < key.length(); i++) {
                final BufferEntry entry2 = new BufferEntry(key.charAt(i), i + 1 == key.length() ? 0 : offset + ENTRY_SIZE, 0, i + 1 == key.length() ? offset + ENTRY_SIZE : 0);
                buffers.add(append(entry2));
                lastKey.add(key.charAt(i));
            }
            appendData(lastIdx, idx);
        }
    }

    private void insertNonAscending(CharSequence key, long lastIdx, long idx) throws IOException {
        // loc should be the next key, lastLoc is the prev key, new key should be inserted between them
        long loc = initialOffset, lastLoc = -1;
        BufferEntry buffer = null, lastBuffer = null;
        for (int i = 0; i < key.length(); i++) {
            // Does the descend or next pointer of last need to be changed
            boolean descend = true;
            // Read the next buffer
            buffer = read(loc);
            // Traverse nexts until we find the correct character
            while (buffer.character < key.charAt(i)) {
                if (buffer.next == 0) {
                    write(loc, buffer.next(offset));
                    for (; i < key.length(); i++) {
                        append(new BufferEntry(key.charAt(i), i + 1 == key.length() ? 0 : offset + ENTRY_SIZE, 0, i + 1 == key.length() ? offset + ENTRY_SIZE : 0));
                    }
                    appendData(lastIdx, idx);
                    return;
                }
                lastLoc = loc;
                loc = buffer.next;
                lastBuffer = buffer;
                buffer = read(buffer.next);
                descend = false;
            }
            // If the character is the same we need to descend
            if (buffer.character == key.charAt(i)) {
                // If we can't descend this key is a superstring of an existing key
                if (buffer.descend == 0) {
                    write(loc, buffer.descend(offset));
                    // Write extra keys
                    for (i = i + 1; i < key.length(); i++) {
                        append(new BufferEntry(key.charAt(i), i + 1 == key.length() ? 0 : offset + ENTRY_SIZE, 0, i + 1 == key.length() ? offset + ENTRY_SIZE : 0));
                    }
                    appendData(lastIdx, idx);
                    return;
                }
                // Otherwise we descend
                lastLoc = loc;
                loc = buffer.descend;
                lastBuffer = buffer;
            } else { // The character is not the same... hence we have a difference
                // The key is lower than every key at index 0, this means we would have to introduce a new initial key... so we fail!
                if (lastBuffer == null) {
                    throw new IllegalArgumentException("Map shift error");
                }
                // We update the old pointer
                if (descend) {
                    if(loc != lastBuffer.descend) {
                        throw new RuntimeException();
                    }
                    write(lastLoc, lastBuffer.descend(offset));
                } else {
                    if(loc != lastBuffer.next) {
                        throw new RuntimeException();
                    }
                    write(lastLoc, lastBuffer.next(offset));
                }
                append(new BufferEntry(key.charAt(i), i + 1 == key.length() ? 0 : offset + ENTRY_SIZE, loc, i + 1 == key.length() ? offset + ENTRY_SIZE : 0));
                // Write extra keys
                for (i = i + 1; i < key.length(); i++) {
                    append(new BufferEntry(key.charAt(i), i + 1 == key.length() ? 0 : offset + ENTRY_SIZE, 0, i + 1 == key.length() ? offset + ENTRY_SIZE : 0));
                }
                appendData(lastIdx, idx);
                return;
            }
        }
        if (buffer.data == 0) {
            // Key is substring of existing key (note loc and buffer are out-of-sync at this point)
            write(lastLoc, buffer.data(offset));
            appendData(lastIdx, idx);
        } else {
            // The key seems to be exactly the same as an existing key... we must fail
            throw new IllegalArgumentException("Duplicate key: " + key);
        }
    }

    private static final class BufferEntry {

        char character;
        long descend;
        long next;
        long data;

        public BufferEntry(char character, long descend, long next, long data) {
            this.character = character;
            this.descend = descend;
            this.next = next;
            this.data = data;
        }

        public BufferEntry descend(long d) {
            return new BufferEntry(character, d, next, data);
        }

        public BufferEntry next(long n) {
            return new BufferEntry(character, descend, n, data);
        }

        public BufferEntry data(long d) {
            return new BufferEntry(character, descend, next, d);
        }
    }
}
