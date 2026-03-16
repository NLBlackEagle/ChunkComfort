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

        // Update fire presence
        if (FireBlockRegistry.isFireBlock(block)) {
            data.setFirePresent(true);
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

        // Update fire presence: scan chunk for remaining fire
        if (FireBlockRegistry.isFireBlock(block)) {
            data.setFirePresent(scanChunkForFire(world, pos));
        }

        // Persist changes
        ComfortWorldData.get(world).setChunkData(posToChunkPos(pos), data);
    }

    /** Scan the entire chunk for any fire blocks */
    private static boolean scanChunkForFire(World world, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        int startX = chunkX * 16;
        int startZ = chunkZ * 16;

        BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < world.getHeight(); y++) {
                    scanPos.setPos(startX + x, y, startZ + z);
                    Block b = world.getBlockState(scanPos).getBlock();
                    if (FireBlockRegistry.isFireBlock(b)) return true;
                }
            }
        }
        return false;
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