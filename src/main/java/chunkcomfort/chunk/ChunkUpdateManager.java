package chunkcomfort.chunk;

import chunkcomfort.registry.BlockComfortRegistry;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class ChunkUpdateManager {

    // Store comfort data per chunk (X,Z)
    private static final Map<String, ChunkComfortData> CHUNK_DATA = new HashMap<String, ChunkComfortData>();

    /**
     * Called when a block is placed.
     */
    public static void onBlockPlaced(World world, BlockPos pos, Block block) {
        ChunkComfortData data = getOrCreateChunkData(pos);
        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getEntry(block);
        if (entry != null) {
            data.addComfort(entry.group, entry.value);
        }
    }

    /**
     * Called when a block is broken/removed.
     */
    public static void onBlockBroken(World world, BlockPos pos, Block block) {
        ChunkComfortData data = getOrCreateChunkData(pos);
        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getEntry(block);
        if (entry != null) {
            data.removeComfort(entry.group, entry.value);
        }
    }

    /**
     * Get the data for a chunk, creating it if missing.
     */
    private static ChunkComfortData getOrCreateChunkData(BlockPos pos) {
        String key = chunkKey(pos);
        if (CHUNK_DATA.containsKey(key)) {
            return CHUNK_DATA.get(key);
        } else {
            ChunkComfortData data = new ChunkComfortData();
            CHUNK_DATA.put(key, data);
            return data;
        }
    }

    /**
     * Generate a unique key for chunk coordinates.
     */
    private static String chunkKey(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return chunkX + "," + chunkZ;
    }

    /**
     * Get existing chunk data (or empty if missing)
     */
    public static ChunkComfortData getChunkData(int chunkX, int chunkZ) {
        String key = chunkX + "," + chunkZ;
        if (CHUNK_DATA.containsKey(key)) {
            return CHUNK_DATA.get(key);
        } else {
            return new ChunkComfortData();
        }
    }
}