package chunkcomfort.config;


import chunkcomfort.ChunkComfort;
import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.player.PlayerComfortManager;
import chunkcomfort.registry.BiomeComfortRegistry;
import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.FireBlockRegistry;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = ChunkComfort.MODID)
public class ForgeConfigHandler {

    @Config.Name("Server Options")
    public static final ServerConfig server = new ServerConfig();

    public static class ServerConfig {

        @Config.Comment("Require shelter from sky for comfort (true/false).")
        @Config.Name("Shelter Requirement (No Skylight)")
        public boolean requireShelter = true;

        @Config.Comment("Require fire nearby for comfort (true/false).")
        @Config.Name("Fire Requirement")
        public boolean requireFire = true;

        @Config.Comment("Minimum light level required for comfort (0-15).")
        @Config.RangeInt(min = 0, max = 15)
        @Config.Name("Minimum Light Level")
        public int minLightLevel = 7;

        @Config.Comment({
                "Vertical range (in blocks) to scan for fire around the player",
                "The scan will check this value relative to the player's Y position",
                "Default 24 (48 blocks total)"
        })
        @Config.RangeInt(min = 1, max = 128)
        @Config.Name("Fire Scan Vertical Range")
        public int fireScanVerticalRange = 24;

        @Config.Comment({
                "Chunk radius checked for comfort, recommended to leave this default.",
                "1 = 3x3 chumks (9 chunks being scanned each comfort check interval)",
                "2 = 5x5 chumks (25 chunks being scanned each comfort check interval)",
                "3 = 7x7 chumks (49 chunks being scanned each comfort check interval)",
                "This becomes very laggy very fast!"
        })
        @Config.RangeInt(min = 0, max = 3)
        public int chunkRadius = 1;

        @Config.Comment({
                "Player comfort check interval (ticks)",
                "20 = 1 second"
        })
        @Config.RangeInt(min = 20, max = 2000)
        public int comfortCheckInterval = 20;

        @Config.Comment({
                "Block comfort entries",
                "Format: <block>,<value>,<group>",
                "Example: minecraft:crafting_table,10,workstation",
                "Note: do not add materials/entities here that can despawn as these blocks are cached"
        })
        @Config.Name("Block Comfort Entries")
        public String[] blockComfortEntries = new String[]{
                "minecraft:bookshelf,1,furniture",
                "minecraft:bed,5,furniture",
                "minecraft:crafting_table,10,workstation",
                "minecraft:armor_stand,3,luxury",
                "minecraft:painting,2,luxury",
                "minecraft:item_frame,2,luxury",
                "minecraft:banner,2,luxury",
                "minecraft:flower_pot,1,luxury",
                "minecraft:torch,1,lightsources",
                "minecraft:lantern,1,lightsources",
                "minecraft:furnace,1,lightsources",

        };

        @Config.Comment({
                "Maximum allowed points per comfort group",
                "Format: <group>,<limit>",
                "Example: furniture,10"
        })
        @Config.Name("Group Comfort Limits")
        public String[] groupLimits = new String[]{
                "furniture,10",
                "workstation,20",
                "luxury,3"
        };


        @Config.Comment("Blocks that count as fire sources")
        @Config.Name("Fire Blocks")
        public String[] fireBlocks = new String[]{
                "minecraft:fire"
        };

        @Config.Comment({
                "Biome comfort modifiers",
                "Format: <biome>,<modifier>",
                "Example: minecraft:plains,5  or  minecraft:desert,-3"
        })
        @Config.Name("Biome Comfort Modifiers")
        public String[] biomeComfortModifiers = new String[]{};

        @Config.Comment({
                "Comfort tier effects",
                "Format: <comfort>,[[<potion>,<amplifier>],[<potion>,<amplifier>]]",
                "Example: 10,[[minecraft:speed,0],[minecraft:regeneration,0]]"
        })
        public String[] comfortEffects = new String[]{
                "10,[[minecraft:speed,0]]",
                "20,[[minecraft:speed,1]]",
                "30,[[minecraft:regeneration,0],[minecraft:speed,0]]"
        };
    }

    public static void reloadRegistries() {
        BlockComfortRegistry.reload(server.blockComfortEntries);
        FireBlockRegistry.reload(server.fireBlocks);
        PlayerComfortManager.reloadConfig();
        BiomeComfortRegistry.reload(server.biomeComfortModifiers);
        AreaComfortCalculator.reloadGroupLimits(server.groupLimits); // new hook
    }

    @Mod.EventBusSubscriber(modid = ChunkComfort.MODID)
    public static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(ChunkComfort.MODID)) {
                ConfigManager.sync(ChunkComfort.MODID, Config.Type.INSTANCE);
                reloadRegistries();
            }
        }
    }

    public static void initialize() {
        reloadRegistries();
    }
}