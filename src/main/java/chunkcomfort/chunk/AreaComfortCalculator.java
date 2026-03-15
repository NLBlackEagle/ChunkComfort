package chunkcomfort.chunk;

import chunkcomfort.registry.FireBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.HashMap;
import java.util.Map;

public class AreaComfortCalculator {

    private static final Map<String, Integer> GROUP_LIMITS = new HashMap<String, Integer>();

    /**
     * Called from config reload to update group limits.
     * Format: <group>,<limit>
     */
    public static void reloadGroupLimits(String[] limits) {
        GROUP_LIMITS.clear();
        if (limits == null) return;

        for (int i = 0; i < limits.length; i++) {
            String line = limits[i];
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

    public static int calculatePlayerComfort(EntityPlayer player) {
        ChunkPos center = new ChunkPos(player.getPosition());

        int comfortActive = calculateComfortActivation(player.world, center.x, center.z, player);

        // If no activation condition is met, comfort system is disabled
        if (comfortActive < 2) {
            return 0;
        }

        int totalComfort = 0;
        Map<String, Integer> summedGroups = new HashMap<>();

        // Scan 3x3 area using cached totals
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkComfortData data = ChunkUpdateManager.getChunkData(center.x + dx, center.z + dz);

                // Sum all groups (cached total per group)
                for (Map.Entry<String, Integer> entry : data.groupTotals.entrySet()) {
                    String group = entry.getKey();
                    int value = entry.getValue();
                    summedGroups.put(group, summedGroups.getOrDefault(group, 0) + value);
                }
            }
        }

        // Apply group limits
        int limitedTotal = 0;
        for (Map.Entry<String, Integer> entry : summedGroups.entrySet()) {
            String group = entry.getKey();
            int value = entry.getValue();
            int limit = getGroupLimit(group);
            limitedTotal += Math.min(value, limit);
        }

        return limitedTotal;
    }

    public static int getGroupLimit(String group) {
        if (GROUP_LIMITS.containsKey(group)) {
            return GROUP_LIMITS.get(group);
        }
        return Integer.MAX_VALUE;
    }
}