package chunkcomfort.chunk;

import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.registry.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;

public class AreaComfortCalculator {

    private static int CACHE_VERSION = 0;
    public static void incrementCacheVersion() {CACHE_VERSION++;}
    public static int getCacheVersion() {return CACHE_VERSION;}

    private static final boolean DEBUG_COMFORT = false;

    private static final Map<String, Integer> GROUP_LIMITS = new HashMap<>();
    private static final Map<UUID, PlayerChunkComfortCache> PLAYER_CACHES = new HashMap<>();

    public static PlayerChunkComfortCache getCache(EntityPlayer player) {
        return PLAYER_CACHES.computeIfAbsent(player.getUniqueID(), k -> new PlayerChunkComfortCache());
    }

    public static void clearAllPlayerCaches() {
        PLAYER_CACHES.clear();
    }

    public static int getRadius() {
        return Math.min(Math.max(ForgeConfigHandler.server.chunkRadius, 0), 3);
    }

    public static void reloadGroupLimits(String[] limits) {
        GROUP_LIMITS.clear();
        if (limits == null) return;

        for (String line : limits) {
            if (line == null || line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 2) continue;

            String group = parts[0];
            int limit;

            try {
                limit = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                limit = Integer.MAX_VALUE;
            }

            GROUP_LIMITS.put(group, limit);
        }
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

    public static int calculatePlayerComfort(EntityPlayer player) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();
        int radius = getRadius();


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
        cache.clear();

        // Collect block and group totals from nearby chunks
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(centerChunkX + dx, centerChunkZ + dz);
                ChunkComfortData data = worldData.getChunkData(chunkPos);

                if (!data.initialized) {
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

            int blockLimit = BlockComfortRegistry.getGroupLimit(group);
            int livingLimit = LivingComfortRegistry.getGroupLimit(group);
            int totalLimit = blockLimit + livingLimit;

            totalComfort += Math.min(value, totalLimit);
        }

        // --- Include biome modifier ---
        String biomeName = Objects.requireNonNull(world.getBiome(playerPos).getRegistryName()).toString();
        int biomeModifier = BiomeComfortRegistry.getBiomeModifier(biomeName);
        totalComfort += biomeModifier;

        // --- Include temporary petting boosts ---
        totalComfort += PettingComfortManager.getActivePettingPoints(player.getUniqueID());

        int finalComfort = Math.max(totalComfort, 0);

        debugComfortBreakdown(player, cache, allGroups, biomeModifier, finalComfort);

        return finalComfort;
    }

    public static void addLivingEntityComfort(World world, BlockPos center, int radius,
                                              PlayerChunkComfortCache cache) {
        AxisAlignedBB box = getAxisAlignedBB(world, center, radius);
        List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, box);

        Map<ResourceLocation, Integer> livingCount = new HashMap<>();

        for (Entity entity : entities) {
            // -----------------------------
            // Living entities (excluding armor stands)
            // -----------------------------
            if (entity instanceof EntityLiving && !(entity instanceof EntityArmorStand)) {
                LivingComfortRegistry.LivingComfortEntry entry = LivingComfortRegistry.getMatchingEntry(entity);
                ResourceLocation id = EntityList.getKey(entity);

                if (entry == null || id == null) continue;

                int count = livingCount.getOrDefault(id, 0);
                if (count >= entry.limit) continue;

                int cacheCount = cache.getEntityCount(entity.getClass());
                if (cacheCount >= entry.limit) continue;

                // Add bonus based on name
                int bonus = NamedPetComfortRegistry.getBonus(EntityList.getKey(entity), entity.getCustomNameTag());
                if (bonus > 0) {
                    cache.addEntityGroupTotal(entry.group, bonus);
                }

                cache.addEntityCount(entity.getClass(), 1);
                cache.addEntityGroupTotal(entry.group, entry.value);



                livingCount.put(id, count + 1);
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
            // Only handle non-living "decorative" comfort entities
            if (entity instanceof net.minecraft.entity.EntityLiving) continue;

            if (!EntityComfortRegistry.isComfortEntity(entity)) continue;
            EntityComfortRegistry.ComfortEntry entry = EntityComfortRegistry.getEntityEntry(entity);
            if (entry == null) continue;

            Class<? extends Entity> clazz = entity.getClass();

            // Handle multi-block or duplicate entities like paintings
            UUID entityId = entity.getUniqueID();
            if (countedDecoratives.contains(entityId)) continue;
            countedDecoratives.add(entityId);

            // Respect per-entity limits
            int currentCount = cache.getDecorativeEntityCount(clazz);
            if (currentCount >= entry.limit) continue;

            cache.addDecorativeEntityCount(clazz, 1);
            cache.addEntityGroupTotal(entry.group, entry.value); // Keep group totals separate from living entities
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

        for (String group : allGroups) {

            int blockValue = cache.groupTotals.getOrDefault(group, 0);
            int entityValue = cache.entityGroupTotals.getOrDefault(group, 0);
            int combined = blockValue + entityValue;

            int blockLimit = BlockComfortRegistry.getGroupLimit(group);
            int livingLimit = LivingComfortRegistry.getGroupLimit(group);
            int totalLimit = blockLimit + livingLimit;

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