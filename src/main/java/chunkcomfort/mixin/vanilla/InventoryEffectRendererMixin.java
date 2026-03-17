package chunkcomfort.mixin.vanilla;

import chunkcomfort.api.ICanBeHidden;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.potion.PotionEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Collection;
import java.util.stream.Collectors;

@Mixin(InventoryEffectRenderer.class)
public abstract class InventoryEffectRendererMixin {

    private static final Logger LOGGER = LogManager.getLogger("ChunkComfort");

    // Toggle this to true to hide all potions (test mode)
    private static final boolean HIDE_ALL_POTIONS = false;

    /**
     * Modifies the local variable 'collection' in drawActivePotionEffects to filter hidden potions.
     * If HIDE_ALL_POTIONS is true, all potions are filtered out for testing.
     */
    @ModifyVariable(
            method = "drawActivePotionEffects",
            at = @At("STORE"),
            ordinal = 0 // the first Collection<PotionEffect> variable stored
    )
    private Collection<PotionEffect> filterHiddenPotions(Collection<PotionEffect> effects) {

        LOGGER.info("ChunkComfort: drawActivePotionEffects triggered, total effects = {}", effects.size());

        effects.forEach(effect -> {
            boolean hidden = effect instanceof ICanBeHidden && ((ICanBeHidden) effect).chunkcomfort$isHidden();
            LOGGER.info("ChunkComfort: effect {} hidden={}", effect.getPotion().getRegistryName(), hidden);
        });

        // Either hide all potions (test mode) or just hidden ones
        Collection<PotionEffect> visible = effects.stream()
                .filter(effect -> !HIDE_ALL_POTIONS && !(effect instanceof ICanBeHidden && ((ICanBeHidden) effect).chunkcomfort$isHidden()))
                .collect(Collectors.toList());

        if (HIDE_ALL_POTIONS) {
            LOGGER.info("ChunkComfort: cancelling all potion rendering for test");
        } else {
            LOGGER.info("ChunkComfort: visible effects count = {}", visible.size());
        }

        return visible;
    }
}