package edu.stanford.nlp.mt.metrics;

import java.util.*;

import edu.stanford.nlp.mt.base.NBestListContainer;
import edu.stanford.nlp.mt.base.RecombinationFilter;
import edu.stanford.nlp.mt.base.ScoredFeaturizedTranslation;

/**
 * 
 * @author danielcer
 * 
 * @param <TK>
 */
public interface EvaluationMetric<TK, FV> {

  /**
	 * 
	 */
  double score(List<ScoredFeaturizedTranslation<TK, FV>> sequences);

  /**
	 * 
	 */
  IncrementalEvaluationMetric<TK, FV> getIncrementalMetric();

  /**
	 * 
	 */
  public IncrementalEvaluationMetric<TK, FV> getIncrementalMetric(
      NBestListContainer<TK, FV> nbestList);

  /**
	 * 
	 */
  public RecombinationFilter<IncrementalEvaluationMetric<TK, FV>> getIncrementalMetricRecombinationFilter();

  /**
	 * 
	 */
  double maxScore();
}
