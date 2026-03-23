package chunkcomfort.chunk;

import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.EntityComfortRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class ChunkUpdateManager {

    /** Called when a block is placed */
    public static void onBlockPlaced(World world, BlockPos pos, Block block) {
        ChunkComfortData data = ComfortWorldData.get(world).getChunkData(posToChunkPos(pos));

        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block);
        if (entry != null) {
            // Update group totals
            data.addComfort(entry.group, entry.value);

            // **New:** Update block counts
            data.blockCounts.put(block, data.blockCounts.getOrDefault(block, 0) + 1);
        }

        ComfortWorldData.get(world).setChunkData(posToChunkPos(pos), data);
    }

    /** Called when a block is broken */
    public static void onBlockBroken(World world, BlockPos pos, Block block) {
        ChunkComfortData data = ComfortWorldData.get(world).getChunkData(posToChunkPos(pos));

        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block);
        if (entry != null) {
            // Update group totals
            data.removeComfort(entry.group, entry.value);

            // **New:** Update block counts
            int current = data.blockCounts.getOrDefault(block, 0);
            if (current <= 1) data.blockCounts.remove(block);
            else data.blockCounts.put(block, current - 1);
        }

        ComfortWorldData.get(world).setChunkData(posToChunkPos(pos), data);
    }

    /** Called when a comfort entity is added to the world */
    public static void onEntityAdded(World world, BlockPos pos, Entity entity) {
        ChunkComfortData data = ComfortWorldData.get(world).getChunkData(posToChunkPos(pos));

        EntityComfortRegistry.ComfortEntry entry = EntityComfortRegistry.getEntityEntry(entity);
        if (entry != null) {
            data.addComfort(entry.group, entry.value);

        }

        ComfortWorldData.get(world).setChunkData(posToChunkPos(pos), data);
    }

    /** Called when a comfort entity is removed from the world */
    public static void onEntityRemoved(World world, BlockPos pos, Entity entity) {
        ChunkComfortData data = ComfortWorldData.get(world).getChunkData(posToChunkPos(pos));

        EntityComfortRegistry.ComfortEntry entry = EntityComfortRegistry.getEntityEntry(entity);
        if (entry != null) {
            data.removeComfort(entry.group, entry.value);

        }

        ComfortWorldData.get(world).setChunkData(posToChunkPos(pos), data);
    }

    /** Convert world position to chunk position */
    private static ChunkPos posToChunkPos(BlockPos pos) {
        return new ChunkPos(pos);
    }
}