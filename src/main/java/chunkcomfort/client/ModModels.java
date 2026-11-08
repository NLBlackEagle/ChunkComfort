package chunkcomfort.client;

import chunkcomfort.ChunkComfort;
import chunkcomfort.hearth.TileEntityHearth;
import chunkcomfort.registry.ModContent;
import chunkcomfort.statue.BlockStatue;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.client.registry.ClientRegistry;
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

    private static final ResourceLocation HEARTH_MODEL = new ResourceLocation(ChunkComfort.MODID, "block/redstone_hearth");

    // Hearth faces that use the redstone quarter of the texture, drawn full bright by HearthGlowRenderer
    private static List<BakedQuad> hearthGlowQuads = Collections.emptyList();

    public static List<BakedQuad> getHearthGlowQuads() {
        return hearthGlowQuads;
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (BlockStatue statue : ModContent.STATUES) {
            register(Item.getItemFromBlock(statue));
        }
        register(ModContent.CANDLE);
        register(Item.getItemFromBlock(ModContent.REDSTONE_HEARTH));
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityHearth.class, new HearthGlowRenderer());
    }

    private static void register(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }

    /** Wrap every statue blockstate model so lit candles are drawn at their slots. */
    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        bakeHearthGlow();
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

    /**
     * Collect the hearth faces that sample the redstone quarter of the hearth texture
     * (bottom-left: u 0-8, v 8-16 in model units). Picking them by UV means a fresh
     * Blockbench export needs no extra glow model, as long as the texture layout stays.
     */
    private static void bakeHearthGlow() {
        hearthGlowQuads = Collections.emptyList();
        IBakedModel hearth;
        try {
            IModel model = ModelLoaderRegistry.getModel(HEARTH_MODEL);
            hearth = model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
        } catch (Exception e) {
            ChunkComfort.LOGGER.error("Could not load the hearth model {}", HEARTH_MODEL, e);
            return;
        }

        List<BakedQuad> all = new ArrayList<>(hearth.getQuads(null, null, 0));
        for (EnumFacing side : EnumFacing.values()) all.addAll(hearth.getQuads(null, side, 0));

        List<BakedQuad> glow = new ArrayList<>();
        for (BakedQuad quad : all) {
            if (usesRedstoneQuarter(quad)) glow.add(quad);
        }
        hearthGlowQuads = glow;
    }

    private static boolean usesRedstoneQuarter(BakedQuad quad) {
        TextureAtlasSprite sprite = quad.getSprite();
        VertexFormat format = quad.getFormat();
        int[] data = quad.getVertexData();
        int stride = format.getIntegerSize();
        int uvOffset = format.getUvOffsetById(0) / 4;
        float uSize = sprite.getMaxU() - sprite.getMinU();
        float vSize = sprite.getMaxV() - sprite.getMinV();
        if (uSize <= 0 || vSize <= 0) return false;

        final float eps = 0.01F;
        for (int v = 0; v < 4; v++) {
            float u = Float.intBitsToFloat(data[v * stride + uvOffset]);
            float vv = Float.intBitsToFloat(data[v * stride + uvOffset + 1]);
            float localU = (u - sprite.getMinU()) / uSize * 16F;
            float localV = (vv - sprite.getMinV()) / vSize * 16F;
            if (localU > 8F + eps || localV < 8F - eps) return false;
        }
        return true;
    }
}
