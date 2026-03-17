package chunkcomfort.mixin.vanilla;

import chunkcomfort.api.ICanBeHidden;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    @Inject(method = "readCustomPotionEffectFromNBT", at = @At("RETURN"), cancellable = true)
    private static void chunkcomfort$readHiddenNBT(NBTTagCompound nbt, CallbackInfoReturnable<PotionEffect> cir) {
        PotionEffect effect = cir.getReturnValue();
        if (effect != null && nbt.hasKey("chunkcomfort$hidden")) {
            ((ICanBeHidden) effect).chunkcomfort$setHidden(nbt.getBoolean("chunkcomfort$hidden"));
        }
    }
}