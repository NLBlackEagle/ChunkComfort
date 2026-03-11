package chunkcomfort.registry;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

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

    public static void reload(String[] configLines) {

        clear();

        if (configLines == null) return;

        for (String blockId : configLines) {

            blockId = blockId.trim();

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