package chunkcomfort.chunk;

import java.util.HashMap;
import java.util.Map;

public class ChunkComfortData {
    public final Map<String, Integer> groupTotals = new HashMap<String, Integer>();

    public void addComfort(String group, int value) {
        if (groupTotals.containsKey(group)) {
            groupTotals.put(group, groupTotals.get(group) + value);
        } else {
            groupTotals.put(group, value);
        }
    }

    // New: remove comfort points safely
    public void removeComfort(String group, int value) {
        if (groupTotals.containsKey(group)) {
            int newValue = groupTotals.get(group) - value;
            if (newValue <= 0) {
                groupTotals.remove(group);
            } else {
                groupTotals.put(group, newValue);
            }
        }
    }

    public int getComfort(String group) {
        if (groupTotals.containsKey(group)) {
            return groupTotals.get(group);
        }
        return 0;
    }
}