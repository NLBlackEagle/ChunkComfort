package chunkcomfort.registry;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class FireBlockRegistry {

    private static final Set<Block> FIRE_BLOCKS = new HashSet<Block>();

    public static void reload(String[] fireBlocks) {

        FIRE_BLOCKS.clear();

        if (fireBlocks == null) {
            return;
        }

        for (String name : fireBlocks) {

            if (name == null || name.isEmpty()) {
                continue;
            }

            try {

                ResourceLocation id = new ResourceLocation(name);

                // Check if the block actually exists
                if (!Block.REGISTRY.containsKey(id)) {
                    continue;
                }

                Block block = Block.REGISTRY.getObject(id);

                // Reject invalid or air blocks
                if (block == null || block == Blocks.AIR) {
                    continue;
                }

                FIRE_BLOCKS.add(block);

            } catch (Exception ignored) {
            }
        }
    }

    public static boolean isFireBlock(Block block) {
        return FIRE_BLOCKS.contains(block);
    }
}