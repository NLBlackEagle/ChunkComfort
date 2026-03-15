package chunkcomfort.chunk;

import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.FireBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class ChunkUpdateManager {

    private static final Map<String, ChunkComfortData> CHUNK_DATA = new HashMap<>();

    public static void onBlockPlaced(World world, BlockPos pos, Block block) {
        ChunkComfortData data = getOrCreateChunkData(pos);

        // Update comfort
        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getEntry(block);
        if (entry != null) {
            data.addComfort(entry.group, entry.value);
        }

        // Update fire presence
        if (FireBlockRegistry.isFireBlock(block)) {
            data.setFirePresent(true);
        }
    }

    public static void onBlockBroken(World world, BlockPos pos, Block block) {
        ChunkComfortData data = getOrCreateChunkData(pos);

        // Update comfort
        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getEntry(block);
        if (entry != null) {
            data.removeComfort(entry.group, entry.value);
        }

        // Update fire presence: we must check if any fire remains
        if (FireBlockRegistry.isFireBlock(block)) {
            data.setFirePresent(scanChunkForFire(world, pos)); // see below
        }
    }

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

    private static ChunkComfortData getOrCreateChunkData(BlockPos pos) {
        String key = chunkKey(pos);
        return CHUNK_DATA.computeIfAbsent(key, k -> new ChunkComfortData());
    }

    private static String chunkKey(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return chunkX + "," + chunkZ;
    }

    public static ChunkComfortData getChunkData(int chunkX, int chunkZ) {
        String key = chunkX + "," + chunkZ;
        return CHUNK_DATA.getOrDefault(key, new ChunkComfortData());
    }
}