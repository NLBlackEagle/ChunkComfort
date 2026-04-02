package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import chunkcomfort.chunk.PettingComfortData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PettingComfortRegistry {

    private static final Map<String, PettingComfortData> registry = new HashMap<>();

    public static void loadFromConfig(String[] configLines) {
        registry.clear();
        if (configLines == null) return;

        for (String line : configLines) {
            if (line == null || line.trim().isEmpty()) continue;

            try {
                PettingComfortData entry = new PettingComfortData(line);
                registry.put(entry.entityId, entry);
            } catch (Exception e) {
                ChunkComfort.LOGGER.warn(
                        net.minecraft.client.resources.I18n.format(
                                "chunkcomfort.config.invalid_petting_entry",
                                line
                        )
                );
            }
        }
    }

    public static PettingComfortData getEntry(String entityId) {
        return registry.get(entityId);
    }

    public static Collection<PettingComfortData> getAllEntries() {
        return registry.values();
    }
}
