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

        // REASON: two-block structures (hammocks, sleeping bags, beds, and metadata-based
        // multi-block blocks like waystones) have unreliable incremental updates because
        // both halves fire PlaceEvent nearly simultaneously, causing a race where both
        // see blockCounts=0 and both pass the limit check. For metadata-based structures,
        // we detect both the registered and unregistered halves as multi-block (see
        // isMultiBlockStructure) and force a full rescan for either half, ensuring the
        // scan path — which correctly enforces the per-entry limit — handles the count.
        if (isMultiBlockStructure(state) || hasRegisteredSiblingMeta(state)) {
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

        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block, state);
        if (entry != null) {
            // REASON: respect the per-entry limit so e.g. a waystone with limit=1
            // stops contributing comfort once one is already tracked in this chunk.
            int currentCount = data.blockCounts.getOrDefault(block, 0);
            if (currentCount < entry.limit) {
                data.addComfort(entry.group, entry.value);
                data.blockCounts.put(block, currentCount + 1);
            }
        }

        worldData.setChunkData(chunkPos, data);
    }

    /** Called when a block is broken */
    public static void onBlockBroken(World world, BlockPos pos, Block block) {

        IBlockState state = world.getBlockState(pos);

        // Same rescan approach for breaking — BreakEvent fires for both halves,
        // invalidating once is enough for calculatePlayerComfort to rescan correctly.
        if (isMultiBlockStructure(state) || hasRegisteredSiblingMeta(state)) {
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

    // Returns true if the current blockstate's metadata IS registered but the block
    // also has other registered metadata entries — meaning it's one half of a
    // metadata-based multi-block structure and should go through the rescan path.
    private static boolean hasRegisteredSiblingMeta(IBlockState state) {
        net.minecraft.block.Block block = state.getBlock();
        int currentMeta = block.getMetaFromState(state);
        boolean currentRegistered = chunkcomfort.registry.BlockComfortRegistry
                .getBlockEntry(block, state) != null;
        if (!currentRegistered) return false; // unregistered halves are caught by isMultiBlockStructure
        for (chunkcomfort.registry.BlockComfortRegistry.BlockKey key
                : chunkcomfort.registry.BlockComfortRegistry.BLOCK_ENTRIES.keySet()) {
            if (key.block == block && key.meta != currentMeta && key.meta != -1) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMultiBlockStructure(IBlockState state) {
        if (state.getBlock() instanceof net.minecraft.block.BlockBed) return true;

        // Check for a "part" property (beds, doors, tall grass, etc.)
        try {
            for (net.minecraft.block.properties.IProperty<?> prop : state.getPropertyKeys()) {
                if (prop.getName().equalsIgnoreCase("part")) return true;
            }
        } catch (Exception ignored) {}

        // REASON: metadata-based multi-block structures (e.g. waystones) don't have a
        // "part" property — they use different metadata values for each half. If the
        // current blockstate's metadata does not match any registered entry but the same
        // block IS registered under a different metadata, this is an unregistered secondary
        // half. Treat it as a multi-block so the chunk is invalidated and rescanned rather
        // than doing incremental updates that race against each other.
        net.minecraft.block.Block block = state.getBlock();
        int currentMeta = block.getMetaFromState(state);
        boolean currentMetaRegistered = chunkcomfort.registry.BlockComfortRegistry
                .getBlockEntry(block, state) != null;
        if (!currentMetaRegistered) {
            // Check if any other metadata of this block is registered
            for (chunkcomfort.registry.BlockComfortRegistry.BlockKey key
                    : chunkcomfort.registry.BlockComfortRegistry.BLOCK_ENTRIES.keySet()) {
                if (key.block == block && key.meta != currentMeta && key.meta != -1) {
                    return true; // sibling metadata is registered — this is the other half
                }
            }
        }

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
