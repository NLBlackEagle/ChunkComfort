package chunkcomfort.config;


import chunkcomfort.ChunkComfort;
import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.handlers.ChunkComfortClientTooltipHandler;
import chunkcomfort.player.PlayerComfortManager;
import chunkcomfort.registry.*;
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

        //Global Settings

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

        @Config.Comment("Enable temperature as a comfort requirement (true/false).")
        @Config.Name("Enable SimpleDifficulty Temperature Comfort Requirement")
        public boolean enableTemperatureComfort = true;

        @Config.Comment({
                "Minimum player temperature (inclusive) required for comfort activation.",
                "Negative values = colder than default, positive = warmer than default",
                "Defaults are set to Hypothermia -1.0 and Hyperthermia 1.0"
        })
        @Config.Name("Minimum Comfort Temperature")
        public double minComfortTemperature = -1.0;

        @Config.Comment({
                "Maximum player temperature (inclusive) allowed for comfort activation."
        })
        @Config.Name("Maximum Comfort Temperature")
        public double maxComfortTemperature = 1.0;

        //Comfort Entries

        @Config.Comment("Blocks that count as fire sources")
        @Config.Name("Fire Blocks")
        public String[] fireBlocks = new String[]{
                "minecraft:fire"
        };

        @Config.Comment("Items that count as fire starters")
        @Config.Name("Fire Source Items")
        public String[] fireSourceItems = new String[] {
                "minecraft:flint_and_steel",
                "minecraft:fire_charge"
        };

        @Config.Comment({
                "Biome comfort modifiers",
                "Format: <biome>,<modifier>",
                "Example: minecraft:plains,5  or  minecraft:desert,-3"
        })
        @Config.Name("Biome Comfort Modifiers")
        public String[] biomeComfortModifiers = new String[]{};


        @Config.Comment({
                "Block Comfort Entries",
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
                "minecraft:skull,1,luxury,10",

                //Legendary
                "minecraft:dragon_egg,25,legendary,1",

                // Lightsources (small contribution)
                "minecraft:torch,1,lightsources,20"

        };

        @Config.Comment({
                "Block ID aliases",
                "Allows multiple block IDs to be treated as one logical block in configs.",
                "Example: minecraft:banner=minecraft:standing_banner,minecraft:wall_banner",
                "Using 'minecraft:banner' will match both banner variants."
        })
        @Config.Name("Block ID aliases")
        public String[] blockAliases = new String[] {
                "minecraft:banner=minecraft:standing_banner,minecraft:wall_banner"
        };

        @Config.Comment({
                "Have living entities wander around your base to boost comfort!",
                "Format: <entity>,<value>,<group>,<entity_limit>,<optional_nbt>",
                "Example: minecraft:ocelot,2,pets,5,{CatType:*,OwnerUUID:*}",
                "Example: minecraft:parrot,2,birds,5",
                "Rules: * is wildcard, {},{} equals {} or {}, a singular {} = no nbt check",
                "{!CatType:1} means it can't have CatType1, {OwnerUUID:'*-*'} will check for - in the OwnerUUID",
                "{OwnerUUID:'*-*'} basically means tamed for practically all entities including modded ones.",
                "Disclaimer: Do not add multiple entities of the same type!"
        })
        @Config.Name("Living Comfort Entries")
        public String[] livingComfortEntries = new String[]{
                "minecraft:ocelot,4,pets,2,{!CatType:0,OwnerUUID:'*-*'}",
                "minecraft:parrot,1,pets,3,{OwnerUUID:'*-*'}",
                "minecraft:wolf,4,pets,2,{OwnerUUID:'*-*'}"
        };

        @Config.Comment({
                "Give your pets names for extra comfort!",
                "Format: <entity>,<name|name2|etc>,<points>"
        })
        @Config.Name("Additional Comfort Names")
        public String[] livingTooltipEntries = new String[]{
                "minecraft:ocelot,'Dilly|Skully|Gizmo',1",
                "minecraft:parrot,'Eagle|BaldEagle|Heaven',1",
                "minecraft:wolf,'Stitch|Lyn',1",
        };

        @Config.Comment({
                "Pet a entity to gain a temporary comfort boost.",
                "Format: <entity>,<comfort_boost>,<entities_pettable>,<boost_seconds>,<boost_cooldown>,<tamed>,<owner_only>,<requires_comfort_activation>",
                "Example: minecraft:ocelot,2,3,pets,60,360,true,true,true",
                "This would boost the owner comfort by 2 points for 1 minute if all minimal comfort requirements are met",
                "such as fire, shelter, etc. The ocelot must be tamed by said owner which can pet the ocelot every 300 seconds.",
                "In this case 3 different ocelots can be petted for a maximum boost of 6 comfort points",
                "It is recommended to keep <entities_pettable> the same as <entity_limit> from Living Comfort Entries"
        })
        @Config.Name("Comfort Petting Entries")
        public String[] pettingComfortEntries = new String[]{
                "minecraft:ocelot,3,2,300,600,true,true,true",
                "minecraft:wolf,3,2,300,600,true,true,true",
                "minecraft:parrot,1,3,300,1200,true,true,true"

        };

        @Config.Comment({
                "Maximum allowed points per comfort group",
                "Format: <group>,<limit>",
                "Example: furniture,10"
        })
        @Config.Name("Group Comfort Limits")
        public String[] groupLimits = new String[]{
                "furniture,90",
                "workstation,50",
                "enchanting,25",
                "luxury,175",
                "lightsources,20",
                "pets,15",
                "legendary,25"
        };

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
                "250,[[minecraft:strength,1],[minecraft:resistance,1],[minecraft:regeneration,1]]",
                "300,[[minecraft:strength,1],[minecraft:resistance,1],[minecraft:regeneration,2]]",
                "350,[[minecraft:strength,1],[minecraft:resistance,2],[minecraft:regeneration,2]]",
                "400,[[minecraft:strength,2],[minecraft:resistance,2],[minecraft:regeneration,2]]"
        };

        @Config.Comment("Percentage chance for messages from Comfort Waking Messages to display after waking up in-game")
        @Config.RangeInt(min = 0, max = 100)
        @Config.Name("Comfort Waking Messages Percentage")
        public int messagePercentage = 100;

        @Config.Comment({
                "Syntax: <comfort-min>,<comfort-max>,<message>",
                "These messages will be displayed randomly after you slept in-game with a",
                "% chance configured in Comfort Waking Messages Percentage",
                "Each message has a min-max comfort requirement for it to display.",
                "0,0 is an exception and will show when the base requirements are not met."
        })
        @Config.Name("Comfort Waking Messages")
        public String[] stagedMessages = new String[]{
                "0,0,'§6You dreamt of a warm, sheltered house with a comfy fire, a pet, and a carpet on the floor'",
                "0,0,'§6As you wake up a thought lingers, surely a sheltered place with a bed, a fire and banners would beat this?'",
                "1,10,'§6You dreamt of petting your pet while watching the flames dance within the fireplace'",
                "10,30,'§6You feel refreshed, though your residence could use some improvements'",
                "30,60,'§6As you wake and look around your house, you feel somewhat established'",
                "60,100,'§6You had one of those fleeting dreams about building a library expansion'",
                "100,150,'§6Open sky, fresh air... it all sounds so romantic, but nothing beats a place like home'",
                "150,200,'§6You dreamt about beating the Ender Dragon and displaying its trophy egg!'",
                "200,250,'§6Cats, dogs, parrots!? All in the same place — wild! But also comfy!'",
                "250,300,'§6As you open your eyes, there is but one thing you can think of... a parrot called Eagle!'",
                "300,350,'§6You dreamt about a world, Dregora... and being sheltered against the probability of being smitten'",
                "350,400,'§6You wake and have an existential crisis: how much more comfort makes things comfier?!'",
                "400,450,'§6You no longer need to dream — you are living it'"
        };
    }

    public static void reloadRegistries() {

        FireBlockRegistry.reload(server.fireBlocks);
        FireSourceItemRegistry.reload(server.fireSourceItems);
        BlockComfortRegistry.reloadAliases(server.blockAliases);
        BlockComfortRegistry.reload(server.blockComfortEntries);
        EntityComfortRegistry.reload(server.blockComfortEntries);
        AreaComfortCalculator.reloadGroupLimits(server.groupLimits);
        LivingComfortRegistry.reload(server.livingComfortEntries);
        PettingComfortRegistry.loadFromConfig(server.pettingComfortEntries);
        BiomeComfortRegistry.reload(server.biomeComfortModifiers);
        NamedPetComfortRegistry.reload(server.livingTooltipEntries);


        PlayerComfortManager.reloadConfig();
        ChunkComfortClientTooltipHandler.refreshFireBlocks();
        ChunkComfortClientTooltipHandler.refreshFireSourceItems();
        ChunkComfortClientTooltipHandler.refreshConfiguredBlocks();
        ChunkComfortClientTooltipHandler.refreshGroupLimits();
        ChunkComfortClientTooltipHandler.refreshNonBlockEntities();
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