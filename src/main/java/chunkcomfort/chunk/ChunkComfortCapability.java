package chunkcomfort.chunk;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

public class ChunkComfortCapability {

    @CapabilityInject(ChunkComfortData.class)
    public static Capability<ChunkComfortData> CHUNK_COMFORT_CAP = null;

}