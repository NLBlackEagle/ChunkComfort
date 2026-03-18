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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerComfortManager {

    private static final Logger LOGGER = LogManager.getLogger("ChunkComfort");

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
                LOGGER.warn("Invalid comfortEffects config entry: {}", line);
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

        // Apply configured tier effects (hidden)
        for (EffectEntry entry : activeTier.effects) {

            PotionEffect effect = new PotionEffect(entry.potion, ForgeConfigHandler.server.comfortCheckInterval + 5, entry.amplifier, true, false);

            player.addPotionEffect(effect);

            PotionEffect active = player.getActivePotionEffect(entry.potion);
            if (active != null) {

                ((ICanBeHidden) active).chunkcomfort$setHidden(true);

                // Send hidden state to client
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

        // Apply COMFORT potion for HUD (always visible)
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

    public static List<String> getEffectsForTier(int tierIndex) {
        if (tierIndex < 0 || tierIndex >= TIERS.size()) return Collections.emptyList();

        ComfortTier tier = TIERS.get(tierIndex);

        return tier.effects.stream()
                .map(entry -> I18n.format(entry.potion.getName()) + " " + (entry.amplifier + 1))
                .collect(Collectors.toList());
    }
}