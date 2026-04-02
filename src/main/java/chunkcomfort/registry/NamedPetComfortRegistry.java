package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class NamedPetComfortRegistry {

    private static final Map<ResourceLocation, Map<String, Integer>> BONUSES = new HashMap<>();

    public static void reload(String[] entries) {
        BONUSES.clear();
        if (entries == null) return;

        for (String line : entries) {
            if (line == null || line.trim().isEmpty()) continue;

            try {
                String[] parts = line.split(",", 3);
                if (parts.length < 3) throw new IllegalArgumentException();

                ResourceLocation id = new ResourceLocation(parts[0].trim());
                String[] names = parts[1].trim().replaceAll("^['\"]|['\"]$", "").split("\\|");

                int points;
                try {
                    points = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException();
                }

                BONUSES.computeIfAbsent(id, k -> new HashMap<>());
                for (String name : names) {
                    BONUSES.get(id).put(name, points);
                }

            } catch (Exception e) {
                ChunkComfort.LOGGER.warn(
                        I18n.format(
                                "chunkcomfort.config.invalid_named_pet_entry",
                                line
                        )
                );
            }
        }
    }

    public static String formatNamesWithPoints(ResourceLocation entityId) {
        Map<String, Integer> names = BONUSES.get(entityId);
        if (names == null || names.isEmpty()) return null;

        // Collect all names (assume same point value for all)
        int points = names.values().stream().findFirst().orElse(0);
        String joined = String.join(", ", names.keySet());

        // Localized string
        return I18n.format("tooltip.chunkcomfort.named_pets", joined, points);
    }

    public static int getBonus(ResourceLocation entityId, String name) {
        if (entityId == null || name == null) return 0;
        Map<String, Integer> map = BONUSES.get(entityId);
        if (map == null) return 0;
        return map.getOrDefault(name, 0);
    }
}