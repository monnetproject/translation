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

import java.util.Arrays;

/**
 *
 * @author John McCrae
 */
public class PhraseTableEntry {

    private final String src;
    private final String trg;
    private final double[] scores;
    private final String[][] otherInfo;

    public PhraseTableEntry(String src, String trg, double[] scores, String[][] otherInfo) {
        this.src = src;
        this.trg = trg;
        this.scores = scores;
        this.otherInfo = otherInfo;
    }

    public static PhraseTableEntry fromLine(String line) {
        final String[] cols = line.split("\\s+\\|\\|\\|\\s+");
        if(cols.length < 3) {
            throw new RuntimeException("Bad line: " + line);
        }
        String src = cols[0];
        String trg = cols[1];
        final String[] scoresStrs = cols[2].split("\\s+");
        final double[] scores = new double[scoresStrs.length];
        int i = 0;
        for(String scoreStr : scoresStrs) {
            scores[i++] = Double.parseDouble(scoreStr);
        }
        final String[][] otherInfo = new String[cols.length-3][];
        for(i = 3; i < cols.length; i++) {
            otherInfo[i-3] = cols[i].split("\\s+");
        }
        return new PhraseTableEntry(src, trg, scores, otherInfo);
    }

    public String[][] getOtherInfo() {
        return otherInfo;
    }

    public double[] getScores() {
        return scores;
    }

    public String getSrc() {
        return src;
    }

    public String getTrg() {
        return trg;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PhraseTableEntry other = (PhraseTableEntry) obj;
        if ((this.src == null) ? (other.src != null) : !this.src.equals(other.src)) {
            return false;
        }
        if ((this.trg == null) ? (other.trg != null) : !this.trg.equals(other.trg)) {
            return false;
        }
        if (!Arrays.equals(this.scores, other.scores)) {
            return false;
        }
        if (!Arrays.deepEquals(this.otherInfo, other.otherInfo)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.src != null ? this.src.hashCode() : 0);
        hash = 97 * hash + (this.trg != null ? this.trg.hashCode() : 0);
        hash = 97 * hash + Arrays.hashCode(this.scores);
        hash = 97 * hash + Arrays.deepHashCode(this.otherInfo);
        return hash;
    }

    @Override
    public String toString() {
        return "PhraseTableEntry{" + "src=" + src + ", trg=" + trg + ", scores=" + Arrays.toString(scores) + ", otherInfo=" + Arrays.deepToString(otherInfo) + '}';
    }
    
    
    
}
