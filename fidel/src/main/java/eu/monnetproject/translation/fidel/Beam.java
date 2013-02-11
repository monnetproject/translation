/**
 * *******************************************************************************
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
package eu.monnetproject.translation.fidel;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;

/**
 *
 * @author John McCrae
 */
public class Beam extends ObjectRBTreeSet<Solution> {
    private final int beamSize;
    
    public Beam(int beamSize) {
        super();
        this.beamSize = beamSize;
    }
    
    public Solution poll() {
        Solution rval = super.first();
        remove(rval);
        return rval;
    }
    
    public double leastScore() {
        return isEmpty() ? Double.NEGATIVE_INFINITY : super.last().score();
    }
    
    public double bestScore() {
        return isEmpty() ? Double.POSITIVE_INFINITY : super.first().score();
    }
    
    public boolean isFull() {
        return super.size() == beamSize;
    }

    @Override
    public boolean add(Solution k) {
        if(size() < beamSize) {
            return super.add(k);
        } else {
            if(size() > beamSize) {
                throw new RuntimeException("The size of this beam... is too damn high!");
            }
            final Solution first = super.last();
            if(k.compareTo(first) < 0) {
                if(!remove(first)) {
                    throw new RuntimeException("Failed to remove");
                }
                return super.add(k);
            } else {
                return false;
            }
        }
    }

    @Override
    public Solution[] toArray() {
        return super.toArray(new Solution[size()]);
    }       
    
    private final ObjectList<RemovalListener> removalListeners = new ObjectArrayList<RemovalListener>();

    @Override
    public boolean remove(Object k) {
        final ObjectListIterator<RemovalListener> iterator = removalListeners.iterator();
        while(iterator.hasNext()) {
            if(iterator.next().onRemove((Solution)k)) {
                iterator.remove();
            }
        }
        return super.remove(k);
    }
    
    public void addRemovalListener(RemovalListener removalListener) {
        removalListeners.add(removalListener);
    }
    
    
    public static interface RemovalListener {
        boolean onRemove(Solution soln);
    }
}