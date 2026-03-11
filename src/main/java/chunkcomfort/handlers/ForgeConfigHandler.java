package chunkcomfort.handlers;

import fermiumbooter.annotations.MixinConfig;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import chunkcomfort.ChunkComfort;
import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.FireBlockRegistry;
import chunkcomfort.registry.ComfortEffectRegistry;

@Config(modid = ChunkComfort.MODID)
public class ForgeConfigHandler {

    @Config.Comment("Server-Side Options")
    @Config.Name("Server Options")
    public static final ServerConfig server = new ServerConfig();

    @Config.Comment("Client-Side Options")
    @Config.Name("Client Options")
    public static final ClientConfig client = new ClientConfig();

    public static class ServerConfig {

        @Config.Comment("Enable vanilla player mixin")
        @Config.Name("Enable Vanilla Player Mixin (Vanilla)")
        @MixinConfig.MixinToggle(
                earlyMixin = "mixins.chunkcomfort.vanilla.json",
                defaultValue = false
        )
        public boolean enableVanillaMixin = false;

        @Config.Comment("Chunk radius checked for comfort")
        @Config.Name("Chunk Radius")
        @Config.RangeInt(min = 0, max = 10)
        public int chunkRadius = 1;

        @Config.Comment("Player comfort check interval (ticks)")
        @Config.Name("Comfort Check Interval")
        @Config.RangeInt(min = 20, max = 2000)
        public int comfortCheckInterval = 100;

        @Config.Comment("Minimum light level required for comfort")
        @Config.Name("Minimum Comfort Light Level")
        @Config.RangeInt(min = 0, max = 15)
        public int minComfortLightLevel = 7;

        @Config.Comment("Maximum depth scanned per column during chunk scan")
        @Config.Name("Column Scan Depth")
        @Config.RangeInt(min = 1, max = 256)
        public int columnScanDepth = 16;

        @Config.Comment({
                "Block comfort entries",
                "Format: <block>,<value>,<limit>,<group>",
                "Example: variedcommodities:chair,2,3,seating"
        })
        @Config.Name("Block Comfort Entries")
        public String[] blockComfortEntries = new String[]{
                "variedcommodities:chair,2,3,seating",
                "variedcommodities:stool_3,2,3,seating",
                "variedcommodities:stool_1,1,3,seating"
        };

        @Config.Comment("Blocks that count as fire sources")
        @Config.Name("Fire Blocks")
        public String[] fireBlocks = new String[]{
                "minecraft:torch",
                "minecraft:fire",
                "minecraft:lantern"
        };

        @Config.Comment({
                "Comfort tier effects",
                "Format: <comfort>,[[<potion>,<amplifier>],[<potion>,<amplifier>]]",
                "Example: -10,[[minecraft:weakness,0],[minecraft:blindness,0]]"
        })
        @Config.Name("Comfort Effects")
        public String[] comfortEffects = new String[]{
                "-10,[[minecraft:weakness,0],[minecraft:blindness,0]]",
                "10,[[minecraft:speed,0]]",
                "20,[[minecraft:regeneration,0]]"
        };
    }

    public static class ClientConfig {

        @Config.Comment("Example client option")
        @Config.Name("Example Client Option")
        public boolean exampleClientOption = true;
    }

    public static void reloadRegistries() {
        BlockComfortRegistry.reload(server.blockComfortEntries);
        FireBlockRegistry.reload(server.fireBlocks);
        ComfortEffectRegistry.reload(server.comfortEffects);
    }

    @Mod.EventBusSubscriber(modid = ChunkComfort.MODID)
    private static class EventHandler {

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