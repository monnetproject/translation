package edu.stanford.nlp.mt.base;

import edu.stanford.nlp.util.Index;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Open addressing backed index for arbitrary object types. Includes support for
 * both traditional matching of keys values based on the equals() method as well
 * as 'identity' matching where by keys are only compared with '=='.
 *
 * WARNING: This is currently experimental code. It exists since in theory open
 * addressing hashing should be more efficient for hashing int values than
 * bucket-chain hashing, which is used to back java.util.HashMap.
 *
 * To do list: - in fear of user stupidity, i think HashMap re-hashes the hash
 * values returned by objects, should we? - finalize interface - rigorous
 * benchmarks - unit tests
 *
 * @author <a href="mailto:daniel.cer@cs.colorado.edu">Daniel Cer</a>
 */
public class OAIndex<K> implements Index<K> {

    private static final long serialVersionUID = 127L;
    static final int INIT_SZ = 1 << 10;
    static final double MAX_LOAD = 0.60;

    // Index<K> sanityIndex = new Index<K>();
    private static final class Data {

        private final boolean identityHash;
        public Object[] keys;
        public int[] values;
        public int[] hashCodes;
        public int[] reverseIndex;
        private int mask;

        public Data(boolean identityHash) {
            this.identityHash = identityHash;
        }

        public int findPos(Object e) {
            int hashCode = supplementalHash(e.hashCode());
            int idealIdx = hashCode & mask;

            if (identityHash) {
                for (int i = 0, idx = idealIdx; i < keys.length; i++, idx++) {
                    if (idx >= keys.length) {
                        idx = 0;
                    }
                    if (keys[idx] == null) {
                        return -idx - 1;
                    }
                    if (keys[idx] == e) {
                        return idx;
                    }
                }
            } else {
                for (int i = 0, idx = idealIdx; i < keys.length; i++, idx++) {
                    if (idx >= keys.length) {
                        idx = 0;
                    }
                    if (keys[idx] == null) {
                        return -idx - 1;
                    }
                    if (hashCodes[idx] != hashCode) {
                        continue;
                    }
                    if (keys[idx].equals(e)) {
                        return idx;
                    }
                }
            }
            return -keys.length - 1;
        }

        private void sizeUp() {
            int newSize = keys.length << 1;
            mask = newSize - 1;
            // System.err.printf("size up to: %d\n", newSize);
            Object[] oldKeys = keys;
            int[] oldValues = values;
            int[] oldHashCodes = hashCodes;
            keys = new Object[newSize];
            values = new int[newSize];
            reverseIndex = new int[newSize];
            Arrays.fill(reverseIndex, -1);
            hashCodes = new int[newSize];
            for (int i = 0; i < oldKeys.length; i++) {
                if (oldKeys[i] == null) {
                    continue;
                }
                int pos = -findPos(oldKeys[i]) - 1;
                keys[pos] = oldKeys[i];
                values[pos] = oldValues[i];
                reverseIndex[values[pos]] = pos;
                hashCodes[pos] = oldHashCodes[i];
            }
        }
    }
    private final Data data;
    private final boolean identityHash;
    int maxIndex;
    int load;

    public OAIndex() {
        identityHash = false;
        data = new Data(identityHash);
        init();
    }

    public OAIndex(boolean identityHash) {
        this.identityHash = identityHash;
        data = new Data(identityHash);
        init();
    }

    @SuppressWarnings("unchecked")
    public Set<K> keySet() {
        Set<K> keySet = new HashSet<K>();
        synchronized (data) {
            for (Object key : data.keys) {
                if (key == null) {
                    continue;
                }
                keySet.add((K) key);
            }
        }
        return keySet;
    }

    public int maxIndex() {
        return maxIndex;
    }

    public int boundOnMaxIndex() {
        synchronized (data) {
            return data.keys.length;
        }
    }

    private void init() {
        synchronized (data) {
            data.keys = new Object[INIT_SZ]; // since we can't create an array of type K[];
            data.values = new int[INIT_SZ];
            data.hashCodes = new int[INIT_SZ];
            data.reverseIndex = new int[INIT_SZ];
            Arrays.fill(data.reverseIndex, -1);
            data.mask = INIT_SZ - 1;
        }
    }

    private static int supplementalHash(int h) {
        // use the same supplemental hash function used by HashMap
        return ((h << 7) - h + (h >>> 9) + (h >>> 17));
    }

    @SuppressWarnings("unchecked")
    public K get(int idx) {
        synchronized (data) {
            int pos = data.reverseIndex[idx];
            if (pos == -1) {
                return null;
            }
            return (K) data.keys[pos];
        }
    }

//    @SuppressWarnings("unused")
//    private int getSearchOffset(int pos, Object key) {
//        int idealIdx = supplementalHash(key.hashCode()) & mask;
//        int distance;
//        if (idealIdx < pos) {
//            synchronized (data) {
//                distance = pos + data.keys.length - idealIdx;
//            }
//        } else {
//            distance = pos - idealIdx;
//        }
//        return distance;
//    }
    private int add(K key, int pos) {
        synchronized (data) {
            if ((load++) / (double) data.keys.length > MAX_LOAD) {
                data.sizeUp();
                pos = -data.findPos(key) - 1;
            }
            data.keys[pos] = key;
            data.values[pos] = maxIndex++;
            data.reverseIndex[data.values[pos]] = pos;
            data.hashCodes[pos] = supplementalHash(key.hashCode());
            return maxIndex - 1;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String toString() {
        Set<Object> keySet = new TreeSet<Object>();
        synchronized (data) {
            for (Object key : data.keys) {
                if (key == null) {
                    continue;
                }
                keySet.add(key);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Object k : keySet) {
            sb.append(k).append(":").append(indexOf((K) k)).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    public int indexOf(K key) {        
        synchronized (data) {
            int pos = data.findPos(key);
            if (pos < 0) {
                return -1;
            }
            return data.values[pos];
        }
    }

    public boolean contains(Object key) {
        synchronized (data) {
            int pos = data.findPos(key);
            return pos >= 0;
        }
    }

    public int indexOf(K key, boolean add) {
        synchronized(data) {
        int pos = data.findPos(key);
        if (pos >= 0) {
            return data.values[pos];
        }
        if (!add) {
            return -1;
        }
        // System.out.printf("adding: %s %d\n", key, -pos-1);
        return add(key, -pos - 1); /*
         * if (pos != sanityIndex.indexOf(key, true)) {
         * System.err.printf("%d != %d", pos,
         * sanityIndex.indexOf(key)); System.exit(-1); }
         */
        }
    }

    static public void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.printf("Usage:\n\tjava ...OAIndex (text file to index)\n");
            System.exit(-1);
        }

        BufferedReader breader = new BufferedReader(new FileReader(args[0]));
        OAIndex<String> oaindex = new OAIndex<String>();
        System.out.printf("Inserting tokens:\n");
        for (String line; (line = breader.readLine()) != null;) {
            String[] tokens = line.split("\\s");
            for (String token : tokens) {
                oaindex.indexOf(token, true);
                System.out.printf("%s: %d (get: %s)\n", token, oaindex.indexOf(token), oaindex.get(oaindex.indexOf(token)));
            }
        }
        System.out.println();
        System.out.printf("Final Index:\n%s\n", oaindex);
    }

    public int size() {
        return load;
    }

    public boolean isEmpty() {
        return load == 0;
    }

    public Iterator<K> iterator() {
        throw new UnsupportedOperationException();
    }

    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    public boolean add(K k) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends K> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public void lock() {
        throw new UnsupportedOperationException();
    }

    public void unlock() {
        throw new UnsupportedOperationException();
    }

    public void saveToWriter(Writer out) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void saveToFilename(String s) {
        throw new UnsupportedOperationException();
    }

    public List<K> objectsList() {
        throw new UnsupportedOperationException();
    }

    public Collection<K> objects(int[] ints) {
        throw new UnsupportedOperationException();
    }

    public boolean isLocked() {
        throw new UnsupportedOperationException();
    }
}
