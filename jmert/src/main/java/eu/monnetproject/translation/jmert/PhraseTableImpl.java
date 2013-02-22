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
package eu.monnetproject.translation.jmert;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import java.util.ArrayList;

/**
 *
 * @author John McCrae
 */
public class PhraseTableImpl extends ArrayList<PhraseTableEntry> implements PhraseTable {

    private final Language foreignLanguage, translationLanguage;
    private final String name;
    private int longestForeignPhrase;

    public PhraseTableImpl(Language foreignLanguage, Language translationLanguage, String name) {
        this.foreignLanguage = foreignLanguage;
        this.translationLanguage = translationLanguage;
        this.name = name;
        this.longestForeignPhrase = -1;
    }
    
    public PhraseTableImpl(Language foreignLanguage, Language translationLanguage, String name, int longestForeignPhrase) {
        this.foreignLanguage = foreignLanguage;
        this.translationLanguage = translationLanguage;
        this.name = name;
        this.longestForeignPhrase = longestForeignPhrase;
    }

    @Override
    public Language getForeignLanguage() {
        return foreignLanguage;
    }

    @Override
    public Language getTranslationLanguage() {
        return translationLanguage;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getLongestForeignPhrase() {
        if(longestForeignPhrase < 0) {
            for(PhraseTableEntry pte : this) {
                final int l = pte.getForeign().asString().split("\\s+").length;
                if(l > longestForeignPhrase) {
                    longestForeignPhrase = l;
                }
            }
        }
        return longestForeignPhrase;
    }
    
    public void addAll(PhraseTable pt) { 
        for (PhraseTableEntry phraseTableEntry : pt) {
            super.add(phraseTableEntry);
        }
    }
}
