package chunkcomfort.registry;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/**
 * Registry for fire blocks
 */
public class FireBlockRegistry {

    private static final Set<Block> FIRE_BLOCKS = new HashSet<>();

    public static void register(Block block) {
        FIRE_BLOCKS.add(block);
    }

    public static boolean contains(Block block) {
        return FIRE_BLOCKS.contains(block);
    }

    public static void clear() {
        FIRE_BLOCKS.clear();
    }

    /**
     * Reload registry from config strings
     * Each line is a block ID, e.g. minecraft:torch
     */
    public static void reload(String[] configLines) {
        clear();

        if (configLines == null) return;

        for (int i = 0; i < configLines.length; i++) {
            String blockId = configLines[i].trim();
            if (blockId.isEmpty()) continue;

            try {
                Block block = Block.REGISTRY.getObject(new ResourceLocation(blockId));
                if (block != null) {
                    register(block);
                }
            } catch (Exception ignored) {}
        }
    }
}