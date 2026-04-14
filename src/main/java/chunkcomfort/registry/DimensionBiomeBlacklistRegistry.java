package chunkcomfort.registry;

import java.util.HashSet;
import java.util.Set;

public class DimensionBiomeBlacklistRegistry {

    private static final Set<String> DIMENSIONS = new HashSet<>();
    private static final Set<String> BIOMES = new HashSet<>();

    public static boolean isDimensionBlocked(String dim) {
        return DIMENSIONS.contains(dim);
    }

    public static boolean isBiomeBlocked(String biome) {
        return BIOMES.contains(biome);
    }

    public static void reload(String[] dims, String[] biomes) {

        DIMENSIONS.clear();
        BIOMES.clear();

        if (dims != null) {
            for (String d : dims) {
                if (d != null && !d.isEmpty()) {
                    DIMENSIONS.add(d.trim());
                }
            }
        }

        if (biomes != null) {
            for (String b : biomes) {
                if (b != null && !b.isEmpty()) {
                    BIOMES.add(b.trim());
                }
            }
        }
    }
}