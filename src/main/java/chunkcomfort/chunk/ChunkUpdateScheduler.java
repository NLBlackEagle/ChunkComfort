package chunkcomfort.chunk;

import chunkcomfort.comfort.ChunkComfortScanner;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayDeque;
import java.util.Queue;

public class ChunkUpdateScheduler {

    private static final Queue<Chunk> scanQueue = new ArrayDeque<>();

    private static final int MAX_CHUNKS_PER_TICK = 5;

    public static void queueChunkForScan(Chunk chunk) {

        ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);

        if (data != null && !data.initialized) {
            ChunkComfortScanner.scanChunk(chunk);
        }
    }

    public static void processQueue() {

        int processed = 0;

        while (processed < MAX_CHUNKS_PER_TICK && !scanQueue.isEmpty()) {

            Chunk chunk = scanQueue.poll();

            if (chunk == null) continue;

            ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);

            if (data != null && !data.initialized) {
                ChunkComfortScanner.scanChunk(chunk);
                data.initialized = true;
            }

            processed++;
        }
    }
}