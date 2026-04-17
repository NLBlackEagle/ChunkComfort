package chunkcomfort.chunk;

import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.player.PlayerComfortManager;
import chunkcomfort.registry.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;

import static chunkcomfort.chunk.ComfortWorldData.hasFireNearby;

public class AreaComfortCalculator {

    private static int CACHE_VERSION = 0;
    private static int cachedMaxComfort = -1;
    public static void incrementCacheVersion() {CACHE_VERSION++;}
    public static int getCacheVersion() {return CACHE_VERSION;}

    private static final boolean DEBUG_COMFORT = false;

    private static final Map<UUID, PlayerChunkComfortCache> PLAYER_CACHES = new HashMap<>();

    public static PlayerChunkComfortCache getCache(EntityPlayer player) {
        return PLAYER_CACHES.computeIfAbsent(player.getUniqueID(), k -> new PlayerChunkComfortCache());
    }

    public static void clearAllPlayerCaches() {
        PLAYER_CACHES.clear();
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

    public static int calculateComfortActivation(World world, EntityPlayer player) {
        BlockPos pos = player.getPosition();
        ComfortRequirements reqs = ComfortRequirementCheck.getRequirementsPresent(world, pos, player);

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

        if (DimensionBiomeBlacklistRegistry.isDimensionBlocked(dimId)
                || DimensionBiomeBlacklistRegistry.isBiomeBlocked(biomeId)) {
            return true;
        }

        return ForgeConfigHandler.server.enableBossBarDetection && ChunkBossState.isBossActive();
    }

    public static int calculatePlayerComfort(EntityPlayer player) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();
        int radius = getRadius();

        if (world.isRemote) return 0;

        if (isEnvironmentBlocked(world, playerPos)) {
            if (PotionRegistry.COMFORT != null) {
                player.removePotionEffect(PotionRegistry.COMFORT);
            }
            return 0;
        }

        int comfortActive = calculateComfortActivation(world, player);
        int requiredConditions = 0;
        if (ForgeConfigHandler.server.requireShelter) requiredConditions++;
        if (ForgeConfigHandler.server.minLightLevel > 0) requiredConditions++;
        if (ForgeConfigHandler.server.requireFire) requiredConditions++;
        if (ForgeConfigHandler.server.enableTemperatureComfort) requiredConditions++;

        if (comfortActive < requiredConditions) {
            if (PotionRegistry.COMFORT != null) {
                player.removePotionEffect(PotionRegistry.COMFORT);
            }
            return 0;
        }

        ComfortWorldData worldData = ComfortWorldData.get(world);
        int centerChunkX = playerPos.getX() >> 4;
        int centerChunkZ = playerPos.getZ() >> 4;

        PlayerChunkComfortCache cache = getCache(player);
        boolean fireNearby = hasFireNearby(world, playerPos, radius);
        cache.clear();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(centerChunkX + dx, centerChunkZ + dz);
                ChunkComfortData data = worldData.getChunkData(chunkPos);

                if (!data.initialized && fireNearby) {
                    worldData.recalcChunkWithFire(world, chunkPos);
                    data = worldData.getChunkData(chunkPos);
                }

                data.blockCounts.forEach(cache::addBlockCount);
                data.groupTotals.forEach(cache::addGroupTotal);
            }
        }

        addLivingEntityComfort(world, playerPos, radius, cache);
        addDecorativeEntityComfort(world, playerPos, radius, cache);

        // --- Combine block and entity group totals ---
        Set<String> allGroups = new HashSet<>();
        allGroups.addAll(cache.groupTotals.keySet());
        allGroups.addAll(cache.entityGroupTotals.keySet());

        int totalComfort = 0;
        for (String group : allGroups) {
            int value = cache.groupTotals.getOrDefault(group, 0)
                    + cache.entityGroupTotals.getOrDefault(group, 0);

            int totalLimit = BlockComfortRegistry.getGroupLimit(group);

            totalComfort += Math.min(value, totalLimit);
        }

        ResourceLocation biomeReg = world.getBiome(playerPos).getRegistryName();
        int biomeModifier = 0;
        if (biomeReg != null) {
            biomeModifier = BiomeComfortRegistry.getBiomeModifier(biomeReg.toString());
        } else {
            chunkcomfort.ChunkComfort.LOGGER.warn("[ChunkComfort] Biome at {} has no registry name, skipping biome modifier", playerPos);
        }
        totalComfort += biomeModifier;

        totalComfort += PettingComfortManager.getActivePettingPoints(player.getUniqueID());

        int finalComfort = Math.max(totalComfort, 0);

        debugComfortBreakdown(player, cache, allGroups, biomeModifier, finalComfort);


        PlayerComfortManager.setCachedComfort(player.getUniqueID(), finalComfort);
        return finalComfort;
    }

    public static void addLivingEntityComfort(World world, BlockPos center, int radius,
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


                Map<ResourceLocation, Integer> livingCount = new HashMap<>();

                for (Entity entity : world.getEntitiesWithinAABB(Entity.class, chunkBox)) {
                    if (!(entity instanceof EntityLiving) || entity instanceof EntityArmorStand) continue;

                    LivingComfortRegistry.LivingComfortEntry entry = LivingComfortRegistry.getMatchingEntry(entity);
                    ResourceLocation id = EntityList.getKey(entity);

                    if (entry == null || id == null) continue;

                    int count = livingCount.getOrDefault(id, 0);
                    if (count >= entry.limit) continue;

                    int bonus = NamedPetComfortRegistry.getBonus(id, entity.getCustomNameTag());
                    if (bonus > 0) {
                        cache.addEntityGroupTotal(entry.group, bonus);
                    }

                    cache.addEntityGroupTotal(entry.group, entry.value);
                    cache.addEntityCount(entity.getClass(), 1);
                    livingCount.put(id, count + 1);
                }
            }
        }
    }



    public static void addDecorativeEntityComfort(World world, BlockPos center, int radius,
                                                  PlayerChunkComfortCache cache) {
        AxisAlignedBB box = getAxisAlignedBB(world, center, radius);
        List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, box);

        // Track which decorative entities we've counted this tick
        Set<UUID> countedDecoratives = new HashSet<>();

        for (Entity entity : entities) {
            if (entity instanceof EntityLiving) continue;
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

            max += BlockComfortRegistry.getGroupLimit(group);
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

    private static void debugComfortBreakdown(
            EntityPlayer player,
            PlayerChunkComfortCache cache,
            Set<String> allGroups,
            int biomeModifier,
            int totalComfort) {

        if (!DEBUG_COMFORT) return;

        System.out.println("======================================");
        System.out.println("[ComfortDebug] Player: " + player.getName());
        System.out.println("[ComfortDebug] TotalComfort: " + totalComfort);
        System.out.println("[ComfortDebug] PettingBoost: " + PettingComfortManager.getActivePettingPoints(player.getUniqueID()));

        for (String group : allGroups) {
            int blockValue = cache.groupTotals.getOrDefault(group, 0);
            int entityValue = cache.entityGroupTotals.getOrDefault(group, 0);
            int combined = blockValue + entityValue;

            int totalLimit = BlockComfortRegistry.getGroupLimit(group);

            int applied = Math.min(combined, totalLimit);

            System.out.println(
                    "[ComfortDebug] Group: " + group +
                            " blocks=" + blockValue +
                            " entities=" + entityValue +
                            " total=" + combined +
                            " limit=" + totalLimit +
                            " applied=" + applied
            );
        }

        System.out.println("[ComfortDebug] BiomeModifier: " + biomeModifier);
        System.out.println("======================================");
    }
}
