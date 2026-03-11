package chunkcomfort.registry;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for block comfort entries
 */
public class BlockComfortRegistry {

    private static final Map<Block, BlockComfortEntry> ENTRIES = new HashMap<>();

    public static void register(Block block, BlockComfortEntry entry) {
        ENTRIES.put(block, entry);
    }

    public static BlockComfortEntry get(Block block) {
        return ENTRIES.get(block);
    }

    public static boolean contains(Block block) {
        return ENTRIES.containsKey(block);
    }

    public static void clear() {
        ENTRIES.clear();
    }

    /**
     * Reload registry from config strings
     * Format: <block>,<value>,<limit>,<group>
     */
    public static void reload(String[] configLines) {
        clear();

        if (configLines == null) return;

        for (int i = 0; i < configLines.length; i++) {
            String line = configLines[i];
            try {
                String[] parts = line.split(",");
                if (parts.length != 4) continue;

                String blockId = parts[0].trim();
                int value = Integer.parseInt(parts[1].trim());
                int limit = Integer.parseInt(parts[2].trim());
                String group = parts[3].trim();

                // 1.12.2 block lookup
                Block block = Block.REGISTRY.getObject(new ResourceLocation(blockId));
                if (block == null) continue;

                register(block, new BlockComfortEntry(blockId, value, limit, group));

            } catch (Exception ignored) {
            }
        }
    }
}