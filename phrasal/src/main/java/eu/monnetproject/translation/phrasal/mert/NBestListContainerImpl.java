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
package eu.monnetproject.translation.phrasal.mert;

import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.NBestListContainer;
import edu.stanford.nlp.mt.base.ScoredFeaturizedTranslation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author John McCrae
 */
public class NBestListContainerImpl implements NBestListContainer<IString, String> {

    private final List<List<ScoredFeaturizedTranslation<IString, String>>> nbestList;

    public NBestListContainerImpl(List<List<ScoredFeaturizedTranslation<IString, String>>> nbestList) {
        this.nbestList = nbestList;
    }

    public NBestListContainerImpl add(List<ScoredFeaturizedTranslation<IString, String>> transes) {
        nbestList.add(transes);
        return this;
    }

    public boolean addAll(List<List<ScoredFeaturizedTranslation<IString, String>>> transes) {
        if (transes.size() != nbestList.size()) {
            throw new IllegalArgumentException();
        }
        boolean changed = false;
        final Iterator<List<ScoredFeaturizedTranslation<IString, String>>> iter = nbestList.iterator();
        for (List<ScoredFeaturizedTranslation<IString, String>> trans : transes) {
            final List<ScoredFeaturizedTranslation<IString, String>> currentList = iter.next();
            NEW_LOOP:
            for (ScoredFeaturizedTranslation<IString, String> nieuw : trans) {
                int i = 0;
                final ListIterator<ScoredFeaturizedTranslation<IString, String>> currentIter = currentList.listIterator();
                while (currentIter.hasNext()) {
                    final ScoredFeaturizedTranslation<IString, String> current = currentIter.next();
                    if (current.translation.equals(nieuw.translation)) {
                        currentIter.set(nieuw);
                        continue NEW_LOOP;
                    }
                }
                changed = true;
                currentList.add(nieuw);
                i++;
            }
            Collections.sort(currentList);
        }
        return changed;
    }

    @Override
    public NBestListContainerImpl clone() {
        return new NBestListContainerImpl(new ArrayList<List<ScoredFeaturizedTranslation<IString, String>>>(nbestList));
    }

    @Override
    public List<List<ScoredFeaturizedTranslation<IString, String>>> nbestLists() {
        return nbestList;
    }
}
