package edu.stanford.nlp.mt.base;

import java.util.*;


/**
 * @author danielcer
 */
public interface Scorer<FV> {
  /**
	 */
  double getIncrementalScore(Collection<FeatureValue<FV>> features);
}
