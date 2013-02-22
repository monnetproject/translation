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
 ********************************************************************************
 */
package eu.monnetproject.translation.jmert;

import eu.monnetproject.translation.jmert.JMertTranslation;
import eu.monnetproject.translation.jmert.Optimizer;
import eu.monnetproject.translation.jmert.JMertOptimizer;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.monnetproject.translation.Feature;
import java.util.Collections;

/**
 *
 * @author John McCrae
 */
public class JMertOptimizerTest {

    public JMertOptimizerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of optimizeOneFeature method, of class JMertOptimizer.
     */
    @Test
    public void testOptimizeOneFeature() {
        System.out.println("optimizeOneFeature");
        List<Collection<JMertTranslation>> nBests = Arrays.asList(
                (Collection<JMertTranslation>)Arrays.asList(new JMertTranslation(new Feature[]{new Feature("F1", 5), new Feature("F2", 3), new Feature("F3", 2)}, 3),
                new JMertTranslation(new Feature[]{new Feature("F1", 2), new Feature("F2", 5), new Feature("F3", 3)}, 4),
                new JMertTranslation(new Feature[]{new Feature("F1", 3), new Feature("F2", 2), new Feature("F3", 5)}, 5)));
        int featureIdx = 0;
        double[] weights = { 3 , 2, 1 };
        JMertOptimizer instance = new JMertOptimizer();
        double expResult = 5.0/3.0;
        double result = instance.optimizeOneFeature(nBests, featureIdx, weights);
        assertEquals(expResult, result, 0.01);
    }
    
    @Test
    public void testOptimizeFeatures() {
        System.out.println("optimizeFeatures");
        List<Collection<JMertTranslation>> nBests = Arrays.asList(
                (Collection<JMertTranslation>)Arrays.asList(new JMertTranslation(new Feature[]{new Feature("F1", 5), new Feature("F2", 3), new Feature("F3", 2)}, 3),
                new JMertTranslation(new Feature[]{new Feature("F1", 2), new Feature("F2", 5), new Feature("F3", 3)}, 4),
                new JMertTranslation(new Feature[]{new Feature("F1", 3), new Feature("F2", 2), new Feature("F3", 5)}, 5)));
        Feature[] weights = { new Feature("F1",3) , new Feature("F2",2), new Feature("F3",1) };
        Optimizer instance = new JMertOptimizer();
        final double[] newWts = instance.optimizeFeatures(nBests, weights,5,Collections.EMPTY_SET);
        assertArrayEquals(new double[] { 0.11,0.0,0.88 }, newWts,0.01);
    }
}