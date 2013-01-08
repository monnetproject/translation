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
package eu.monnetproject.translation.phrasal.mert;

import edu.stanford.nlp.mt.base.FeatureValue;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.NBestListContainer;
import edu.stanford.nlp.mt.base.ScoredFeaturizedTranslation;
import edu.stanford.nlp.mt.decoder.util.Scorer;
import edu.stanford.nlp.mt.decoder.util.StaticScorer;
import edu.stanford.nlp.mt.metrics.EvaluationMetric;
import edu.stanford.nlp.mt.metrics.IncrementalEvaluationMetric;
import edu.stanford.nlp.mt.metrics.IncrementalNBestEvaluationMetric;
import edu.stanford.nlp.mt.metrics.ScorerWrapperEvaluationMetric;
import edu.stanford.nlp.mt.tune.GreedyMultiTranslationMetricMax;
import edu.stanford.nlp.mt.tune.MultiTranslationMetricMax;
import edu.stanford.nlp.mt.tune.NBestOptimizer;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.util.ErasureUtils;
import edu.stanford.nlp.mt.base.OAIndex;
import eu.monnetproject.translation.monitor.Messages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author John McCrae
 */
public final class MERTImpl {
    private static long SEED = 8682522807148012L;
    private static final double NO_PROGRESS_MCMC_TIGHT_DIFF = 1e-6;
    private static final int MCMC_MIN_BATCHES = 0;
    private static final int MCMC_MAX_BATCHES_TIGHT = 50;
    private static final int MCMC_BATCH_SAMPLES = 10;
    private static double C = 100;
    private static double MIN_PLATEAU_DIFF = 0.0;
    private static final int SEARCH_WINDOW = Integer.parseInt(System.getProperty(
            "SEARCH_WINDOW", "1"));
    private static final double MAX_LOCAL_ALL_GAP_WTS_REUSE = 0.035;
    static final double MIN_OBJECTIVE_DIFF = 1e-5;
    private static final SmoothingType smoothingType = SmoothingType.valueOf(System.getProperty("SMOOTHING_TYPE", "min"));
    private static boolean filterUnreachable = Boolean.parseBoolean(System.getProperty(
            "FILTER_UNREACHABLE", "false"));
    private static boolean filterStrictlyUnreachable = Boolean.parseBoolean(System.getProperty("FILTER_STRICTLY_UNREACHABLE", "false"));
    Random random;
    NBestListContainer<IString, String> nbest;
    EvaluationMetric<IString, String> emetric;
    Queue<Counter<String>> startingPoints;
    private Counter<String> bestWts;
    private final Counter<String> fixedWts = new ClassicCounter<String>();
    private double bestObj = Double.POSITIVE_INFINITY;
    private final OAIndex<String> featureIndex = new OAIndex<String>();
    private boolean breakTiesWithLastBest = false;
    private final boolean mcmcObj = (System.getProperty("mcmcELossDirExact") != null
            || System.getProperty("mcmcELossSGD") != null || System.getProperty("mcmcELossCG") != null);

    public MERTImpl(EvaluationMetric<IString, String> emetric, List<Counter<String>> previousWts, NBestListContainer<IString, String> nbest, NBestListContainer<IString, String> localNbest) {
        this(emetric, previousWts, nbest, localNbest, 20);
    }

    public MERTImpl(EvaluationMetric<IString, String> emetric, List<Counter<String>> previousWts, NBestListContainer<IString, String> nbest, NBestListContainer<IString, String> localNbest, int nStartingPoints) {
        this.emetric = emetric;
        this.nbest = nbest;
        Counter<String> initialWts = previousWts.get(0);

        StaticScorer scorer = new StaticScorer(initialWts, featureIndex);

        // Load nbest list:
        double initialObjValue, nbestEval = 0.0;
        if (mcmcObj) {
            initialObjValue = mcmcTightExpectedEval(nbest, initialWts,
                    emetric);
        } else {
            initialObjValue = nbestEval;
        }

        List<ScoredFeaturizedTranslation<IString, String>> localNbestArgmax = transArgmax(
                localNbest, initialWts);
        List<ScoredFeaturizedTranslation<IString, String>> nbestArgmax = transArgmax(
                nbest, initialWts);
        double localNbestEval = emetric.score(localNbestArgmax);
        nbestEval = emetric.score(nbestArgmax);
        boolean reuseWeights = Math.abs(localNbestEval - nbestEval) < MAX_LOCAL_ALL_GAP_WTS_REUSE;
        ////log.fine(String.format("Eval: %f Local eval: %f", nbestEval, localNbestEval));
        ////log.fine(String.format("Rescoring entries"));
        // rescore all entries by weights
        ////log.fine(String.format("n-best list sizes %d, %d", localNbest.nbestLists().size(), nbest.nbestLists().size()));
        if (localNbest.nbestLists().size() != nbest.nbestLists().size()) {
          //  //log.fine(String.format(
          //          "Error incompatible local and cummulative n-best lists, sizes %d != %d",
          //          localNbest.nbestLists().size(), nbest.nbestLists().size()));
            System.exit(-1);
        }
        {
            int lI = -1;
            for (List<ScoredFeaturizedTranslation<IString, String>> nbestlist : nbest.nbestLists()) {
                lI++;
                List<ScoredFeaturizedTranslation<IString, String>> lNbestList = localNbest.nbestLists().get(lI);
                // If we wanted, we could get the value of minReachableScore by just
                // checking the bottom of the n-best list.
                // However, lets make things robust to the order of the entries in the
                // n-best list being mangled as well as
                // score rounding.
                double minReachableScore = Double.POSITIVE_INFINITY;
                double maxReachableScore = Double.NEGATIVE_INFINITY;
                for (ScoredFeaturizedTranslation<IString, String> trans : lNbestList) {
                    double score = scorer.getIncrementalScore(trans.features);
                    if (score < minReachableScore) {
                        minReachableScore = score;
                    }
                    if (score > maxReachableScore) {
                        maxReachableScore = score;
                    }
                }
                if (nbestlist.isEmpty()) {
                    throw new RuntimeException(
                            String.format(
                            "Nbest list of size zero at %d. Perhaps Phrasal ran out of memory?",
                            lI));
                }
                ////log.fine(String.format("l %d - min reachable score: %f (orig size: %d)",
                //        lI, minReachableScore, nbestlist.size()));
                for (ScoredFeaturizedTranslation<IString, String> trans : nbestlist) {
                    trans.score = scorer.getIncrementalScore(trans.features);
                    if (trans.score > minReachableScore && filterUnreachable) // mark for
                    // deletion
                    // (potentially
                    // unreachable)
                    {
                        trans.score = Double.NaN;
                    }
                    if (trans.score > maxReachableScore && filterStrictlyUnreachable) { // mark
                        // for
                        // deletion
                        // (unreachable)
                        trans.score = Double.NaN;
                    }
                }
            }
        }

        ////log.fine(String.format("removing anything that might not be reachable"));
        // remove everything that might not be reachable
        for (int lI = 0; lI < nbest.nbestLists().size(); lI++) {
            List<ScoredFeaturizedTranslation<IString, String>> newList = new ArrayList<ScoredFeaturizedTranslation<IString, String>>(
                    nbest.nbestLists().get(lI).size());
            List<ScoredFeaturizedTranslation<IString, String>> lNbestList = localNbest.nbestLists().get(lI);

            for (ScoredFeaturizedTranslation<IString, String> trans : nbest.nbestLists().get(lI)) {
                if (!Double.isNaN(trans.score)) {
                    newList.add(trans);
                }
            }
            if (filterUnreachable) {
                newList.addAll(lNbestList); // otherwise entries are already on the
            }                                    // n-best list
            nbest.nbestLists().set(lI, newList);
            ////log.fine(String.format(
            //        "l %d - final (filtered) combined n-best list size: %d", lI,
            //        newList.size()));
        }

        // add entries for all wts in n-best list
        for (List<ScoredFeaturizedTranslation<IString, String>> nbestlist : nbest.nbestLists()) {
            for (ScoredFeaturizedTranslation<IString, String> trans : nbestlist) {
                for (FeatureValue<String> f : trans.features) {
                    if (f != null) { // QUES(frm:danc): Why is this here? is there a bug where null features are sometimes included in the features collection
                        initialWts.incrementCount(f.name, 0);
                        for (Counter<String> prevWt : previousWts) {
                            prevWt.incrementCount(f.name, 0);
                        }
                    }
                }
            }
        }

        startingPoints = new LinkedList<Counter<String>>();
        for (int i = 0; i < nStartingPoints; i++) {
            Counter<String> wts;
            if (i == 0) {
                wts = initialWts;
            } else {
                if (i < previousWts.size()) {
                    wts = previousWts.get(i);
                } else {
                    wts = randomWts(initialWts.keySet());
                }
            }
            startingPoints.add(wts);
        }

        //nInitialStartingPoints = startingPoints.size();

        //lrate = (C != 0 ? DEFAULT_UNSCALED_L_RATE / C : DEFAULT_UNSCALED_L_RATE);
        //System.out.printf("sgd lrate: %e\n", lrate);

        //if (reuseWeights) {
        //    //log.fine(String.format("Re-using initial wts, gap: %e",
        //            Math.abs(localNbestEval - nbestEval)));
        //} else {
        //    //log.fine(String.format("*NOT* Re-using initial wts, gap: %e max gap: %e",
        //            Math.abs(localNbestEval - nbestEval), MAX_LOCAL_ALL_GAP_WTS_REUSE));
        //}

        removeWts(initialWts, fixedWts);
        double initialEval = evalAtPoint(nbest, initialWts, emetric);
        updateBest(initialWts, -initialEval);
        //System.out.printf("Initial Eval Score: %e\n", initialEval);
        //System.out.printf("Initial Weights:\n==================\n");
    }

    public Counter<String> getBestWeights() {
        int nInitialStartingPoints = startingPoints.size();

        while (true) {

            Counter<String> wts;

            int sz;
            synchronized (startingPoints) {
                sz = startingPoints.size();
                wts = startingPoints.poll();
            }
            if (wts == null) {
                break;
            }

            int ptI = nInitialStartingPoints - sz;

            // Make the seed a function of current starting point, to
            // ensure experiments are reproducible:
            List<Double> v = new ArrayList<Double>(wts.values());
            Collections.sort(v);
            v.add(SEED * 1.0);
            long threadSeed = Arrays.hashCode(v.toArray());
            this.random = new Random(threadSeed);

          //  System.out.printf("\npoint %d - initial wts: %s", ptI, wts.toString());
           // System.out.printf("\npoint %d - seed: %d\n", ptI, threadSeed);

            NBestOptimizer opt = new CerStyleOptimizer(this, ptI);
//            System.err.println("using: " + opt.toString());

            // Make sure weights that shouldn't be optimized are not in wts:
            removeWts(wts, fixedWts);
            Counter<String> optWts = opt.optimize(wts);
            // Temporarily add them back before normalization:
            if (fixedWts != null) {
                optWts.addAll(fixedWts);
            }
            Counter<String> newWts;
            if (opt.doNormalization()) {
                ////log.fine(String.format("Normalizing weights"));
                newWts = normalize(optWts);
            } else {
                ////log.fine(String.format("Saving unnormalized weights"));
                newWts = optWts;
            }
            // Remove them again:
            removeWts(newWts, fixedWts);

            double evalAt = evalAtPoint(nbest, newWts, emetric);
            double mcmcEval = mcmcTightExpectedEval(nbest, newWts, emetric);
            double mcmcEval2 = mcmcTightExpectedEval(nbest, bestWts, emetric, false);

            double obj = (mcmcObj ? mcmcEval : -evalAt);
            if (!opt.selfWeightUpdate()) {
                ////log.fine("Non-self weight update");
                updateBest(newWts, -evalAt);
            }
         //   System.out.printf("\npoint %d - final wts: %s", ptI, newWts.toString());
          //  System.out.printf(
//                    "\npoint %d - eval: %e E(eval): %e obj: %e best obj: %e (l1: %f)\n\n",
  //                  ptI, evalAt, mcmcEval2, obj, bestObj, l1norm(newWts));
        }
        
        return bestWts;
    }

    public static Counter<String> removeWts(Counter<String> wts, Counter<String> fixedWts) {
        if (fixedWts != null) {
            for (String s : fixedWts.keySet()) {
                wts.remove(s);
            }
        }
        return wts;
    }

    static public Counter<String> normalize(Counter<String> wts) {
        Counters.multiplyInPlace(wts, 1.0 / l1norm(wts));
        return wts;
    }

    public double evalAtPoint(NBestListContainer<IString, String> nbest,
            Counter<String> optWts, EvaluationMetric<IString, String> emetric) {
        Counter<String> wts = optWts;
        if (fixedWts != null) {
            wts = new ClassicCounter<String>(optWts);
            removeWts(wts, fixedWts);
            wts.addAll(fixedWts);
        }
        Scorer<String> scorer = new StaticScorer(wts, featureIndex);
        IncrementalEvaluationMetric<IString, String> incEval = emetric.getIncrementalMetric();
        IncrementalNBestEvaluationMetric<IString, String> incNBestEval = null;
        boolean isNBestEval = false;
        if (incEval instanceof IncrementalNBestEvaluationMetric) {
            incNBestEval = (IncrementalNBestEvaluationMetric<IString, String>) incEval;
            isNBestEval = true;
        }
        for (int i = 0; i < nbest.nbestLists().size(); i++) {
            List<ScoredFeaturizedTranslation<IString, String>> nbestlist = nbest.nbestLists().get(i);
            ScoredFeaturizedTranslation<IString, String> highestScoreTrans = null;
            double highestScore = Double.NEGATIVE_INFINITY;
            int highestIndex = -1;
            for (int j = 0; j < nbestlist.size(); j++) {
                ScoredFeaturizedTranslation<IString, String> trans = nbestlist.get(j);
                double score = scorer.getIncrementalScore(trans.features);
                if (score > highestScore) {
                    highestScore = score;
                    highestScoreTrans = trans;
                    highestIndex = j;
                }
            }
            if (isNBestEval) {
                incNBestEval.add(highestIndex, highestScoreTrans);
            } else {
                incEval.add(highestScoreTrans);
            }
        }
        double score = incEval.score();
        return score;
    }

    public double mcmcTightExpectedEval(NBestListContainer<IString, String> nbest,
            Counter<String> wts, EvaluationMetric<IString, String> emetric) {
        return mcmcTightExpectedEval(nbest, wts, emetric, true);
    }
    static boolean alwaysSkipMCMC = true;

    public double mcmcTightExpectedEval(NBestListContainer<IString, String> nbest,
            Counter<String> wts, EvaluationMetric<IString, String> emetric,
            boolean regularize) {
        if (alwaysSkipMCMC) {
            return 0;
        }
        ////log.fine(String.format("TMCMC weights:\n%s\n\n", Counters.toString(wts, 35)));

        // for quick mixing, get current classifier argmax
        List<ScoredFeaturizedTranslation<IString, String>> argmax = transArgmax(
                nbest, wts), current = new ArrayList<ScoredFeaturizedTranslation<IString, String>>(
                argmax);

        // recover which candidates were selected
        int[] argmaxCandIds = new int[current.size()];
        Arrays.fill(argmaxCandIds, -1);
        for (int i = 0; i < nbest.nbestLists().size(); i++) {
            for (int j = 0; j < nbest.nbestLists().get(i).size(); j++) {
                if (current.get(i) == nbest.nbestLists().get(i).get(j)) {
                    argmaxCandIds[i] = j;
                }
            }
        }

        Scorer<String> scorer = new StaticScorer(wts, featureIndex);

        int cnt = 0;
        double dEEval = Double.POSITIVE_INFINITY;

        // expected value sum
        double sumExpL = 0.0;
        for (int batch = 0; (Math.abs(dEEval) > NO_PROGRESS_MCMC_TIGHT_DIFF || batch < MCMC_MIN_BATCHES)
                && batch < MCMC_MAX_BATCHES_TIGHT; batch++) {

            double oldExpL = sumExpL / cnt;

            for (int bi = 0; bi < MCMC_BATCH_SAMPLES; bi++) {
                // gibbs mcmc sample
                if (cnt != 0) // always sample once from argmax
                {
                    for (int sentId = 0; sentId < nbest.nbestLists().size(); sentId++) {
                        double Z = 0;
                        double[] num = new double[nbest.nbestLists().get(sentId).size()];
                        int pos = -1;
                        for (ScoredFeaturizedTranslation<IString, String> trans : nbest.nbestLists().get(sentId)) {
                            pos++;
                            Z += num[pos] = Math.exp(scorer.getIncrementalScore(trans.features));
                        }

                        int selection = -1;
                        if (Z != 0) {
                            double rv = random.nextDouble() * Z;
                            for (int i = 0; i < num.length; i++) {
                                if ((rv -= num[i]) <= 0) {
                                    selection = i;
                                    break;
                                }
                            }
                        } else {
                            selection = random.nextInt(num.length);
                        }

                        if (Z == 0) {
                            Z = 1.0;
                            num[selection] = 1.0 / num.length;
                        }
                        ErasureUtils.noop(Z);

                        if (selection == -1) {
                            selection = random.nextInt(num.length);
                        }

                        // adjust current
                        current.set(sentId, nbest.nbestLists().get(sentId).get(selection));
                    }
                }

                // collect derivative relevant statistics using sample
                cnt++;

                // adjust currentF & eval
                double eval = emetric.score(current);

                sumExpL += eval;
            }

            dEEval = (oldExpL != oldExpL ? Double.POSITIVE_INFINITY : oldExpL
                    - sumExpL / cnt);

            //log.fine(String.format("TBatch: %d dEEval: %e cnt: %d", batch, dEEval, cnt));
            //log.fine(String.format("E(loss) = %e (sum: %e)", sumExpL / cnt, sumExpL));
        }

        // objective 0.5*||w||_2^2 - C * E(Eval), e.g. 0.5*||w||_2^2 - C * E(BLEU)
        double l2wts = Counters.L2Norm(wts);
        double obj = (C != 0 && regularize ? 0.5 * l2wts * l2wts - C * sumExpL
                / cnt : -sumExpL / cnt);
        //log.fine(String.format(
//                "Regularized objective 0.5*||w||_2^2 - C * E(Eval): %e", obj));
        //log.fine(String.format("C: %e\n", C));
        //log.fine(String.format("||w||_2^2: %e", l2wts * l2wts));
        //log.fine(String.format("E(loss) = %e", sumExpL / cnt));
        return obj;
    }

    public boolean updateBest(Counter<String> newWts, double obj) {
        return updateBest(newWts, obj, false);
    }

    public boolean updateBest(Counter<String> newWts, double obj, boolean force) {
        boolean nonZero = Counters.L2Norm(newWts) > 0.0;
        synchronized (MERTImpl.class) {
            boolean better = false;
            if (bestObj > obj) {
                Messages.info(String.format("<<<IMPROVED BEST: %f -> %f with {{{%s}}}.>>>",
                        -bestObj, -obj, Counters.toString(newWts, 100)));
                better = true;
            } else if (bestObj == obj && breakTiesWithLastBest) {
                Messages.info(String.format("<<<SAME BEST: %f with {{{%s}}}.>>>", -bestObj,
                        Counters.toString(newWts, 100)));
                better = true;
            }
            if (force) {
                Messages.info(String.format("<<<FORCED BEST UPDATE: %f -> %f>>>", -bestObj,
                        -obj));
            }
            if ((better && nonZero) || force) {
                bestWts = newWts;
                bestObj = obj;
                return true;
            }
            return false;
        }
    }

    static public double l1norm(Counter<String> wts) {
        double sum = 0;
        for (String f : wts.keySet()) {
            sum += Math.abs(wts.getCount(f));
        }

        return sum;
    }

    public List<ScoredFeaturizedTranslation<IString, String>> transArgmax(
            NBestListContainer<IString, String> nbest, Counter<String> wts) {
        Scorer<String> scorer = new StaticScorer(wts, featureIndex);
        MultiTranslationMetricMax<IString, String> oneBestSearch = new GreedyMultiTranslationMetricMax<IString, String>(
                new ScorerWrapperEvaluationMetric<IString, String>(scorer));
        return oneBestSearch.maximize(nbest);
    }

    public Counter<String> lineSearch(NBestListContainer<IString, String> nbest,
            Counter<String> optWts, Counter<String> direction,
            EvaluationMetric<IString, String> emetric, double bestWeightSoFar) {

        Counter<String> initialWts = optWts;
        if (fixedWts != null) {
            initialWts = new ClassicCounter<String>(optWts);
            initialWts.addAll(fixedWts);
        }

        Scorer<String> currentScorer = new StaticScorer(initialWts, featureIndex);
        Scorer<String> slopScorer = new StaticScorer(direction, featureIndex);
        ArrayList<Double> intercepts = new ArrayList<Double>();
        Map<Double, Set<InterceptIDs>> interceptToIDs = new HashMap<Double, Set<InterceptIDs>>();

        {
            int lI = -1;
            for (List<ScoredFeaturizedTranslation<IString, String>> nbestlist : nbest.nbestLists()) {
                lI++;
                // calculate slops/intercepts
                double[] m = new double[nbestlist.size()];
                double[] b = new double[nbestlist.size()];
                {
                    int tI = -1;
                    for (ScoredFeaturizedTranslation<IString, String> trans : nbestlist) {
                        tI++;
                        m[tI] = slopScorer.getIncrementalScore(trans.features);
                        b[tI] = currentScorer.getIncrementalScore(trans.features);
                    }
                }

                // find -inf*dir candidate
                int firstBest = 0;
                for (int i = 1; i < m.length; i++) {
                    if (m[i] < m[firstBest]
                            || (m[i] == m[firstBest] && b[i] > b[firstBest])) {
                        firstBest = i;
                    }
                }

                Set<InterceptIDs> niS = interceptToIDs.get(Double.NEGATIVE_INFINITY);
                if (niS == null) {
                    niS = new HashSet<InterceptIDs>();
                    interceptToIDs.put(Double.NEGATIVE_INFINITY, niS);
                }

                niS.add(new InterceptIDs(lI, firstBest));

                // find & save all intercepts
                double interceptLimit = Double.NEGATIVE_INFINITY;
                for (int currentBest = firstBest; currentBest != -1;) {
                    // find next intersection
                    double nearestIntercept = Double.POSITIVE_INFINITY;
                    int nextBest = -1;
                    for (int i = 0; i < m.length; i++) {
                        double intercept = (b[currentBest] - b[i])
                                / (m[i] - m[currentBest]); // wow just like middle school
                        if (intercept <= interceptLimit + MIN_PLATEAU_DIFF) {
                            continue;
                        }
                        if (intercept < nearestIntercept) {
                            nextBest = i;
                            nearestIntercept = intercept;
                        }
                    }
                    if (nearestIntercept == Double.POSITIVE_INFINITY) {
                        break;
                    }
                    intercepts.add(nearestIntercept);
                    interceptLimit = nearestIntercept;
                    Set<InterceptIDs> s = interceptToIDs.get(nearestIntercept);
                    if (s == null) {
                        s = new HashSet<InterceptIDs>();
                        interceptToIDs.put(nearestIntercept, s);
                    }
                    s.add(new InterceptIDs(lI, nextBest));
                    currentBest = nextBest;
                }
            }
        }

        // check eval score at each intercept;
        double bestEval = Double.NEGATIVE_INFINITY;
        // Counter<String> bestWts = initialWts;
        if (intercepts.isEmpty()) {
            return initialWts;
        }
        intercepts.add(Double.NEGATIVE_INFINITY);
        Collections.sort(intercepts);
        resetQuickEval(emetric, nbest);
        //System.out.printf("Checking %d points", intercepts.size() - 1);

        double[] evals = new double[intercepts.size()];
        double[] chkpts = new double[intercepts.size()];

        for (int i = 0; i < intercepts.size(); i++) {
            double chkpt;
            if (i == 0) {
                chkpt = intercepts.get(i + 1) - 1.0;
            } else if (i + 1 == intercepts.size()) {
                chkpt = intercepts.get(i) + 1.0;
            } else {
                if (intercepts.get(i) < 0 && intercepts.get(i + 1) > 0) {
                    chkpt = 0;
                } else {
                    chkpt = (intercepts.get(i) + intercepts.get(i + 1)) / 2.0;
                }
            }
            double eval = quickEvalAtPoint(nbest,
                    interceptToIDs.get(intercepts.get(i)));

            chkpts[i] = chkpt;
            evals[i] = eval;
        }

        int bestPt = -1;
        for (int i = 0; i < evals.length; i++) {
            double eval = windowSmooth(evals, i, SEARCH_WINDOW);
            if (bestEval < eval) {
                bestPt = i;
                bestEval = eval;
            }
        }
        
       // System.out.printf(" - best eval: %f\n", bestEval);

        Counter<String> newWts = new ClassicCounter<String>(initialWts);
        Counters.addInPlace(newWts, direction, chkpts[bestPt]);
        if(bestWeightSoFar > bestEval) {
            return optWts;
        }
        return removeWts(normalize(newWts), fixedWts);
    }
    IncrementalEvaluationMetric<IString, String> quickIncEval;

    private void resetQuickEval(EvaluationMetric<IString, String> emetric,
            NBestListContainer<IString, String> nbest) {
        quickIncEval = emetric.getIncrementalMetric();
        int sz = nbest.nbestLists().size();
        for (int i = 0; i < sz; i++) {
            quickIncEval.add(null);
        }
    }

    private double quickEvalAtPoint(NBestListContainer<IString, String> nbest, Set<InterceptIDs> s) {
        for (InterceptIDs iId : s) {
            ScoredFeaturizedTranslation<IString, String> trans = nbest.nbestLists().get(iId.list).get(iId.trans);
            quickIncEval.replace(iId.list, trans);
        }
        return quickIncEval.score();
    }

    static double windowSmooth(double[] a, int pos, int window) {
        int strt = Math.max(0, pos - window);
        int nd = Math.min(a.length, pos + window + 1);

        if (smoothingType == SmoothingType.min) {
            int minLoc = strt;
            for (int i = strt + 1; i < nd; i++) {
                if (a[i] < a[minLoc]) {
                    minLoc = i;
                }
            }
            return a[minLoc];
        } else if (smoothingType == SmoothingType.avg) {
            double avgSum = 0;
            for (int i = strt; i < nd; i++) {
                avgSum += a[i];
            }

            return avgSum / (nd - strt);
        } else {
            throw new RuntimeException();
        }
    }
    private static final List<String> generativeFeatures = Arrays.asList(
            "TM:lex(f|t)",
            "TM:lex(t|f)",
            "TM:phi(f|t)",
            "TM:phi(t|f)",
            "TM:count",
            "TM:uniq",
            "SG:CrossingCount",
            "SG:Length",
            "SG:GapCount",
            "SG:Gap2Count",
            "SG:Gap3Count",
            "SG:Gap4Count",
            "SG:GapSizeLProb",
            "SG:GapSizeLProbPerBin",
            "SG:GapSizeLProbPerBin:0",
            "SG:GapSizeLProbPerBin:1",
            "SG:GapSizeLProbPerBin:2",
            "SG:GapSizeLProbPerBin:4",
            "SG:GapSizeLProbPerBin:5",
            "SG:GapSizeLProbPerBin:6",
            "TG:CrossingCount",
            "TG:Length",
            "TG:GapCount",
            "TG:Gap2Count",
            "TG:Gap3Count",
            "TG:Gap4Count",
            "TG:GapSizeLProb",
            "TG:GapSizeLProbPerBin",
            "TG:GapSizeLProbPerBin:0",
            "TG:GapSizeLProbPerBin:1",
            "TG:GapSizeLProbPerBin:2",
            "TG:GapSizeLProbPerBin:3",
            "TG:GapSizeLProbPerBin:4",
            "TG:GapSizeLProbPerBin:5",
            "TG:GapSizeLProbPerBin:6",
            "LM",
            "LM2",
            "LM3",
            "LM4",
            "LM5",
            "LinearDistortion",
            "LexR:monotoneWithNext",
            "LexR:monotoneWithPrevious",
            "LexR:swapWithNext",
            "LexR:swapWithPrevious",
            "LexR:discontinuousWithNext",
            "LexR:discontinuousWithPrevious",
            "LexR:discontinuous2WithNext",
            "LexR:discontinuous2WithPrevious",
            "LexR:containmentWithNext",
            "LexR:containmentWithPrevious",
            "LexR:NB",
            "UnknownWord");

    static Counter<String> randomWts(Set<String> keySet) {
        Counter<String> randpt = new ClassicCounter<String>();
        final Random globalRandom = new Random();
        for (String f : keySet) {
            if (generativeFeatures.contains(f)) {
                randpt.setCount(f, globalRandom.nextDouble());
            } else {
                randpt.setCount(f, globalRandom.nextDouble() * 2 - 1.0);
            }
        }

        //log.fine(String.format("random Wts: %s", randpt));
        return randpt;
    }

    static public long getSeed() {
        return SEED;
    }

    static class InterceptIDs {

        final int list;
        final int trans;

        InterceptIDs(int list, int trans) {
            this.list = list;
            this.trans = trans;
        }
    }

    enum SmoothingType {

        avg, min
    }
}
