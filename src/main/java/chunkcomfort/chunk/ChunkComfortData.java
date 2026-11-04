package chunkcomfort.chunk;

import chunkcomfort.registry.BlockComfortRegistry;
import net.minecraft.block.Block;

import java.util.HashMap;
import java.util.Map;

public class ChunkComfortData {
    // REASON: block counts are now RAW (uncapped). Per-entry block limits used to be applied
    // per chunk, so a structure spanning 4 chunks got 4x every limit (e.g. 20 beds instead
    // of 5). Limits are now applied area-wide in AreaComfortCalculator from these raw counts.
    public final Map<Block, Integer> blockCounts = new HashMap<>();

    /** Raw count per registered comfort entry key (block + metadata), used for area-wide limits. */
    public final Map<BlockComfortRegistry.BlockKey, Integer> keyCounts = new HashMap<>();

    /** Points from decorative (non-living) entities only. Block points are derived from keyCounts. */
    public final Map<String, Integer> groupTotals = new HashMap<>();
    public int totalComfort = 0;
    public boolean initialized;

    public long lastRecalcTick = -1;

    // Fire caching
    public boolean firePresent = false;

    public void addBlock(BlockComfortRegistry.BlockKey key, Block block) {
        if (key == null || block == null) return;
        keyCounts.merge(key, 1, Integer::sum);
        blockCounts.merge(block, 1, Integer::sum);
    }

    public void removeBlock(BlockComfortRegistry.BlockKey key, Block block) {
        if (key != null) decrement(keyCounts, key);
        if (block != null) decrement(blockCounts, block);
    }

    private static <K> void decrement(Map<K, Integer> map, K key) {
        int current = map.getOrDefault(key, 0);
        if (current <= 1) map.remove(key);
        else map.put(key, current - 1);
    }

    public void addComfort(String group, int value) {
        if (group == null || value <= 0) return;

        int current = groupTotals.getOrDefault(group, 0);

        // increment total for this group
        groupTotals.put(group, current + value);

        // increment overall total comfort
        totalComfort += value;
    }

    public void removeComfort(String group, int value) {
        if (group == null || value <= 0) return;

        int current = groupTotals.getOrDefault(group, 0);
        if (current <= 0) return;

        int applied = Math.min(value, current);

        int newValue = current - applied;
        if (newValue <= 0) groupTotals.remove(group);
        else groupTotals.put(group, newValue);

        totalComfort -= applied;
        if (totalComfort < 0) totalComfort = 0;
    }
}
