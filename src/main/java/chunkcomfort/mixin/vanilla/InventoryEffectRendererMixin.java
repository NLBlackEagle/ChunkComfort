package chunkcomfort.mixin.vanilla;

import chunkcomfort.api.ICanBeHidden;
import chunkcomfort.player.PlayerComfortManager;
import chunkcomfort.registry.PotionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.potion.PotionEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraftforge.fml.client.config.GuiUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(InventoryEffectRenderer.class)
public abstract class InventoryEffectRendererMixin {

    private static final Logger LOGGER = LogManager.getLogger("ChunkComfort");

    // Filter out hidden potions from Comfort
    @ModifyVariable(
            method = "drawActivePotionEffects",
            at = @At("STORE"),
            ordinal = 0
    )
    private Collection<PotionEffect> filterHiddenPotions(Collection<PotionEffect> effects) {
        Collection<PotionEffect> visible = effects.stream()
                .filter(effect -> !(effect instanceof ICanBeHidden && ((ICanBeHidden) effect).chunkcomfort$isHidden()))
                .collect(Collectors.toList());
        return visible;
    }

    @Inject(
            method = "drawActivePotionEffects",
            at = @At("RETURN")
    )
    private void drawComfortTooltip(CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        PotionEffect comfortEffect = mc.player.getActivePotionEffect(PotionRegistry.COMFORT);
        if (comfortEffect == null) return;

        List<String> effects = PlayerComfortManager.getEffectsForTier(comfortEffect.getAmplifier());
        if (effects.isEmpty()) return;

        // Mouse coordinates
        int rawMouseX = Mouse.getX();
        int rawMouseY = Mouse.getY();
        ScaledResolution sr = new ScaledResolution(mc);
        int mouseX = rawMouseX * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - (rawMouseY * sr.getScaledHeight() / mc.displayHeight) - 1;

        // Visible effects, sorted like vanilla
        List<PotionEffect> visibleEffects = mc.player.getActivePotionEffects().stream()
                .filter(e -> !(e instanceof ICanBeHidden && ((ICanBeHidden) e).chunkcomfort$isHidden()))
                .sorted((a, b) -> a.getPotion().getName().compareToIgnoreCase(b.getPotion().getName()))
                .collect(Collectors.toList());

        // Find Comfort effect index
        int comfortIndex = -1;
        for (int i = 0; i < visibleEffects.size(); i++) {
            if (visibleEffects.get(i).getPotion() == PotionRegistry.COMFORT) {
                comfortIndex = i;
                break;
            }
        }
        if (comfortIndex < 0) return;

        // Determine GUI coordinates
        int guiLeft, guiTop;
        int potionX, potionY;

        if (mc.currentScreen instanceof GuiInventory) {
            GuiInventory inv = (GuiInventory) mc.currentScreen;
            guiLeft = inv.getGuiLeft();
            guiTop = inv.getGuiTop();
            potionX = guiLeft - 124; // Survival
        } else if (mc.currentScreen instanceof GuiContainerCreative) {
            GuiContainerCreative creative = (GuiContainerCreative) mc.currentScreen;
            guiLeft = creative.getGuiLeft();
            guiTop = creative.getGuiTop();

            // Fully dynamic: calculate shift based on first visible potion
            // Vanilla shifts Creative icons slightly right
            potionX = guiLeft - 124;
            for (PotionEffect effect : visibleEffects) {
                if (!(effect instanceof ICanBeHidden && ((ICanBeHidden) effect).chunkcomfort$isHidden())) {
                    // First visible icon defines the shift
                    potionX = potionX + (effect.getPotion() == PotionRegistry.COMFORT ? 0 : 28);
                    break;
                }
            }
        } else {
            return; // unsupported screen
        }

        potionY = guiTop + 8 + comfortIndex * 33;

        int iconWidth = 140;
        int iconHeight = 32;

        LOGGER.info("[ComfortTooltip] mouse=({},{}), icon=({},{}+{}x{}), comfortIndex={}, visibleEffects={}",
                mouseX, mouseY, potionX, potionY, iconWidth, iconHeight, comfortIndex, visibleEffects.size());

        // Hover check
        if (mouseX >= potionX && mouseX <= potionX + iconWidth &&
                mouseY >= potionY && mouseY <= potionY + iconHeight) {

            LOGGER.info("[ComfortTooltip] Hover detected over Comfort effect!");

            List<String> tooltip = new ArrayList<>();
            tooltip.add("Comfort Effects:");
            tooltip.addAll(effects);

            GuiUtils.drawHoveringText(
                    tooltip,
                    mouseX + 12,
                    mouseY + 12,
                    sr.getScaledWidth(),
                    sr.getScaledHeight(),
                    -1,
                    mc.fontRenderer
            );
        } else {
            LOGGER.info("[ComfortTooltip] Hover NOT detected over Comfort effect");
        }
    }
}