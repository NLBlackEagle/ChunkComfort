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

        @Config.Comment({
                "Set this to true if you do not want wine from the mod rustic to have a effect on hidden potions",
                "and the comfort"
        })
        @Config.Name("Rustic Wine Comfort Blacklist")
        public boolean stopRusticWine = true;

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
                "Format: <block>,<value>,<group>,<block_limit>",
                "Example: minecraft:crafting_table,10,workstation,10",
                "This will make it so you can add up to 10 crafting_tables each giving 10 points,",
                "any extra crafting_tables added won't give points. Also if the group limit of",
                "workstations is 5 then it only counts up to 50 points in total.",
                "Note: do not add materials/entities here that can despawn as these blocks are cached"
        })
        @Config.Name("Block Comfort Entries")
        public String[] blockComfortEntries = new String[]{
                // Furniture (cheap/low points)
                "minecraft:bookshelf,2,furniture,25",
                "minecraft:bed,8,furniture,5",

                // Workstations (functional blocks)
                "minecraft:crafting_table,2,workstation,5",
                "minecraft:furnace,1,workstation,5",
                "minecraft:cauldron,3,workstation,5",
                "minecraft:brewing_stand,10,workstation,2",

                // Enchanting (rare, late-game goal)
                "minecraft:enchanting_table,25,enchanting,1",

                // Luxury / decorative (low-per-block)
                "minecraft:jukebox,4,luxury,3",
                "minecraft:armor_stand,5,luxury,10",
                "minecraft:painting,1,luxury,25",
                "minecraft:item_frame,1,luxury,15",
                "minecraft:carpet,1,luxury,25",
                "minecraft:banner,3,luxury,10",
                "minecraft:flower_pot,1,luxury,10",

                // Lightsources (small contribution)
                "minecraft:torch,1,lightsources,20"

        };

        @Config.Comment({
                "Maximum allowed points per comfort group",
                "Format: <group>,<limit>",
                "Example: furniture,10"
        })
        @Config.Name("Group Comfort Limits")
        public String[] groupLimits = new String[]{
                "10,[[minecraft:haste,0]]",                               // Early: small boost
                "30,[[minecraft:speed,0]]",                               // Early mid-game
                "60,[[minecraft:regeneration,0]]",                        // Mid-game minor regen
                "100,[[minecraft:speed,1],[minecraft:regeneration,1]]",  // Mid-late combo
                "150,[[minecraft:strength,0],[minecraft:speed,1]]",      // Late-game perk
                "200,[[minecraft:resistance,1],[minecraft:regeneration,1]]", // Strong late-game
                "250,[[minecraft:strength,1],[minecraft:resistance,1],[minecraft:regeneration,1]]", // Epic base
                "300,[[minecraft:strength,1],[minecraft:resistance,1],[minecraft:regeneration,2]]", // Near max
                "350,[[minecraft:strength,2],[minecraft:resistance,2],[minecraft:regeneration,2]]"  // Max comfort
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
                "10,[[minecraft:haste,0]]",
                "30,[[minecraft:speed,0]]",
                "60,[[minecraft:regeneration,0]]",
                "100,[[minecraft:speed,1],[minecraft:regeneration,1]]",
                "150,[[minecraft:strength,0],[minecraft:speed,1]]",
                "200,[[minecraft:resistance,1],[minecraft:regeneration,1]]",
                "250,[[minecraft:strength,1],[minecraft:resistance,1],[minecraft:regeneration,1]]"
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