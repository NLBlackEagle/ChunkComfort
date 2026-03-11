package chunkcomfort.handlers;

import chunkcomfort.chunk.ChunkBlockUpdateScheduler;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

public class ChunkBlockUpdateHandler {

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        ChunkBlockUpdateScheduler.processQueue();
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        ChunkBlockUpdateScheduler.queueBlockUpdate(
                event.getWorld(),
                event.getPos(),
                null,
                event.getPlacedBlock().getBlock()
        );
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        ChunkBlockUpdateScheduler.queueBlockUpdate(
                event.getWorld(),
                event.getPos(),
                event.getState().getBlock(),
                null
        );
    }
}