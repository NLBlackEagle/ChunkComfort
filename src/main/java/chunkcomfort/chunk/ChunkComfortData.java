package chunkcomfort.chunk;

import java.util.HashMap;
import java.util.Map;

public class ChunkComfortData {
    public final Map<String, Integer> groupTotals = new HashMap<>();
    public int totalComfort = 0;
    public boolean initialized;

    public long lastRecalcTick = -1;

    // Fire caching
    public boolean firePresent = false;

    public void addComfort(String group, int value) {
        groupTotals.put(group, groupTotals.getOrDefault(group, 0) + value);
        totalComfort += value;
    }

    public void removeComfort(String group, int value) {
        if (!groupTotals.containsKey(group)) return;
        int newValue = groupTotals.get(group) - value;
        if (newValue <= 0) groupTotals.remove(group);
        else groupTotals.put(group, newValue);
        totalComfort = Math.max(0, totalComfort - value);
    }
}