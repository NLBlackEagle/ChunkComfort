package chunkcomfort.client;

import chunkcomfort.ChunkComfort;
import chunkcomfort.registry.ModContent;
import chunkcomfort.statue.BlockStatue;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Item models for the statues and candle, and the candle-drawing statue models (client only). */
@Mod.EventBusSubscriber(modid = ChunkComfort.MODID, value = Side.CLIENT)
public class ModModels {

    private static final ResourceLocation CANDLE_MODEL = new ResourceLocation(ChunkComfort.MODID, "block/candle");

    // Candle quads (block space, standing at 8, 0, 8), also used for the ghost candle when aiming
    private static List<BakedQuad> candleQuads = Collections.emptyList();

    public static List<BakedQuad> getCandleQuads() {
        return candleQuads;
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (BlockStatue statue : ModContent.STATUES) {
            register(Item.getItemFromBlock(statue));
        }
        register(ModContent.CANDLE);
    }

    private static void register(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }

    /** Wrap every statue blockstate model so lit candles are drawn at their slots. */
    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        IBakedModel candle;
        try {
            IModel model = ModelLoaderRegistry.getModel(CANDLE_MODEL);
            candle = model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
        } catch (Exception e) {
            ChunkComfort.LOGGER.error("Could not load the statue candle model {}", CANDLE_MODEL, e);
            return;
        }

        // The candle model has no cullfaces, but collect every side to be safe
        List<BakedQuad> quads = new ArrayList<>(candle.getQuads(null, null, 0));
        for (EnumFacing side : EnumFacing.values()) quads.addAll(candle.getQuads(null, side, 0));
        candleQuads = quads;

        for (BlockStatue statue : ModContent.STATUES) {
            for (EnumFacing facing : EnumFacing.HORIZONTALS) {
                ModelResourceLocation location = new ModelResourceLocation(statue.getRegistryName(), "facing=" + facing.getName());
                IBakedModel base = event.getModelRegistry().getObject(location);
                if (base != null) {
                    event.getModelRegistry().putObject(location, new StatueBakedModel(base, quads, statue.type));
                }
            }
        }
    }
}
