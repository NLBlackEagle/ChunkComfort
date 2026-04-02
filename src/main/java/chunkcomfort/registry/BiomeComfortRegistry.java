package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import net.minecraft.util.text.translation.I18n;

import java.util.HashMap;
import java.util.Map;

public class BiomeComfortRegistry {

    private static final Map<String, Integer> BIOME_MODIFIERS = new HashMap<>();

    public static void reload(String[] entries) {
        BIOME_MODIFIERS.clear();
        if (entries == null) return;



        for (String line : entries) {

            if (line == null || line.trim().isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 2) {
                ChunkComfort.LOGGER.warn(I18n.translateToLocalFormatted("chunkcomfort.config.invalid_entry", line));
                continue;
            }

            try {
                String biome = parts[0].trim();
                int value = Integer.parseInt(parts[1].trim());

                BIOME_MODIFIERS.put(biome, value);

            } catch (Exception e) {
                ChunkComfort.LOGGER.warn(I18n.translateToLocalFormatted("chunkcomfort.config.invalid_entry", line));
            }
        }
    }

    public static int getBiomeModifier(String biomeName) {
        return BIOME_MODIFIERS.getOrDefault(biomeName, 0);
    }
}