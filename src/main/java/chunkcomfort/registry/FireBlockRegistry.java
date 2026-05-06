package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FireBlockRegistry {

    // Blocks registered with no metadata — match any variant
    private static final Set<Block> WILDCARD_FIRE_BLOCKS = new HashSet<>();

    // Blocks registered with specific metadata — only match those exact values
    // Map<Block, Set<Integer>> means: for this block, only these meta values count
    private static final Map<Block, Set<Integer>> META_FIRE_BLOCKS = new HashMap<>();

    public static void reload(String[] fireBlocks) {

        WILDCARD_FIRE_BLOCKS.clear();
        META_FIRE_BLOCKS.clear();

        if (fireBlocks == null) return;

        for (String entry : fireBlocks) {

            if (entry == null || entry.trim().isEmpty()) continue;

            // Split on # to separate the block name from optional metadata
            // e.g. "minecraft:wool#1" -> parts[0] = "minecraft:wool", parts[1] = "1"
            // e.g. "minecraft:fire"   -> parts[0] = "minecraft:fire", no parts[1]
            String[] parts = entry.split("@", 2);
            String name = parts[0].trim();

            if (name.isEmpty()) continue;

            try {
                ResourceLocation id = new ResourceLocation(name);

                if (!Block.REGISTRY.containsKey(id)) {
                    ChunkComfort.LOGGER.warn(
                            I18n.translateToLocalFormatted(
                                    "chunkcomfort.config.invalid_fire_block_entry",
                                    entry
                            )
                    );
                    continue;
                }

                Block block = Block.REGISTRY.getObject(id);

                if (block == null || block == Blocks.AIR) {
                    ChunkComfort.LOGGER.warn(
                            I18n.translateToLocalFormatted(
                                    "chunkcomfort.config.invalid_fire_block_entry",
                                    entry
                            )
                    );
                    continue;
                }

                if (parts.length == 1) {
                    // No # present — wildcard, match any metadata
                    WILDCARD_FIRE_BLOCKS.add(block);
                } else {
                    // # present — parse the metadata value
                    try {
                        int meta = Integer.parseInt(parts[1].trim());

                        // getOrDefault won't work here because we need to also put the
                        // new set back into the map. computeIfAbsent does both in one
                        // step: if the key is missing it creates a new HashSet, puts it
                        // in the map, and returns it — all atomically.
                        META_FIRE_BLOCKS.computeIfAbsent(block, k -> new HashSet<>()).add(meta);

                    } catch (NumberFormatException e) {
                        ChunkComfort.LOGGER.warn(
                                I18n.translateToLocalFormatted(
                                        "chunkcomfort.config.invalid_fire_block_entry",
                                        entry
                                )
                        );
                    }
                }

            } catch (Exception e) {
                ChunkComfort.LOGGER.warn(
                        I18n.translateToLocalFormatted(
                                "chunkcomfort.config.invalid_fire_block_entry",
                                entry
                        )
                );
            }
        }
    }

    /**
     * Checks whether the given block state counts as a fire block.
     *
     * We accept an IBlockState rather than a Block so we can read the metadata
     * from the world directly. The caller should pass world.getBlockState(pos)
     * rather than just the block — this is the standard 1.12 pattern for
     * metadata-aware block checks.
     *
     * WHY getMetaFromState?
     * In 1.12, block metadata is stored as IBlockState but serialised as an
     * integer (0-15) for comparisons. Block.getMetaFromState() converts the
     * current state back to that integer so we can compare it against the
     * values the player put in the config.
     */
    public static boolean isFireBlock(IBlockState state) {
        Block block = state.getBlock();

        // Wildcard match — block is registered with no metadata restriction
        if (WILDCARD_FIRE_BLOCKS.contains(block)) return true;

        // Metadata match — block is registered with specific metadata values
        Set<Integer> allowedMeta = META_FIRE_BLOCKS.get(block);
        if (allowedMeta != null) {
            int meta = block.getMetaFromState(state);
            return allowedMeta.contains(meta);
        }

        return false;
    }

    /**
     * Convenience overload for callers that only have a Block, not an IBlockState.
     * This returns true only if the block is registered as a wildcard (no metadata).
     * Use the IBlockState overload whenever possible for accurate metadata checks.
     */
    public static boolean isFireBlock(Block block) {
        return WILDCARD_FIRE_BLOCKS.contains(block);
    }
}