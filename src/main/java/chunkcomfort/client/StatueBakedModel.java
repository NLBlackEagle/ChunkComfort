package chunkcomfort.client;

import chunkcomfort.statue.BlockStatue;
import chunkcomfort.statue.StatueType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Statue model plus one candle at each lit slot. The lit slots come from the
 * CANDLE_MASK extended state, the positions from StatueLayouts.
 */
public class StatueBakedModel implements IBakedModel {

    private final IBakedModel base;
    private final List<BakedQuad> candleQuads;
    private final StatueType type;

    // Quads per (facing, candle mask); chunk rebuilds call getQuads often
    private final Map<Long, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    public StatueBakedModel(IBakedModel base, List<BakedQuad> candleQuads, StatueType type) {
        this.base = base;
        this.type = type;
        this.candleQuads = candleQuads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        List<BakedQuad> baseQuads = base.getQuads(state, side, rand);

        // Candles go in the general (side == null) list so they're never face-culled
        if (side != null || !(state instanceof IExtendedBlockState)) return baseQuads;

        Integer mask = ((IExtendedBlockState) state).getValue(BlockStatue.CANDLE_MASK);
        if (mask == null || mask == 0) return baseQuads;

        EnumFacing facing = state.getValue(BlockStatue.FACING);
        long key = ((long) facing.getHorizontalIndex() << 32) | (mask & 0xFFFFFFFFL);

        return cache.computeIfAbsent(key, k -> {
            List<BakedQuad> quads = new ArrayList<>(baseQuads);
            List<StatueLayouts.Slot> layout = StatueLayouts.get(type);

            for (int slot = 0; slot < layout.size(); slot++) {
                if ((mask & (1 << slot)) == 0) continue;

                // The candle model stands centred on the block (8, 0, 8); move it onto the slot
                StatueLayouts.Slot candle = layout.get(slot);
                Vec3d p = StatueType.rotate(candle.pos, facing);
                float ox = (float) (p.x - 0.5), oy = (float) p.y, oz = (float) (p.z - 0.5);
                for (BakedQuad quad : candleQuads) quads.add(place(quad, ox, oy, oz, (float) candle.height));
            }
            return quads;
        });
    }

    /**
     * Moves a candle quad onto its slot and resizes the wax to the slot's height: everything up to
     * CANDLE_HEIGHT is stretched or squashed, everything above it (the wick) just moves up or down.
     */
    static BakedQuad place(BakedQuad quad, float dx, float dy, float dz, float height) {
        float wax = (float) (StatueType.CANDLE_HEIGHT / 16.0);
        float newWax = height / 16.0F;
        VertexFormat format = quad.getFormat();
        int[] data = quad.getVertexData().clone();
        int stride = format.getIntegerSize();
        int posOffset = format.getOffset(format.getElements().indexOf(DefaultVertexFormats.POSITION_3F)) / 4;

        for (int v = 0; v < 4; v++) {
            int i = v * stride + posOffset;
            data[i]     = Float.floatToRawIntBits(Float.intBitsToFloat(data[i]) + dx);
            float y = Float.intBitsToFloat(data[i + 1]);
            y = y <= wax + 1e-4F ? y * newWax / wax : y + newWax - wax;
            data[i + 1] = Float.floatToRawIntBits(y + dy);
            data[i + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(data[i + 2]) + dz);
        }

        return new BakedQuad(data, quad.getTintIndex(), quad.getFace(), quad.getSprite(), quad.shouldApplyDiffuseLighting(), format);
    }

    // --- Everything else comes from the statue model ---

    @Override public boolean isAmbientOcclusion() { return base.isAmbientOcclusion(); }
    @Override public boolean isGui3d() { return base.isGui3d(); }
    @Override public boolean isBuiltInRenderer() { return base.isBuiltInRenderer(); }
    @Override public TextureAtlasSprite getParticleTexture() { return base.getParticleTexture(); }
    @Override @SuppressWarnings("deprecation") public ItemCameraTransforms getItemCameraTransforms() { return base.getItemCameraTransforms(); }
    @Override public ItemOverrideList getOverrides() { return base.getOverrides(); }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
        return base.handlePerspective(type);
    }
}
