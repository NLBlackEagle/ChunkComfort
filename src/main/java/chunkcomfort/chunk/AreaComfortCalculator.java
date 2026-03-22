package chunkcomfort.chunk;

import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.registry.BiomeComfortRegistry;
import chunkcomfort.registry.FireBlockRegistry;
import chunkcomfort.registry.LivingComfortRegistry;
import chunkcomfort.registry.PotionRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AreaComfortCalculator {

    private static final Map<String, Integer> GROUP_LIMITS = new HashMap<>();

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

    /**
     * Check activation conditions: shelter + light + fire.
     */
    public static int calculateComfortActivation(World world, EntityPlayer player) {
        int comfortActive = 0;
        BlockPos playerPos = player.getPosition();

        // Step 1: Shelter check
        boolean shelterOk = false;
        if (ForgeConfigHandler.server.requireShelter && !world.canSeeSky(playerPos.up())) {
            comfortActive++;
            shelterOk = true;
        }

        // Step 2: Light check
        boolean lightOk = false;
        int light = world.getLight(playerPos);
        if (ForgeConfigHandler.server.minLightLevel > 0 && light >= ForgeConfigHandler.server.minLightLevel) {
            comfortActive++;
            lightOk = true;
        }

        // Step 3: Fire check (only if fire is required and at least shelter or light contributed)
        if (ForgeConfigHandler.server.requireFire && (shelterOk || lightOk)) {
            int radius = getRadius();
            int verticalRange = ForgeConfigHandler.server.fireScanVerticalRange;
            int minY = Math.max(0, playerPos.getY() - verticalRange);
            int maxY = Math.min(world.getHeight() - 1, playerPos.getY() + verticalRange);

            try {
                boolean fireFound = ChunkScanner.anyBlockMatches(
                        world,
                        playerPos,
                        radius,
                        minY,
                        maxY,
                        FireBlockRegistry::isFireBlock
                );

                if (fireFound) comfortActive++;

            } catch (ChunkScanner.StopScanException e) {
                // early exit already handled in anyBlockMatches
                comfortActive++;
            }
        }

        return comfortActive;
    }

    /**
     * Live fire scan around player within chunk radius.
     */
    public static boolean isFirePresent(World world, BlockPos playerPos) {
        if (!ForgeConfigHandler.server.requireFire) return false;

        boolean shelterOk = !ForgeConfigHandler.server.requireShelter || !world.canSeeSky(playerPos.up());
        int light = world.getLight(playerPos);
        boolean lightOk = ForgeConfigHandler.server.minLightLevel <= 0 || light >= ForgeConfigHandler.server.minLightLevel;
        if (!shelterOk && !lightOk) return false;

        ChunkPos chunkPos = new ChunkPos(playerPos);
        ComfortWorldData worldData = ComfortWorldData.get(world);
        ChunkComfortData data = worldData.getOrCreateChunkData(world, chunkPos);

        // Self-healing: recalc with fire if uninitialized
        if (!data.initialized) {
            worldData.recalcChunkWithFire(world, chunkPos);
            data = worldData.getOrCreateChunkData(world, chunkPos);
        }

        return data.firePresent;
    }

    /**
     * Calculate total comfort for the player using cached group totals.
     */
    public static int calculatePlayerComfort(EntityPlayer player) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();
        int radius = getRadius();

        // Step 1: Check activation conditions first (shelter, light, fire)
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

        // Step 2: Sum cached chunk comfort data
        Map<String, Integer> summedGroups = new HashMap<>();
        ComfortWorldData worldData = ComfortWorldData.get(world);

        int centerChunkX = playerPos.getX() >> 4;
        int centerChunkZ = playerPos.getZ() >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(centerChunkX + dx, centerChunkZ + dz);
                ChunkComfortData data = worldData.getChunkData(chunkPos);

                if (!data.initialized) {
                    worldData.recalcChunkWithFire(world, chunkPos);
                    data = worldData.getChunkData(chunkPos);
                }

                for (Map.Entry<String, Integer> entry : data.groupTotals.entrySet()) {
                    summedGroups.put(
                            entry.getKey(),
                            summedGroups.getOrDefault(entry.getKey(), 0) + entry.getValue()
                    );
                }
            }
        }

        AreaComfortCalculator.addLivingEntityComfort(world, playerPos, radius, summedGroups);

        // Step 3: Apply group limits
        int totalComfort = 0;
        for (Map.Entry<String, Integer> entry : summedGroups.entrySet()) {
            String group = entry.getKey();
            int value = entry.getValue();
            int limit = getGroupLimit(group);
            totalComfort += Math.min(value, limit);
        }

        // Step 4: Add biome modifier
        String biomeName = Objects.requireNonNull(world.getBiome(playerPos).getRegistryName()).toString();
        int biomeModifier = BiomeComfortRegistry.getBiomeModifier(biomeName);
        totalComfort += biomeModifier;

        return Math.max(totalComfort, 0);
    }

    public static void addLivingEntityComfort(World world, BlockPos center, int radius, Map<String, Integer> summedGroups) {

        AxisAlignedBB box = getAxisAlignedBB(world, center, radius);

        List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, box);

        // Track how many times each entity type contributes
        Map<ResourceLocation, Integer> entityCount = new HashMap<>();

        for (Entity entity : entities) {
            if (!LivingComfortRegistry.isComfortEntity(entity)) continue;

            LivingComfortRegistry.LivingComfortEntry entry = LivingComfortRegistry.getEntry(entity);
            if (entry == null) continue;

            // 1. Get the ResourceLocation in 1.12.2
            ResourceLocation id = EntityList.getKey(entity);
            if (id == null) continue;

            int count = entityCount.getOrDefault(id, 0);
            if (count >= entry.limit) continue; // enforce per-entity limit

            summedGroups.put(
                    entry.group,
                    summedGroups.getOrDefault(entry.group, 0) + entry.value
            );

            entityCount.put(id, count + 1);
        }
    }

    private static AxisAlignedBB getAxisAlignedBB(World world, BlockPos center, int radius) {
        int blockRadius = (radius * 16) + 8; // horizontal scan radius
        int verticalRange = ForgeConfigHandler.server.fireScanVerticalRange;

        // Clamp Y coordinates to world limits (0..world height - 1)
        int minY = Math.max(0, center.getY() - verticalRange);
        int maxY = Math.min(world.getHeight() - 1, center.getY() + verticalRange);

        // Create bounding box
        AxisAlignedBB box = new AxisAlignedBB(
                center.getX() - blockRadius,
                minY,
                center.getZ() - blockRadius,
                center.getX() + blockRadius,
                maxY,
                center.getZ() + blockRadius
        );
        return box;
    }

    public static int getGroupLimit(String group) {
        return GROUP_LIMITS.getOrDefault(group, Integer.MAX_VALUE);
    }
}