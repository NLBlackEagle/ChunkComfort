package chunkcomfort.registry;

import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class BiomeComfortRegistry {

    private static final Map<String, Integer> BIOME_MODIFIERS = new HashMap<>();

    public static void reload(String[] entries) {
        BIOME_MODIFIERS.clear();
        if (entries == null) return;

        for (String line : entries) {
            if (line == null || line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 2) continue;

            String biome = parts[0];
            int value;
            try {
                value = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                value = 0;
            }

            BIOME_MODIFIERS.put(biome, value);
        }
    }

    public static int getBiomeModifier(String biomeName) {
        return BIOME_MODIFIERS.getOrDefault(biomeName, 0);
    }
}