package chunkcomfort.registry;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.HashMap;
import java.util.Map;

public class BlockComfortRegistry {

    private static final Map<Block, ComfortEntry> ENTRIES = new HashMap<Block, ComfortEntry>();

    static {
        // Default hardcoded entries
        ENTRIES.put(Blocks.BOOKSHELF, new ComfortEntry(1, "furniture"));
        ENTRIES.put(Blocks.CRAFTING_TABLE, new ComfortEntry(10, "workstation"));
    }

    public static ComfortEntry getEntry(Block block) {
        return ENTRIES.get(block);
    }

    /**
     * Reload the registry from config entries.
     * Each entry format: <block>,<value>,<limit>,<group>
     */
    public static void reload(String[] entries) {
        ENTRIES.clear();

        if (entries == null) return;

        for (int i = 0; i < entries.length; i++) {
            String line = entries[i];
            if (line == null || line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 4) continue;

            String blockName = parts[0];
            int value;
            // We can ignore 'limit' here in this registry
            String group = parts[3];

            try {
                value = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                value = 0;
            }

            // Resolve block from registry
            Block block = Block.getBlockFromName(blockName);
            if (block != null) {
                ENTRIES.put(block, new ComfortEntry(value, group));
            }
        }
    }

    public static class ComfortEntry {
        public final int value;
        public final String group;

        public ComfortEntry(int value, String group) {
            this.value = value;
            this.group = group;
        }
    }
}