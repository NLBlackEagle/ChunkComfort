package chunkcomfort.chunk;

import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.FireBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class ChunkUpdateManager {

    /** Called when a block is placed */
    public static void onBlockPlaced(World world, BlockPos pos, Block block) {
        ChunkComfortData data = ComfortWorldData.get(world).getChunkData(posToChunkPos(pos));

        // Update comfort
        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getEntry(block);
        if (entry != null) {
            data.addComfort(entry.group, entry.value);
        }

        // Persist changes
        ComfortWorldData.get(world).setChunkData(posToChunkPos(pos), data);
    }

    /** Called when a block is broken */
    public static void onBlockBroken(World world, BlockPos pos, Block block) {
        ChunkComfortData data = ComfortWorldData.get(world).getChunkData(posToChunkPos(pos));

        // Update comfort
        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getEntry(block);
        if (entry != null) {
            data.removeComfort(entry.group, entry.value);
        }

        // Persist changes
        ComfortWorldData.get(world).setChunkData(posToChunkPos(pos), data);
    }

    /** Helper to convert world position to chunk position */
    private static ChunkPos posToChunkPos(BlockPos pos) {
        return new ChunkPos(pos);
    }

    /** Retrieve chunk data without creating new */
    public static ChunkComfortData getChunkData(World world, int chunkX, int chunkZ) {
        return ComfortWorldData.get(world).getChunkData(new ChunkPos(chunkX, chunkZ));
    }
}