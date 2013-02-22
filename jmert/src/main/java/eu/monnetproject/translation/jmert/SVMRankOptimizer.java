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
package eu.monnetproject.translation.jmert;

import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.monitor.Messages;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 *
 * @author John McCrae
 */
public class SVMRankOptimizer implements Optimizer {

    private static final double C = Double.parseDouble(System.getProperty("C", "1"));

    @Override
    public double[] optimizeFeatures(List<Collection<JMertTranslation>> nBests, Feature[] initWeights, int nIters, Set<String> unused) {
        Messages.info("Optimizing " + nBests.size() + " interesting nBest lists");
        
        int n = 0;
        final ArrayList<double[]> x = new ArrayList<double[]>();
        final ArrayList<int[]> spA = new ArrayList<int[]>();
        int row_no = 0;
        for (Collection<JMertTranslation> translations : nBests) {
            for (JMertTranslation translation : translations) {
                double[] x2 = new double[initWeights.length];
                for (Feature f : translation.features) {
                    x2[indexOfName(initWeights, f.name)] = f.score;
                }
                x.add(x2);
                int row_no2 = n;
                for (JMertTranslation translation2 : translations) {
                    if (row_no2 == row_no) {
                        row_no2++;
                        continue;
                    }
                    if (translation.score > translation2.score) {
                        spA.add(new int[]{row_no, row_no2});
                    }
                    row_no2++;
                }
                row_no++;
            }
            n += translations.size();
        }

        final SVMRank rank = new SVMRank(x.toArray(new double[x.size()][]), spA.toArray(new int[spA.size()][]), C);
        
        final double[] wts = rank.solve();
        
        if (allZero(wts)) {
            return toDoubleArray(initWeights);
        }
        double sumWts = 0.0;
        for (int i = 0; i < wts.length; i++) {
            if(wts[i] < 0) {
                wts[i] = 1e-20;
            }
            sumWts += wts[i];
        }
        
        if(sumWts == 0.0) {
            sumWts = 1e-20;
        }
        
        for (int i = 0; i < wts.length; i++) {
            wts[i] /= sumWts;
        }
        return wts;
    }

    private int indexOfName(Feature[] feature, String name) {
        for (int i = 0; i < feature.length; i++) {
            if (feature[i].name.equals(name)) {
                return i;
            }
        }
        throw new RuntimeException("Unknown feature " + name);
    }

    private boolean allZero(double[] wts) {
        for (int i = 0; i < wts.length; i++) {
            if (wts[i] != 0.0) {
                return false;
            }
        }
        return true;
    }

    private double[] toDoubleArray(Feature[] initWeights) {
        double[] da = new double[initWeights.length];
        for (int i = 0; i < initWeights.length; i++) {
            da[i] = initWeights[i].score;
        }
        return da;
    }
}
