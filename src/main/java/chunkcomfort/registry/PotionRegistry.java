package chunkcomfort.registry;

import chunkcomfort.potion.ComfortPotion;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class PotionRegistry {

    // Public static reference to your Comfort potion
    public static final ComfortPotion COMFORT = ComfortPotion.INSTANCE;

    // Call this in preInit
    public static void registerPotions() {
        ForgeRegistries.POTIONS.register(COMFORT);
    }
}