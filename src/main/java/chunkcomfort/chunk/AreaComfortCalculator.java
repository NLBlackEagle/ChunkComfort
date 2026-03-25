package chunkcomfort.chunk;

import chunkcomfort.config.ForgeConfigHandler;
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

public class AreaComfortCalculator {

    private static final Map<String, Integer> GROUP_LIMITS = new HashMap<>();
    private static final Map<UUID, PlayerChunkComfortCache> PLAYER_CACHES = new HashMap<>();

    public static PlayerChunkComfortCache getCache(EntityPlayer player) {
        return PLAYER_CACHES.computeIfAbsent(player.getUniqueID(), k -> new PlayerChunkComfortCache());
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
        ComfortRequirements reqs = ComfortRequirementCheck.getRequirementsPresent(world, pos);

        int comfort = 0;
        if (reqs.shelterOk) comfort++;
        if (reqs.lightOk) comfort++;
        if (reqs.fireOk) comfort++;

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

        addLivingEntityComfort(world, playerPos, radius, cache.groupTotals, cache);

        int totalComfort = 0;
        for (Map.Entry<String, Integer> entry : cache.groupTotals.entrySet()) {
            String group = entry.getKey();
            int value = entry.getValue();

            int blockLimit = BlockComfortRegistry.getGroupLimit(group);
            int livingLimit = LivingComfortRegistry.getGroupLimit(group);
            int totalLimit = blockLimit + livingLimit;

            totalComfort += Math.min(value, totalLimit);
        }

        String biomeName = Objects.requireNonNull(world.getBiome(playerPos).getRegistryName()).toString();
        int biomeModifier = BiomeComfortRegistry.getBiomeModifier(biomeName);
        totalComfort += biomeModifier;

        return Math.max(totalComfort, 0);
    }

    public static void addLivingEntityComfort(World world, BlockPos center, int radius,
                                              Map<String, Integer> summedGroups, PlayerChunkComfortCache cache) {
        AxisAlignedBB box = getAxisAlignedBB(world, center, radius);
        List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, box);

        // Temporary counters to enforce per-entity limits
        Map<ResourceLocation, Integer> livingCount = new HashMap<>();
        Map<Class<? extends Entity>, Integer> nonLivingCount = new HashMap<>();

        for (Entity entity : entities) {
            // -----------------------------
            // Living entities (ocelots, parrots, horses, etc.)
            // -----------------------------
            if (entity instanceof EntityLiving && !(entity instanceof EntityArmorStand)) {

                // Get the registry entry (includes NBT matching)
                LivingComfortRegistry.LivingComfortEntry entry = LivingComfortRegistry.getMatchingEntry(entity);
                if (entry == null) continue; // Skip unconfigured or NBT-mismatch

                ResourceLocation id = EntityList.getKey(entity);
                if (id == null) continue;

                int count = livingCount.getOrDefault(id, 0);
                if (count >= entry.limit) continue;

                // Update group sums
                summedGroups.merge(entry.group, entry.value, Integer::sum);
                livingCount.put(id, count + 1);

                // Update player cache
                if (cache != null) {
                    cache.addEntityCount(entity.getClass(), 1);
                    cache.addEntityGroupTotal(entry.group, entry.value);
                }
            }
            // -----------------------------
            // Non-living / block-like entities (armor stands, paintings, modded entities)
            // -----------------------------
            else {
                Class<? extends Entity> clazz = entity.getClass();

                if (!EntityComfortRegistry.isComfortEntity(entity)) continue;

                EntityComfortRegistry.ComfortEntry entry = EntityComfortRegistry.getEntityEntry(entity);
                if (entry == null) continue;

                int count = nonLivingCount.getOrDefault(clazz, 0);
                if (count >= entry.limit) continue;

                if (cache != null && cache.getEntityCount(clazz) >= 0) continue;

                summedGroups.merge(entry.group, entry.value, Integer::sum);
                nonLivingCount.put(clazz, count + 1);

                if (cache != null) {
                    cache.addEntityCount(clazz, 1);
                    cache.addEntityGroupTotal(entry.group, entry.value);
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
}