package chunkcomfort.potion;

import chunkcomfort.ChunkComfort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Base class for all custom potions in ChunkComfort.
 * Handles HUD rendering, inventory rendering, and client-side visibility.
 */
public abstract class PotionBase extends Potion {

    private final ResourceLocation texture;
    protected int liquidColor;

    public PotionBase(String name, boolean isBadEffect, int liquidColor) {
        super(isBadEffect, liquidColor);
        this.liquidColor = liquidColor;
        this.texture = new ResourceLocation(ChunkComfort.MODID, "textures/effects/" + name + ".png");
        setRegistryName(ChunkComfort.MODID, name);
        setPotionName(ChunkComfort.MODID + ".effects." + name);
    }

    @Override
    public boolean hasStatusIcon() {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) {
        renderInventoryEffect(x, y, effect, Minecraft.getMinecraft());
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        mc.getTextureManager().bindTexture(texture);
        Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 7, 0, 0, 18, 18, 18, 18);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
        renderHUDEffect(x, y, effect, Minecraft.getMinecraft(), alpha);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        mc.getTextureManager().bindTexture(texture);
        Gui.drawModalRectWithCustomSizedTexture(x + 3, y + 3, 0, 0, 18, 18, 18, 18);
    }
}