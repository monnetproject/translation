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
package eu.monnetproject.translation.fidel;

import java.util.Arrays;

/**
 *
 * @author John McCrae
 */
public class Beam {

    private double leastScore = Double.NEGATIVE_INFINITY;
    private int beamSize;
    private final Solution[] solns;
    private int size;
    private int offset;

    public Beam(int beamSize) {
        this.beamSize = beamSize;
        this.solns = new Solution[beamSize];
        size = offset = 0;
    }

    public double leastScore() {
        return leastScore;
    }

    public void add(Solution solution) {
        if (solution.score < leastScore && size == beamSize) {
            return;
        }
        for (int i = 0; i < size; i++) {
            int pos = (i + offset) % beamSize;
            if (solns[pos].equals(solution)) {
                return;
            }
            if (solns[pos].score < solution.score) {
                final int endOfBeam = (offset + size) % beamSize;
                if (pos >= offset) {
                    // Shuffle those before the offset
                    if (endOfBeam == offset) {
                        if (offset > 0) {
                            System.arraycopy(solns, 0, solns, 1, offset - 1);
                        }
                    } else if (endOfBeam < offset) {
                        if (endOfBeam != 0) {
                            System.arraycopy(solns, 0, solns, 1, endOfBeam);
                        }
                    }
                    // Copy the end to the start
                    if (beamSize <= offset + size && offset != 0) {
                        solns[0] = solns[beamSize - 1];
                    }
                    // Shuffle up those after pos
                    System.arraycopy(solns, pos, solns, pos + 1, beamSize - pos - 1);
                } else {
                    // Shuffle those before the offset
                    if (endOfBeam == offset) {
                        System.arraycopy(solns, pos, solns, pos + 1, offset - 1 - pos);
                    } else {
                        assert (endOfBeam < offset);
                        assert (pos < endOfBeam);
                        System.arraycopy(solns, pos, solns, pos + 1, endOfBeam - pos);
                    }
                }
                // Insert at pos
                solns[pos] = solution;
                if (size != beamSize) {
                    size++;
                }
                leastScore = solns[endOfBeam == offset ? (offset == 0 ? beamSize - 1 : offset - 1) : endOfBeam].score;
                return;
            }
        }
        // Insert at end of beam
        int pos = (size + offset) % beamSize;
        assert (size < beamSize);
        solns[pos] = solution;
        size++;
        leastScore = solution.score;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public Solution poll() {
        if (size == 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        Solution soln = solns[offset];
        offset = (offset + 1) % beamSize;
        size--;
        if (size == 0) {
            leastScore = Double.NEGATIVE_INFINITY;
        }
        return soln;
    }

    public int size() {
        return size;
    }

    public boolean isFull() {
        return size == beamSize;
    }

    public Solution get(int i) {
        if (i >= size || i < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return solns[(offset + i) % beamSize];
    }

    public Solution[] toArray() {
        Solution[] solnArr = new Solution[size];
        System.arraycopy(solns, offset, solnArr, 0, Math.min(beamSize - offset, size));
        if (offset + size > beamSize) {
            System.arraycopy(solns, 0, solnArr, beamSize - offset, offset + size - beamSize);
        }
        return solnArr;
    }
}
