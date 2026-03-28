package chunkcomfort.chunk;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerChunkComfortCache {

    private static final Map<UUID, PlayerChunkComfortCache> PLAYER_CACHES = new HashMap<>();

    public final Map<Block, Integer> blockCounts = new HashMap<>();
    public final Map<String, Integer> groupTotals = new HashMap<>();

    public final Map<Class<? extends Entity>, Integer> entityCounts = new HashMap<>();
    public final Map<String, Integer> entityGroupTotals = new HashMap<>();

    // Temporary comfort per entity instance
    public final Map<UUID, TemporaryComfortBoost> tempComforts = new HashMap<>();

    public BlockPos lastPos = null;

    /** Clear the cache before recalculating */
    public void clear() {
        blockCounts.clear();
        groupTotals.clear();
        entityCounts.clear();
        entityGroupTotals.clear();
        lastPos = null;
        tempComforts.clear();
    }

    public static class TemporaryComfortBoost {
        public final int amount;
        public final long expireTime;
        public final Class<? extends Entity> type; // entity type for maxPettable counting

        public TemporaryComfortBoost(int amount, long expireTime, Class<? extends Entity> type) {
            this.amount = amount;
            this.expireTime = expireTime;
            this.type = type;
        }
    }

    /** Add temporary comfort for a specific entity */
    public void addTemporaryComfort(UUID entityId, int amount, int durationSeconds, Class<? extends Entity> type) {
        tempComforts.put(entityId,
                new TemporaryComfortBoost(amount, System.currentTimeMillis() + durationSeconds * 1000L, type));
    }

    /** Get total temporary comfort (sum of all active boosts) */
    public float getTemporaryComfort() {
        long now = System.currentTimeMillis();
        tempComforts.entrySet().removeIf(e -> e.getValue().expireTime < now);
        return (float) tempComforts.values().stream().mapToDouble(b -> b.amount).sum();
    }

    /** Count active boosts of a specific entity type */
    public long countActiveBoostsByType(Class<? extends Entity> type) {
        long now = System.currentTimeMillis();
        tempComforts.entrySet().removeIf(e -> e.getValue().expireTime < now);
        return tempComforts.values().stream().filter(b -> b.type == type).count();
    }

    public static PlayerChunkComfortCache get(EntityPlayer player) {
        return PLAYER_CACHES.computeIfAbsent(player.getUniqueID(), k -> new PlayerChunkComfortCache());
    }

    // ----------------- existing block/entity methods -----------------
    public void addBlockCount(Block block, int count) {
        blockCounts.put(block, blockCounts.getOrDefault(block, 0) + count);
    }

    public void addGroupTotal(String group, int total) {
        groupTotals.put(group, groupTotals.getOrDefault(group, 0) + total);
    }

    public void addEntityCount(Class<? extends Entity> entityClass, int count) {
        entityCounts.put(entityClass, entityCounts.getOrDefault(entityClass, 0) + count);
    }

    public void addEntityGroupTotal(String group, int total) {
        entityGroupTotals.put(group, entityGroupTotals.getOrDefault(group, 0) + total);
    }

    public int getEntityCount(Class<? extends Entity> clazz) {
        return entityCounts.getOrDefault(clazz, 0);
    }

    public void removeBlockCount(Block block, int count) {
        int current = blockCounts.getOrDefault(block, 0);
        if (current <= count) blockCounts.remove(block);
        else blockCounts.put(block, current - count);
    }

    public void removeGroupTotal(String group, int total) {
        int current = groupTotals.getOrDefault(group, 0);
        if (current <= total) groupTotals.remove(group);
        else groupTotals.put(group, current - total);
    }

    public void removeEntityCount(Class<? extends Entity> entityClass, int count) {
        int current = entityCounts.getOrDefault(entityClass, 0);
        if (current <= count) entityCounts.remove(entityClass);
        else entityCounts.put(entityClass, current - count);
    }

    public void removeEntityGroupTotal(String group, int total) {
        int current = entityGroupTotals.getOrDefault(group, 0);
        if (current <= total) entityGroupTotals.remove(group);
        else entityGroupTotals.put(group, current - total);
    }
}