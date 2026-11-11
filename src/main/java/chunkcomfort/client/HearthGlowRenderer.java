package chunkcomfort.client;

import chunkcomfort.hearth.BlockHearth;
import chunkcomfort.hearth.TileEntityHearth;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.model.pipeline.LightUtil;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Draws the hearth's redstone faces again at full brightness so they glow in the dark.
 *
 * The block model already contains these faces (normal lighting), so beyond tile entity
 * render distance the redstone is still there, just not glowing. A polygon offset pulls the
 * bright copy in front of the normal one so the two don't z-fight.
 */
public class HearthGlowRenderer extends TileEntitySpecialRenderer<TileEntityHearth> {

    @Override
    public void render(TileEntityHearth te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        List<BakedQuad> quads = ModModels.getHearthGlowQuads();
        if (quads.isEmpty()) return;

        IBlockState state = te.getWorld().getBlockState(te.getPos());
        if (!(state.getBlock() instanceof BlockHearth)) return;

        // Same rotation as the blockstate: north = 0, east = 90, south = 180, west = 270
        float yRot = (state.getValue(BlockHearth.FACING).getHorizontalAngle() + 180F) % 360F;

        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        GlStateManager.rotate(-yRot, 0F, 1F, 0F);
        GlStateManager.translate(-0.5, 0, -0.5);

        GlStateManager.disableLighting();
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-1F, -10F);
        GlStateManager.color(1F, 1F, 1F, 1F);
        float lastX = OpenGlHelper.lastBrightnessX;
        float lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

        int color = pulseColor(te, partialTicks);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
        for (BakedQuad quad : quads) {
            LightUtil.renderQuadColor(buffer, quad, color);
        }
        tess.draw();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
        GlStateManager.disablePolygonOffset();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    /**
     * Slow ember-like breathing: two sine waves of different speeds so it never looks like a
     * clean loop, offset per position so neighbouring hearths don't pulse in sync. Green and
     * blue dim more than red, so the dim phase reads deeper red rather than just darker.
     */
    private static int pulseColor(TileEntityHearth te, float partialTicks) {
        long seed = te.getPos().toLong() * 31L;
        float phase = (seed & 0xFFFF) / 65536F * (float) (Math.PI * 2);
        float t = (te.getWorld().getTotalWorldTime() % 24000L) + partialTicks;
        float wave = 0.65F * (float) Math.sin(t * 0.09F + phase)   // ~3.5 s cycle
                   + 0.35F * (float) Math.sin(t * 0.23F + phase * 2F); // faster flicker on top
        float p = 0.5F + 0.5F * wave; // 0..1

        int r = (int) (255F * (0.85F + 0.15F * p));
        int gb = (int) (255F * (0.55F + 0.45F * p));
        return 0xFF000000 | (r << 16) | (gb << 8) | gb;
    }
}
