package chunkcomfort.registry;

import chunkcomfort.chunk.PettingComfortData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PettingComfortRegistry {

    private static final Map<String, PettingComfortData> registry = new HashMap<>();

    public static void loadFromConfig(String[] configLines) {
        registry.clear();
        for (String line : configLines) {
            PettingComfortData entry = new PettingComfortData(line);
            registry.put(entry.entityId, entry);
        }
    }

    public static PettingComfortData getEntry(String entityId) {
        return registry.get(entityId);
    }

    public static Collection<PettingComfortData> getAllEntries() {
        return registry.values();
    }
}
