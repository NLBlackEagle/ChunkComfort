package chunkcomfort;

import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.debug.CommandChunkComfort;
import chunkcomfort.handlers.ChunkComfortClientTooltipHandler;
import chunkcomfort.handlers.ChunkComfortEventHandler;
import chunkcomfort.handlers.ComfortBlockParticleHandler;
import chunkcomfort.network.NetworkHandler;
import chunkcomfort.registry.PotionRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = ChunkComfort.MODID,
        version = ChunkComfort.VERSION,
        name = ChunkComfort.NAME,
        dependencies = "required-after:fermiumbooter"
)
public class ChunkComfort {

    public static final String MODID = "chunkcomfort";
    public static final String VERSION = "ChunkComfort.Mod.Version";
    public static final String NAME = "ChunkComfort";

    public static final Logger LOGGER = LogManager.getLogger();

    public static boolean completedLoading = false;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        // Initialize Forge config
        ForgeConfigHandler.initialize();
        NetworkHandler.register();
        PotionRegistry.registerPotions();

        // Register event handler for block place/break updates
        MinecraftForge.EVENT_BUS.register(new ChunkComfortEventHandler());
        MinecraftForge.EVENT_BUS.register(new ComfortBlockParticleHandler());

        // Tooltip handler / overlay
        if (event.getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new ChunkComfortClientTooltipHandler());
        }

        LOGGER.info("ChunkComfort preInit complete.");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        completedLoading = true;
        LOGGER.info("ChunkComfort postInit complete. Mod is ready.");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        // Register the debug command
        event.registerServerCommand(new CommandChunkComfort());
    }
}

/*

todo: add fireplaces tooltips
todo: add sleep mechanic insert random string: "10,You dreamt of a warm house with a comfy <fire-block-name>, a pet and a carpet on the floor"
      below comfort 10
 */