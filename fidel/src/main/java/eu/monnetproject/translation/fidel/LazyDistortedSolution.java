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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Arrays;

/**
 *
 * @author John McCrae
 */
public class LazyDistortedSolution implements Solution, Beam.RemovalListener {

    private final PhraseTranslation candidate;
    private final SolutionImpl soln;
    private final int[] buf;
    private final int pos, d, j;
    private final double futureCost, ddScore, ptScore;
    private static final double LAZY_BONUS = 1.0;
    private final BufferCache bufferCache;
    private final double[] features;

    @SuppressWarnings("LeakingThisInConstructor")
    public LazyDistortedSolution(PhraseTranslation candidate, SolutionImpl soln, int[] buf, int pos, int d, int j, double futureCost, double ddScore, double ptScore, double[] weights, BufferCache bufferCache, double[] features) {
        this.candidate = candidate;
        this.soln = soln;
        this.buf = bufferCache.lock(this);
        System.arraycopy(buf, 0, this.buf, 0, pos+candidate.words.length);
        this.pos = pos;
        this.d = d;
        this.j = j;
        this.futureCost = futureCost;
        this.ddScore = ddScore;
        this.ptScore = ptScore;
        this.bufferCache = bufferCache;
        this.features = features;
    }
    
    public SolutionImpl evaluate(double[] weights, IntegerLanguageModel languageModel, int lmN) {
        double tptScore = tryPutTranslation(candidate, weights, buf, pos, languageModel, lmN, d);
        // Get the score of the solution
        final double score = tptScore
                + soln.score
                + futureCost
                - soln.futureCost
                + ddScore;

        if (Double.isNaN(score)) {
            return null;
        }

        features[FidelDecoder.LM] += tptScore / weights[FidelDecoder.LM];
        
        return new SolutionImpl(j, Arrays.copyOfRange(buf, 0, pos + candidate.words.length), FidelDecoder.recalcDist(soln.dist, candidate.words.length, d), score, futureCost,features);
    }

    public static double tryPutTranslation(PhraseTranslation pt, double[] weights,
            final int[] buf, int pos, IntegerLanguageModel languageModel, int lmN, int dist) {
        double score = 0.0;
        //for (int j = 0; j < pt.scores.length; j++) {
//            score += weights[FidelDecoder.PT + j] * pt.scores[j];
  //      }
        // remove the "lost n-grams"
        for (int i = 0; i < Math.min(lmN, dist); i++) {
            score -= weights[FidelDecoder.LM] * FidelDecoder.lmScore(buf, pos - i, languageModel, lmN, weights[FidelDecoder.UNK]);
        }
        // shift the n-grams
        FidelDecoder.rightShiftBuffer(buf, pt.words.length, pos - dist);

        //for (int w : pt.p) {
        for (int i = 0; i < pt.words.length; i++) {
            buf[pos - dist + i] = pt.words[i];
            score += weights[FidelDecoder.LM] * FidelDecoder.lmScore(buf, pos + i + 1, languageModel, lmN, weights[FidelDecoder.UNK]);
        }
        for (int i = 0; i < Math.min(lmN, dist); i++) {
            score += weights[FidelDecoder.LM] * FidelDecoder.lmScore(buf, pos - i, languageModel, lmN, weights[FidelDecoder.UNK]);
        }
        // Change should be undone:
        //leftShiftBuffer(buf, pt.w.length, pos - dist);
        //assert (!Double.isInfinite(score) && !Double.isNaN(score));
        return Double.isNaN(score) ? Double.NEGATIVE_INFINITY : score;
    }
    
    @Override
    public void printSoln(Int2ObjectMap<String> wordMap) {
        System.err.println("LAZY_SOLN");
    }

    @Override
    public double score() {
        return ptScore
                + soln.score
                + futureCost
                - soln.futureCost
                + ddScore;
    }

    @Override
    public int[] soln() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[] features() {
        return features;
    }
    
    @Override
    public int compareTo(Solution t) {
        
        if (score() < t.score()) {
            return +1;
        } else if (score() > t.score()) {
            return -1;
        } else {
            if (t instanceof SolutionImpl) {    
                return +1;
            } else {
                final int thisHash = System.identityHashCode(this);
                final int tHash = System.identityHashCode(t);
                if(tHash < thisHash) {
                    return -1;
                } else if(tHash > thisHash) {
                    return +1;
                } else {
                    return this.equals(t) ? 0 : -1;
                }
            }
        }
    }

    @Override
    public boolean onRemove(Solution soln) {
        if(soln == this) {
            bufferCache.delock(this);
            return true;
        } else {
            return false;
        }
    }
    
    
}
