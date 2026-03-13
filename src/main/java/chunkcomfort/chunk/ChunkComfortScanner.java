package chunkcomfort.chunk;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import chunkcomfort.chunk.ChunkComfortData.GroupData;
import chunkcomfort.registry.BlockComfortEntry;
import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.FireBlockRegistry;
import chunkcomfort.handlers.ForgeConfigHandler;

public class ChunkComfortScanner {

    public static void scanChunk(Chunk chunk) {

        ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);
        if (data == null || data.initialized) return;

        data.fireCount = 0; // reset fire count at the start of the scan
        int maxY = chunk.getWorld().getHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int remainingDepth = ForgeConfigHandler.server.columnScanDepth;

                for (int y = maxY - 1; y >= 0 && remainingDepth > 0; y--, remainingDepth--) {

                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = chunk.getBlockState(pos).getBlock();

                    BlockComfortEntry entry = BlockComfortRegistry.get(block);
                    if (entry != null) {
                        GroupData group = data.groups.computeIfAbsent(entry.getGroup(), k -> new GroupData());
                        group.limit = entry.getLimit();
                        group.counts.merge(entry.getValue(), 1, Integer::sum);
                    }

                    if (FireBlockRegistry.contains(block)) {
                        data.fireCount++; // count each fire block once
                    }

                    if (block == Blocks.BEDROCK) break;
                }
            }
        }

        data.comfortScore = data.groups.values().stream()
                .mapToInt(g -> g.counts.entrySet().stream()
                        .mapToInt(e -> e.getKey() * e.getValue())
                        .sum())
                .sum();

        data.initialized = true;
    }
}