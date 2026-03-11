package chunkcomfort;

import chunkcomfort.chunk.ChunkComfortData;
import chunkcomfort.chunk.ChunkComfortStorage;
import chunkcomfort.handlers.ChunkBlockUpdateHandler;
import chunkcomfort.handlers.ChunkCapabilityHandler;
import chunkcomfort.handlers.ForgeConfigHandler;
import chunkcomfort.handlers.ModRegistry;
import chunkcomfort.debug.CommandChunkComfort;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;

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

        ModRegistry.init();
        ForgeConfigHandler.initialize();

        chunkcomfort.handlers.RegistryTest.runTest();

        CapabilityManager.INSTANCE.register(
                ChunkComfortData.class,
                new ChunkComfortStorage(),
                ChunkComfortData::new
        );

        MinecraftForge.EVENT_BUS.register(new ChunkCapabilityHandler());
        MinecraftForge.EVENT_BUS.register(new ChunkBlockUpdateHandler());
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