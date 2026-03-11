package chunkcomfort.chunk;

import chunkcomfort.comfort.ChunkComfortScanner;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.chunk.Chunk;

import java.util.HashMap;
import java.util.Map;

public class ChunkInitializationManager {

    private static final Map<EntityPlayer, PlayerChunkData> lastPlayerChunk = new HashMap<>();

    private static final int TICK_INTERVAL = 20;

    public static void initChunkIfNeeded(Chunk chunk) {

        ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);

        if (data == null || data.initialized) return;

        ChunkUpdateScheduler.queueChunkForScan(chunk);
    }

    private static class PlayerChunkData {

        int x;
        int z;

        public PlayerChunkData(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
}