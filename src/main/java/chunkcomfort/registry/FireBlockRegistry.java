package chunkcomfort.registry;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
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

                ResourceLocation rl = new ResourceLocation(blockId);

                // Prevent invalid IDs defaulting to AIR
                if (!Block.REGISTRY.containsKey(rl)) {
                    System.out.println("[ChunkComfort] Invalid fire block in config: " + blockId);
                    continue;
                }

                Block block = Block.REGISTRY.getObject(rl);

                if (block != null && block != Blocks.AIR) {
                    register(block);
                }

            } catch (Exception e) {
                System.out.println("[ChunkComfort] Failed parsing fire block: " + blockId);
            }
        }
    }
}