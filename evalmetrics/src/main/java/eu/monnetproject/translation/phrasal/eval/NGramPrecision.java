package eu.monnetproject.translation.phrasal.eval;

import edu.stanford.nlp.mt.base.Pair;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author John McCrae
 */
public class NGramPrecision {

//    private final Tokenizer referenceTokenizer;// = Services.get(TokenizerFactory.class).getTokenizer(Script.LATIN);
//    private final LabelExtractor labelExtractor;
//    private final int n;
//    private boolean countDuplicate;
//
//    NGramPrecision(int n, Tokenizer referenceTokenizer, LabelExtractorFactory labelExtractorFactory) {
//        this.labelExtractor = labelExtractorFactory.getExtractor(Collections.EMPTY_LIST, false, false);
//        this.referenceTokenizer = referenceTokenizer;
//    	this.n = n;
//    	this.countDuplicate = false;
//    }
//    
//    Object[] getFirst4Instances(Tokenizer refTokenizer, LabelExtractorFactory labelExtractorFactory) {
//    	return new Object[] { new NGramPrecision(1,refTokenizer,labelExtractorFactory),
//    		                  new NGramPrecision(2,refTokenizer,labelExtractorFactory),
//    		                  new NGramPrecision(3,refTokenizer,labelExtractorFactory),
//    	new NGramPrecision(4,refTokenizer,labelExtractorFactory) };
//    }
//    		                  
//    
//    public NGramPrecision(LabelExtractor referenceExtractor, Tokenizer referenceTokenizer, int n, LabelExtractorFactory labelExtractorFactory) {
//        this.labelExtractorFactory = labelExtractorFactory;
//        this.referenceTokenizer = referenceTokenizer;
//        if(n <= 0)
//            throw new IllegalArgumentException("n-gram value must be greater than 0");
//        this.n = n;
//        this.countDuplicate = false;
//    }
//
//    public NGramPrecision(LabelExtractor referenceExtractor, Tokenizer referenceTokenizer, int n, boolean countDuplicate, LabelExtractorFactory labelExtractorFactory) {
//        this.labelExtractorFactory = labelExtractorFactory;
//        this.referenceTokenizer = referenceTokenizer;
//        this.n = n;
//        if(n <= 0)
//            throw new IllegalArgumentException("n-gram value must be greater than 0");
//        this.countDuplicate = countDuplicate;
//    }
//
//    public Collection<TranslationEvaluationMetric> evaluate(Entity entity, Language srcLang, Language language,Translation result) {
//        Collection<String> references = getLabels(entity, language);
//        if (references.isEmpty()) {
//            return Collections.singletonList((TranslationEvaluationMetric) new EvaluationMetricImpl(getMetricName(), Double.NaN));
//        }
//        List<Token> resultTokens = referenceTokenizer.tokenize(result.getLabel());
//        List<List<Token>> referenceTokens = new ArrayList<List<Token>>(references.size());
//        for (String reference : references) {
//            referenceTokens.add(referenceTokenizer.tokenize(reference));
//        }
//        
//        return Collections.singletonList((TranslationEvaluationMetric) new EvaluationMetricImpl(getMetricName(), getEvaluationMetic(resultTokens, referenceTokens)));
//    }
    
    private final boolean countDuplicate = false;
    private final int n;

    public NGramPrecision(int n) {
        this.n = n;
    }
    
    

    public double getEvaluationMetic(List<String> resultTokens, List<List<String>> referenceTokens) {
        Set<Pair<Integer, Integer>> duplicateCounter = countDuplicate ? new HashSet<Pair<Integer, Integer>>() : null;
        int match = 0;
        int idx = -1;
        RES_TK:
        for (String resultToken : resultTokens) {
            idx++;
            int idx3 = -1;
            boolean matched = false;
                REF_TK:
            for (List<String> refTkList : referenceTokens) {
                idx3++;
                int idx2 = -1;
                for (String refToken : refTkList) {
                    idx2++;
                    if (resultToken.toLowerCase().equals(refToken.toLowerCase()) &&
                            (!countDuplicate || !duplicateCounter.contains(new Pair<Integer, Integer>(idx3, idx2))) &&
                            (n == 1 || nGramMatch(resultTokens, refTkList, idx, idx2))) {
                        if(!matched) {
                            match++;
                            matched = true;
                        }
                        if (countDuplicate) {
                            duplicateCounter.add(new Pair<Integer, Integer>(idx3, idx2));
                            continue REF_TK;
                        } else {
                            continue RES_TK;
                        }
                    }
                }
            }
        }
        final double score = (double) match / (double)(resultTokens.size()-n+1);
        return score;
    }

    private boolean nGramMatch(List<String> tokenList1, List<String> tokenList2, int idx1, int idx2) {
        Iterator<String> iter1 = tokenList1.listIterator(idx1);
        Iterator<String> iter2 = tokenList2.listIterator(idx2);
        int match = 0;
        int i = 0;
        while (iter1.hasNext() && iter2.hasNext()) {
            String t1 = iter1.next(), t2 = iter2.next();
            if (t1.toLowerCase().equals(t2.toLowerCase())) {
                match++;
            }
            if (n == match) {
                return true;
            }
            i++;
            if (i >= n) {
                return false;
            }
        }
        return false;

    }
}
