package eu.monnetproject.translation.sources.common;

import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.ChunkList;
import java.util.ArrayList;

/**
 *
 * @author John McCrae
 */
public class ChunkListImpl extends ArrayList<Chunk> implements ChunkList {

    public void addAll(ChunkList chunkList) {
        for(Chunk chunk : chunkList) {
            super.add(chunk);
        }
    }
    
}
