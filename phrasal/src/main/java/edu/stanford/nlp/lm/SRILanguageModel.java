package edu.stanford.nlp.lm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.io.RuntimeIOException;

/**
 * Language model class using SRILM native code.
 *
 * @author Steven Bethard
 * @author Michel Galley
 */
public class SRILanguageModel {

  private static final int lm_start_sym_id = 11; //1-10 reserved for special symbols
  private static final int lm_end_sym_id = 5000001; //max vocab 16M

  static {
    System.loadLibrary("srilm");
  }

  private final SWIGTYPE_p_Ngram languageModel;
  private final SWIGTYPE_p_Vocab vocabulary;
  private int order;

  /**
   * Load a language model from the given file.
   * 
   * @param file  An ARPA backoff N-gram model format file.
   */
  public SRILanguageModel(File file) {
    this.order = findOrder(file);
    this.vocabulary = srilm.initVocab(lm_start_sym_id, lm_end_sym_id);
    this.languageModel = srilm.initLM(this.order, this.vocabulary);
    srilm.readLM(this.languageModel, file.getPath());
  }
  
  /**
   * Get the language model order, i.e. the size of an N-gram.
   * 
   * @return The size of an N-gram.
   */
  public int getOrder() {
    return this.order;
  }

  /**
   * Determine the log-probability of the last word given the preceding words:
   * 
   * This is the same as {@link #getWordLogProb(List, int)}, but using the
   * maximum size of N-gram supported by the model.
   * 
   * @param words A list of words.
   * @return      The log-probability (base 10).
   */
  public double getWordLogProb(List<String> words) {
    if (words.size() < 1) {
      throw new RuntimeException("at least one word is required");
    }
    int historySize = words.size() - 1;
    SWIGTYPE_p_unsigned_int history = srilm.new_unsigned_array(historySize + 1);
    for (int i = 0; i < historySize; ++i) {
      long id = srilm.getIndexForWord(this.vocabulary, words.get(i));
      srilm.unsigned_array_setitem(history, historySize - 1 - i, id);
    }
    srilm.unsigned_array_setitem(history, historySize, srilm.getVocab_None());
    long id = srilm.getIndexForWord(this.vocabulary, words.get(historySize));
    double prob = srilm.getWordProb(this.languageModel, id, history);
    srilm.delete_unsigned_array(history);
    return prob;
  }
  
  /**
   * Determine the log-probability of the last word given the preceding N - 1 words:
   * 
   *   log<sub>10</sub>(P(w<sub>n</sub>|w<sub>n-1</sub>, ...))
   * 
   * @param words The list of words.
   * @param order The size of N-gram to use, that is, one more than the number
   *              of preceding words to consider. Must be no greater than the
   *              order ({@link #getOrder}) of the model.
   * @return      The log-probability (base 10).
   */
  public double getWordLogProb(List<String> words, int order) {
    if (order > this.order) {
      String msg = "order %d is greater than the model order %d";
      throw new RuntimeException(String.format(msg, order, this.order));
    }
    int start = Math.max(0, words.size() - order);
    return this.getWordLogProb(words.subList(start, words.size()));
  }

  /**
   * Determine the log-probability of the sentence.
   * 
   * This is the same as {@link #getSentenceLogProb(List, int)}, but using the
   * maximum size of N-gram supported by the model.
   * 
   * @param words The words in the sentence.
   * @return      The log-probability (base 10).
   */
  public double getSentenceLogProb(List<String> words) {
    int nWords = words.size();
    SWIGTYPE_p_unsigned_int array = srilm.new_unsigned_array(nWords + 1);
    for (int i = 0; i < nWords; ++i) {
      long index = srilm.getIndexForWord(this.vocabulary, words.get(i));
      srilm.unsigned_array_setitem(array, i, index);
    }
    srilm.unsigned_array_setitem(array, nWords, srilm.getVocab_None());
    double prob = srilm.getSentenceProb(this.languageModel, array);
    srilm.delete_unsigned_array(array);
    return prob;
  }

  /**
   * Determine the log-probability of the sentence.
   * 
   * SRILM inserts special start and end symbols, &lt;s&gt; and &lt;/s&gt;, and
   * then calculates this as:
   *   log<sub>10</sub>(P(w<sub>1</sub> | &lt;s&gt;)) +
   *   log<sub>10</sub>(P(w<sub>2</sub> | w<sub>1</sub>, ...)) +
   *   log<sub>10</sub>(P(w<sub>3</sub> | w<sub>2</sub>, ...)) +
   *   ...
   *   log<sub>10</sub>(P(&lt;/s&gt; | w<sub>n</sub>, ...)
   * 
   * @param words The words in the sentence.
   * @param order The size of N-gram to use, that is, one more than the number
   *              of preceding words to consider. Must be no greater than the
   *              order ({@link #getOrder}) of the model.
   * @return      The log-probability (base 10).
   */
  public double getSentenceLogProb(List<String> words, int order) {
    if (order > this.order) {
      String msg = "order %d is greater than the model order %d";
      throw new RuntimeException(String.format(msg, order, this.order));
    }
    List<String> sentence = new ArrayList<String>();
    sentence.add("<s>");
    sentence.addAll(words);
    sentence.add("</s>");
    double logProb = 0.0;
    for (int i = 2; i <= sentence.size(); ++i) {
      int start = Math.max(0, i - order);
      double wordLogProb = this.getWordLogProb(sentence.subList(start, i));
      if (wordLogProb != Double.NEGATIVE_INFINITY) {
        logProb += wordLogProb;
      }
    }
    return logProb;
  }
  /**
   * Determine the order (N-gram size) of a language model
   * 
   * @param file An ARPA backoff N-gram model format file.
   * @return     The largest N-gram size available in the model.
   */
  private static int findOrder(File file) {
    try {
      Pattern ngramPattern = Pattern.compile("^ngram (\\d+)=\\d+");
      BufferedReader reader = IOUtils.readReaderFromString(file.getPath());
      int maxOrder = -1;
      int lineNo = 0;
      for (String line; lineNo < 100 && (line = reader.readLine()) != null; ++lineNo) {
        Matcher matcher = ngramPattern.matcher(line);
        if (matcher.matches()) {
          int order = Integer.parseInt(matcher.group(1));
          if (order > maxOrder) {
            maxOrder = order;
          }
        } else if (line.matches("^\\1-grams:")) {
          break;
        }
      }
      if (maxOrder < 1) {
        throw new RuntimeException("unable to determine language model order");
      }
      return maxOrder;
    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }

  /**
   * Determine how many times SRILM needs to backoff in an n-gram before it finds
   * an n-gram present in the language model.
   *
   * @param words The specified n-gram.
   * @return A value between 0 and list.size(), inclusive. The function returns
   * 0 if the given n-gram is present in the language model, and it returns
   * words.size() if the last element of the list is an OOV word.
   */
  public int backoffCount(List<String> words) {
    if (words.size() < 1) {
      throw new RuntimeException("at least one word is required");
    }
    int historySize = words.size();
    SWIGTYPE_p_unsigned_int history = srilm.new_unsigned_array(historySize);
    for (int i = 0; i < historySize; ++i) {
      long id = srilm.getIndexForWord(this.vocabulary, words.get(i));
      srilm.unsigned_array_setitem(history, i, id);
    }
    long depth = srilm.getDepth(this.languageModel, history, historySize);
    srilm.delete_unsigned_array(history);
    return (int)(historySize-depth);
  }

}
