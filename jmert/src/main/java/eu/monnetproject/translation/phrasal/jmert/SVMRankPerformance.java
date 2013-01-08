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
package eu.monnetproject.translation.phrasal.jmert;

import java.util.Arrays;
import java.util.Random;

/**
 *
 * @author John McCrae
 */
public class SVMRankPerformance {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = "4 3 2 5 10".split(" ");
        }
        if (args.length != 5 && args.length != 6) {
            throw new IllegalArgumentException();
        }
        final Random r = new Random();
        final int setSize = Integer.parseInt(args[0]);
        final int levels = Integer.parseInt(args[1]);
        final int sets = Integer.parseInt(args[2]);
        final int vecLength = Integer.parseInt(args[3]);
        final int reps = Integer.parseInt(args[4]);
        final boolean sparse = args.length == 6;

        long timeTaken = 0;
        
        System.in.read();
        
        for (int rep = 0; rep < reps; rep++) {
            final double[][] X = new double[setSize * levels * sets][vecLength];
            final double[][] A = new double[setSize * setSize * levels * (levels - 1) / 2 * sets][setSize * levels * sets];
            final double[] C = new double[setSize * setSize * levels * (levels - 1) / 2 * sets];
            final int[][] spA = new int[setSize * setSize * levels * (levels - 1) / 2 * sets][2];

            for (int i = 0; i < sets; i++) {
                for (int j = 0; j < levels; j++) {
                    for (int k = 0; k < setSize; k++) {
                        for (int l = 0; l < vecLength; l++) {
                            for (int m = 0; m <= j; m++) {
                                X[(i * levels + j) * setSize + k][l] += r.nextDouble();
                            }
                        }
                    }
                }
            }
            int n = 0;
            for (int i = 0; i < sets; i++) {
                for (int j = 0; j < levels; j++) {
                    for (int m = 0; m < setSize; m++) {
                        for (int k = j + 1; k < levels; k++) {
                            for (int l = 0; l < setSize; l++) {
                                spA[n][0] = (i * levels + j) * setSize + m;
                                spA[n][1] = (i * levels + k) * setSize + l;
                                A[n][(i * levels + j) * setSize + m] = 1;
                                A[n++][(i * levels + k) * setSize + l] = -1;
                            }
                        }
                    }
                }
            }
            long solveTime = System.currentTimeMillis();
            final double[] w;
            if (sparse) {
                final SVMRank svmRank = new SVMRank(X, spA, 1.0);
                w = svmRank.solve();
            } else {
                final SVMRank svmRank = new SVMRank(X, A, 1.0);
                w = svmRank.solve();
            }
            timeTaken += System.currentTimeMillis() - solveTime;
            System.out.println(Arrays.toString(w));
        }
        System.out.println("Time: " + timeTaken + "ms");
    }
}
