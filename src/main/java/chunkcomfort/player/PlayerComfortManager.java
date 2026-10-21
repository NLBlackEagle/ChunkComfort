package chunkcomfort.player;

import chunkcomfort.api.ICanBeHidden;
import chunkcomfort.api.IRefreshableEffect;
import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.network.NetworkHandler;
import chunkcomfort.network.PacketStatueStatus;
import chunkcomfort.network.PacketSyncHiddenEffect;
import chunkcomfort.registry.PotionRegistry;
import chunkcomfort.registry.StatueRegistry;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityEffect;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

import java.util.*;

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

    /** One comfort tier effect, and the statue candle it needs (null = always on once unlocked). */
    public static class EffectEntry {
        public final Potion potion;
        public final int amplifier;
        @Nullable public final StatueRegistry.Requirement statue;

        EffectEntry(Potion potion, int amplifier, @Nullable StatueRegistry.Requirement statue) {
            this.potion = potion;
            this.amplifier = amplifier;
            this.statue = statue;
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

                    String potionId = parts[0].trim();
                    int amplifier = Integer.parseInt(parts[1].trim());

                    // Optional statue + candle spot: the effect only runs while that candle is lit
                    StatueRegistry.Requirement statue = null;
                    if (parts.length >= 4) {
                        statue = StatueRegistry.parseRequirement(parts[2], parts[3], line);
                    } else if (parts.length == 3) {
                        LOGGER.warn("comfortEffects group needs both a statue and a candle spot: [{}] in {}", clean, line);
                    }

                    Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(potionId));
                    if (potion != null) {
                        tier.effects.add(new EffectEntry(potion, amplifier, statue));
                    } else if (statue != null) {
                        LOGGER.warn("Unknown potion {} in comfortEffects entry: {}", potionId, line);
                    }
                    // Statue spots show the effect (or "not found") even when the potion is missing
                    if (statue != null) StatueRegistry.addSlotEffect(statue, new StatueRegistry.SlotEffect(potion, comfort, amplifier));
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

        Set<Integer> litStatueSlots = AreaComfortCalculator.getCache(player).litStatueSlots;

        // Tell the client which statue candles are lit and the player's comfort, so the Comfort
        // tooltip and statue hints can mark effects that are locked or still waiting on a candle
        if (player instanceof EntityPlayerMP) {
            NetworkHandler.INSTANCE.sendTo(new PacketStatueStatus(litStatueSlots, comfort), (EntityPlayerMP) player);
        }

        // The comfort effects that should be running now (only lit ones)
        Map<Potion, EffectEntry> wanted = new HashMap<>();
        if (activeTier != null) {
            for (EffectEntry entry : getStackedEffects(tierIndex, litStatueSlots)) {
                // Effects listed on a statue only activate while their candle is lit
                if (StatueRegistry.isLit(entry.statue, litStatueSlots)) wanted.put(entry.potion, entry);
            }
        }

        // REASON: comfort effects used to be re-added with a short duration and left to run out,
        // so their timer counted down into the red and they lingered after leaving the zone.
        // Now they are topped up every check and removed as soon as they no longer apply.
        // Only effects comfort added (marked hidden) are touched, never a potion the player drank.
        removeComfortEffects(player, wanted.keySet());

        for (EffectEntry entry : wanted.values()) {
            PotionEffect active = player.getActivePotionEffect(entry.potion);

            if (active != null && isComfortEffect(active) && active.getAmplifier() == entry.amplifier) {
                refreshDuration(player, active);
                continue;
            }
            // The player's own potion of this type is running, leave it alone until it ends
            if (active != null && !isComfortEffect(active)) continue;
            // Our effect at another level (tier changed): replace it
            if (active != null) player.removePotionEffect(entry.potion);

            player.addPotionEffect(new PotionEffect(
                    entry.potion,
                    effectDuration(),
                    entry.amplifier,
                    true,
                    // Beacon Style: particles make the client show it like a beacon effect
                    ForgeConfigHandler.display.beaconStyle
            ));

            PotionEffect added = player.getActivePotionEffect(entry.potion);
            if (added != null) {
                ((ICanBeHidden) added).chunkcomfort$setHidden(true);

                if (player instanceof EntityPlayerMP) {
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

        if (activeTier == null) return;

        applyComfortPotion(player, tierIndex);
    }

    /** Duration given to comfort effects; topped up every check, so it never gets close to running out. */
    private static int effectDuration() {
        return Math.max(600, ForgeConfigHandler.server.comfortCheckInterval * 2);
    }

    /** An effect comfort added (comfort marks its effects hidden). */
    private static boolean isComfortEffect(PotionEffect effect) {
        return effect instanceof ICanBeHidden && ((ICanBeHidden) effect).chunkcomfort$isHidden();
    }

    /** Removes the comfort-added effects that are not in keep (all of them when keep is empty). */
    private static void removeComfortEffects(EntityPlayer player, Set<Potion> keep) {
        List<Potion> stale = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getPotion() == PotionRegistry.COMFORT) continue;
            if (isComfortEffect(effect) && !keep.contains(effect.getPotion())) stale.add(effect.getPotion());
        }
        // Removing sends the removal to the client, so the icon disappears right away
        for (Potion potion : stale) player.removePotionEffect(potion);
    }

    /**
     * Resets an effect's duration in place and tells the client. Removing and re-adding it would
     * also reset the effect, e.g. Health Boost and Absorption would take their hearts away each time.
     */
    private static void refreshDuration(EntityPlayer player, PotionEffect effect) {
        ((IRefreshableEffect) effect).chunkcomfort$setDuration(effectDuration());
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).connection.sendPacket(new SPacketEntityEffect(player.getEntityId(), effect));
        }
    }

    private static void applyComfortPotion(EntityPlayer player, int tierIndex) {
        if (PotionRegistry.COMFORT == null) return;

        PotionEffect current = player.getActivePotionEffect(PotionRegistry.COMFORT);

        // Same tier: just top it up, so its timer never runs low
        if (current != null && current.getAmplifier() == tierIndex) {
            refreshDuration(player, current);
            return;
        }

        // Gaining comfort at all counts too, so tier 0 gets its first-milestone toast
        boolean tierUp = current == null || tierIndex > current.getAmplifier();

        player.removePotionEffect(PotionRegistry.COMFORT);
        player.addPotionEffect(new PotionEffect(PotionRegistry.COMFORT, effectDuration(), tierIndex, false, true));

        if (tierUp && !player.world.isRemote && shouldShowTierUpToast(player, tierIndex)) {
            NetworkHandler.INSTANCE.sendTo(
                    new chunkcomfort.network.PacketTierUp(tierIndex),
                    (EntityPlayerMP) player
            );
        }
    }

    // REASON: the Comfort potion is removed when the player leaves their comfort zone,
    // so walking back in counted as a tier up and showed the toast again every time.
    // The toast is now a one-time milestone: the highest tier a player has seen a toast
    // for is stored in their persisted NBT (survives death and relogging), and only
    // passing that tier shows a new toast.
    private static final String HIGHEST_TOAST_TIER_KEY = "chunkcomfort_highestToastTier";

    private static boolean shouldShowTierUpToast(EntityPlayer player, int tierIndex) {
        NBTTagCompound entityData = player.getEntityData();
        NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        int highestSeen = persisted.hasKey(HIGHEST_TOAST_TIER_KEY) ? persisted.getInteger(HIGHEST_TOAST_TIER_KEY) : -1;
        if (tierIndex <= highestSeen) return false;

        persisted.setInteger(HIGHEST_TOAST_TIER_KEY, tierIndex);
        // getCompoundTag returns a detached tag when missing, so write it back
        entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        return true;
    }

    /**
     * The first comfort tier (0 = Comfort I) whose threshold reaches this comfort, or -1 if no
     * tier does. Used to show "Unlocked at Comfort III" instead of raw comfort points.
     */
    public static int getTierForComfort(int comfort) {
        for (int i = 0; i < TIERS.size(); i++) {
            if (TIERS.get(i).comfort >= comfort) return i;
        }
        return -1;
    }

    public static boolean isTierThreshold(int comfort) {
        for (ComfortTier tier : TIERS) {
            if (tier.comfort == comfort) return true;
        }
        return false;
    }

    /** Returns the comfort threshold of the next tier above tierIndex, or -1 if already at max. */
    public static int getNextTierThreshold(int tierIndex) {
        int nextIndex = tierIndex + 1;
        if (nextIndex >= TIERS.size()) return -1;
        return TIERS.get(nextIndex).comfort;
    }

    // REASON: only the highest reached tier's effects used to apply, so reaching a new
    // tier replaced every earlier effect. Tiers now stack: every effect from tier 0 up to
    // tierIndex is active. A potion listed in several tiers keeps its highest amplifier
    // (e.g. Regeneration II replaces Regeneration I) and its first-unlocked position.
    // Entries whose statue candle is lit win over unlit ones, so an unlit Regeneration II
    // doesn't hide a lit (or ungated) Regeneration I.
    public static List<EffectEntry> getStackedEffects(int tierIndex, Set<Integer> litStatueSlots) {
        if (tierIndex < 0 || tierIndex >= TIERS.size()) return Collections.emptyList();

        Map<Potion, EffectEntry> stacked = new LinkedHashMap<>();
        for (int i = 0; i <= tierIndex; i++) {
            for (EffectEntry entry : TIERS.get(i).effects) {
                EffectEntry existing = stacked.get(entry.potion);
                boolean lit = StatueRegistry.isLit(entry.statue, litStatueSlots);
                boolean existingLit = existing != null && StatueRegistry.isLit(existing.statue, litStatueSlots);

                if (existing == null || (lit && !existingLit)
                        || (lit == existingLit && entry.amplifier > existing.amplifier)) {
                    stacked.put(entry.potion, entry);
                }
            }
        }
        return new ArrayList<>(stacked.values());
    }

    /** Display name of a comfort effect, e.g. "Haste 1". */
    public static String getDisplayName(EffectEntry entry) {
        return I18n.format(entry.potion.getName()) + " " + (entry.amplifier + 1);
    }
}
