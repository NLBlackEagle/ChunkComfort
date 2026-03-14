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

    public static int calculateComfortActivation(World world, int chunkX, int chunkZ) {

        int comfortActive = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {

                Chunk chunk = world.getChunk(chunkX + dx, chunkZ + dz);

                if (chunkContainsFire(chunk)) {
                    comfortActive += 1;
                    return comfortActive; // one fire source is enough
                }
            }
        }

        return comfortActive;
    }

    private static boolean chunkContainsFire(Chunk chunk) {

        int startX = chunk.x * 16;
        int startZ = chunk.z * 16;

        World world = chunk.getWorld();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < world.getHeight(); y++) {

                    pos.setPos(startX + x, y, startZ + z);

                    Block block = world.getBlockState(pos).getBlock();

                    if (FireBlockRegistry.isFireBlock(block)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static int calculatePlayerComfort(EntityPlayer player) {
        ChunkPos center = new ChunkPos(player.getPosition());

        int comfortActive = calculateComfortActivation(player.world, center.x, center.z);

        // If no activation condition is met, comfort system is disabled
        if (comfortActive < 1) {
            return 0;
        }

        Map<String, Integer> summed = new HashMap<String, Integer>();

        // Scan 3x3 area
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkComfortData data = ChunkUpdateManager.getChunkData(center.x + dx, center.z + dz);
                for (Map.Entry<String, Integer> entry : data.groupTotals.entrySet()) {
                    String group = entry.getKey();
                    int value = entry.getValue();
                    if (summed.containsKey(group)) {
                        summed.put(group, summed.get(group) + value);
                    } else {
                        summed.put(group, value);
                    }
                }
            }
        }

        // Apply group limits
        int totalComfort = 0;
        for (Map.Entry<String, Integer> entry : summed.entrySet()) {
            String group = entry.getKey();
            int value = entry.getValue();
            int limit;
            if (GROUP_LIMITS.containsKey(group)) {
                limit = GROUP_LIMITS.get(group);
            } else {
                limit = Integer.MAX_VALUE;
            }
            totalComfort += Math.min(value, limit);
        }

        return totalComfort;
    }

    public static int getGroupLimit(String group) {
        if (GROUP_LIMITS.containsKey(group)) {
            return GROUP_LIMITS.get(group);
        }
        return Integer.MAX_VALUE;
    }
}