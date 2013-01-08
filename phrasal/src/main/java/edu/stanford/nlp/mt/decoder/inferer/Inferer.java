package edu.stanford.nlp.mt.decoder.inferer;

import java.util.*;

import edu.stanford.nlp.mt.base.RichTranslation;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.decoder.util.ConstrainedOutputSpace;
import edu.stanford.nlp.mt.decoder.util.Scorer;

/**
 * 
 * @author danielcer
 */
public interface Inferer<TK, FV> {
    
    public static int DEFAULT_BEAM_SIZE = 200;

  /**
   *
   */
  RichTranslation<TK, FV> translate(Sequence<TK> foreign, int translationId,
      ConstrainedOutputSpace<TK, FV> constrainedOutputSpace,
      List<Sequence<TK>> targets, int beamSize);

  RichTranslation<TK, FV> translate(Scorer<FV> scorer, Sequence<TK> foreign,
      int translationId, ConstrainedOutputSpace<TK, FV> constrainedOutputSpace,
      List<Sequence<TK>> targets, int beamSize);

  /**
   *
   */
  List<RichTranslation<TK, FV>> nbest(Sequence<TK> foreign, int translationId,
      ConstrainedOutputSpace<TK, FV> constrainedOutputSpace,
      List<Sequence<TK>> targets, int beamSize, int size);

  List<RichTranslation<TK, FV>> nbest(Scorer<FV> scorer, Sequence<TK> foreign,
      int translationId, ConstrainedOutputSpace<TK, FV> constrainedOutputSpace,
      List<Sequence<TK>> targets, int beamSize, int size);

}
