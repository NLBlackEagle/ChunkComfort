package chunkcomfort.mixin.vanilla;

import chunkcomfort.api.ICanBeHidden;
import chunkcomfort.player.PlayerComfortManager;
import chunkcomfort.registry.PotionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
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
        if (mc.player == null) {
            LOGGER.info("drawComfortTooltip: player is null");
            return;
        }

        PotionEffect comfortEffect = mc.player.getActivePotionEffect(PotionRegistry.COMFORT);
        if (comfortEffect == null) {
            LOGGER.info("drawComfortTooltip: Comfort effect not active");
            return;
        }

        List<String> effects = PlayerComfortManager.getEffectsForTier(comfortEffect.getAmplifier());
        if (effects.isEmpty()) {
            LOGGER.info("drawComfortTooltip: No Comfort effects for tier {}", comfortEffect.getAmplifier());
            return;
        }

        // Get raw mouse coordinates
        int rawMouseX = Mouse.getX();
        int rawMouseY = Mouse.getY();

        ScaledResolution sr = new ScaledResolution(mc);
        int mouseX = rawMouseX * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - (rawMouseY * sr.getScaledHeight() / mc.displayHeight) - 1;

        // Get visible effects, sorted like vanilla
        List<PotionEffect> visibleEffects = mc.player.getActivePotionEffects().stream()
                .filter(e -> !(e instanceof ICanBeHidden && ((ICanBeHidden) e).chunkcomfort$isHidden()))
                .sorted((a, b) -> a.getPotion().getName().compareToIgnoreCase(b.getPotion().getName()))
                .collect(Collectors.toList());

        // Find index of Comfort effect
        int comfortIndex = -1;
        for (int i = 0; i < visibleEffects.size(); i++) {
            if (visibleEffects.get(i).getPotion() == PotionRegistry.COMFORT) {
                comfortIndex = i;
                break;
            }
        }

        LOGGER.info("Comfort effect index in visible effects: {}", comfortIndex);

        if (comfortIndex < 0) {
            LOGGER.info("drawComfortTooltip: Comfort not found in visible effects");
            return;
        }

        if (!(mc.currentScreen instanceof GuiInventory)) {
            LOGGER.info("drawComfortTooltip: Not in Inventory GUI, currentScreen={}", mc.currentScreen);
            return;
        }
        GuiInventory inv = (GuiInventory) mc.currentScreen;

        // Vanilla positions for potion icons in inventory (C area)
        int iconX = inv.getGuiLeft() - 124;
        int iconY = inv.getGuiTop() + 8 + comfortIndex * 33;
        int iconWidth = 140;
        int iconHeight = 32;

        LOGGER.info("Mouse position: ({},{}), Icon area: ({},{},{},{})",
                mouseX, mouseY, iconX, iconY, iconWidth, iconHeight);

        // Hover check
        if (mouseX >= iconX && mouseX <= iconX + iconWidth &&
                mouseY >= iconY && mouseY <= iconY + iconHeight) {

            LOGGER.info("Hover detected over Comfort effect!");

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
            LOGGER.info("Hover NOT detected over Comfort effect");
        }
    }
}