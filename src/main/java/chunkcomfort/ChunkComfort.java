package chunkcomfort;

import chunkcomfort.handlers.ForgeConfigHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import chunkcomfort.handlers.ModRegistry;


@Mod(modid = ChunkComfort.MODID, version = ChunkComfort.VERSION, name = ChunkComfort.NAME, dependencies = "required-after:fermiumbooter")
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

        // Run test
        chunkcomfort.handlers.RegistryTest.runTest();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        completedLoading = true;
    }

}