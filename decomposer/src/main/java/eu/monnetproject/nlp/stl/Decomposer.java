package eu.monnetproject.nlp.stl;

import java.util.List;
import java.util.SortedSet;

/**
 * A decomposer to decompose terms
 *
 * @author Tobias Wunner
 */
public interface Decomposer {

  /**
   * Decomposes a term and returns a list of components as Strings
   * @param term
   * @return The decomposition or an empty list if no decomposition could be found
   */
    // Why do we need this? - JMc
  //public List<String> decomposeBest(String term);

  /**
   * Decomposes a term and returns a map of decompositions with scores as keyset
   * @param term
   * @return The decompositions ranked by score
   */
  public SortedSet<List<String>> decomposeRanked(String term);

}
