package chunkcomfort.chunk;

import chunkcomfort.ChunkComfort;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.hearth.TileEntityHearth;
import chunkcomfort.network.NetworkHandler;
import chunkcomfort.network.PacketComfortStatus;
import chunkcomfort.player.PlayerComfortManager;
import chunkcomfort.registry.*;
import chunkcomfort.statue.TileEntityStatue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public class AreaComfortCalculator {

    private static int CACHE_VERSION = 0;
    private static int cachedMaxComfort = -1;
    public static void incrementCacheVersion() {CACHE_VERSION++;}
    public static int getCacheVersion() {return CACHE_VERSION;}

    private static final Map<UUID, PlayerChunkComfortCache> PLAYER_CACHES = new HashMap<>();

    public static PlayerChunkComfortCache getCache(EntityPlayer player) {
        return PLAYER_CACHES.computeIfAbsent(player.getUniqueID(), k -> new PlayerChunkComfortCache());
    }

    public static void clearAllPlayerCaches() {
        PLAYER_CACHES.clear();
        AREA_CACHE.clear();
    }


    public static void removePlayerCache(UUID playerId) {
        PLAYER_CACHES.remove(playerId);
    }

    public static int getRadius() {
        return Math.min(Math.max(ForgeConfigHandler.server.chunkRadius, 0), 3);
    }

    public static boolean isComfortActive(EntityPlayer player) {
        return player != null && player.isPotionActive(PotionRegistry.COMFORT);
    }

    // ---------------------------------------------------------------------
    // Hearth anchors
    // ---------------------------------------------------------------------

    /** True when the two positions are within the comfort radius (in chunks) and the vertical range. */
    private static boolean inRange(BlockPos a, BlockPos b, int radius) {
        return Math.abs((a.getX() >> 4) - (b.getX() >> 4)) <= radius
                && Math.abs((a.getZ() >> 4) - (b.getZ() >> 4)) <= radius
                && Math.abs(a.getY() - b.getY()) <= ForgeConfigHandler.server.fireScanVerticalRange;
    }

    /** Placement time of a hearth; anchors without one (e.g. other configured blocks) count as newest. */
    private static long anchorAge(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileEntityHearth ? ((TileEntityHearth) te).placedTime : Long.MAX_VALUE;
    }

    /** Strict "a is older than b", with position as tie-break so exactly one of two hearths wins. */
    private static boolean isOlder(long ageA, BlockPos a, long ageB, BlockPos b) {
        if (ageA != ageB) return ageA < ageB;
        return a.toLong() < b.toLong();
    }

    /**
     * Every live hearth within the radius of center. Index entries whose block is gone
     * (broken without an event, e.g. explosions) are dropped on the way. Returns a copy so
     * callers can run nested lookups without touching the index sets mid-iteration.
     */
    private static List<BlockPos> anchorsNear(World world, BlockPos center) {
        List<BlockPos> result = new ArrayList<>();
        int radius = getRadius();
        ComfortWorldData worldData = ComfortWorldData.get(world);
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Set<BlockPos> set = worldData.getAnchors(world, new ChunkPos(cx + dx, cz + dz));
                if (set.isEmpty()) continue;
                Iterator<BlockPos> it = set.iterator();
                while (it.hasNext()) {
                    BlockPos anchor = it.next();
                    if (!FireBlockRegistry.isFireBlock(world.getBlockState(anchor))) {
                        it.remove();
                        continue;
                    }
                    if (inRange(center, anchor, radius)) result.add(anchor);
                }
            }
        }
        return result;
    }

    /**
     * A hearth makes its own comfort zone only when no older hearth is within its radius.
     * REASON: a second hearth placed next to the first must not start a second, overlapping
     * zone. This is decided live rather than stored, so breaking the old hearth hands the
     * zone to the next oldest one without any bookkeeping.
     */
    public static boolean isActiveAnchor(World world, BlockPos anchor) {
        return findOlderAnchorNear(world, anchor) == null;
    }

    /** An older hearth within the radius of anchor (the one blocking its zone), or null. */
    public static BlockPos findOlderAnchorNear(World world, BlockPos anchor) {
        long age = anchorAge(world, anchor);
        for (BlockPos other : anchorsNear(world, anchor)) {
            if (!other.equals(anchor) && isOlder(anchorAge(world, other), other, age, anchor)) return other;
        }
        return null;
    }

    /** The nearest active hearth whose zone contains pos, or null. */
    public static BlockPos findActiveAnchor(World world, BlockPos pos) {
        List<BlockPos> candidates = anchorsNear(world, pos);
        candidates.sort(Comparator.comparingDouble(a -> a.distanceSq(pos)));
        for (BlockPos anchor : candidates) {
            if (isActiveAnchor(world, anchor)) return anchor;
        }
        return null;
    }

    public static int calculateComfortActivation(World world, EntityPlayer player, boolean hearthNearby) {
        BlockPos pos = player.getPosition();
        ComfortRequirements reqs = ComfortRequirementCheck.getRequirementsPresent(world, pos, player, hearthNearby);
        return countMetRequirements(reqs);
    }

    private static int countMetRequirements(ComfortRequirements reqs) {
        int comfort = 0;
        if (reqs.shelterOk) comfort++;
        if (reqs.lightOk) comfort++;
        if (reqs.fireOk) comfort++;
        if (reqs.temperatureOk) comfort++;

        return comfort;
    }

    public static boolean isEnvironmentBlocked(World world, BlockPos pos) {
        String dimId = world.provider.getDimensionType().getName();

        // REASON: replaced Objects.requireNonNull() with an explicit null check.
        // requireNonNull() with no message produces an NPE with no context if a modded
        // biome lacks a registry name. Now we log a warning and return false (unblocked)
        // rather than crashing the server tick.
        ResourceLocation biomeReg = world.getBiome(pos).getRegistryName();
        if (biomeReg == null) {
            chunkcomfort.ChunkComfort.LOGGER.warn("[ChunkComfort] Biome at {} has no registry name, skipping blacklist check", pos);
            return false;
        }
        String biomeId = biomeReg.toString();

        return DimensionBiomeBlacklistRegistry.isDimensionBlocked(dimId)
                || DimensionBiomeBlacklistRegistry.isBiomeBlocked(biomeId);
    }

    public static boolean isBossBlocked() {
        return ForgeConfigHandler.server.enableBossBarDetection && ChunkBossState.isBossActive();
    }

    /**
     * Turns raw block counts summed over the whole comfort area into group points, applying
     * each entry's block limit once for the area. Entries shared by aliases (e.g. standing and
     * wall banners) are the same ComfortEntry object, so they share one limit.
     */
    public static void addAreaLimitedBlockPoints(Map<BlockComfortRegistry.BlockKey, Integer> areaKeyCounts,
                                                 java.util.function.BiConsumer<String, Integer> groupSink) {
        Map<BlockComfortRegistry.ComfortEntry, Integer> perEntry = new IdentityHashMap<>();
        areaKeyCounts.forEach((key, count) -> {
            BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.BLOCK_ENTRIES.get(key);
            if (entry != null) perEntry.merge(entry, count, Integer::sum);
        });
        perEntry.forEach((entry, count) -> {
            int points = Math.min(count, entry.limit) * entry.value;
            if (points > 0) groupSink.accept(entry.group, points);
        });
    }

    /**
     * Returned by calculatePlayerComfort when the player isn't in a comfort zone (requirements
     * missing, blacklisted, boss nearby). 0 means the zone is active but has no points yet,
     * which still earns the base Comfort tier.
     */
    public static final int NOT_IN_ZONE = -1;

    public static int calculatePlayerComfort(EntityPlayer player) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();

        if (world.isRemote) return 0;

        if (isEnvironmentBlocked(world, playerPos) || isBossBlocked()) {
            if (PotionRegistry.COMFORT != null) {
                player.removePotionEffect(PotionRegistry.COMFORT);
            }
            // REASON: early returns previously skipped setCachedComfort, leaving a
            // stale value in the cache that the client-side render kept displaying.
            PlayerComfortManager.setCachedComfort(player.getUniqueID(), 0);
            return NOT_IN_ZONE;
        }

        // REASON: the hearth lookup is a few index reads now (no block scan), so it runs up
        // front and doubles as the fire/hearth requirement result.
        BlockPos anchor = findActiveAnchor(world, playerPos);

        ComfortRequirements reqs = ComfortRequirementCheck.getRequirementsPresent(world, playerPos, player, anchor != null);
        int comfortActive = countMetRequirements(reqs);

        // Tell the client which requirements are missing so the item tooltip can show them
        if (player instanceof EntityPlayerMP) {
            NetworkHandler.INSTANCE.sendTo(new PacketComfortStatus(reqs.getMissingFlags()), (EntityPlayerMP) player);
        }

        int requiredConditions = 0;
        if (ForgeConfigHandler.server.requireShelter) requiredConditions++;
        if (ForgeConfigHandler.server.minLightLevel > 0) requiredConditions++;
        if (ForgeConfigHandler.server.requireFire) requiredConditions++;
        if (ForgeConfigHandler.server.enableTemperatureComfort) requiredConditions++;

        if (comfortActive < requiredConditions) {
            if (PotionRegistry.COMFORT != null) {
                player.removePotionEffect(PotionRegistry.COMFORT);
            }
            PlayerComfortManager.setCachedComfort(player.getUniqueID(), 0);
            return NOT_IN_ZONE;
        }

        // REASON: the comfort area is centred on the hearth's chunk, not the player's, so the
        // zone stays put while the player moves around inside it. Every player at the same
        // hearth shares one result. Without a hearth (only possible when the hearth is not a
        // requirement) the area follows the player like before.
        AreaResult area = anchor != null
                ? getSharedArea(world, anchor)
                : computeArea(world, playerPos, false);

        PlayerChunkComfortCache cache = getCache(player);
        cache.copyAreaFrom(area.data);

        int finalComfort = Math.max(area.comfort + PettingComfortManager.getActivePettingPoints(player.getUniqueID()), 0);

        PlayerComfortManager.setCachedComfort(player.getUniqueID(), finalComfort);
        return finalComfort;
    }

    /** Comfort counted around one center: blocks, entities, statues and biome (no per-player parts). */
    private static final class AreaResult {
        final PlayerChunkComfortCache data = new PlayerChunkComfortCache();
        int comfort;
        long tick;
        int version;
    }

    /** dimension -> hearth position -> last result for that hearth's area */
    private static final Map<Integer, Map<BlockPos, AreaResult>> AREA_CACHE = new HashMap<>();

    private static AreaResult getSharedArea(World world, BlockPos anchor) {
        long now = world.getTotalWorldTime();
        int maxAge = Math.max(ForgeConfigHandler.server.comfortCheckInterval, 1);
        Map<BlockPos, AreaResult> dimCache = AREA_CACHE.computeIfAbsent(world.provider.getDimension(), k -> new HashMap<>());

        AreaResult cached = dimCache.get(anchor);
        if (cached != null && cached.version == CACHE_VERSION && now - cached.tick < maxAge) {
            return cached;
        }

        // Drop results nobody has used for a while (hearth broken, players left)
        dimCache.values().removeIf(r -> now - r.tick >= maxAge * 2L);

        AreaResult fresh = computeArea(world, anchor, true);
        dimCache.put(anchor.toImmutable(), fresh);
        return fresh;
    }

    private static AreaResult computeArea(World world, BlockPos center, boolean hasAnchor) {
        int radius = getRadius();
        ComfortWorldData worldData = ComfortWorldData.get(world);
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;

        AreaResult result = new AreaResult();
        result.tick = world.getTotalWorldTime();
        result.version = CACHE_VERSION;
        PlayerChunkComfortCache cache = result.data;
        Map<BlockComfortRegistry.BlockKey, Integer> areaKeyCounts = new HashMap<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(centerChunkX + dx, centerChunkZ + dz);
                ChunkComfortData data = worldData.getChunkData(chunkPos);

                // Full block scans only happen around a hearth, never in the open wilderness
                if (!data.initialized && hasAnchor) {
                    worldData.recalcChunkWithFire(world, chunkPos);
                    data = worldData.getChunkData(chunkPos);
                }

                data.blockCounts.forEach(cache::addBlockCount);
                data.keyCounts.forEach((key, count) -> areaKeyCounts.merge(key, count, Integer::sum));
                // decorative entity points; block points are added area-wide below
                data.groupTotals.forEach(cache::addGroupTotal);

                // REASON: candles are placed by right-clicking, not by placing blocks, so the
                // block-count cache never sees them. Reading the chunk's tile entities directly
                // is cheap (no block scan) and always current. Only already-loaded chunks.
                Chunk chunk = world.getChunkProvider().getLoadedChunk(chunkPos.x, chunkPos.z);
                if (chunk != null) {
                    for (TileEntity te : chunk.getTileEntityMap().values()) {
                        if (te instanceof TileEntityStatue) {
                            ((TileEntityStatue) te).collectLitSlots(cache.litStatueSlots);
                        }
                    }
                }
            }
        }

        addAreaLimitedBlockPoints(areaKeyCounts, cache::addGroupTotal);

        addEntityComfort(world, center, radius, cache);

        // --- Combine block and entity group totals ---
        Set<String> allGroups = new HashSet<>();
        allGroups.addAll(cache.groupTotals.keySet());
        allGroups.addAll(cache.entityGroupTotals.keySet());

        int totalComfort = 0;
        for (String group : allGroups) {
            int value = cache.groupTotals.getOrDefault(group, 0)
                    + cache.entityGroupTotals.getOrDefault(group, 0);
            totalComfort += Math.min(value, GroupLimitRegistry.getCap(group));
        }

        // Biome of the zone's center (the hearth), so the whole zone shares one modifier
        ResourceLocation biomeReg = world.getBiome(center).getRegistryName();
        if (biomeReg != null) {
            totalComfort += BiomeComfortRegistry.getBiomeModifier(biomeReg.toString());
        } else {
            chunkcomfort.ChunkComfort.LOGGER.warn("[ChunkComfort] Biome at {} has no registry name, skipping biome modifier", center);
        }

        result.comfort = totalComfort;
        return result;
    }

    // REASON: previously two separate methods — addLivingEntityComfort (one AABB query
    // per chunk) and addDecorativeEntityComfort (one AABB query over the whole area).
    // Living entities and decorative entities are mutually exclusive (living = EntityLiving,
    // decorative = non-EntityLiving), so a single query per chunk can feed both branches
    // in the same loop, halving the number of world.getEntitiesWithinAABB() calls.
    // The decorative dedup set (countedDecoratives) is kept per-chunk since each chunk
    // query is independent; this preserves the original behaviour where an entity on a
    // chunk boundary can only be counted once per the chunk whose box first contains it.
    public static void addEntityComfort(World world, BlockPos center, int radius,
                                        PlayerChunkComfortCache cache) {
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;

        int verticalRange = ForgeConfigHandler.server.fireScanVerticalRange;
        int minY = Math.max(0, center.getY() - verticalRange);
        int maxY = Math.min(world.getHeight() - 1, center.getY() + verticalRange);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(centerChunkX + dx, centerChunkZ + dz);

                AxisAlignedBB chunkBox = new AxisAlignedBB(
                        chunkPos.getXStart(), minY, chunkPos.getZStart(),
                        chunkPos.getXEnd() + 1, maxY, chunkPos.getZEnd() + 1
                );

                // REASON: previously keyed on ResourceLocation, which meant all NBT
                // variants of the same entity ID (e.g. every if_mob_skull type) shared
                // one counter. After counting 1 cyclops skull the limit was hit and
                // stymphalian skulls were blocked even though they are separate config
                // entries with their own limit. Keying on the LivingComfortEntry object
                // itself gives each config line its own independent counter.
                Map<LivingComfortRegistry.LivingComfortEntry, Integer> livingCount = new HashMap<>();
                Set<UUID> countedDecoratives = new HashSet<>();

                for (Entity entity : world.getEntitiesWithinAABB(Entity.class, chunkBox)) {
                    if (entity instanceof EntityLiving) {
                        // --- Living entity branch ---
                        if (entity instanceof EntityArmorStand) continue;

                        LivingComfortRegistry.LivingComfortEntry entry = LivingComfortRegistry.getMatchingEntry(entity);
                        ResourceLocation id = EntityList.getKey(entity);

                        if (entry == null || id == null) continue;

                        int count = livingCount.getOrDefault(entry, 0);
                        if (count >= entry.limit) continue;

                        int bonus = NamedPetComfortRegistry.getBonus(id, entity.getCustomNameTag());
                        if (bonus > 0) {
                            cache.addEntityGroupTotal(entry.group, bonus);
                        }

                        cache.addEntityGroupTotal(entry.group, entry.value);
                        cache.addEntityCount(entity.getClass(), 1);

                        // Populate per-variant count so tooltip shows correct count per skull type
                        String contextKey = LivingComfortRegistry.extractEntityContextKey(id, entity);
                        if (contextKey != null) {
                            cache.addContextEntityCount(contextKey, 1);
                        }

                        livingCount.put(entry, count + 1);

                    } else {
                        // --- Decorative (non-living) entity branch ---
                        if (!EntityComfortRegistry.isComfortEntity(entity)) continue;

                        EntityComfortRegistry.ComfortEntry entry = EntityComfortRegistry.getEntityEntry(entity);
                        if (entry == null) continue;

                        UUID entityId = entity.getUniqueID();
                        if (countedDecoratives.contains(entityId)) continue;
                        countedDecoratives.add(entityId);

                        int currentCount = cache.getDecorativeEntityCount(entity.getClass());
                        if (currentCount >= entry.limit) continue;

                        cache.addDecorativeEntityCount(entity.getClass(), 1);
                        cache.addEntityGroupTotal(entry.group, entry.value);
                    }
                }
            }
        }
    }

    public static AxisAlignedBB getAxisAlignedBB(World world, BlockPos center, int radius) {
        int blockRadius = (radius * 16) + 8;
        int verticalRange = ForgeConfigHandler.server.fireScanVerticalRange;

        int minY = Math.max(0, center.getY() - verticalRange);
        int maxY = Math.min(world.getHeight() - 1, center.getY() + verticalRange);

        return new AxisAlignedBB(
                center.getX() - blockRadius, minY, center.getZ() - blockRadius,
                center.getX() + blockRadius, maxY, center.getZ() + blockRadius
        );
    }

    public static void invalidateMaxComfortCache() {
        cachedMaxComfort = -1;
    }

    public static int getMaxComfort() {

        if (cachedMaxComfort >= 0) {
            return cachedMaxComfort;
        }

        int max = 0;

        // ---------------------------------------
        // GROUP LIMITS (blocks + living entities)
        // ---------------------------------------

        Set<String> allGroups = new HashSet<>();

        allGroups.addAll(BlockComfortRegistry.getAllGroups());
        allGroups.addAll(LivingComfortRegistry.getAllGroups());

        for (String group : allGroups) {

            max += GroupLimitRegistry.getCap(group);
        }

        // ---------------------------------------
        // BIOME BONUS
        // ---------------------------------------

        max += BiomeComfortRegistry.getMaxModifier();

        // ---------------------------------------
        // NAMED PET BONUSES
        // ---------------------------------------

        max += NamedPetComfortRegistry.getMaxPossibleComfort();

        // ---------------------------------------
        // PETTING SYSTEM
        // ---------------------------------------

        max += PettingComfortRegistry.getMaxPossibleComfort();

        cachedMaxComfort = Math.max(max, 0);
        return cachedMaxComfort;
    }
}
