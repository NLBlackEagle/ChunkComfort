package chunkcomfort.mixin.vanilla;

import chunkcomfort.api.ICanBeHidden;
import chunkcomfort.potion.ComfortPotion;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * No potion swirls from effects ChunkComfort gives.
 *
 * REASON: comfort effects keep showParticles = true (with Beacon Style) because in 1.12 that flag
 * also decides whether the effect shows on the HUD. So instead of turning it off, they are left out
 * of the particle colour the server syncs to clients. With nothing left the colour is 0, and the
 * client spawns no particles; a potion the player drank still gives its normal swirls.
 */
@Mixin(EntityLivingBase.class)
public abstract class PotionParticlesMixin {

    @Redirect(method = "updatePotionMetadata",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/potion/PotionUtils;getPotionColorFromEffectList(Ljava/util/Collection;)I"))
    private int chunkcomfort$colorWithoutComfort(Collection<PotionEffect> effects) {
        return PotionUtils.getPotionColorFromEffectList(chunkcomfort$withoutComfort(effects));
    }

    @Redirect(method = "updatePotionMetadata",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;areAllPotionsAmbient(Ljava/util/Collection;)Z"))
    private boolean chunkcomfort$ambientWithoutComfort(Collection<PotionEffect> effects) {
        return EntityLivingBase.areAllPotionsAmbient(chunkcomfort$withoutComfort(effects));
    }

    private static List<PotionEffect> chunkcomfort$withoutComfort(Collection<PotionEffect> effects) {
        List<PotionEffect> out = new ArrayList<>(effects.size());
        for (PotionEffect effect : effects) {
            boolean comfort = effect.getPotion() instanceof ComfortPotion
                    || (effect instanceof ICanBeHidden && ((ICanBeHidden) effect).chunkcomfort$isHidden());
            if (!comfort) out.add(effect);
        }
        return out;
    }
}
