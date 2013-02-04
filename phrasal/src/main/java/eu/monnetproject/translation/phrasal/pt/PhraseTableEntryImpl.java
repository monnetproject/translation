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
package eu.monnetproject.translation.phrasal.pt;

import eu.monnetproject.translation.AlignedPhraseTableEntry;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.PhraseTableEntry;
import java.util.Arrays;

/**
 *
 * @author John McCrae, Tobias Wunner
 */
public class PhraseTableEntryImpl implements AlignedPhraseTableEntry, Comparable<PhraseTableEntry> {
    private final Label foreign, translation;
    private final Feature[] features;
    private final Object alignments;

    public PhraseTableEntryImpl(Label foreign, Label translation, Feature[] features, Object alignments) {
        this.foreign = foreign;
        this.translation = translation;
        this.features = features;
        this.alignments = alignments;
    }
    
    @Override
    public Object getAlignments() {
        return alignments;
    }

    @Override
    public Label getForeign() {
        return foreign;
    }

    @Override
    public Label getTranslation() {
        return translation;
    }

    @Override
    public Feature[] getFeatures() {
        return features;
    }

    @Override
    public double getApproxScore() {
        double score = 0.0;
        for(Feature f : features) {
            score += f.score;
        }
        return score;
    }
    
    

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PhraseTableEntryImpl other = (PhraseTableEntryImpl) obj;
        if (this.foreign != other.foreign && (this.foreign == null || !this.foreign.equals(other.foreign))) {
            return false;
        }
        if (this.translation != other.translation && (this.translation == null || !this.translation.equals(other.translation))) {
            return false;
        }
        if (!Arrays.equals(this.features, other.features)) {
            return false;
        }
        if (this.alignments != other.alignments && (this.alignments == null || !this.alignments.equals(other.alignments))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (this.foreign != null ? this.foreign.hashCode() : 0);
        hash = 71 * hash + (this.translation != null ? this.translation.hashCode() : 0);
        hash = 71 * hash + Arrays.hashCode(this.features);
        hash = 71 * hash + (this.alignments != null ? this.alignments.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "PhraseTableEntryImpl{" + "foreign=" + foreign + ", translation=" + translation + ", features=" + Arrays.toString(features) + ", alignments=" + alignments + '}';
    }

    public double getScore() {
      return getScore(this);
    }

    public double getScore(PhraseTableEntry entry) {
      double totalscore = 0;
      for(Feature feat:entry.getFeatures())
        totalscore=totalscore+feat.score;
      return totalscore;
    }

    @Override
    public int compareTo(PhraseTableEntry e2) {
      double diff = getScore()-getScore(e2);
      if (diff>0)
        return 1;
      else if (diff==0)
        return 0;
      else if (diff<0)
        return -1;
      else
      return -1;
    }

    
}
