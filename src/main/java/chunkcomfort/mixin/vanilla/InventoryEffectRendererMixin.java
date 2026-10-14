package chunkcomfort.mixin.vanilla;

import chunkcomfort.api.ICanBeHidden;
import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.client.ClientComfortStatus;
import chunkcomfort.player.PlayerComfortManager;
import chunkcomfort.registry.PotionBlacklistRegistry;
import chunkcomfort.registry.PotionRegistry;
import chunkcomfort.registry.StatueRegistry;
import chunkcomfort.statue.StatueType;
import com.google.common.collect.Ordering;
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
import org.spongepowered.asm.mixin.injection.Redirect;
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

    // REASON: vanilla sorts effects by duration (shortest first), so the Comfort icon
    // moved around the list while the hover tooltip below assumed it was the top row.
    // Force Comfort to be drawn first and keep vanilla order for everything else.
    @Redirect(
            method = "drawActivePotionEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Ordering;sortedCopy(Ljava/lang/Iterable;)Ljava/util/List;",
                    remap = false
            )
    )
    private List<PotionEffect> sortComfortFirst(Ordering<PotionEffect> ordering, Iterable<PotionEffect> effects) {
        List<PotionEffect> sorted = ordering.sortedCopy(effects);

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getPotion() == PotionRegistry.COMFORT) {
                sorted.add(0, sorted.remove(i));
                break;
            }
        }

        return sorted;
    }

    @Inject(
            method = "drawActivePotionEffects",
            at = @At("RETURN")
    )
    private void drawComfortTooltip(CallbackInfo ci) {

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;


        // Comfort is always drawn as the top row (see sortComfortFirst)
        PotionEffect comfortEffect = mc.player.getActivePotionEffect(PotionRegistry.COMFORT);
        if (comfortEffect == null) return;
        if (comfortEffect instanceof ICanBeHidden && ((ICanBeHidden) comfortEffect).chunkcomfort$isHidden()) return;

        // --- Positive effects ---
        Set<Integer> litStatueSlots = ClientComfortStatus.litStatueSlots;
        List<PlayerComfortManager.EffectEntry> effects =
                PlayerComfortManager.getStackedEffects(comfortEffect.getAmplifier(), litStatueSlots);

        if (effects.isEmpty()) return;

        // --- Compute comfort ONCE ---
        EntityPlayer player = mc.player;
        int comfort = PlayerComfortManager.getCachedComfort(player);
        int maxComfort = AreaComfortCalculator.getMaxComfort();

        // --- Build NEGATED effect lines ---
        // A potion is negated if any unlocked entry for it is lit; otherwise name the candle that would negate it
        Set<Potion> negatedPotions = new LinkedHashSet<>();
        Map<Potion, StatueRegistry.Requirement> waitingPotions = new LinkedHashMap<>();

        for (PotionBlacklistRegistry.Entry entry : PotionBlacklistRegistry.getEntries()) {
            if (comfort < entry.comfort) continue;

            Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(entry.potionId));
            if (potion == null) continue;

            if (StatueRegistry.isLit(entry.statue, litStatueSlots)) negatedPotions.add(potion);
            else waitingPotions.putIfAbsent(potion, entry.statue);
        }

        List<String> negated = new ArrayList<>();
        for (Potion potion : negatedPotions) {
            negated.add(I18n.format("tooltip.chunkcomfort.positive.effects.minus") + " " + I18n.format(potion.getName()));
        }
        for (Map.Entry<Potion, StatueRegistry.Requirement> waiting : waitingPotions.entrySet()) {
            if (negatedPotions.contains(waiting.getKey())) continue;
            StatueRegistry.Requirement candle = waiting.getValue();
            negated.add(I18n.format("tooltip.chunkcomfort.negative.effects.needs", I18n.format(waiting.getKey().getName()),
                    candle.statue.getLocalizedName(), StatueType.roman(candle.slot + 1)));
        }

        // --- Mouse coordinates ---
        int rawMouseX = Mouse.getX();
        int rawMouseY = Mouse.getY();
        ScaledResolution sr = new ScaledResolution(mc);

        int mouseX = rawMouseX * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight()
                - (rawMouseY * sr.getScaledHeight() / mc.displayHeight) - 1;

        // --- GUI positioning ---
        int guiLeft, guiTop;

        if (mc.currentScreen instanceof GuiInventory) {
            GuiInventory inv = (GuiInventory) mc.currentScreen;
            guiLeft = inv.getGuiLeft();
            guiTop = inv.getGuiTop();

        } else if (mc.currentScreen instanceof GuiContainerCreative) {
            GuiContainerCreative creative = (GuiContainerCreative) mc.currentScreen;
            guiLeft = creative.getGuiLeft();
            guiTop = creative.getGuiTop();

        } else {
            return;
        }

        // Comfort is the top row, so no row offset is needed
        int potionX = guiLeft - 124;
        int potionY = guiTop + 8;

        // --- Hover area ---
        int hoverX = potionX;
        int hoverY = potionY - 10;
        int hoverWidth = 120;
        int hoverHeight = 32;

        if (mouseX >= hoverX && mouseX <= hoverX + hoverWidth &&
                mouseY >= hoverY && mouseY <= hoverY + hoverHeight) {

            List<String> tooltip = new ArrayList<>();

            tooltip.add(I18n.format("tooltip.chunkcomfort.level", comfort, maxComfort));

            int nextThreshold = PlayerComfortManager.getNextTierThreshold(comfortEffect.getAmplifier());
            if (nextThreshold >= 0) {
                tooltip.add(I18n.format("tooltip.chunkcomfort.level.next", nextThreshold - comfort));
            } else {
                tooltip.add(I18n.format("tooltip.chunkcomfort.level.maxed"));
            }

            tooltip.add("");

            tooltip.add(I18n.format("tooltip.chunkcomfort.positive.effects"));

            for (PlayerComfortManager.EffectEntry entry : effects) {
                String effect = PlayerComfortManager.getDisplayName(entry);

                // Unlocked but waiting on a statue candle: name the statue and candle that light it
                if (!StatueRegistry.isLit(entry.statue, litStatueSlots)) {
                    tooltip.add(I18n.format("tooltip.chunkcomfort.positive.effects.needs", effect,
                            entry.statue.statue.getLocalizedName(), StatueType.roman(entry.statue.slot + 1)));
                    continue;
                }

                tooltip.add(I18n.format("tooltip.chunkcomfort.positive.effects.plus") + " " + effect);
            }

            if (!negated.isEmpty()) {
                tooltip.add("");
                tooltip.add(I18n.format("tooltip.chunkcomfort.negative.effects"));
                tooltip.addAll(negated);
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
