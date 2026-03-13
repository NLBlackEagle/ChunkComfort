package chunkcomfort;

import chunkcomfort.debug.CommandChunkComfort;
import chunkcomfort.handlers.ChunkEventHandler;
import chunkcomfort.registry.ModRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
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

    @Instance(MODID)
    public static ChunkComfort instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModRegistry.init();  // Initialize config, capabilities, and registries
        MinecraftForge.EVENT_BUS.register(new ChunkEventHandler());  // Register chunk-related events
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        completedLoading = true;
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandChunkComfort());
    }
}