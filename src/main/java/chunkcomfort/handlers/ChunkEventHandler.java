package chunkcomfort.handlers;

import chunkcomfort.chunk.*;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

public class ChunkEventHandler {

    @SubscribeEvent
    public void attachChunkCapabilities(AttachCapabilitiesEvent<Chunk> event) {
        ChunkComfortData data = new ChunkComfortData();
        event.addCapability(
                new ResourceLocation("chunkcomfort", "chunk_comfort"),
                new ChunkComfortProvider(data)
        );
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ChunkUpdateManager.processQueues();
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {

        Chunk chunk = event.getWorld().getChunk(event.getPos());

        ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);

        // If chunk never scanned, do it now
        if (!data.initialized) {
            ChunkComfortScanner.scanChunk(chunk);
        }

        ChunkUpdateManager.queueBlockUpdate(
                event.getWorld(),
                event.getPos(),
                null,
                event.getPlacedBlock().getBlock()
        );
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {

        ChunkUpdateManager.queueBlockUpdate(
                event.getWorld(),
                event.getPos(),
                event.getState().getBlock(),
                null
        );
    }
}