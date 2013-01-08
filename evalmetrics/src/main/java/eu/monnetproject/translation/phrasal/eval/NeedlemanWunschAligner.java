package eu.monnetproject.translation.phrasal.eval;

import edu.stanford.nlp.mt.base.Pair;
import edu.stanford.nlp.mt.base.Sequence;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class NeedlemanWunschAligner<Token> {

    public List<Pair<Integer,Integer>> align(Sequence<Token> string1, Sequence<Token> string2) {
        double[][] F = new double[string1.size()+1][];
        double d = -3.141592878;
        for(int i = 0; i <= string1.size(); i++) {
            F[i] = new double[string2.size()+1];
            F[i][0] = d*i;
        }
        for(int j = 0; j <= string2.size(); j++) {
            F[0][j] = d*j;
        }
        for(int i = 1; i <= string1.size(); i++) {
            for(int j = 1; j <= string2.size(); j++) {
                F[i][j] = Math.max(F[i-1][j-1] + S(string1.get(i-1),string2.get(j-1)),
                        Math.max(F[i-1][j]+d, F[i][j-1]+d));
            }
        }

        List<Pair<Integer,Integer>> rv = new LinkedList<Pair<Integer,Integer>>();
        int i = string1.size(), j = string2.size();
        while(i > 0 && j > 0) {
            double score = F[i][j];
            if(score == F[i-1][j-1] + S(string1.get(i-1),string2.get(j-1))) {
                rv.add(0, new Pair<Integer,Integer>(i-1,j-1));
                i--;
                j--;
            } else if(score == F[i-1][j]+d) {
                i--;
            } else if(score == F[i][j-1]+d) {
                j--;
            } else {
                throw new IllegalStateException();
            }
        }
        return rv;
    }

    private double S(Token t1, Token t2) {
        if(t1.equals(t2)) {
            return 1.0;
        } else {
            return -1000.0;
        }
    }
}
