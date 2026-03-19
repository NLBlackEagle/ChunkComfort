package chunkcomfort.registry;

import net.minecraft.block.Block;
import java.util.*;

public class BlockComfortRegistry {

    // Config-driven blocks
    private static final Map<Block, ComfortEntry> BLOCK_ENTRIES = new HashMap<>();

    /**
     * Reload block comfort entries from config
     * Each entry: <block_id>,<value>,<group>,<limit>
     */
    public static void reload(String[] entries) {
        BLOCK_ENTRIES.clear();
        if (entries == null) return;

        for (String line : entries) {
            if (line == null || line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 3) continue;

            String id = parts[0];
            int value;
            String group = parts[2];

            try {
                value = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                value = 0;
            }

            int limit = value;
            if (parts.length >= 4) {
                try {
                    limit = Integer.parseInt(parts[3]);
                } catch (NumberFormatException ignored) {}
            }

            Block block = Block.getBlockFromName(id);
            if (block != null) {
                BLOCK_ENTRIES.put(block, new ComfortEntry(value, group, limit));
            }
        }
    }

    public static boolean isComfortBlock(Block block) {
        return BLOCK_ENTRIES.containsKey(block);
    }

    public static ComfortEntry getBlockEntry(Block block) {
        return BLOCK_ENTRIES.get(block);
    }

    public static String getGroup(Block block) {
        ComfortEntry entry = BLOCK_ENTRIES.get(block);
        return entry != null ? entry.group : null;
    }

    public static int getValue(Block block) {
        ComfortEntry entry = BLOCK_ENTRIES.get(block);
        return entry != null ? entry.value : 0;
    }

    /**
     * Registry entry
     */
    public static class ComfortEntry {
        public final int value;
        public final String group;
        public final int limit;

        public ComfortEntry(int value, String group, int limit) {
            this.value = value;
            this.group = group;
            this.limit = limit;
        }
    }
}