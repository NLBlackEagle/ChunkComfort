package chunkcomfort.handlers;

import chunkcomfort.ChunkComfort;
import chunkcomfort.potion.PotionComfort;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChunkComfort.MODID)
public class ModRegistry {

    public static void init() {

        ForgeConfigHandler.initialize();

        PlayerComfortHandler.register();
        PotionComfort.register();

        PotionComfort.loadConfig();
    }
}