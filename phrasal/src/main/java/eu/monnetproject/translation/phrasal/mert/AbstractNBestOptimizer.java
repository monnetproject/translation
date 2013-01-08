package eu.monnetproject.translation.phrasal.mert;

import edu.stanford.nlp.mt.metrics.EvaluationMetric;
import edu.stanford.nlp.mt.tune.NBestOptimizer;
import edu.stanford.nlp.mt.base.IString;

import edu.stanford.nlp.mt.base.NBestListContainer;
import java.util.Random;

/**
 * @author Michel Galley, Daniel Cer
 */
public abstract class AbstractNBestOptimizer implements NBestOptimizer {

  protected final NBestListContainer<IString,String> nbest;

  protected final MERTImpl mert;
  protected final Random random;
  protected final EvaluationMetric<IString, String> emetric;

  public boolean doNormalization() {
    return true;
  }

  public AbstractNBestOptimizer(MERTImpl mert) {
    this.mert = mert;
    this.emetric = mert.emetric;
    this.random = mert.random;
    this.nbest = mert.nbest;
  }
  
  @Override 
  public boolean selfWeightUpdate() {
    return false;
  }
}
