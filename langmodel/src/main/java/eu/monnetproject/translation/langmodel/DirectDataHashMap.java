/* Generic definitions */
/* Assertions (useful to generate conditional code) */
/* Current type and class (and size, if applicable) */
/* Value methods */
/* Interfaces (keys) */
/* Interfaces (values) */
/* Abstract implementations (keys) */
/* Abstract implementations (values) */
/* Static containers (keys) */
/* Static containers (values) */
/* Implementations */
/* Synchronized wrappers */
/* Unmodifiable wrappers */
/* Other wrappers */
/* Methods (keys) */
/* Methods (values) */
/* Methods (keys/values) */
/* Methods that have special names depending on keys (but the special names depend on values) */
/* Equality */
/* Object/Reference-only definitions (keys) */
/* Object/Reference-only definitions (values) */
/*		 
 * Copyright (C) 2002-2012 Sebastiano Vigna 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package eu.monnetproject.translation.langmodel;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import static it.unimi.dsi.fastutil.HashCommon.arraySize;
import static it.unimi.dsi.fastutil.HashCommon.maxFill;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Modified from FastUtil HashMap
 *
 * @see Hash
 * @see HashCommon
 */
public class DirectDataHashMap implements Hash {

    public static final long serialVersionUID = 0L;
    /**
     * The array of keys.
     */
    protected transient int[] key[];
    /**
     * The array of values.
     */
    protected transient double[] value[];
    /**
     * The array telling whether a position is used.
     */
    protected transient boolean used[];
    /**
     * The acceptable load factor.
     */
    protected final float f;
    /**
     * The current table size.
     */
    protected transient int n;
    /**
     * Threshold after which we rehash. It must be the table size times
     * {@link #f}.
     */
    protected transient int maxFill;
    /**
     * The mask for wrapping a position counter.
     */
    protected transient int mask;
    /**
     * Number of entries in the set.
     */
    protected int size;

    /**
     * Creates a new hash map.
     *
     * <p>The actual table size will be the least power of two greater than
     * <code>expected</code>/
     * <code>f</code>.
     *
     * @param expected the expected number of elements in the hash set.
     * @param f the load factor.
     */
    @SuppressWarnings("unchecked")
    public DirectDataHashMap(final int expected, final float f) {
        if (f <= 0 || f > 1) {
            throw new IllegalArgumentException("Load factor must be greater than 0 and smaller than or equal to 1");
        }
        if (expected < 0) {
            throw new IllegalArgumentException("The expected number of elements must be nonnegative");
        }
        this.f = f;
        n = arraySize(expected, f);
        mask = n - 1;
        maxFill = maxFill(n, f);
        key = new int[n][];
        value = new double[n][];
        used = new boolean[n];
    }
    // Use a much lower load factor than standard to guarantee quick retrieval
    public static final float VFAST_LOAD_FACTOR = 0.4F;

    /**
     * Creates a new hash map with initial expected
     * {@link Hash#DEFAULT_INITIAL_SIZE} entries and
     * {@link Hash#DEFAULT_LOAD_FACTOR} as load factor.
     */
    public DirectDataHashMap() {
        this(DEFAULT_INITIAL_SIZE, VFAST_LOAD_FACTOR);
    }
    
    
    public DirectDataHashMap(Data data) {
        this.f = VFAST_LOAD_FACTOR;
        n = data.k.length;
        mask = n - 1;
        this.maxFill = data.maxFill;
        this.key = data.k;
        this.value = data.v;
        this.used = data.used;
    }
    
    private final double[] defRetValue = null;

    /*
     * The following methods implements some basic building blocks used by
     * all accessors. They are (and should be maintained) identical to those used in OpenHashSet.drv.
     */

    public double[] put(final int[] k, final double[] v) {
        // The starting point.
        int pos = MurmurHash.hash32(k) & mask;
        // There's always an unused entry.
        while (used[ pos]) {
            if (((key[ pos]) == null ? (k) == null : Arrays.equals(key[ pos],k))) {
                final double[] oldValue = value[ pos];
                value[ pos] = v;
                return oldValue;
            }
            pos = (pos + 1) & mask;
        }
        used[ pos] = true;
        key[ pos] = k;
        value[ pos] = v;
        if (++size >= maxFill) {
            rehash(arraySize(size + 1, f));
        }
        return defRetValue;
    }

    @SuppressWarnings("unchecked")
    public double[] get(final int[] k) {
        // The starting point.
        int pos = MurmurHash.hash32(k) & mask;
        // There's always an unused entry.
        while (used[ pos]) {
            if (((key[ pos]) == null ? (k) == null : (key[ pos]).equals(k))) {
                return value[ pos];
            }
            pos = (pos + 1) & mask;
        }
        return defRetValue;
    }
    
    @SuppressWarnings("unchecked")
    protected void rehash(final int newN) {
        int i = 0, pos;
        final boolean used[] = this.used;
        int[] k;
        final int[][] key = this.key;
        final double[][] value = this.value;
        final int newMask = newN - 1;
        final int[] newKey[] = new int[newN][];
        final double[] newValue[] = new double[newN][];
        final boolean newUsed[] = new boolean[newN];
        for (int j = size; j-- != 0;) {
            while (!used[ i]) {
                i++;
            }
            k = key[ i];
            pos = MurmurHash.hash32(k) & newMask;
            while (newUsed[ pos]) {
                pos = (pos + 1) & newMask;
            }
            newUsed[ pos] = true;
            newKey[ pos] = k;
            newValue[ pos] = value[ i];
            i++;
        }
        n = newN;
        mask = newMask;
        maxFill = maxFill(n, f);
        this.key = newKey;
        this.value = newValue;
        this.used = newUsed;
    }
    
    public Data getData() {
        if(size < maxFill / 2) {
            rehash(arraySize(size + 1, f));
        }
        return new Data(key, value, used, maxFill);
    }
    
    public static class Data implements Serializable {
        private static final long serialVersionUID = 4866192445505108704L;
        public final int[][] k;
        public final double[][] v;
        public final boolean[] used;
        public final int maxFill;

        public Data(int[][] k, double[][] v, boolean[] used, int maxFill) {
            this.k = k;
            this.v = v;
            this.used = used;
            this.maxFill = maxFill;
        }
        
        
    }
}
