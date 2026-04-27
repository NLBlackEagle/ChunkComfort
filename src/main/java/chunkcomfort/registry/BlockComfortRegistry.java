package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import java.util.*;

public class BlockComfortRegistry {

    // Config-driven blocks
    // REASON: key is now BlockKey (Block + optional metadata) instead of bare Block,
    // so entries like waystones:waystone:2 only match the specific metadata value.
    // Entries without metadata (meta = -1) continue to match any metadata, preserving
    // all existing config behaviour.
    public static final Map<BlockKey, ComfortEntry> BLOCK_ENTRIES = new LinkedHashMap<>();
    public static final Map<String, String[]> BLOCK_ALIASES = new HashMap<>();

    public static class BlockKey {
        public final Block block;
        public final int meta; // -1 = match any

        public BlockKey(Block block, int meta) {
            this.block = block;
            this.meta = meta;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BlockKey)) return false;
            BlockKey other = (BlockKey) o;
            return block == other.block && meta == other.meta;
        }

        @Override
        public int hashCode() {
            return 31 * block.hashCode() + meta;
        }
    }

    /**
     * Reload block aliases from config
     * Format: key=alias1,alias2,alias3
     * Example: minecraft:banner=minecraft:standing_banner,minecraft:wall_banner
     */
    public static void reloadAliases(String[] aliases) {
        BLOCK_ALIASES.clear();
        if (aliases == null) {
            return;
        }


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

            line = line.split("#", 2)[0].trim();

            if (line == null || line.trim().isEmpty()) continue;

            try {

                String[] parts = line.split(",");

                if (parts.length < 3)
                    throw new IllegalArgumentException();

                String rawId = parts[0].trim();
                int value = Integer.parseInt(parts[1].trim());
                String group = parts[2].trim();

                int limit = value;

                if (parts.length >= 4) {
                    limit = Integer.parseInt(parts[3].trim());
                }

                // REASON: support optional metadata suffix in block ID (e.g. waystones:waystone:2).
                // Split on the last colon only if the part after it is a number.
                String id = rawId;
                int meta = -1; // -1 = match any metadata
                int lastColon = rawId.lastIndexOf(':');
                if (lastColon > 0) {
                    String afterColon = rawId.substring(lastColon + 1);
                    try {
                        meta = Integer.parseInt(afterColon);
                        id = rawId.substring(0, lastColon);
                    } catch (NumberFormatException ignored) {
                        // not metadata — id stays as rawId, meta stays -1
                    }
                }

                ComfortEntry entry = new ComfortEntry(value, group, limit);

                registerBlockAndAliases(id, meta, entry);

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
     * Registers a block and all its aliases using BlockKey (block + optional metadata).
     */
    private static void registerBlockAndAliases(String id, int meta, ComfortEntry entry) {

        Block block = Block.getBlockFromName(id);
        if (block != null) {
            BLOCK_ENTRIES.put(new BlockKey(block, meta), entry);
        }

        String[] aliases = BLOCK_ALIASES.get(id);

        if (aliases != null) {
            for (String aliasId : aliases) {
                Block aliasBlock = Block.getBlockFromName(aliasId);
                if (aliasBlock != null) {
                    BLOCK_ENTRIES.put(new BlockKey(aliasBlock, meta), entry);
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

    // Returns the registered BlockKey that matched for this blockstate.
    // Used by the chunk scan to key per-entry counters against the registered entry
    // rather than the actual metadata, so wildcard entries share one counter across
    // all metadata variants of the same block.
    public static BlockKey getKeyForEntry(Block block, IBlockState state) {
        int meta = block.getMetaFromState(state);
        if (BLOCK_ENTRIES.containsKey(new BlockKey(block, meta))) return new BlockKey(block, meta);
        if (BLOCK_ENTRIES.containsKey(new BlockKey(block, -1))) return new BlockKey(block, -1);
        return null;
    }

    // REASON: lookup by blockstate so metadata-specific entries are correctly matched.
    // Tries exact (block + meta) first, then falls back to wildcard (block + meta=-1).
    public static ComfortEntry getBlockEntry(Block block, IBlockState state) {
        int meta = block.getMetaFromState(state);
        ComfortEntry exact = BLOCK_ENTRIES.get(new BlockKey(block, meta));
        if (exact != null) return exact;
        return BLOCK_ENTRIES.get(new BlockKey(block, -1));
    }

    // Block-only overload: returns the first matching entry for this block regardless of
    // metadata. Used by callers that only have a Block (tooltip, particle, event handlers).
    // The chunk scan uses the blockstate overload for precise metadata matching.
    public static ComfortEntry getBlockEntry(Block block) {
        ComfortEntry wildcard = BLOCK_ENTRIES.get(new BlockKey(block, -1));
        if (wildcard != null) return wildcard;
        for (Map.Entry<BlockKey, ComfortEntry> e : BLOCK_ENTRIES.entrySet()) {
            if (e.getKey().block == block) return e.getValue();
        }
        return null;
    }

    public static boolean isComfortBlock(Block block, IBlockState state) {
        return getBlockEntry(block, state) != null;
    }

    public static boolean isComfortBlock(Block block) {
        return getBlockEntry(block) != null;
    }

    public static String getGroup(Block block, IBlockState state) {
        ComfortEntry entry = getBlockEntry(block, state);
        return entry != null ? entry.group : null;
    }

    public static String getGroup(Block block) {
        ComfortEntry entry = getBlockEntry(block);
        return entry != null ? entry.group : null;
    }

    public static int getValue(Block block, IBlockState state) {
        ComfortEntry entry = getBlockEntry(block, state);
        return entry != null ? entry.value : 0;
    }

    public static int getValue(Block block) {
        ComfortEntry entry = getBlockEntry(block);
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

    public static Set<String> getAllGroups() {
        Set<String> groups = new HashSet<>();

        for (ComfortEntry entry : BLOCK_ENTRIES.values()) {
            if (entry.group != null) {
                groups.add(entry.group);
            }
        }

        return groups;
    }
}