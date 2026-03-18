package chunkcomfort.mixin.vanilla;

import chunkcomfort.api.ICanBeHidden;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.stream.Collectors;

// Some mods ignore ambience = true in the potion effect constructor so we forcefully hide ICanBeHidden potions here

@Mixin(GuiIngame.class)
public abstract class GuiIngameMixin {

    @Inject(method = "renderPotionEffects", at = @At("HEAD"), cancellable = true)
    private void cancelHiddenPotions(ScaledResolution scaledRes, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        // Check if all potions are hidden
        List<PotionEffect> visibleEffects = mc.player.getActivePotionEffects().stream()
                .filter(effect -> !(effect instanceof ICanBeHidden && ((ICanBeHidden) effect).chunkcomfort$isHidden()))
                .collect(Collectors.toList());

        if (visibleEffects.isEmpty()) {
            // Cancel vanilla render if nothing should be shown
            ci.cancel();
        }
    }
}