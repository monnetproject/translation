package eu.monnetproject.translation.phrasal.jmert;

import eu.monnetproject.translation.Feature;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;

import eu.monnetproject.translation.monitor.Messages;
import java.io.PrintWriter;
import java.util.*;

public class OLSOptimizer implements Optimizer {

    private final boolean dumpMatrix = Boolean.parseBoolean(System.getProperty("eu.monnetproject.translation.phrasal.jmert.dump", "false"));

    @Override
    public double[] optimizeFeatures(List<Collection<JMertTranslation>> nBests,
            Feature[] initWeights, int nIters, Set<String> unused) {
        Messages.info("Optimizing " + nBests.size() + " interesting nBest lists");
        double[] wts = new double[initWeights.length];
        int n = 0;
        for (Collection<JMertTranslation> translations : nBests) {
            final double[] singleWts = optimizeFeaturesSingle(translations, initWeights, nIters, unused);
            if (singleWts != null) {
                for (int i = 0; i < singleWts.length; i++) {
                    wts[i] += singleWts[i];
                }
                n++;
            }
        }
        if(allZero(wts)) {
            return toDoubleArray(initWeights);
        }
        for (int i = 0; i < wts.length; i++) {
            wts[i] /= n;
        }
        return wts;
    }

    public double[] optimizeFeaturesSingle(Collection<JMertTranslation> nBests, Feature[] initWeights, int nIter, Set<String> unused) {
        double[] wts = new double[initWeights.length];

        // Calculate number of rows so we can allocate a matrix
        int nrow = nBests.size();

        double[][] x2 = new double[nrow][initWeights.length];
        double[] y = new double[nrow];

        // Create basic matrices
        int row_no = 0;
        for (JMertTranslation translation : nBests) {
            for (Feature f : translation.features) {
                x2[row_no][indexOfName(initWeights, f.name)] = f.score;
            }
            y[row_no] = translation.score;
            row_no++;

        }

        // We remove 'singular' (all the same columns)
        final boolean[] singular = singularColumns(x2);
        final double[][] x = removeSingularity(x2, singular);

        if (dumpMatrix) {
            try {
                final PrintWriter out = new PrintWriter("tune-matrix");
                for (int i = 0; i < initWeights.length; i++) {
                    if (!singular[i]) {
                        out.print("\"" + initWeights[i].name + "\",");
                    }
                }
                out.println("\"y\"");//,\"z\"");
                for (int i = 0; i < x.length; i++) {
                    for (int j = 0; j < x[i].length; j++) {
                        out.print(x[i][j] + ",");
                    }
                    out.print(y[i]);
//                    out.println("," + z[i]);
                }
                out.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        try {
            double[] params = calculateWeights(y, x);


            // Discard intercept
            final double[] features = new double[params.length - 1];
            System.arraycopy(params, 1, features, 0, features.length);

            // Normalize
            normalize(features);

            int k = 0;
            for (int i = 0; i < features.length; i++) {
                if (!singular[i]) {
                    wts[i] = features[k++];
                } else {
                    wts[i] = initWeights[i].score;
                }
            }
        } catch (Exception ex) {
            Messages.warning(ex.getMessage());
            return null;
            // ignore (likely too little data)
        }
        if (allZero(wts)) {
            return null;//toDoubleArray(initWeights);
        }
        normalize(wts);
        for (int i = 0; i < wts.length; i++) {
            if (wts[i] < 0) {
                wts[i] = 1e-4;
            }
        }
        return wts;
    }

    private double[] calculateWeights(double[] y, final double[][] x) {
        // Use Ordinary Least Squares regression
        final OLSMultipleLinearRegression linearRegression = new OLSMultipleLinearRegression();
        linearRegression.newSampleData(y, x);
        final double[] params = linearRegression.estimateRegressionParameters();
        return params;
//        double[] params = new double[x[0].length];
//        for(int i = 0; i < x[0].length; i++) {
//            final OLSMultipleLinearRegression linearRegression = new OLSMultipleLinearRegression();
//            linearRegression.newSampleData(y, col(x,i));
//            final double[] p = linearRegression.estimateRegressionParameters();
//            params[i] = p[0];
//        }
//        return params;
    }

    private int indexOfName(Feature[] feature, String name) {
        for (int i = 0; i < feature.length; i++) {
            if (feature[i].name.equals(name)) {
                return i;
            }
        }
        throw new RuntimeException("Unknown feature " + name);
    }

    private boolean[] singularColumns(double[][] x) {
        if (x.length == 0) {
            return new boolean[0];
        }
        if (x.length == 1) {
            return new boolean[x[0].length];
        }
        double[] sameValue = new double[x[0].length];
        System.arraycopy(x[0], 0, sameValue, 0, sameValue.length);
        for (int i = 1; i < x.length; i++) {
            for (int j = 0; j < x[i].length; j++) {
                if (sameValue[j] != x[i][j]) {
                    sameValue[j] = Double.NEGATIVE_INFINITY;
                }
            }
        }
        boolean[] singular = new boolean[x[0].length];
        for (int i = 0; i < x[0].length; i++) {
            singular[i] = sameValue[i] != Double.NEGATIVE_INFINITY;
        }
        return singular;

    }

    private double[][] removeSingularity(double[][] x, boolean[] isSingular) {
        int distinct = 0;
        for (int i = 0; i < isSingular.length; i++) {
            if (!isSingular[i]) {
                distinct++;
            }
        }
        double[][] rval = new double[x.length][distinct];
        for (int i = 0; i < x.length; i++) {
            int k = 0;
            for (int j = 0; j < x[i].length; j++) {
                if (!isSingular[j]) {
                    rval[i][k++] = x[i][j];
                }
            }
        }
        return rval;
    }

    private void printMatrix(double[][] x) {
        final DecimalFormat df = new DecimalFormat("0.00");
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[i].length; j++) {
                System.out.print(df.format(x[i][j]) + " ");
            }
            System.out.println();
        }

    }
    private final Random random = new Random();

    private final double noise() {
        return 1e-06 * random.nextDouble();
    }

    private void normalize(double[] weights) {
        double sum = 0.0;
        for (int i = 0; i < weights.length; i++) {
            if (Double.isNaN(weights[i]) || Double.isInfinite(weights[i])) {
                Messages.warning("Bad weight... resetting to zero");
                weights[i] = 0;
            }
            sum += Math.abs(weights[i]);
        }
        for (int i = 0; i < weights.length; i++) {
            if (sum == 0) {
                weights[i] = 1.0 / weights.length;
            } else {
                weights[i] /= sum;
            }
        }
    }

    private boolean allZero(double[] wts) {
        for (int i = 0; i < wts.length; i++) {
            if (wts[i] != 0.0) {
                return false;
            }
        }
        return true;
    }

    private double[] toDoubleArray(Feature[] initWeights) {
        double[] da = new double[initWeights.length];
        for (int i = 0; i < initWeights.length; i++) {
            da[i] = initWeights[i].score;
        }
        return da;
    }

    private double[][] col(double[][] x, int i) {
        final double[][] x2 = new double[x.length][1];
        for (int j = 0; j < x.length; j++) {
            x2[j][0] = x[j][i];
        }
        return x2;
    }
}
