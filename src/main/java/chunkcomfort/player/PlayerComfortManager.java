package chunkcomfort.player;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.registry.PotionRegistry;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.util.*;

public class PlayerComfortManager {

    private static class EffectEntry {
        Potion potion;
        int amplifier;

        EffectEntry(Potion potion, int amplifier) {
            this.potion = potion;
            this.amplifier = amplifier;
        }
    }

    private static class ComfortTier {
        int comfort;
        List<EffectEntry> effects = new ArrayList<>();
    }

    private static final List<ComfortTier> TIERS = new ArrayList<>();

    public static void reloadConfig() {
        TIERS.clear();
        String[] config = ForgeConfigHandler.server.comfortEffects;

        for (String line : config) {
            try {
                String[] split = line.split(",", 2);
                int comfort = Integer.parseInt(split[0]);

                String effects = split[1].replace("[[", "").replace("]]", "");
                String[] entries = effects.split("\\],\\[");

                ComfortTier tier = new ComfortTier();
                tier.comfort = comfort;

                for (String entry : entries) {
                    String clean = entry.replace("[", "").replace("]", "");
                    String[] parts = clean.split(",");

                    String potionId = parts[0];
                    int amplifier = Integer.parseInt(parts[1]);

                    Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(potionId));
                    if (potion != null) tier.effects.add(new EffectEntry(potion, amplifier));
                }

                TIERS.add(tier);

            } catch (Exception e) {
                System.out.println("[ChunkComfort] Invalid comfortEffects config entry: " + line);
            }
        }

        TIERS.sort(Comparator.comparingInt(t -> t.comfort));
    }

    public static void applyComfortEffects(EntityPlayer player) {

        int comfort = AreaComfortCalculator.calculatePlayerComfort(player);

        ComfortTier activeTier = null;
        int tierIndex = 0;

        for (int i = 0; i < TIERS.size(); i++) {
            ComfortTier tier = TIERS.get(i);
            if (comfort >= tier.comfort) {
                activeTier = tier;
                tierIndex = i;
            }
        }

        if (activeTier == null) return;

        // Apply configured potion effects silently (no HUD)
        for (EffectEntry entry : activeTier.effects) {
            Potion potion = entry.potion;

            // Apply normally
            player.addPotionEffect(new PotionEffect(
                    potion,
                    20,           // duration in ticks
                    entry.amplifier,
                    true,          // ambient = subtle
                    false          // no in-world particles
            ));
        }

        // Apply custom "Comfort" potion for HUD visual indicator
        applyComfortPotion(player, tierIndex);
    }

    private static void applyComfortPotion(EntityPlayer player, int tierIndex) {
        if (PotionRegistry.COMFORT == null) return;

        PotionEffect current = player.getActivePotionEffect(PotionRegistry.COMFORT);
        int duration = 600; // ticks

        if (current == null || current.getAmplifier() != tierIndex || current.getDuration() < 200) {

            player.addPotionEffect(new PotionEffect(PotionRegistry.COMFORT, duration, tierIndex, false, true));
        }
    }
}