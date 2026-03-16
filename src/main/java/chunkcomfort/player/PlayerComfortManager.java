package chunkcomfort.player;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.config.ForgeConfigHandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    // === CONFIG PARSER WITH DEBUG ===
    public static void reloadConfig() {

        TIERS.clear();
        System.out.println("[ChunkComfort] Reloading comfort tiers...");

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

                    if (potion != null) {
                        tier.effects.add(new EffectEntry(potion, amplifier));
                        System.out.println("[ChunkComfort] Parsed effect: " + potionId + " amplifier: " + amplifier);
                    } else {
                        System.out.println("[ChunkComfort] WARNING: Potion not found: " + potionId);
                    }
                }

                TIERS.add(tier);
                System.out.println("[ChunkComfort] Loaded comfort tier: " + comfort + " with " + tier.effects.size() + " effects");

            } catch (Exception e) {
                System.out.println("[ChunkComfort] Invalid comfortEffects config entry: " + line);
                e.printStackTrace();
            }
        }

        TIERS.sort(Comparator.comparingInt(t -> t.comfort));
        System.out.println("[ChunkComfort] Total comfort tiers loaded: " + TIERS.size());
    }

    // === APPLY EFFECTS WITH DEBUG ===
    public static void applyComfortEffects(EntityPlayer player) {

        int comfort = AreaComfortCalculator.calculatePlayerComfort(player);
        System.out.println("[ChunkComfort] Player " + player.getName() + " comfort: " + comfort + " | tiers loaded: " + TIERS.size());

        ComfortTier activeTier = null;
        for (ComfortTier tier : TIERS) {
            if (comfort >= tier.comfort) {
                activeTier = tier;
            }
        }

        if (activeTier == null) {
            System.out.println("[ChunkComfort] No active tier for comfort " + comfort);
            return;
        }

        System.out.println("[ChunkComfort] Applying comfort tier: " + activeTier.comfort + " with " + activeTier.effects.size() + " effects");

        for (EffectEntry entry : activeTier.effects) {
            player.addPotionEffect(new PotionEffect(
                    entry.potion,
                    220,
                    entry.amplifier,
                    true,
                    false
            ));
            System.out.println("[ChunkComfort] Applied potion " + entry.potion.getRegistryName() + " amplifier: " + entry.amplifier);
        }
    }
}