package chunkcomfort.potion;

import chunkcomfort.handlers.ForgeConfigHandler;
import chunkcomfort.player.PlayerComfortStorage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

import java.util.*;

public class PotionComfort {

    private static final TreeMap<Integer, List<PotionEffect>> tiers = new TreeMap<>();

    public static void loadConfig() {

        tiers.clear();

        for (String line : ForgeConfigHandler.server.comfortEffects) {

            try {

                String[] parts = line.split(",", 2);

                int threshold = Integer.parseInt(parts[0].trim());

                List<PotionEffect> effects = new ArrayList<>();

                String effectsPart = parts[1].trim();

                effectsPart = effectsPart.substring(1, effectsPart.length() - 1);

                String[] potionPairs = effectsPart.split("\\],\\[");

                for (String pair : potionPairs) {

                    pair = pair.replace("[", "").replace("]", "");

                    String[] p = pair.split(",");

                    Potion potion = Potion.getPotionFromResourceLocation(p[0].trim());

                    int amp = Integer.parseInt(p[1].trim());

                    if (potion != null) {
                        effects.add(new PotionEffect(potion, 200, amp, false, true));
                    }
                }

                tiers.put(threshold, effects);

            } catch (Exception ignored) {}
        }
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new PotionComfort());
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {

        EntityPlayer player = event.player;

        float comfort = PlayerComfortStorage.getPlayerComfort(player);

        Integer tier = tiers.floorKey((int) comfort);

        if (tier == null) return;

        List<PotionEffect> effects = tiers.get(tier);

        for (PotionEffect effect : effects) {
            player.addPotionEffect(new PotionEffect(effect));
        }
    }
}