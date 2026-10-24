package chunkcomfort.mixin.potioncore;

import chunkcomfort.api.ICanBeHidden;
import chunkcomfort.registry.PotionRegistry;
import com.tmtravlr.potioncore.PotionCoreEventHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// REASON: Potion Core gives Potion Sickness once a player has too many beneficial effects,
// and the effects comfort grants (plus the Comfort potion itself) counted towards that limit.
// The only getActivePotionEffects call in onLivingUpdate is the sickness count, so leaving
// comfort's effects out of it there doesn't change anything else Potion Core does.
@Mixin(value = PotionCoreEventHandler.class, remap = false)
public abstract class PotionCoreEventHandlerMixin {

    @Redirect(
            method = "onLivingUpdate(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;getActivePotionEffects()Ljava/util/Collection;", remap = true)
    )
    private static Collection<PotionEffect> chunkcomfort$skipComfortEffects(EntityLivingBase entity) {
        List<PotionEffect> counted = new ArrayList<>();
        for (PotionEffect effect : entity.getActivePotionEffects()) {
            if (effect.getPotion() == PotionRegistry.COMFORT) continue;
            if (effect instanceof ICanBeHidden && ((ICanBeHidden) effect).chunkcomfort$isHidden()) continue;
            counted.add(effect);
        }
        return counted;
    }
}
