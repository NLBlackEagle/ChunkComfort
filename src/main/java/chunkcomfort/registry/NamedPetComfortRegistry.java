package chunkcomfort.registry;

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
            if (line == null || line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 3) continue;

            ResourceLocation id = new ResourceLocation(parts[0].trim());
            String[] names = parts[1].trim().replaceAll("^['\"]|['\"]$", "").split("\\|");
            int points;
            try { points = Integer.parseInt(parts[2].trim()); }
            catch (NumberFormatException e) { continue; }

            BONUSES.computeIfAbsent(id, k -> new HashMap<>());
            for (String name : names) {
                BONUSES.get(id).put(name, points);
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