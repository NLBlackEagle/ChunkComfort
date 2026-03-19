package chunkcomfort.chunk;

import net.minecraft.block.Block;

import java.util.HashMap;
import java.util.Map;

public class ChunkComfortData {
    public final Map<Block, Integer> blockCounts = new HashMap<>();
    public final Map<String, Integer> groupTotals = new HashMap<>();
    public int totalComfort = 0;
    public boolean initialized;

    public long lastRecalcTick = -1;

    // Fire caching
    public boolean firePresent = false;

    public void addComfort(String group, int value) {
        if (group == null || value <= 0) return;

        int current = groupTotals.getOrDefault(group, 0);

        // increment total for this group
        groupTotals.put(group, current + value);

        // increment overall total comfort
        totalComfort += value;
    }

    public void removeComfort(String group, int value) {
        if (group == null || value <= 0) return;

        int current = groupTotals.getOrDefault(group, 0);
        if (current <= 0) return;

        int applied = Math.min(value, current);

        int newValue = current - applied;
        if (newValue <= 0) groupTotals.remove(group);
        else groupTotals.put(group, newValue);

        totalComfort -= applied;
        if (totalComfort < 0) totalComfort = 0;
    }
}