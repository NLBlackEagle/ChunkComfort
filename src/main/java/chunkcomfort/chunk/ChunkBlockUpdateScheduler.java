package chunkcomfort.chunk;

import chunkcomfort.comfort.GroupScoreCalculator;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import chunkcomfort.chunk.ChunkComfortData.GroupData;
import chunkcomfort.registry.BlockComfortEntry;
import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.FireBlockRegistry;

import java.util.ArrayDeque;
import java.util.Queue;

public class ChunkBlockUpdateScheduler {

    private static final int MAX_BLOCK_UPDATES_PER_TICK = 10;
    private static final Queue<BlockUpdate> updateQueue = new ArrayDeque<>();

    /** Add a block update to the queue */
    public static void queueBlockUpdate(World world, BlockPos pos, Block oldBlock, Block newBlock) {
        updateQueue.add(new BlockUpdate(world, pos, oldBlock, newBlock));
    }

    /** Process queued block updates; call this once per server tick */
    public static void processQueue() {

        int processed = 0;

        while (processed < MAX_BLOCK_UPDATES_PER_TICK && !updateQueue.isEmpty()) {

            BlockUpdate update = updateQueue.poll();
            if (update == null) continue;

            Chunk chunk = update.world.getChunk(update.pos);
            if (chunk == null) continue;

            ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);
            if (data == null) continue;

            if (!data.initialized) {
                data.initialized = true;
            }

            int oldGroupDelta = 0;
            int newGroupDelta = 0;

            // OLD BLOCK
            if (update.oldBlock != null) {

                BlockComfortEntry oldEntry = BlockComfortRegistry.get(update.oldBlock);

                if (oldEntry != null) {

                    GroupData group = data.groups.get(oldEntry.getGroup());

                    if (group != null) {

                        int count = group.counts.getOrDefault(oldEntry.getValue(), 0);

                        if (count > 0) {
                            group.counts.put(oldEntry.getValue(), count - 1);
                            group.currentScore = Math.max(0, group.currentScore - oldEntry.getValue());
                        }

                        oldGroupDelta = oldEntry.getValue();
                    }
                }

                if (FireBlockRegistry.contains(update.oldBlock)) {
                    data.fireCount = Math.max(0, data.fireCount - 1);
                }
            }

            // NEW BLOCK
            if (update.newBlock != null) {

                BlockComfortEntry newEntry = BlockComfortRegistry.get(update.newBlock);

                if (newEntry != null) {

                    GroupData group = data.groups.computeIfAbsent(newEntry.getGroup(), k -> new GroupData());

                    group.limit = newEntry.getLimit();
                    group.counts.merge(newEntry.getValue(), 1, Integer::sum);
                    group.currentScore += newEntry.getValue();

                    newGroupDelta = newEntry.getValue();
                }

                if (FireBlockRegistry.contains(update.newBlock)) {
                    data.fireCount++;
                }
            }

            for (ChunkComfortData.GroupData gd : data.groups.values()) {
                GroupScoreCalculator.calculate(gd);
            }

            data.comfortScore =
                    data.groups.values().stream()
                            .mapToInt(gd -> gd.currentScore)
                            .sum();

            processed++;
        }
    }

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
}