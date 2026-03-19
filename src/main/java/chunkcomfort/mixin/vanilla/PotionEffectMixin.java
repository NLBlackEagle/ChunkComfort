package chunkcomfort.mixin.vanilla;

import chunkcomfort.api.ICanBeHidden;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.potion.ComfortPotion;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionEffect.class)
public class PotionEffectMixin implements ICanBeHidden {

    @Unique
    private boolean chunkcomfort$isHidden = false;

    @Override
    public boolean chunkcomfort$isHidden() {
        return this.chunkcomfort$isHidden;
    }

    @Override
    public void chunkcomfort$setHidden(boolean hidden) {
        this.chunkcomfort$isHidden = hidden;
    }

    // Write hidden flag to NBT
    @Inject(method = "writeCustomPotionEffectToNBT", at = @At("TAIL"))
    private void chunkcomfort$writeHiddenNBT(NBTTagCompound nbt, CallbackInfoReturnable<NBTTagCompound> cir) {
        nbt.setBoolean("chunkcomfort$hidden", this.chunkcomfort$isHidden);
    }

    // Read hidden flag from NBT (static factory method)
    @Inject(method = "readCustomPotionEffectFromNBT", at = @At("RETURN"))
    private static void chunkcomfort$readHiddenNBT(NBTTagCompound nbt, CallbackInfoReturnable<PotionEffect> cir) {
        PotionEffect effect = cir.getReturnValue();
        if (effect != null && nbt.hasKey("chunkcomfort$hidden")) {
            ((ICanBeHidden) effect).chunkcomfort$setHidden(nbt.getBoolean("chunkcomfort$hidden"));
        }
    }

    // Stop rustic wine from increasing the duration of comfort and hidden potion effects
    @Inject(method = "combine(Lnet/minecraft/potion/PotionEffect;)V", at = @At("HEAD"), cancellable = true)
    private void chunkcomfort$preventWine(PotionEffect other, CallbackInfo ci) {
        if (!ForgeConfigHandler.server.stopRusticWine) return;

        PotionEffect self = (PotionEffect) (Object) this;

        boolean isHidden = false;
        if (self instanceof ICanBeHidden) {
            isHidden = ((ICanBeHidden) self).chunkcomfort$isHidden();
        }

        boolean isComfort = self.getPotion() instanceof ComfortPotion;

        if (isHidden || isComfort) {
            // Cancel any combine (duration increase) for hidden or Comfort potions
            ci.cancel();
        }
    }
}