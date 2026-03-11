package chunkcomfort.handlers;

import chunkcomfort.chunk.ChunkComfortData;
import chunkcomfort.chunk.ChunkComfortProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChunkCapabilityHandler {

    @SubscribeEvent
    public void attachChunkCapabilities(AttachCapabilitiesEvent<Chunk> event) {

        ChunkComfortData data = new ChunkComfortData();
        data.initialized = true;

        event.addCapability(
                new ResourceLocation("chunkcomfort", "chunk_comfort"),
                new ChunkComfortProvider(data)
        );
    }
}