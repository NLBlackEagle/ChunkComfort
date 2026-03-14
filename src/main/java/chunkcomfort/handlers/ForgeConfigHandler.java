package chunkcomfort.handlers;


import chunkcomfort.ChunkComfort;
import chunkcomfort.registry.BlockComfortRegistry;
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

        @Config.Comment("Chunk radius checked for comfort")
        @Config.RangeInt(min = 0, max = 10)
        public int chunkRadius = 1;

        @Config.Comment("Player comfort check interval (ticks)")
        @Config.RangeInt(min = 20, max = 2000)
        public int comfortCheckInterval = 100;

        @Config.Comment({
                "Block comfort entries",
                "Format: <block>,<value>,<limit>,<group>",
                "Example: minecraft:crafting_table,2,3,workstation"
        })
        public String[] blockComfortEntries = new String[]{
                "minecraft:bookshelf,1,10,furniture",
                "minecraft:crafting_table,10,5,workstation"
        };

        @Config.Comment("Blocks that count as fire sources")
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
        public String[] comfortEffects = new String[]{
                "-10,[[minecraft:weakness,0],[minecraft:blindness,0]]",
                "10,[[minecraft:speed,0]]",
                "20,[[minecraft:regeneration,0]]"
        };
    }

    public static void reloadRegistries() {
        BlockComfortRegistry.reload(server.blockComfortEntries);
        //FireBlockRegistry.reload(server.fireBlocks);
        //ComfortEffectRegistry.reload(server.comfortEffects);
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