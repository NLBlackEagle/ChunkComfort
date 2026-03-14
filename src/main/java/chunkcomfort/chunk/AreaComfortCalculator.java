package chunkcomfort.chunk;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.Map;

public class AreaComfortCalculator {
    // Hardcoded group limits for now
    private static final Map<String, Integer> GROUP_LIMITS = new HashMap<String, Integer>();
    static {
        GROUP_LIMITS.put("furniture", 10);
        GROUP_LIMITS.put("workstation", 5);
    }

    public static int calculatePlayerComfort(EntityPlayer player) {
        ChunkPos center = new ChunkPos(player.getPosition());
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
}