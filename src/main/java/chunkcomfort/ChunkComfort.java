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

        // Initialize Forge config
        ForgeConfigHandler.initialize();

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
todo: run a system wide diagnostics and see where I can patch things up
todo: drinking/eating things increase comfort temporarily (DARN SCOPE CREEP!)
todo: fix IF skulls not working, should be entities & comforts beds;
    so we have changed those files and the limits shows correctly but the count is not accurate displayed in the tooltip. it currently says:
    Entity points: 10 Limit: 0/1 - 10 is correct but 0/1 should be 1/1 as one is placed.
    Group: Collectibles Points: 10/1 - 10 is correct however 1 is not, 1 should be the group limit for collectibles which is 50.

todo: I want to expand the system with NBT matching so that the following works:
        iceandfire:dragonskull=iceandfire:dragon_skull
        iceandfire:if_mob_skull=iceandfire:amphithere_skull,SkullType:5
        iceandfire:if_mob_skull=iceandfire:hippogryph_skull,SkullType:0
        iceandfire:if_mob_skull=iceandfire:hydra_skull,SkullType:7
        iceandfire:if_mob_skull=iceandfire:troll_skull,SkullType:4
        iceandfire:if_mob_skull=iceandfire:cyclops_skull,SkullType:1
        iceandfire:if_mob_skull=iceandfire:seaserpent_skull,SkullType:6
        iceandfire:if_mob_skull=iceandfire:cockatrice_skull,SkullType:2
        iceandfire:if_mob_skull=iceandfire:stymphalian_skull,SkullType:3
        And thus properly maps to:
        iceandfire:dragonskull,10,collectibles,1,{}
        iceandfire:if_mob_skull,1,collectibles,1,{SkullType:5} #amphithere
        iceandfire:if_mob_skull,1,collectibles,1,{SkullType:0} #hippogryph
        iceandfire:if_mob_skull,3,collectibles,1,{SkullType:7} #hydra
        iceandfire:if_mob_skull,3,collectibles,1,{SkullType:4} #troll
        iceandfire:if_mob_skull,3,collectibles,1,{SkullType:1} #cyclops
        iceandfire:if_mob_skull,5,collectibles,1,{SkullType:6} #seaserpent
        iceandfire:if_mob_skull,1,collectibles,1,{SkullType:2} #cockatrice
        iceandfire:if_mob_skull,1,collectibles,1,{SkullType:3} #stymphalian
        Unless you have better proposals for the included config:


todo: test these mods:
waystones
variedcommodities
quark
nuclearcraft
inspirations
iceandfire (skulls not working)
fish undead rising
cookingforblockheads (old one)
comforts (Beds not working)
biomesoplenty
betternether
 */