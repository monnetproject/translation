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
package eu.monnetproject.translation.jsonpt;

import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.TranslationSource;
import java.util.Map;

/**
 *
 * @author John McCrae
 */
public class JSONPhraseTableSource implements TranslationSource {

    private final Map<String, PhraseTable> table;
    private final PhraseTable empty;

    public JSONPhraseTableSource(Map<String, PhraseTable> table, PhraseTable empty) {
        this.table = table;
        this.empty = empty;
    }
    public static final String FIVESCORE_PHI_t_f = "phi(t|f)";
    public static final String FIVESCORE_LEX_t_f = "lex(t|f)";
    public static final String FIVESCORE_PHI_f_t = "phi(f|t)";
    public static final String FIVESCORE_LEX_f_t = "lex(f|t)";
    public static final String FIVESCORE_PHRASE_PENALTY = "phrasePenalty";

//    @Override
//    public int featureCount() {
//        return 5;
//    }

    @Override
    public String[] featureNames() {
        return new String[]{
                    FIVESCORE_PHI_t_f, FIVESCORE_LEX_t_f, FIVESCORE_PHI_f_t,
                    FIVESCORE_LEX_f_t, FIVESCORE_PHRASE_PENALTY};
    }

    @Override
    public PhraseTable candidates(Chunk label) {
        if (table.containsKey(label.getSource())) {
            return table.get(label.getSource());
        } else {
            return empty;
        }
    }

    @Override
    public String getName() {
        return "jsonpt";
    }

    @Override
    public void close() {
    }
}
