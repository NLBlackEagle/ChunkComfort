package chunkcomfort.mixin.vanilla;

import chunkcomfort.api.ICanBeHidden;
import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.player.PlayerComfortManager;
import chunkcomfort.registry.PotionBlacklistRegistry;
import chunkcomfort.registry.PotionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(InventoryEffectRenderer.class)
public abstract class InventoryEffectRendererMixin {

    @ModifyVariable(
            method = "drawActivePotionEffects",
            at = @At("STORE"),
            ordinal = 0
    )
    private Collection<PotionEffect> filterHiddenPotions(Collection<PotionEffect> effects) {
        return effects.stream()
                .filter(effect -> !(effect instanceof ICanBeHidden &&
                        ((ICanBeHidden) effect).chunkcomfort$isHidden()))
                .collect(Collectors.toList());
    }

    @Inject(
            method = "drawActivePotionEffects",
            at = @At("RETURN")
    )
    private void drawComfortTooltip(CallbackInfo ci) {

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        // --- Build visible effects EXACTLY like vanilla ---
        List<PotionEffect> visibleEffects =
                mc.player.getActivePotionEffects().stream()
                        .filter(e -> !(e instanceof ICanBeHidden &&
                                ((ICanBeHidden) e).chunkcomfort$isHidden()))
                        .sorted((a, b) ->
                                a.getPotion().getName()
                                        .compareToIgnoreCase(b.getPotion().getName()))
                        .collect(Collectors.toCollection(ArrayList::new));

        // --- Move Comfort effect to top SAFELY ---
        PotionEffect comfortEffect = null;

        Iterator<PotionEffect> iterator = visibleEffects.iterator();
        while (iterator.hasNext()) {
            PotionEffect e = iterator.next();

            if (e.getPotion() == PotionRegistry.COMFORT) {
                comfortEffect = e;
                iterator.remove();
                break;
            }
        }

        if (comfortEffect == null) return;

        visibleEffects.add(0, comfortEffect);

        // --- Positive effects ---
        List<String> effects =
                PlayerComfortManager.getEffectsForTier(comfortEffect.getAmplifier());

        if (effects.isEmpty()) return;

        // --- Compute comfort ONCE ---
        EntityPlayer player = mc.player;
        int comfort = AreaComfortCalculator.calculatePlayerComfort(player);

        // --- Build NEGATED effects ---
        List<String> negated = new ArrayList<>();

        for (Map.Entry<Integer, Set<String>> entry :
                PotionBlacklistRegistry.getBlacklist().entrySet()) {

            if (comfort < entry.getKey()) continue;

            for (String potionId : entry.getValue()) {

                Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(potionId));
                if (potion == null) continue;

                String name = I18n.format(potion.getName());

                if (!negated.contains(name)) {
                    negated.add(name);
                }
            }
        }

        // --- Mouse coordinates ---
        int rawMouseX = Mouse.getX();
        int rawMouseY = Mouse.getY();
        ScaledResolution sr = new ScaledResolution(mc);

        int mouseX = rawMouseX * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight()
                - (rawMouseY * sr.getScaledHeight() / mc.displayHeight) - 1;

        // Comfort forced to top
        int comfortIndex = 0;

        // --- GUI positioning ---
        int guiLeft, guiTop;
        int potionX;

        if (mc.currentScreen instanceof GuiInventory) {
            GuiInventory inv = (GuiInventory) mc.currentScreen;
            guiLeft = inv.getGuiLeft();
            guiTop = inv.getGuiTop();
            potionX = guiLeft - 124;

        } else if (mc.currentScreen instanceof GuiContainerCreative) {
            GuiContainerCreative creative = (GuiContainerCreative) mc.currentScreen;
            guiLeft = creative.getGuiLeft();
            guiTop = creative.getGuiTop();

            potionX = guiLeft - 124;

            if (!visibleEffects.isEmpty()
                    && visibleEffects.get(0).getPotion() != PotionRegistry.COMFORT) {
                potionX += 28;
            }

        } else {
            return;
        }

        int potionY = guiTop + 8 + comfortIndex * 33;

        // --- Hover area ---
        int hoverX = potionX;
        int hoverY = potionY - 10;
        int hoverWidth = 120;
        int hoverHeight = 32;

        if (mouseX >= hoverX && mouseX <= hoverX + hoverWidth &&
                mouseY >= hoverY && mouseY <= hoverY + hoverHeight) {

            List<String> tooltip = new ArrayList<>();

            tooltip.add(I18n.format("tooltip.chunkcomfort.positive.effects"));

            for (String effect : effects) {
                tooltip.add(I18n.format("tooltip.chunkcomfort.positive.effects.plus") + " " + effect);
            }

            if (!negated.isEmpty()) {
                tooltip.add("");
                tooltip.add(I18n.format("tooltip.chunkcomfort.negative.effects"));

                for (String effect : negated) {
                    tooltip.add(I18n.format("tooltip.chunkcomfort.positive.effects.minus") + " " + effect);
                }
            }

            GuiUtils.drawHoveringText(
                    tooltip,
                    mouseX + 12,
                    mouseY + 12,
                    sr.getScaledWidth(),
                    sr.getScaledHeight(),
                    -1,
                    mc.fontRenderer
            );
        }
    }
}