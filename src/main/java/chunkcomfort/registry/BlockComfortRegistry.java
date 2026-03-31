package chunkcomfort.registry;

import net.minecraft.block.Block;
import java.util.*;

public class BlockComfortRegistry {

    // Config-driven blocks
    public static final Map<Block, ComfortEntry> BLOCK_ENTRIES = new HashMap<>();
    public static final Map<String, String[]> BLOCK_ALIASES = new HashMap<>();

    /**
     * Reload block aliases from config
     * Format: key=alias1,alias2,alias3
     * Example: minecraft:banner=minecraft:standing_banner,minecraft:wall_banner
     */
    public static void reloadAliases(String[] aliases) {
        BLOCK_ALIASES.clear();
        if (aliases == null) return;

        for (String line : aliases) {
            if (line == null || line.isEmpty()) continue;
            String[] split = line.split("=");
            if (split.length != 2) continue;

            String key = split[0].trim();
            String[] vals = split[1].split(",");
            for (int i = 0; i < vals.length; i++) vals[i] = vals[i].trim();
            BLOCK_ALIASES.put(key, vals);
        }
    }

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

            ComfortEntry entry = new ComfortEntry(value, group, limit);

            // Register main block + aliases
            registerBlockAndAliases(id, entry);
        }
    }

    /**
     * Registers a block and all its aliases
     */
    private static void registerBlockAndAliases(String id, ComfortEntry entry) {
        // Register main block
        Block block = Block.getBlockFromName(id);
        if (block != null) BLOCK_ENTRIES.put(block, entry);

        // Register aliases
        String[] aliases = BLOCK_ALIASES.get(id);
        if (aliases != null) {
            for (String aliasId : aliases) {
                Block aliasBlock = Block.getBlockFromName(aliasId);
                if (aliasBlock != null) {
                    BLOCK_ENTRIES.put(aliasBlock, entry);
                }
            }
        }
    }

    /**
     * Returns the canonical ID used in the config for a given registry name string,
     * either the main ID or the alias key it belongs to.
     */
    public static String getCanonicalIdFromRegistryName(String registryName) {
        if (registryName == null) return null;

        // Check if this registryName is a main key
        if (BLOCK_ALIASES.containsKey(registryName)) return registryName;

        // Otherwise, check if it is an alias
        for (Map.Entry<String, String[]> entry : BLOCK_ALIASES.entrySet()) {
            String key = entry.getKey();
            String[] aliases = entry.getValue();
            for (String alias : aliases) {
                if (registryName.equals(alias)) return key; // return main ID
            }
        }

        // fallback: return the original registry name
        return registryName;
    }

    /**
     * Returns the canonical ID used in the config for this block,
     * either the block's registry name or the alias key it belongs to.
     */
    public static String getCanonicalId(Block block) {
        if (block == null) return null;

        String blockId = Block.REGISTRY.getNameForObject(block).toString();

        for (Map.Entry<String, String[]> entry : BLOCK_ALIASES.entrySet()) {
            String key = entry.getKey();
            String[] aliases = entry.getValue();
            if (key.equals(blockId)) return key; // main block
            for (String alias : aliases) {
                if (alias.equals(blockId)) return key; // return main alias key
            }
        }
        return blockId; // fallback: block ID
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

    public static int getGroupLimit(String groupName) {
        if (groupName == null || groupName.isEmpty()) return 0;

        int totalLimit = 0;
        for (ComfortEntry entry : BLOCK_ENTRIES.values()) {
            if (groupName.equals(entry.group)) {
                totalLimit += entry.limit;
            }
        }
        return totalLimit;
    }
}