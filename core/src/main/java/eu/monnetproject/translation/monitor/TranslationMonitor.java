/**********************************************************************************
 * Copyright (c) 2011, Monnet Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Monnet Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
package eu.monnetproject.translation.monitor;

import eu.monnetproject.translation.Translation;
import java.net.URI;

/**
 * A monitor that stores the results of the translation procedure
 * 
 * @author John McCrae
 */
public interface TranslationMonitor {
    /**
     * Record a translation
     * 
     * @param translation 
     */
    void recordTranslation(Translation translation);
    
    /**
     * Record a metric score for an ontology
     * @param ontologyID A (unique) identifier for this ontology
     * @param metricName The name of the metric
     * @param metricValue The score for this metric
     * @param ontologySize The number of labels used to calculate this metric score
     */
    void recordOntologyScore(String ontologyID, String metricName, double metricValue, int ontologySize);
        
    /**
     * Start the monitor. This indicates that execution has begun, so a timestamp should be recorded (as such this should be a fast operation).
     */
    void start();
    
    /**
     *  Stop the monitor. This indicates that execution has begun, so a timestamp should be recorded. 
     */
    void end();
    
    /**
     * Commit all information from this monitor
     */
    void commit() throws Exception;
}
