package chunkcomfort.client;

import chunkcomfort.ChunkComfort;
import chunkcomfort.player.PlayerComfortManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class ComfortTierUpToast implements IToast {

    private static final ResourceLocation TEXTURE_TOAST =
            new ResourceLocation("textures/gui/toasts.png");
    private static final ResourceLocation TEXTURE_ICON =
            new ResourceLocation(ChunkComfort.MODID, "textures/effects/comfort.png");

    private final int tierIndex;
    private long firstDrawTime;
    private boolean hasDrawnBefore;

    public ComfortTierUpToast(int tierIndex) {
        this.tierIndex = tierIndex;
    }

    @Override
    public Visibility draw(GuiToast toastGui, long delta) {
        if (!hasDrawnBefore) {
            firstDrawTime = delta;
            hasDrawnBefore = true;
        }

        Minecraft mc = toastGui.getMinecraft();

        // Draw toast background
        mc.getTextureManager().bindTexture(TEXTURE_TOAST);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        toastGui.drawTexturedModalRect(0, 0, 0, 32, 160, 32);

        // Draw comfort icon on the left
        mc.getTextureManager().bindTexture(TEXTURE_ICON);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        toastGui.drawModalRectWithCustomSizedTexture(6, 7, 0, 0, 18, 18, 18, 18);

        boolean isMax = PlayerComfortManager.getNextTierThreshold(tierIndex) < 0;

        String title = I18n.format(isMax
                ? "toast.chunkcomfort.tierup.title.max"
                : "toast.chunkcomfort.tierup.title");

        // Per-tier flavour line, falling back to a generic message if not defined
        String flavorKey = "toast.chunkcomfort.tierup.tier" + tierIndex;
        String flavor = I18n.format(flavorKey);
        if (flavor.equals(flavorKey)) {
            flavor = I18n.format("toast.chunkcomfort.tierup.flavor");
        }

        mc.fontRenderer.drawString(title, 30, 7, 0x500050);
        mc.fontRenderer.drawString(flavor, 30, 18, 0x000000);

        return delta - firstDrawTime < 5000L ? Visibility.SHOW : Visibility.HIDE;
    }

    public static void show(int tierIndex) {
        Minecraft.getMinecraft().getToastGui().add(new ComfortTierUpToast(tierIndex));
    }
}
