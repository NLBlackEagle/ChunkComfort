package chunkcomfort.chunk;

import java.util.HashMap;
import java.util.Map;

import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.registry.BiomeComfortRegistry;
import chunkcomfort.registry.PotionRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class AreaComfortCalculator {

    private static final Map<String, Integer> GROUP_LIMITS = new HashMap<>();

    /**
     * Called from config reload to update group limits.
     * Format: <group>,<limit>
     */
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
     * Check activation conditions: shelter + fire.
     */
    public static int calculateComfortActivation(World world, int chunkX, int chunkZ, EntityPlayer player) {

        int comfortActive = 0;
        BlockPos playerPos = player.getPosition();

        // Shelter requirement
        if (ForgeConfigHandler.server.requireShelter && !world.canSeeSky(playerPos.up())) {
            comfortActive += 1;
        }

        // Minimum light requirement
        int light = world.getLight(playerPos);
        if (ForgeConfigHandler.server.minLightLevel > 0 && light >= ForgeConfigHandler.server.minLightLevel) {
            comfortActive += 1;
        }

        // Fire requirement (toggleable)
        if (ForgeConfigHandler.server.requireFire) {
            boolean fireFound = false;
            ComfortWorldData worldData = ComfortWorldData.get(world);

            // Clamp radius to safe range (0–3)
            int radius = Math.min(Math.max(ForgeConfigHandler.server.chunkRadius, 0), 3);

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    ChunkPos pos = new ChunkPos(chunkX + dx, chunkZ + dz);
                    ChunkComfortData data = worldData.getChunkData(pos);
                    if (data.hasFire()) {
                        fireFound = true;
                        break;
                    }
                }
                if (fireFound) break;
            }

            if (fireFound) comfortActive += 1;
        }

        return comfortActive;
    }

    /**
     * Calculate total comfort for the player using cached group totals.
     */
    public static int calculatePlayerComfort(EntityPlayer player) {
        ChunkPos center = new ChunkPos(player.getPosition());

        // Check base comfort activation first
        int comfortActive = calculateComfortActivation(player.world, center.x, center.z, player);

        // Count how many conditions are enabled
        int requiredConditions = 0;
        if (ForgeConfigHandler.server.requireShelter) requiredConditions++;
        if (ForgeConfigHandler.server.minLightLevel > 0) requiredConditions++;
        if (ForgeConfigHandler.server.requireFire) requiredConditions++;

        // Remove comfort potion immediately if conditions not met
        if (comfortActive < requiredConditions) {
            if (PotionRegistry.COMFORT != null) {
                player.removePotionEffect(PotionRegistry.COMFORT);
            }
            return 0;
        }

        // Clamp radius to a safe range (0–3)
        int radius = Math.min(Math.max(ForgeConfigHandler.server.chunkRadius, 0), 3);

        Map<String, Integer> summedGroups = new HashMap<>();
        ComfortWorldData worldData = ComfortWorldData.get(player.world);

        // Loop over chunks dynamically based on radius
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);
                ChunkComfortData data = worldData.getChunkData(pos);

                for (Map.Entry<String, Integer> entry : data.groupTotals.entrySet()) {
                    summedGroups.put(entry.getKey(),
                            summedGroups.getOrDefault(entry.getKey(), 0) + entry.getValue());
                }
            }
        }

        // Sum comfort, respecting group limits
        int totalComfort = 0;
        for (Map.Entry<String, Integer> entry : summedGroups.entrySet()) {
            String group = entry.getKey();
            int value = entry.getValue();
            int limit = getGroupLimit(group);
            totalComfort += Math.min(value, limit);
        }

        // Apply biome modifier
        String biomeName = player.world.getBiome(player.getPosition()).getRegistryName().toString();
        int biomeModifier = BiomeComfortRegistry.getBiomeModifier(biomeName);
        totalComfort += biomeModifier;

        return Math.max(totalComfort, 0);
    }

    public static int getGroupLimit(String group) {
        return GROUP_LIMITS.getOrDefault(group, Integer.MAX_VALUE);
    }
}