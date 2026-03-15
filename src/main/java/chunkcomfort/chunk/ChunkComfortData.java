package chunkcomfort.chunk;

import java.util.HashMap;
import java.util.Map;

public class ChunkComfortData {
    public final Map<String, Integer> groupTotals = new HashMap<String, Integer>();

    // New cached fields
    public int totalComfort = 0;     // total of all groups, before group limits
    public boolean hasFire = false;  // whether this chunk contains fire

    public void addComfort(String group, int value) {
        if (groupTotals.containsKey(group)) {
            groupTotals.put(group, groupTotals.get(group) + value);
        } else {
            groupTotals.put(group, value);
        }

        // update cached total
        totalComfort += value;
    }

    public void removeComfort(String group, int value) {
        if (groupTotals.containsKey(group)) {
            int newValue = groupTotals.get(group) - value;
            if (newValue <= 0) {
                groupTotals.remove(group);
            } else {
                groupTotals.put(group, newValue);
            }

            // update cached total
            totalComfort -= value;
            if (totalComfort < 0) totalComfort = 0; // safety check
        }
    }

    public int getComfort(String group) {
        return groupTotals.getOrDefault(group, 0);
    }

    public void setFirePresent(boolean fire) {
        hasFire = fire;
    }

    public boolean hasFire() {
        return hasFire;
    }
}