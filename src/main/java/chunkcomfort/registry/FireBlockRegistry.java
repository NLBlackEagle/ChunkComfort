package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import java.util.HashSet;
import java.util.Set;

public class FireBlockRegistry {

    private static final Set<Block> FIRE_BLOCKS = new HashSet<Block>();

    public static void reload(String[] fireBlocks) {

        FIRE_BLOCKS.clear();

        if (fireBlocks == null) return;

        for (String name : fireBlocks) {

            if (name == null || name.trim().isEmpty()) continue;

            try {
                ResourceLocation id = new ResourceLocation(name);

                if (!Block.REGISTRY.containsKey(id)) {
                    ChunkComfort.LOGGER.warn(
                            I18n.translateToLocalFormatted(
                                    "chunkcomfort.config.invalid_fire_block_entry",
                                    name
                            )
                    );
                    continue;
                }

                Block block = Block.REGISTRY.getObject(id);

                if (block == null || block == Blocks.AIR) {
                    ChunkComfort.LOGGER.warn(
                            I18n.translateToLocalFormatted(
                                    "chunkcomfort.config.invalid_fire_block_entry",
                                    name
                            )
                    );
                    continue;
                }

                FIRE_BLOCKS.add(block);

            } catch (Exception e) {
                ChunkComfort.LOGGER.warn(
                        I18n.translateToLocalFormatted(
                                "chunkcomfort.config.invalid_fire_block_entry",
                                name
                        )
                );
            }
        }
    }

    public static boolean isFireBlock(Block block) {
        return FIRE_BLOCKS.contains(block);
    }
}