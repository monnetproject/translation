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
package eu.monnetproject.translation.phrasal.eval;

import edu.stanford.nlp.mt.base.NBestListContainer;
import edu.stanford.nlp.mt.base.Pair;
import edu.stanford.nlp.mt.base.RecombinationFilter;
import edu.stanford.nlp.mt.base.ScoredFeaturizedTranslation;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.base.State;
import edu.stanford.nlp.mt.metrics.AbstractMetric;
import edu.stanford.nlp.mt.metrics.IncrementalEvaluationMetric;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class METEORMetric<TK, FV> extends AbstractMetric<TK, FV> {

    private final List<List<Sequence<TK>>> references;

    public METEORMetric(List<List<Sequence<TK>>> references) {
        this.references = references;
    }

    @Override
    public IncrementalEvaluationMetric<TK, FV> getIncrementalMetric() {
        return new METEORIncrementalMetric();
    }

    @Override
    public IncrementalEvaluationMetric<TK, FV> getIncrementalMetric(NBestListContainer<TK, FV> nbestList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RecombinationFilter<IncrementalEvaluationMetric<TK, FV>> getIncrementalMetricRecombinationFilter() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double maxScore() {
        return 1.0;
    }

    public class METEORIncrementalMetric implements IncrementalEvaluationMetric<TK, FV> {

        private int n = 0;
        private double[] value = new double[references.size()];
        private final NeedlemanWunschAligner<TK> aligner = new NeedlemanWunschAligner<TK>();
        private final List<Sequence<TK>> sequences = new ArrayList<Sequence<TK>>(references.size());
        
        
        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public IncrementalEvaluationMetric<TK, FV> add(ScoredFeaturizedTranslation<TK, FV> trans) {
            double valMean = 0.0;
            final List<Sequence<TK>> referenceTkss = references.get(n);
            for (Sequence<TK> referenceTks : referenceTkss) {
                List<Pair<Integer, Integer>> alignment = align(trans.translation, referenceTks);

                double prec = (double) alignment.size() / (double) trans.translation.size();
                double recall = (double) alignment.size() / (double) referenceTks.size();
                double fm = 10.0 * prec * recall / (recall + 9.0 * prec);
                if (prec == 0.0 && recall == 0.0) {
                    fm = 0.0;
                }

                alignment = aligner.align(trans.translation, referenceTks);

                double val = fm * (1 - (0.5 * Math.pow((double) countChunks(alignment) / (double) alignment.size(), 3)));
                if (alignment.isEmpty()) {
                    val = 0.0;
                }
                valMean += val;
            }
            value[n] = valMean / referenceTkss.size();
            n++;
            return this;
        }

        private int countChunks(List<Pair<Integer, Integer>> alignment) {
            int chunks = 0;
            int last1 = -1, last2 = -1;
            for (Pair<Integer, Integer> align : alignment) {
                if (align.first != last1 + 1
                        || align.second != last2 + 1) {
                    chunks++;
                }
                last1 = align.first;
                last2 = align.second;
            }
            if (chunks == 0) {
                return 1;
            }
            return chunks;
        }

        private List<Pair<Integer, Integer>> align(Sequence<TK> tokens1, Sequence<TK> tokens2) {
            LinkedList<Pair<Integer, Integer>> candidates = new LinkedList<Pair<Integer, Integer>>();
            for (int i = 0; i < tokens1.size(); i++) {
                for (int j = 0; j < tokens2.size(); j++) {
                    if (tokens1.get(i).equals(tokens2.get(j))) {
                        candidates.add(new Pair<Integer, Integer>(i, j));
                    }
                }
            }
            LinkedList<Pair<Integer, Integer>> alignedCandidates = new LinkedList<Pair<Integer, Integer>>();
            for (List<Pair<Integer, Integer>> group : group(candidates)) {
                alignedCandidates.addAll(_align(group));
            }
            return alignedCandidates;
        }

        private List<Pair<Integer, Integer>> _align(List<Pair<Integer, Integer>> candidates) {
            List<Pair<Integer, Integer>> bestSoln = candidates;
            for (int i = 0; i < candidates.size(); i++) {
                for (int j = i + 1; j < candidates.size(); j++) {
                    Pair<Integer, Integer> candidate1 = candidates.get(i), candidate2 = candidates.get(j);
                    if (candidate1.first.equals(candidate2.first)
                            || candidate1.second.equals(candidate2.second)) {
                        List<Pair<Integer, Integer>> sol1 = new LinkedList<Pair<Integer, Integer>>(candidates);
                        sol1.remove(candidate1);
                        sol1 = _align(sol1);
                        List<Pair<Integer, Integer>> sol2 = new LinkedList<Pair<Integer, Integer>>(candidates);
                        sol2.remove(candidate2);
                        sol2 = _align(sol2);
                        if (sol2.size() < sol1.size()) {
                            bestSoln = sol1;
                        } else {
                            bestSoln = sol2;
                        }
                        return bestSoln;
                    }
                }
            }
            return bestSoln;
        }

        private List<List<Pair<Integer, Integer>>> group(LinkedList<Pair<Integer, Integer>> candidates) {
            List<List<Pair<Integer, Integer>>> groups = new LinkedList<List<Pair<Integer, Integer>>>();
            while (!candidates.isEmpty()) {
                Pair<Integer, Integer> candidate1 = candidates.pop();
                List<Pair<Integer, Integer>> targetGroup = null;
                for (int i = 0; i < groups.size(); i++) {
                    List<Pair<Integer, Integer>> groupOld = groups.get(i);
                    List<Pair<Integer, Integer>> group = new ArrayList<Pair<Integer, Integer>>(groupOld.size());
                    group.addAll(groupOld);
                    for (Pair<Integer, Integer> candidate2 : group) {
                        if (candidate1.first.equals(candidate2.first)
                                || candidate1.second.equals(candidate2.second)) {
                            if (targetGroup == null) {
                                groupOld.add(candidate1);
                                targetGroup = groupOld;
                            } else {
                                targetGroup.add(candidate1);
                                targetGroup.addAll(group);
                                groups.remove(i);
                            }
                        }
                    }
                }
                if (targetGroup == null) {
                    targetGroup = new LinkedList<Pair<Integer, Integer>>();
                    targetGroup.add(candidate1);
                    groups.add(targetGroup);
                }
            }
            return groups;
        }

        @Override
        public IncrementalEvaluationMetric<TK, FV> replace(int index, ScoredFeaturizedTranslation<TK, FV> trans) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double score() {
            double v = 0.0;
            for(int i = 0; i < n; i++) {
                v += value[i];
            }
            return v / n;
        }

        @Override
        public double maxScore() {
            return 1.0;
        }

        @Override
        public int size() {
            return sequences.size();
        }

        @Override
        public int compareTo(IncrementalEvaluationMetric<TK, FV> o) {
            if(o instanceof METEORMetric<?,?>.METEORIncrementalMetric) {
                final METEORIncrementalMetric m = (METEORIncrementalMetric)o;
                double diff = 0.0;
                for(int i = 0; i < n; i++) {
                    diff += value[i] - m.value[i];
                }
                if(diff != 0.0) {
                    return (int)(Math.signum(diff) * Math.ceil(Math.abs(diff)));
                } else {
                    return 0;
                }
            } else {
                int h1 = hashCode();
                int h2 = o.hashCode();
                if(h1 < h2) {
                    return -1;
                } else if(h1 > h2) {
                    return 1;
                } else if(!this.equals(o)) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }

        @Override
        public double partialScore() {
            throw new UnsupportedOperationException();
        }

        @Override
        public State<IncrementalEvaluationMetric<TK, FV>> parent() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int depth() {
            return sequences.size();
        }
    }
}
