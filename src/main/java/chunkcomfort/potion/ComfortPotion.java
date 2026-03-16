package chunkcomfort.potion;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;

import javax.annotation.Nonnull;

public class ComfortPotion extends PotionBase {

    public static final ComfortPotion INSTANCE = new ComfortPotion();

    public ComfortPotion() {super("comfort", false, 0x00000000);}

    @Override
    public boolean isReady(int duration, int amplifier) { return true; }

    @Override
    public boolean shouldRender(@Nonnull PotionEffect effect) {
        return true;
    }

    @Override
    public boolean shouldRenderHUD(@Nonnull PotionEffect effect) {
        return true;
    }

    @Override
    public boolean shouldRenderInvText(@Nonnull PotionEffect effect) {
        return true;
    }

    @Override
    public void performEffect(EntityLivingBase entity, int amplifier) {
    }
}