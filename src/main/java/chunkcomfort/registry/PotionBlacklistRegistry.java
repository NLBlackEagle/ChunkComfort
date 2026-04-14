package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PotionBlacklistRegistry {

    private static final Map<Integer, Set<String>> blacklist = new HashMap<>();

    public static Map<Integer, Set<String>> getBlacklist() {
        return blacklist;
    }

    public static boolean isBlocked(int comfort, String potion) {

        for (Map.Entry<Integer, Set<String>> entry : blacklist.entrySet()) {

            if (comfort >= entry.getKey()) {
                if (entry.getValue().contains(potion)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void reload(String[] entries) {

        blacklist.clear();

        for (String entry : entries) {
            try {
                String[] parts = entry.split(",", 2);

                int comfort = Integer.parseInt(parts[0].trim());

                String rawList = parts[1].trim();

                if (rawList.startsWith("[") && rawList.endsWith("]")) {
                    rawList = rawList.substring(1, rawList.length() - 1);
                }

                String[] potions = rawList.split(",");

                Set<String> set = blacklist.computeIfAbsent(comfort, k -> new HashSet<>());

                for (String potion : potions) {
                    potion = potion.trim();
                    if (!potion.isEmpty()) {
                        set.add(potion);
                    }
                }

            } catch (Exception e) {
                ChunkComfort.LOGGER.error("Invalid potion blacklist entry: " + entry);
            }
        }
    }
}