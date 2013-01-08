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

import java.io.IOException;
import java.io.InputStream;

/**
 * Read an input stream line by line, recording the beginning and end of the line in bytes
 * 
 * @author John McCrae
 */
public class CountingLineReader {
    private final InputStream is;
    // The current absolute read point in the stream
    private long pos = 0;
    // The last point where a line was returned, absolutely in the stream
    private long mark = 0;
    // The current read point, relative to the buffer
    private int bufPos = 0;
    // The last point where a line was returned, relative to the buffer
    private int bufMark;
    // The number of valid bytes in the buffer
    private int read = 0;
    private final int BUF_SIZE;
    private final byte[] buf;
    

    public CountingLineReader(InputStream is) {
        this.is = is;
        BUF_SIZE = 32768;
        bufMark = BUF_SIZE;
        buf = new byte[BUF_SIZE];
        
    }
    
    public CountingLineReader(InputStream is, int bufferSize) {
        this.is = is;
        BUF_SIZE = bufferSize;
        bufMark = BUF_SIZE;
        buf = new byte[BUF_SIZE];
    }
    
    public Line readLine() throws IOException {
        if(bufPos == -1) {
            return null;
        }
        while(true) {
            if(bufPos == read)  {
                if(bufMark != BUF_SIZE) {
                    System.arraycopy(buf, bufMark, buf, 0, BUF_SIZE-bufMark);
                }
                read = is.read(buf,BUF_SIZE-bufMark,bufMark);
                if(read == -1) {
                    int size = (int)(pos - mark);
                    if(size > 0) {
                        Line rval = new Line(new String(buf, 0, size),mark,pos);
                        bufPos = -1;
                        return rval;
                    } else {
                        return null;
                    }
                }
                bufPos = BUF_SIZE - bufMark;
                read += bufPos;
                bufMark = 0;
            }
            if(bufPos == BUF_SIZE) {
                throw new IOException("A single line exceeded the size of the buffer... cannot read");
            }
            if(buf[bufPos] == '\n') {
                int size = (int)(pos-mark);
                Line rval = new Line(new String(buf,bufPos - size, size), mark, pos);
                pos++;
                bufPos++;
                mark = pos;
                bufMark = bufPos;
                return rval;
            }
            pos++;
            bufPos++;
        }
    }
    
    
    public static class Line {
        public String line;
        public long start;
        public long end;

        public Line(String line, long start, long end) {
            this.line = line;
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Line other = (Line) obj;
            if ((this.line == null) ? (other.line != null) : !this.line.equals(other.line)) {
                return false;
            }
            if (this.start != other.start) {
                return false;
            }
            if (this.end != other.end) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 23 * hash + (this.line != null ? this.line.hashCode() : 0);
            hash = 23 * hash + (int) (this.start ^ (this.start >>> 32));
            hash = 23 * hash + (int) (this.end ^ (this.end >>> 32));
            return hash;
        }

        @Override
        public String toString() {
            return line + "  [" + start + ", " + end + ']';
        }
        
    }
}
