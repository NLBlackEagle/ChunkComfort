package chunkcomfort.chunk;

import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.EntityComfortRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class ChunkUpdateManager {

    /** Called when a block is placed */
    public static void onBlockPlaced(World world, BlockPos pos, Block block) {

        IBlockState state = world.getBlockState(pos);

        // REASON: two-block structures (hammocks, sleeping bags, beds) have unreliable
        // incremental updates because:
        // 1. PlaceEvent fires before the second half exists in the world, so
        //    findPrimaryPos may not find it yet.
        // 2. The event may fire for the non-primary half, which isPrimaryBlock skips.
        // For the first placement this doesn't matter because the chunk is uninitialized
        // and calculatePlayerComfort triggers a full rescan. But for subsequent placements
        // the chunk is already initialized and the incremental update is the only path.
        // Solution: mark the chunk uninitialized so calculatePlayerComfort rescans it,
        // same as what /chunkcomfort reload does. This is reliable regardless of which
        // half fired the event or whether the second half exists yet.
        if (isMultiBlockStructure(state)) {
            ComfortWorldData worldData = ComfortWorldData.get(world);
            ChunkPos chunkPos = new ChunkPos(pos);
            worldData.getChunkData(chunkPos).initialized = false;
            worldData.markDirty();
            return;
        }

        if (!ComfortWorldData.isPrimaryBlock(state)) return;

        ComfortWorldData worldData = ComfortWorldData.get(world);
        ChunkPos chunkPos = new ChunkPos(pos);
        ChunkComfortData data = worldData.getChunkData(chunkPos);

        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block);
        if (entry != null) {
            data.addComfort(entry.group, entry.value);
            data.blockCounts.put(block, data.blockCounts.getOrDefault(block, 0) + 1);
        }

        worldData.setChunkData(chunkPos, data);
    }

    /** Called when a block is broken */
    public static void onBlockBroken(World world, BlockPos pos, Block block) {

        IBlockState state = world.getBlockState(pos);

        // Same rescan approach for breaking — BreakEvent fires for both halves,
        // invalidating once is enough for calculatePlayerComfort to rescan correctly.
        if (isMultiBlockStructure(state)) {
            ComfortWorldData worldData = ComfortWorldData.get(world);
            ChunkPos chunkPos = new ChunkPos(pos);
            worldData.getChunkData(chunkPos).initialized = false;
            worldData.markDirty();
            return;
        }

        if (!ComfortWorldData.isPrimaryBlock(state)) return;

        ComfortWorldData worldData = ComfortWorldData.get(world);
        ChunkPos chunkPos = new ChunkPos(pos);
        ChunkComfortData data = worldData.getChunkData(chunkPos);

        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block);
        if (entry != null) {
            data.removeComfort(entry.group, entry.value);

            int current = data.blockCounts.getOrDefault(block, 0);
            if (current <= 1) data.blockCounts.remove(block);
            else data.blockCounts.put(block, current - 1);
        }

        worldData.setChunkData(chunkPos, data);
    }

    public static boolean isMultiBlockStructurePublic(net.minecraft.block.state.IBlockState state) {
        return isMultiBlockStructure(state);
    }

    /**
     * Directly invalidates a chunk so calculatePlayerComfort triggers a full rescan.
     * Used by MultiPlaceEvent where event.getPos() is the click position rather than
     * the placed block position, making isMultiBlockStructure unreliable.
     */
    public static void invalidateChunk(World world, BlockPos pos) {
        ComfortWorldData worldData = ComfortWorldData.get(world);
        ChunkPos chunkPos = new ChunkPos(pos);
        worldData.getChunkData(chunkPos).initialized = false;
        worldData.markDirty();
    }

    private static boolean isMultiBlockStructure(IBlockState state) {
        if (state.getBlock() instanceof net.minecraft.block.BlockBed) return true;
        try {
            for (net.minecraft.block.properties.IProperty<?> prop : state.getPropertyKeys()) {
                if (prop.getName().equalsIgnoreCase("part")) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /** Called when a comfort entity is added to the world */
    public static void onEntityAdded(World world, BlockPos pos, Entity entity) {
        ComfortWorldData worldData = ComfortWorldData.get(world);
        ChunkPos chunkPos = new ChunkPos(pos);
        ChunkComfortData data = worldData.getChunkData(chunkPos);

        EntityComfortRegistry.ComfortEntry entry = EntityComfortRegistry.getEntityEntry(entity);
        if (entry != null) {
            data.addComfort(entry.group, entry.value);
        }

        worldData.setChunkData(chunkPos, data);
    }

    /** Called when a comfort entity is removed from the world */
    public static void onEntityRemoved(World world, BlockPos pos, Entity entity) {
        ComfortWorldData worldData = ComfortWorldData.get(world);
        ChunkPos chunkPos = new ChunkPos(pos);
        ChunkComfortData data = worldData.getChunkData(chunkPos);

        EntityComfortRegistry.ComfortEntry entry = EntityComfortRegistry.getEntityEntry(entity);
        if (entry != null) {
            data.removeComfort(entry.group, entry.value);
        }

        worldData.setChunkData(chunkPos, data);
    }
}
