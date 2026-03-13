package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import chunkcomfort.chunk.ChunkComfortData;
import chunkcomfort.chunk.ChunkComfortStorage;
import chunkcomfort.handlers.ForgeConfigHandler;
import chunkcomfort.player.PlayerComfortHandler;
import chunkcomfort.potion.PotionComfort;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChunkComfort.MODID)
public class ModRegistry {

    public static void init() {
        ForgeConfigHandler.initialize();
        chunkcomfort.debug.RegistryTest.runTest();
        CapabilityManager.INSTANCE.register(
                ChunkComfortData.class,
                new ChunkComfortStorage(),
                ChunkComfortData::new
        );
        PlayerComfortHandler.register();
        PotionComfort.register();
        PotionComfort.loadConfig();
    }
}