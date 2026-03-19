package chunkcomfort.chunk;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class ChunkScanner {

    /**
     * Scan all blocks in a chunk within Y range and call blockConsumer for each non-air block.
     */
    public static void scanChunk(World world, ChunkPos chunkPos, int minY, int maxY, BiConsumer<BlockPos, Block> blockConsumer) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startX = chunkPos.x << 4;
        int startZ = chunkPos.z << 4;
        maxY = Math.min(maxY, world.getHeight() - 1);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    pos.setPos(startX + x, y, startZ + z);
                    Block block = world.getBlockState(pos).getBlock();
                    if (!block.isAir(world.getBlockState(pos), world, pos)) {
                        blockConsumer.accept(pos, block);
                    }
                }
            }
        }
    }

    /**
     * Scan chunks around a center position, radius in chunks.
     */
    public static void scanChunks(World world, BlockPos center, int radius, int minY, int maxY, BiConsumer<BlockPos, Block> blockConsumer) {
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(centerChunkX + dx, centerChunkZ + dz);
                scanChunk(world, chunkPos, minY, maxY, blockConsumer);
            }
        }
    }

    /**
     * Returns true if any block in the scanned area matches the predicate.
     * Useful for fire detection or other early-exit checks.
     */
    public static boolean anyBlockMatches(World world, BlockPos center, int radius, int minY, int maxY, Predicate<Block> predicate) {
        final boolean[] found = {false};

        scanChunks(world, center, radius, minY, maxY, (pos, block) -> {
            if (predicate.test(block)) {
                found[0] = true;
                throw new StopScanException(); // break all loops
            }
        });

        return found[0];
    }

    /**
     * Custom exception to break nested loops for early exit.
     */
    public static class StopScanException extends RuntimeException {}
}