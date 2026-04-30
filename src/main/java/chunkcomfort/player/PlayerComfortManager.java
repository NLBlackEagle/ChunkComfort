package chunkcomfort.player;

import chunkcomfort.api.ICanBeHidden;
import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.network.NetworkHandler;
import chunkcomfort.network.PacketSyncHiddenEffect;
import chunkcomfort.registry.PotionRegistry;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerComfortManager {

    private static final Logger LOGGER = LogManager.getLogger("ChunkComfort");
    private static final Map<UUID, Integer> COMFORT_CACHE = new HashMap<>();

    public static void setCachedComfort(UUID id, int value) {
        COMFORT_CACHE.put(id, value);
    }

    public static void clearComfortCache() {
        COMFORT_CACHE.clear();
    }

    public static int getCachedComfort(EntityPlayer player) {
        return COMFORT_CACHE.getOrDefault(player.getUniqueID(), 0);
    }

    private static class EffectEntry {
        final Potion potion;
        final int amplifier;

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
                LOGGER.warn("Invalid comfortEffects config entry: {}", line);
            }
        }

        TIERS.sort(Comparator.comparingInt(t -> t.comfort));
    }


    public static void applyComfortEffects(EntityPlayer player, int comfort) {
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

        for (EffectEntry entry : activeTier.effects) {
            PotionEffect effect = new PotionEffect(
                    entry.potion,
                    ForgeConfigHandler.server.comfortCheckInterval * 2,
                    entry.amplifier,
                    true,
                    false
            );

            player.addPotionEffect(effect);

            PotionEffect active = player.getActivePotionEffect(entry.potion);
            if (active != null) {
                ((ICanBeHidden) active).chunkcomfort$setHidden(true);

                if (!player.world.isRemote) {
                    NetworkHandler.INSTANCE.sendTo(
                            new PacketSyncHiddenEffect(
                                    player.getEntityId(),
                                    Potion.getIdFromPotion(entry.potion),
                                    true
                            ),
                            (EntityPlayerMP) player
                    );
                }
            }
        }

        applyComfortPotion(player, tierIndex);
    }

    private static void applyComfortPotion(EntityPlayer player, int tierIndex) {
        if (PotionRegistry.COMFORT == null) return;

        PotionEffect current = player.getActivePotionEffect(PotionRegistry.COMFORT);
        int duration = 600;

        if (current == null || current.getAmplifier() != tierIndex || current.getDuration() < 220) {
            player.removePotionEffect(PotionRegistry.COMFORT);
            player.addPotionEffect(new PotionEffect(PotionRegistry.COMFORT, duration, tierIndex, false, true));
        }
    }

    /** Returns the comfort threshold of the next tier above tierIndex, or -1 if already at max. */
    public static int getNextTierThreshold(int tierIndex) {
        int nextIndex = tierIndex + 1;
        if (nextIndex >= TIERS.size()) return -1;
        return TIERS.get(nextIndex).comfort;
    }

    public static List<String> getEffectsForTier(int tierIndex) {
        if (tierIndex < 0 || tierIndex >= TIERS.size()) return Collections.emptyList();

        return TIERS.get(tierIndex).effects.stream()
                .map(entry -> I18n.format(entry.potion.getName()) + " " + (entry.amplifier + 1))
                .collect(Collectors.toList());
    }
}
