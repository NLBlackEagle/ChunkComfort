package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import net.minecraft.block.Block;
import net.minecraft.util.text.translation.I18n;

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

            if (line == null || line.trim().isEmpty()) continue;

            String[] split = line.split("=");

            if (split.length != 2) {
                ChunkComfort.LOGGER.warn(
                        I18n.translateToLocalFormatted(
                                "chunkcomfort.config.invalid_block_alias",
                                line
                        )
                );
                continue;
            }

            String key = split[0].trim();

            String[] vals = split[1].split(",");
            for (int i = 0; i < vals.length; i++) {
                vals[i] = vals[i].trim();
            }

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

            if (line == null || line.trim().isEmpty()) continue;

            try {

                String[] parts = line.split(",");

                if (parts.length < 3)
                    throw new IllegalArgumentException();

                String id = parts[0].trim();
                int value = Integer.parseInt(parts[1].trim());
                String group = parts[2].trim();

                int limit = value;

                if (parts.length >= 4) {
                    limit = Integer.parseInt(parts[3].trim());
                }

                ComfortEntry entry = new ComfortEntry(value, group, limit);

                registerBlockAndAliases(id, entry);

            } catch (Exception e) {

                ChunkComfort.LOGGER.warn(
                        I18n.translateToLocalFormatted(
                                "chunkcomfort.config.invalid_block_entry",
                                line
                        )
                );
            }
        }
    }

    /**
     * Registers a block and all its aliases
     */
    private static void registerBlockAndAliases(String id, ComfortEntry entry) {

        boolean registered = false;

        // Try canonical block (may not exist!)
        Block block = Block.getBlockFromName(id);
        if (block != null) {
            BLOCK_ENTRIES.put(block, entry);
            registered = true;
        }

        // ALWAYS process aliases
        String[] aliases = BLOCK_ALIASES.get(id);

        if (aliases != null) {
            for (String aliasId : aliases) {

                Block aliasBlock = Block.getBlockFromName(aliasId);

                if (aliasBlock != null) {
                    BLOCK_ENTRIES.put(aliasBlock, entry);
                    registered = true;
                }
            }
        }

        if (!registered) {
            ChunkComfort.LOGGER.warn("No valid block found for {}", id);
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