package chunkcomfort.mixin.dynsurroundhud;

import chunkcomfort.api.ICanBeHidden;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.orecruncher.dshuds.hud.PotionHUD;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(PotionHUD.class)
public abstract class PotionHUDMixin {

    @Inject(method = "doRender", at = @At("HEAD"), remap = false)
    private void cancelHiddenPotions(RenderGameOverlayEvent.Pre event, CallbackInfo ci) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.POTION_ICONS) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        // Remove hidden potions from this.potions
        try {
            java.lang.reflect.Field potionsField = PotionHUD.class.getDeclaredField("potions");
            potionsField.setAccessible(true);
            Object potionsObj = potionsField.get(this);

            if (potionsObj instanceof java.util.Collection) {
                Collection<?> potions = (Collection<?>) potionsObj;
                potions.removeIf(potionInfo -> {
                    try {
                        java.lang.reflect.Method getEffect = potionInfo.getClass().getDeclaredMethod("getPotionEffect");
                        getEffect.setAccessible(true);
                        Object effect = getEffect.invoke(potionInfo);
                        return effect instanceof ICanBeHidden && ((ICanBeHidden) effect).chunkcomfort$isHidden();
                    } catch (Exception e) {
                        return false;
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}