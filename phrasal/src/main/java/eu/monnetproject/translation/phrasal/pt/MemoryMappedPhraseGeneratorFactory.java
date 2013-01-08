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

import edu.stanford.nlp.mt.base.CombinedPhraseGenerator;
import edu.stanford.nlp.mt.base.DTUTable;
import edu.stanford.nlp.mt.base.FlatPhraseTable;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.IdentityPhraseGenerator;
import edu.stanford.nlp.mt.decoder.feat.IsolatedPhraseFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.UnknownWordFeaturizer;
import edu.stanford.nlp.mt.decoder.util.PhraseGenerator;
import edu.stanford.nlp.mt.decoder.util.Scorer;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class MemoryMappedPhraseGeneratorFactory {

    static public <FV> PhraseGenerator<IString> factory(
            IsolatedPhraseFeaturizer<IString, FV> phraseFeaturizer,
            Scorer<FV> scorer, String... pgSpecs) throws IOException {

        List<PhraseGenerator<IString>> pharoahList = new LinkedList<PhraseGenerator<IString>>();
        List<PhraseGenerator<IString>> finalList = new LinkedList<PhraseGenerator<IString>>();

        String[] filenames = pgSpecs[1].split(System.getProperty("path.separator"));
        for (String filename : filenames) {
            // System.err.printf("loading pt: %s\n", filename);
            pharoahList.add(new MemoryMappedPhraseTable<FV>(phraseFeaturizer, scorer,
                    filename));
        }
        int phraseLimit = -1;
        if (pgSpecs.length == 3) {
            String phraseLimitStr = pgSpecs[2];
            try {
                phraseLimit = Integer.parseInt(phraseLimitStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException(
                        String.format(
                        "Specified phrase limit, %s, can not be parsed as an integer value\n",
                        phraseLimitStr));
            }
        }

        finalList.add(new CombinedPhraseGenerator<IString>(pharoahList,
                CombinedPhraseGenerator.Type.CONCATENATIVE));

        finalList.add(new IdentityPhraseGenerator<IString, FV>(phraseFeaturizer,
                scorer, UnknownWordFeaturizer.UNKNOWN_PHRASE_TAG));

        CombinedPhraseGenerator.Type combinationType = CombinedPhraseGenerator.Type.STRICT_DOMINANCE;

        if (phraseLimit == -1) {
            return new CombinedPhraseGenerator<IString>(finalList, combinationType);
        } else {
            return new CombinedPhraseGenerator<IString>(finalList, combinationType,
                    phraseLimit);
        }
    }
}
