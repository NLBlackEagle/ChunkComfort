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
                "Enable boss bar based comfort blocking",
                "If disabled, boss bars will not affect environment comfort logic"
        })
        @Config.Name("Enable Boss Bar Comfort Blocking")
        public boolean enableBossBarDetection = true;

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

        @Config.Name("Custom Spawn Eggs")
        @Config.Comment({
                "Format: <entity_registry_name>=<spawn_egg_item>,<nbt_path>",
                "Example: lycanitesmobs:roc=lycanitesmobs:avianspawn,CreatureInfoSpawnEgg.creaturename",
                "This allows the mod to recognize Lycanites spawn eggs for comfort calculations."
        })
        public String[] customSpawnEggs = new String[] {
                "lycanitesmobs:roc=lycanitesmobs:avianspawn,CreatureInfoSpawnEgg.creaturename",
                "lycanitesmobs:morock=lycanitesmobs:dragonspawn,CreatureInfoSpawnEgg.creaturename"
        };


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
                "minecraft:anvil,5,workstation,2",

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

                // Collectibles
                "minecraft:skull,1,collectibles,10",
                "minecraft:dragon_egg,20,collectibles,1",
                "minecraft:beacon,20,collectibles,1",

                // Lightsources (small contribution)
                "minecraft:torch,1,lightsources,20",
                "minecraft:redstone_torch,1,lightsources,20",
                "minecraft:redstone_lamp,1,lightsources,20"

        };

        @Config.Comment({
                "Block ID aliases",
                "Allows multiple block IDs to be treated as one logical block in configs.",
                "Example: minecraft:banner=minecraft:standing_banner,minecraft:wall_banner",
                "Using 'minecraft:banner' will match both banner variants."
        })
        @Config.Name("Block ID aliases")
        public String[] blockAliases = new String[] {
                "minecraft:banner=minecraft:standing_banner,minecraft:wall_banner",
                "minecraft:redstone_lamp=minecraft:lit_redstone_lamp,minecraft:redstone_lamp"
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
                "minecraft:villager,1,villagers,10,{Profession:5}",
                "minecraft:ocelot,4,pets,2,{!CatType:0,OwnerUUID:'*-*'}",
                "minecraft:parrot,1,pets,3,{OwnerUUID:'*-*'}",
                "minecraft:wolf,4,pets,2,{OwnerUUID:'*-*'}",
                "minecraft:horse,2,pets,1,{OwnerUUID:'*-*'}",
                "minecraft:donkey,2,pets,1,{OwnerUUID:'*-*'}",
                "minecraft:mule,2,pets,1,{OwnerUUID:'*-*'}",
                "minecraft:llama,2,pets,1,{OwnerUUID:'*-*'}",
                "minecraft:skeleton_horse,2,pets,1,{OwnerUUID:'*-*'}",
                "minecraft:squid,farm,5,{}",
                "minecraft:sheep,1,farm,5,{}",
                "minecraft:cow,1,farm,5,{}",
                "minecraft:chicken,1,farm,5,{}",
                "minecraft:mooshroom,5,farm,1,{}"
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
                "minecraft:donkey,'Donkey',1",
                "minecraft:villager,'ijsbrand',1",
                "minecraft:squid,'Octo',5"

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
                "minecraft:parrot,1,3,300,1200,true,true,true",
                "minecraft:horse,2,1,300,600,true,true,true",
                "minecraft:donkey,1,1,300,600,true,true,true",
                "minecraft:mule,1,1,300,600,true,true,true",
                "minecraft:llama,1,1,300,600,true,true,true",
                "minecraft:skeleton_horse,3,1,300,1200,true,true,true"
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
                "collectibles,50",
                "villagers,10",
                "farm,10"
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
                "425,[[minecraft:strength,2],[minecraft:resistance,2],[minecraft:regeneration,2]]"
        };

        @Config.Comment({
                "Dimension blacklist (comfort disabled in these dimensions)",
                "Format: <dimension_id>",
                "Example: the_nether",
                "Use /chunkcomfort biome to check dimension/biome_id"
        })
        @Config.Name("Dimension Blacklist")
        public String[] dimensionBlacklist = new String[]{
                "the_nether"
        };

        @Config.Comment({
                "Biome blacklist (comfort disabled in these biomes)",
                "Format: <biome_id>",
                "Example: minecraft:hell"
        })
        @Config.Name("Biome Blacklist")
        public String[] biomeBlacklist = new String[]{
                "minecraft:hell"
        };

        @Config.Comment({
                "Potion blacklist per comfort tier",
                "Format: <min_comfort>,[<potion_id>,<another_potion_id>]",
                "Example: 60,[minecraft:poison]",
                "Example: 100,[minecraft:wither,minecraft:mining_fatigue]"
        })
        @Config.Name("Comfort Potion Blacklist")
        public String[] comfortPotionBlacklist = new String[]{
                "60,[minecraft:poison]",
                "100,[minecraft:wither,minecraft:mining_fatigue]"
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
                "0,0,chunkcomfort.config.message.1",
                "0,0,chunkcomfort.config.message.2",
                "1,10,chunkcomfort.config.message.3",
                "10,30,chunkcomfort.config.message.4",
                "30,60,chunkcomfort.config.message.5",
                "60,100,chunkcomfort.config.message.6",
                "100,150,chunkcomfort.config.message.7",
                "150,200,chunkcomfort.config.message.8",
                "200,250,chunkcomfort.config.message.9",
                "250,300,chunkcomfort.config.message.10",
                "300,350,chunkcomfort.config.message.11",
                "350,425,chunkcomfort.config.message.12",
                "425,450,chunkcomfort.config.message.13"
        };
    }

    public static void reloadRegistries() {

        try { DimensionBiomeBlacklistRegistry.reload(server.dimensionBlacklist,server.biomeBlacklist); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Dimension and Biome Blacklist", e); }

        try { PotionBlacklistRegistry.reload(server.comfortPotionBlacklist); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Potion Blacklist", e); }

        try { FireBlockRegistry.reload(server.fireBlocks); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Fire Blocks", e); }

        try { FireSourceItemRegistry.reload(server.fireSourceItems); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Fire Source Items", e); }

        try { BlockComfortRegistry.reloadAliases(server.blockAliases); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Block Aliases", e); }

        try { BlockComfortRegistry.reload(server.blockComfortEntries); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Block Comfort Entries", e); }

        try { EntityComfortRegistry.reload(server.blockComfortEntries); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Entity Comfort Entries", e); }

        try { CustomSpawnEggRegistry.reload(server.customSpawnEggs); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Custom Spawn Eggs", e); }

        try { AreaComfortCalculator.reloadGroupLimits(server.groupLimits); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Group Limits", e); }

        try { LivingComfortRegistry.reload(server.livingComfortEntries); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Living Comfort Entries", e); }

        try { PettingComfortRegistry.loadFromConfig(server.pettingComfortEntries); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Petting Comfort Entries", e); }

        try { BiomeComfortRegistry.reload(server.biomeComfortModifiers); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Biome Comfort Modifiers", e); }

        try { NamedPetComfortRegistry.reload(server.livingTooltipEntries); }
        catch (Exception e) { ChunkComfort.LOGGER.error("Failed to reload Named Pet Comfort Entries", e); }

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