package chunkcomfort.registry;

import chunkcomfort.config.ForgeConfigHandler;

import java.util.HashMap;
import java.util.Map;

public class GroupLimitRegistry {

    private static final Map<String,Integer> GROUP_LIMITS = new HashMap<>();

    public static void reload() {
        GROUP_LIMITS.clear();

        for (String entry : ForgeConfigHandler.server.groupLimits) {
            String[] parts = entry.split(",");
            if (parts.length != 2) continue;

            GROUP_LIMITS.put(parts[0].trim(),
                    Integer.parseInt(parts[1].trim()));
        }
    }

    public static int getCap(String group) {
        return GROUP_LIMITS.getOrDefault(group, Integer.MAX_VALUE);
    }
}