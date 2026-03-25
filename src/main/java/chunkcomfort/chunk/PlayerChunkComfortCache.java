package chunkcomfort.chunk;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class PlayerChunkComfortCache {

    public final Map<Block, Integer> blockCounts = new HashMap<>();
    public final Map<String, Integer> groupTotals = new HashMap<>();

    // New maps for entities
    public final Map<Class<? extends Entity>, Integer> entityCounts = new HashMap<>();
    public final Map<String, Integer> entityGroupTotals = new HashMap<>();

    public BlockPos lastPos = null;

    /** Clear the cache before recalculating */
    public void clear() {
        blockCounts.clear();
        groupTotals.clear();
        entityCounts.clear();
        entityGroupTotals.clear();
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

    /** Add entity count */
    public void addEntityCount(Class<? extends Entity> entityClass, int count) {
        entityCounts.put(entityClass, entityCounts.getOrDefault(entityClass, 0) + count);
    }

    /** Add entity group total */
    public void addEntityGroupTotal(String group, int total) {
        entityGroupTotals.put(group, entityGroupTotals.getOrDefault(group, 0) + total);
    }

    public int getEntityCount(Class<? extends Entity> clazz) {
        return entityCounts.getOrDefault(clazz, 0);
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

    /** Remove entity count */
    public void removeEntityCount(Class<? extends Entity> entityClass, int count) {
        int current = entityCounts.getOrDefault(entityClass, 0);
        if (current <= count) entityCounts.remove(entityClass);
        else entityCounts.put(entityClass, current - count);
    }

    /** Remove entity group total */
    public void removeEntityGroupTotal(String group, int total) {
        int current = entityGroupTotals.getOrDefault(group, 0);
        if (current <= total) entityGroupTotals.remove(group);
        else entityGroupTotals.put(group, current - total);
    }
}