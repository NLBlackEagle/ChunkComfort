package chunkcomfort.api;

import net.minecraft.potion.PotionEffect;

public interface ICanBeHidden {

    boolean chunkcomfort$isHidden();

    void chunkcomfort$setHidden(boolean hidden);

    /**
     * Whether an effect should be left out of the effect lists (HUD, inventory).
     * Comfort effects are always marked hidden (that also stops Rustic wine extending them), but
     * with Beacon Style on the server gives them particles, and then they are shown like beacon effects.
     */
    static boolean isHiddenFromDisplay(PotionEffect effect) {
        return effect instanceof ICanBeHidden && ((ICanBeHidden) effect).chunkcomfort$isHidden()
                && !effect.doesShowParticles();
    }

}
