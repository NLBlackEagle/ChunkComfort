package chunkcomfort.chunk;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class PlayerChunkComfortCache {

    public final Map<Block, Integer> blockCounts = new HashMap<>();
    public final Map<String, Integer> groupTotals = new HashMap<>();
    public BlockPos lastPos = null;

    /** Clear the cache before recalculating */
    public void clear() {
        blockCounts.clear();
        groupTotals.clear();
        lastPos = null;
    }

    /** Add a block count */
    public void addBlockCount(Block block, int count) {
        blockCounts.put(block, blockCounts.getOrDefault(block, 0) + count);
    }

    /** Add a group total */
    public void addGroupTotal(String group, int total) {
        groupTotals.put(group, groupTotals.getOrDefault(group, 0) + total);
    }

    /** Remove a block count (for broken blocks) */
    public void removeBlockCount(Block block, int count) {
        int current = blockCounts.getOrDefault(block, 0);
        if (current <= count) blockCounts.remove(block);
        else blockCounts.put(block, current - count);
    }

    /** Remove group total (for removed blocks/entities) */
    public void removeGroupTotal(String group, int total) {
        int current = groupTotals.getOrDefault(group, 0);
        if (current <= total) groupTotals.remove(group);
        else groupTotals.put(group, current - total);
    }
}