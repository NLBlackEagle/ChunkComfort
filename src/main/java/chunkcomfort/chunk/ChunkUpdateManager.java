package chunkcomfort.chunk;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import chunkcomfort.chunk.ChunkComfortData.GroupData;
import chunkcomfort.registry.BlockComfortEntry;
import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.FireBlockRegistry;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class ChunkUpdateManager {

    private static final int MAX_BLOCK_UPDATES_PER_TICK = 10;
    private static final int MAX_CHUNKS_PER_TICK = 5;

    private static final Queue<BlockUpdate> blockUpdateQueue = new ArrayDeque<>();
    private static final Queue<Chunk> chunkScanQueue = new ArrayDeque<>();

    private static final Map<EntityPlayer, PlayerChunkData> lastPlayerChunk = new HashMap<>();


    /* ---------------- INITIALIZATION ---------------- */

    public static void initChunkIfNeeded(Chunk chunk) {

        ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);

        if (data == null || data.initialized) return;

        queueChunkForScan(chunk);
    }


    /* ---------------- BLOCK UPDATE QUEUE ---------------- */

    public static void queueBlockUpdate(World world, BlockPos pos, Block oldBlock, Block newBlock) {
        blockUpdateQueue.add(new BlockUpdate(world, pos, oldBlock, newBlock));
    }


    /* ---------------- CHUNK SCAN QUEUE ---------------- */

    public static void queueChunkForScan(Chunk chunk) {

        ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);

        if (data != null && !data.initialized) {
            chunkScanQueue.add(chunk);
        }
    }


    /* ---------------- PROCESS TICK ---------------- */

    public static void processQueues() {

        processChunkScans();
        processBlockUpdates();
    }


    /* ---------------- PROCESS CHUNK SCANS ---------------- */

    private static void processChunkScans() {

        int processed = 0;

        while (processed < MAX_CHUNKS_PER_TICK && !chunkScanQueue.isEmpty()) {

            Chunk chunk = chunkScanQueue.poll();
            if (chunk == null) continue;

            ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);

            if (data != null && !data.initialized) {
                ChunkComfortScanner.scanChunk(chunk);
                data.initialized = true;
            }

            processed++;
        }
    }


    /* ---------------- PROCESS BLOCK UPDATES ---------------- */

    private static void processBlockUpdates() {

        int processed = 0;

        while (processed < MAX_BLOCK_UPDATES_PER_TICK && !blockUpdateQueue.isEmpty()) {

            BlockUpdate update = blockUpdateQueue.poll();
            if (update == null) continue;

            Chunk chunk = update.world.getChunk(update.pos);
            if (chunk == null) continue;

            ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);
            if (data == null) continue;

            if (!data.initialized) {
                data.initialized = true;
            }

            /* OLD BLOCK */

            if (update.oldBlock != null) {

                BlockComfortEntry oldEntry = BlockComfortRegistry.get(update.oldBlock);

                if (oldEntry != null) {

                    GroupData group = data.groups.get(oldEntry.getGroup());

                    if (group != null) {

                        int count = group.counts.getOrDefault(oldEntry.getValue(), 0);

                        if (count > 0) {
                            group.counts.put(oldEntry.getValue(), count - 1);
                        }
                    }
                }

                if (FireBlockRegistry.contains(update.oldBlock)) {
                    data.fireCount = Math.max(0, data.fireCount - 1);
                }
            }

            /* NEW BLOCK */

            if (update.newBlock != null) {

                BlockComfortEntry newEntry = BlockComfortRegistry.get(update.newBlock);

                if (newEntry != null) {

                    GroupData group = data.groups.computeIfAbsent(newEntry.getGroup(), k -> new GroupData());

                    group.limit = newEntry.getLimit();
                    group.counts.merge(newEntry.getValue(), 1, Integer::sum);
                }

                if (FireBlockRegistry.contains(update.newBlock)) {
                    data.fireCount++;
                }
            }

            /* RECALCULATE SCORES */

            for (GroupData gd : data.groups.values()) {
                GroupScoreCalculator.calculate(gd);
            }

            data.comfortScore =
                    data.groups.values()
                            .stream()
                            .mapToInt(gd -> gd.currentScore)
                            .sum();

            processed++;
        }
    }


    /* ---------------- INTERNAL DATA ---------------- */

    private static class BlockUpdate {

        final World world;
        final BlockPos pos;
        final Block oldBlock;
        final Block newBlock;

        BlockUpdate(World world, BlockPos pos, Block oldBlock, Block newBlock) {
            this.world = world;
            this.pos = pos;
            this.oldBlock = oldBlock;
            this.newBlock = newBlock;
        }
    }

    private static class PlayerChunkData {

        int x;
        int z;

        PlayerChunkData(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
}