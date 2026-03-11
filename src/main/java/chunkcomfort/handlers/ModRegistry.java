package chunkcomfort.handlers;

import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import chunkcomfort.ChunkComfort;
import chunkcomfort.potion.PotionExample;


@Mod.EventBusSubscriber(modid = ChunkComfort.MODID)
public class ModRegistry {

        public static PotionType examplePotion = new PotionType("example", new PotionEffect(PotionExample.INSTANCE)).setRegistryName(new ResourceLocation(ChunkComfort.MODID, "example"));

        public static void init() {

        }

        @SubscribeEvent
        public static void registerPotionEvent(RegistryEvent.Register<Potion> event) {
                event.getRegistry().register(PotionExample.INSTANCE);
        }

        @SubscribeEvent
        public static void registerPotionTypeEvent(RegistryEvent.Register<PotionType> event) {
                event.getRegistry().register(examplePotion);
                PotionHelper.addMix(PotionTypes.THICK, Items.DIAMOND, ModRegistry.examplePotion);
        }
}