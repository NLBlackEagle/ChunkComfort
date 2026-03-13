package chunkcomfort.registry;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

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

    public static void reload(String[] configLines) {
        clear();
        if (configLines == null) return;

        for (String line : configLines) {
            try {
                String[] parts = line.split(",");
                if (parts.length != 4) continue;

                String blockId = parts[0].trim();
                int value = Integer.parseInt(parts[1].trim());
                int limit = Integer.parseInt(parts[2].trim());
                String group = parts[3].trim();

                Block block = Block.REGISTRY.getObject(new ResourceLocation(blockId));

                // Skip invalid or air blocks
                if (block == null || block == Blocks.AIR) {
                    System.out.println("[ChunkComfort] Invalid or AIR block in config: " + blockId);
                    continue;
                }

                register(block, new BlockComfortEntry(blockId, value, limit, group));
            } catch (Exception ignored) {}
        }
    }
}