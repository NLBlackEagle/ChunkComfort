package chunkcomfort.chunk;

import java.util.HashMap;
import java.util.Map;

import chunkcomfort.registry.BiomeComfortRegistry;
import net.minecraft.entity.player.EntityPlayer;
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

        // Shelter requirement (no skylight)
        if (!world.canSeeSky(player.getPosition().up())) {
            comfortActive += 1;
        }

        boolean fireFound = false;

        // Fire Requirement using cached hasFire
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkComfortData data = ChunkUpdateManager.getChunkData(chunkX + dx, chunkZ + dz);
                if (data.hasFire()) {
                    fireFound = true;
                    break;
                }
            }
            if (fireFound) break;
        }

        if (fireFound) {
            comfortActive += 1;
        }

        return comfortActive;
    }

    /**
     * Calculate total comfort for the player using cached group totals.
     */
    public static int calculatePlayerComfort(EntityPlayer player) {
        ChunkPos center = new ChunkPos(player.getPosition());

        int comfortActive = calculateComfortActivation(player.world, center.x, center.z, player);

        if (comfortActive < 2) return 0;

        Map<String, Integer> summedGroups = new HashMap<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkComfortData data = ChunkUpdateManager.getChunkData(center.x + dx, center.z + dz);
                for (Map.Entry<String, Integer> entry : data.groupTotals.entrySet()) {
                    summedGroups.put(entry.getKey(), summedGroups.getOrDefault(entry.getKey(), 0) + entry.getValue());
                }
            }
        }

        int totalComfort = 0;
        for (Map.Entry<String, Integer> entry : summedGroups.entrySet()) {
            String group = entry.getKey();
            int value = entry.getValue();
            int limit = getGroupLimit(group);
            totalComfort += Math.min(value, limit);
        }

        // Apply biome comfort modifier
        String biomeName = player.world.getBiome(player.getPosition()).getRegistryName().toString();
        int biomeModifier = BiomeComfortRegistry.getBiomeModifier(biomeName);

        totalComfort += biomeModifier;
        if (totalComfort < 0) totalComfort = 0;

        return totalComfort;
    }

    public static int getGroupLimit(String group) {
        return GROUP_LIMITS.getOrDefault(group, Integer.MAX_VALUE);
    }
}