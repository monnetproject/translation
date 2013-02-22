/**
 * ********************************************************************************
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
import java.util.*;

/**
 *
 * @author John McCrae
 */
public class JMertOptimizer implements Optimizer {


    @Override
    public double[] optimizeFeatures(List<Collection<JMertTranslation>> nBests, Feature[] initWeights, int nIters, Set<String> unused) {
        double[] wts = toDoubleArray(initWeights);
        normalize(wts);
        System.arraycopy(toDoubleArray(initWeights), 0, wts, 0, initWeights.length);
        for (int n = 0; n < nIters; n++) {
            double[] newWts = new double[wts.length];
            double[] newerWts = new double[wts.length]; // Newer weights tracks infinite values, without causing problems
            System.arraycopy(wts, 0, newWts, 0, wts.length);
            System.arraycopy(wts, 0, newerWts, 0, wts.length);
            for (int i = 0; i < initWeights.length; i++) {
                newerWts[i] = optimizeOneFeature(nBests, i, newWts);
                if (newerWts[i] != Double.MAX_VALUE) {
                    newWts[i] = newerWts[i];
                    normalize(newWts);
                }
            }
            safeNormalize(newerWts);
            newWts = newerWts;
            double delta = 0.0;
            for (int i = 0; i < initWeights.length; i++) {
                delta += Math.abs(newWts[i] - wts[i]);
            }
            if (delta < 0.001) {
                return newWts;
            } else {
                wts = newWts;
            }
        }
        Messages.warning("JMert hit iteration limit");
        return wts;
    }

    private double[] toDoubleArray(Feature[] initWeights) {
        double[] da = new double[initWeights.length];
        for (int i = 0; i < initWeights.length; i++) {
            da[i] = initWeights[i].score;
        }
        return da;
    }

    private void normalize(double[] weights) {
        double sum = 0.0;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] == Double.MAX_VALUE) {
                // instead of summing we simply return 1 for all +Infs
                for (int i2 = 0; i2 < weights.length; i2++) {
                    weights[i2] = weights[i2] == Double.MAX_VALUE ? 1.0 : 0.0;
                }
                return;
            }
            sum += Math.abs(weights[i]);
        }
        for (int i = 0; i < weights.length; i++) {
            if (sum == 0) {
                weights[i] = 1.0 / weights.length;
            } else {
                weights[i] /= sum;
            }
        }
    }
    public static double INFINITY_EFFECT = 10.0;

    private void safeNormalize(double[] weights) {
        double sum = 0.0;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] == Double.MAX_VALUE) {
                weights[i] = INFINITY_EFFECT;
            }
            sum += Math.abs(weights[i]);
        }
        for (int i = 0; i < weights.length; i++) {
            if (sum == 0) {
                weights[i] = 1.0 / weights.length;
            } else {
                weights[i] /= sum;
            }
        }
    }


    public double optimizeOneFeature(List<Collection<JMertTranslation>> nBests, final int featureIdx, double[] weights) {
        final LinkedList<LambdaScore> lambdas = new LinkedList<LambdaScore>();
        for (Collection<JMertTranslation> translations : nBests) {
            merge(lambdas, lambdas(translations, featureIdx, weights));
        }
        if (lambdas.isEmpty()) {
            // No intersections
            return weights[featureIdx];
        }

        LambdaScore bestL = lambdas.pop();
        double bestScore = 0.0;
        double score = 0.0;
        for (LambdaScore ld : lambdas) {
            score += ld.score;
            // >= as we prefer to avoid OMEGA boosted scores
            if (score > bestScore) {
                bestL = ld;
                bestScore = score;
            }
        }
        return bestL.lambda;
    }

    private void merge(List<LambdaScore> primary, Collection<LambdaScore> secondary) {
        int idx = 0;
        for (LambdaScore ld : secondary) {
            while (idx < primary.size() && ld.lambda > primary.get(idx).lambda) {
                idx++;
            }
            primary.add(idx, ld);
        }
    }

    // This method follows the convex maximum curve 
    // Picture like like this given by Koehn eq 9.26 (p267)
    // We have lines whose offset is the sum of other features times weights and 
    // gradient is the current feature value.
    //            |
    //            |
    //            |C
    //           /|
    //          / |
    //         /  |
    // A______/B__|_
    //       /    |
    // We 'start' at A (at x=-Inf) and then follow to B and the C (and finally head to y=+Inf)
    // The return value is then B and C where lambda is the x coord of B/C and delta is change
    // in eval metric at that point.
    private List<LambdaScore> lambdas(Collection<JMertTranslation> translations, final int featureIdx, double[] weights) {

        // Firslty we find the extreme values
        final ArrayList<JMertTranslation> translationStack = new ArrayList<JMertTranslation>(translations);
        JMertTranslation minTranslation = null;
        double maxGradient = -Double.MAX_VALUE, minGradient = Double.MAX_VALUE;
        double bestOffset = -Double.MAX_VALUE;

        for (JMertTranslation translation : translations) {
            double gradient = translation.features[featureIdx].score;
            if (gradient > maxGradient) {
                maxGradient = gradient;
            }
            // We also set the translation offset
            translation.offset = 0.0;
            for (int j = 0; j < translation.features.length; j++) {
                if (j != featureIdx) {
                    translation.offset += weights[j] * translation.features[j].score;
                }
            }
            if (gradient < minGradient
                    || // Select highest offset
                    (gradient == minGradient && translation.offset > bestOffset)) {
                minGradient = gradient;
                minTranslation = translation;
                bestOffset = translation.offset;
            }
        }

        double gradient = minGradient;
        double offset = minTranslation.offset;
        double lambda = -Double.MAX_VALUE;
        translationStack.remove(minTranslation);
        JMertTranslation prevTranslation = minTranslation;
        JMertTranslation prevPrevTranslation = minTranslation;

        final ArrayList<LambdaScore> lambdaScores = new ArrayList<LambdaScore>(translations.size());
        while (gradient < maxGradient) {
            JMertTranslation nextTranslation = null;
            double bestLambda = Double.MAX_VALUE;
            double bestLambdaGrad = Double.NaN;

            // We move up to the next lambda value, and prune all translations
            // whose gradient is lower than the last value (and hence cannot intercept)
            final Iterator<JMertTranslation> translationStackIterator = translationStack.iterator();
            while (translationStackIterator.hasNext()) {
                final JMertTranslation candidate = translationStackIterator.next();
                if (candidate.features[featureIdx].score <= gradient) {
                    translationStackIterator.remove();
                    continue;
                }
                if (candidate.features[featureIdx].score == gradient) {
                    continue;
                }
                double nextLambda = (offset - candidate.offset)
                        / (candidate.features[featureIdx].score - gradient);
                if (nextLambda > lambda && (nextLambda < bestLambda
                        || // Complex condition if intersections collide (common at lambda = 0)
                        (nextLambda == bestLambda && candidate.features[featureIdx].score > bestLambdaGrad))) {
                    bestLambda = nextLambda;
                    bestLambdaGrad = candidate.features[featureIdx].score;
                    nextTranslation = candidate;
                }
            }

            // This should never happen as either the current lambda is not at
            // maximal gradient, so the maximal gradient line will intersect 
            // before +Inf
            if (nextTranslation == null) {
                nextTranslation = translationStack.get(0);
                gradient = nextTranslation.features[featureIdx].score;
                offset = nextTranslation.offset;
                System.err.println("X(" + gradient + "," + offset + ")=" + bestLambda);
                throw new RuntimeException("Math error, Rules of universe changed? Nah... likely just a bug");
            }
            // Add to lambda stack
            lambdaScores.add(new LambdaScore(bestLambda, prevTranslation.score - prevPrevTranslation.score));
            gradient = nextTranslation.features[featureIdx].score;
            offset = nextTranslation.offset;
            lambda = bestLambda;
            translationStack.remove(nextTranslation);
            prevPrevTranslation = prevTranslation;
            prevTranslation = nextTranslation;
        }
        
        lambdaScores.add(new LambdaScore(Double.MAX_VALUE, prevTranslation.score - prevPrevTranslation.score));

        return lambdaScores;
    }

    private static class LambdaScore {

        public double lambda;
        public double score;

        public LambdaScore(double lambda, double score) {
            this.lambda = lambda;
            this.score = score;
        }
    }
}
