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
    public final Map<Class<? extends Entity>, Integer> livingEntityCounts = new HashMap<>();
    public final Map<String, Integer> entityGroupTotals = new HashMap<>();
    public final Map<Class<? extends Entity>, Integer> decorativeEntityCounts = new HashMap<>();

    // REASON: livingEntityCounts is keyed on Java Class, which is the same object
    // for all NBT variants of the same entity (e.g. all if_mob_skull types share
    // one class). This means the tooltip count is the total of ALL skull types
    // combined, not per-type. contextEntityCounts is keyed on "entityId|SkullType:1"
    // (matching the CONTEXT_MAP format in LivingComfortRegistry) so each variant
    // is counted independently and the tooltip shows the correct per-type count.
    public final Map<String, Integer> contextEntityCounts = new HashMap<>();

    public int cacheVersion = 0; // default
    public void ensureUpToDate() {
        if (cacheVersion != AreaComfortCalculator.getCacheVersion()) {
            clear();
            cacheVersion = AreaComfortCalculator.getCacheVersion();
        }
    }

    public BlockPos lastPos = null;

    /** Clear the cache before recalculating */
    public void clear() {
        blockCounts.clear();
        groupTotals.clear();
        livingEntityCounts.clear();
        entityGroupTotals.clear();
        decorativeEntityCounts.clear();
        contextEntityCounts.clear();
        lastPos = null;
    }

    public static PlayerChunkComfortCache get(EntityPlayer player) {
        return PLAYER_CACHES.computeIfAbsent(player.getUniqueID(), k -> new PlayerChunkComfortCache());
    }

    // ----------------- block/entity helpers -----------------
    public void addDecorativeEntityCount(Class<? extends Entity> entityClass, int count) {
        decorativeEntityCounts.put(entityClass, decorativeEntityCounts.getOrDefault(entityClass, 0) + count);
    }

    public int getDecorativeEntityCount(Class<? extends Entity> clazz) {
        return decorativeEntityCounts.getOrDefault(clazz, 0);
    }

    public void addBlockCount(Block block, int count) {
        blockCounts.put(block, blockCounts.getOrDefault(block, 0) + count);
    }

    public void addGroupTotal(String group, int total) {
        groupTotals.put(group, groupTotals.getOrDefault(group, 0) + total);
    }

    public void addEntityCount(Class<? extends Entity> entityClass, int count) {
        livingEntityCounts.put(entityClass, livingEntityCounts.getOrDefault(entityClass, 0) + count);
    }

    public void addContextEntityCount(String contextKey, int count) {
        contextEntityCounts.put(contextKey, contextEntityCounts.getOrDefault(contextKey, 0) + count);
    }

    public int getContextEntityCount(String contextKey) {
        return contextEntityCounts.getOrDefault(contextKey, 0);
    }

    public void addEntityGroupTotal(String group, int total) {
        entityGroupTotals.put(group, entityGroupTotals.getOrDefault(group, 0) + total);
    }

    public int getEntityCount(Class<? extends Entity> clazz) {
        return livingEntityCounts.getOrDefault(clazz, 0);
    }

    public void removeBlockCount(Block block, int count) {
        int current = blockCounts.getOrDefault(block, 0);
        if (current <= count) blockCounts.remove(block);
        else blockCounts.put(block, current - count);
    }

    public boolean isEmpty() {
        return blockCounts.isEmpty()
                && groupTotals.isEmpty()
                && livingEntityCounts.isEmpty()
                && entityGroupTotals.isEmpty();
    }

    public void removeGroupTotal(String group, int total) {
        int current = groupTotals.getOrDefault(group, 0);
        if (current <= total) groupTotals.remove(group);
        else groupTotals.put(group, current - total);
    }

    public void removeEntityCount(Class<? extends Entity> entityClass, int count) {
        int current = livingEntityCounts.getOrDefault(entityClass, 0);
        if (current <= count) livingEntityCounts.remove(entityClass);
        else livingEntityCounts.put(entityClass, current - count);
    }

    public void removeEntityGroupTotal(String group, int total) {
        int current = entityGroupTotals.getOrDefault(group, 0);
        if (current <= total) entityGroupTotals.remove(group);
        else entityGroupTotals.put(group, current - total);
    }
}