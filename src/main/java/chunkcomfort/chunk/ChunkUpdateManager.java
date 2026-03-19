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
            data.addComfort(entry.group, entry.value);
        }

        ComfortWorldData.get(world).setChunkData(posToChunkPos(pos), data);
    }

    /** Called when a block is broken */
    public static void onBlockBroken(World world, BlockPos pos, Block block) {
        ChunkComfortData data = ComfortWorldData.get(world).getChunkData(posToChunkPos(pos));

        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block);
        if (entry != null) {
            data.removeComfort(entry.group, entry.value);
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

        System.out.println("Removing entity: " + entity);

        EntityComfortRegistry.ComfortEntry entry = EntityComfortRegistry.getEntityEntry(entity);
        if (entry != null) {
            data.removeComfort(entry.group, entry.value);
            System.out.println("Removed entity: " + entity + ", entry=" + entry);
        }

        ComfortWorldData.get(world).setChunkData(posToChunkPos(pos), data);
    }

    /** Convert world position to chunk position */
    private static ChunkPos posToChunkPos(BlockPos pos) {
        return new ChunkPos(pos);
    }
}