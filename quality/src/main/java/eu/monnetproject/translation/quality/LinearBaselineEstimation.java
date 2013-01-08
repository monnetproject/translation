/**
 * ********************************************************************************
 * Copyright (c) 2011, Monnet Project All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. * Neither the name of the Monnet Project nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *******************************************************************************
 */
package eu.monnetproject.translation.quality;

import eu.monnetproject.translation.LanguageModelFactory;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.TranslationConfidence;
import eu.monnetproject.translation.TranslationSourceFactory;
import java.util.Arrays;

/**
 *
 * @author John McCrae
 */
public class LinearBaselineEstimation implements TranslationConfidence {
    
    private final BaselineFeatures baselineFeatures;
    
    public LinearBaselineEstimation(LanguageModelFactory lmFactory, TranslationSourceFactory sourceFactory) {
        this.baselineFeatures = new BaselineFeatures(lmFactory, sourceFactory);
    }
    // These weights are a linear regression on the WMT-12 training set
    private static final double[] wts = new double[]{
        5.553234e-03, -4.501907e-03, 1.751624e-02, 8.830076e-05, 2.999061e-04,
        -6.539043e-02, 1.153774e-01, -3.169376e-02, 1.228615e+00, 0.000000e+00,
        0.000000e+00, -2.552694e-01, 0.000000e+00, -1.305874e+01, -1.205805e+00,
        -3.416531e-02, 5.398025e-02
    };
    private static final double offset = 0.840;
    
    @Override
    public double confidence(Translation translation) {
        final double[] scores = baselineFeatures.allFeatures(translation);
        double sum = offset;
        assert (scores.length == wts.length);
        for (int i = 0; i < scores.length; i++) {
            if (!Double.isNaN(scores[i]) && !Double.isInfinite(scores[i])) {
                sum += wts[i] * scores[i];
            }
        }
        if (sum < 0) {
            return 0;
        } else if (sum > 1) {
            return 1;
        } else {
            return sum;
        }
    }
}
