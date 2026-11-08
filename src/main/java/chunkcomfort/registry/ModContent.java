package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import chunkcomfort.hearth.BlockHearth;
import chunkcomfort.hearth.TileEntityHearth;
import chunkcomfort.statue.BlockStatue;
import chunkcomfort.statue.StatueType;
import chunkcomfort.statue.TileEntityStatue;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.List;

/** Blocks, items and tile entities added by ChunkComfort (statues, candles and the hearth). */
@Mod.EventBusSubscriber(modid = ChunkComfort.MODID)
public class ModContent {

    public static final List<BlockStatue> STATUES = new ArrayList<>();
    public static Item CANDLE;
    public static Block REDSTONE_HEARTH;

    public static final CreativeTabs TAB = new CreativeTabs(ChunkComfort.MODID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(CANDLE);
        }
    };

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        for (StatueType type : StatueType.values()) {
            BlockStatue statue = new BlockStatue(type);
            statue.setRegistryName(ChunkComfort.MODID, type.name);
            statue.setTranslationKey(ChunkComfort.MODID + "." + type.name);
            statue.setCreativeTab(TAB);
            STATUES.add(statue);
            event.getRegistry().register(statue);
        }

        REDSTONE_HEARTH = new BlockHearth()
                .setRegistryName(ChunkComfort.MODID, "redstone_hearth")
                .setTranslationKey(ChunkComfort.MODID + ".redstone_hearth")
                .setCreativeTab(TAB);
        event.getRegistry().register(REDSTONE_HEARTH);

        GameRegistry.registerTileEntity(TileEntityStatue.class, new ResourceLocation(ChunkComfort.MODID, "statue"));
        GameRegistry.registerTileEntity(TileEntityHearth.class, new ResourceLocation(ChunkComfort.MODID, "hearth"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (BlockStatue statue : STATUES) {
            event.getRegistry().register(new ItemBlock(statue).setRegistryName(statue.getRegistryName()));
        }

        event.getRegistry().register(new ItemBlock(REDSTONE_HEARTH).setRegistryName(REDSTONE_HEARTH.getRegistryName()));

        CANDLE = new Item()
                .setRegistryName(ChunkComfort.MODID, "candle")
                .setTranslationKey(ChunkComfort.MODID + ".candle")
                .setCreativeTab(TAB);
        event.getRegistry().register(CANDLE);
    }
}
