package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import chunkcomfort.statue.BlockStatue;
import chunkcomfort.statue.StatueType;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Which effects each statue candle spot controls, and which items count as candles.
 * Every candle spot on a statue takes a candle; only the spots that a comfortEffects or
 * Comfort Potion Blacklist entry points at have an effect (and a flame while a candle is in them).
 *
 * An entry that names a statue and candle spot is "statue-gated":
 *  - a comfort tier effect only applies while that candle is lit
 *  - a Comfort Potion Blacklist potion is only negated while that candle is lit
 * Entries without a statue work as before.
 */
public class StatueRegistry {

    /** The statue candle a comfort effect or negation needs. */
    public static class Requirement {
        public final Block statue;
        public final int slot; // 0-based

        Requirement(Block statue, int slot) {
            this.statue = statue;
            this.slot = slot;
        }
    }

    /** What one candle spot does: a comfort effect (amplifier >= 0) or a negation (amplifier -1). */
    public static class SlotEffect {
        @Nullable public final Potion potion; // null = the potion id in the config was not found
        public final int comfort;
        public final int amplifier;

        public SlotEffect(@Nullable Potion potion, int comfort, int amplifier) {
            this.potion = potion;
            this.comfort = comfort;
            this.amplifier = amplifier;
        }

        public boolean isNegation() {
            return amplifier < 0;
        }
    }

    // Statue -> (slot index, 0-based -> effects), only the slots that have an effect
    private static final Map<Block, Map<Integer, List<SlotEffect>>> SLOTS = new HashMap<>();
    private static final Set<Item> CANDLE_ITEMS = new HashSet<>();

    /** Called before comfortEffects and the blacklist are parsed; they add their statue entries again. */
    public static void clearSlots() {
        SLOTS.clear();
    }

    /**
     * Parses the optional "<statue>,<candle_spot>" part of a comfortEffects / blacklist entry.
     * The statue can be written with or without "chunkcomfort:", spots are 1-19.
     * Returns null (and logs why) when the statue or spot is invalid.
     */
    @Nullable
    public static Requirement parseRequirement(String statueId, String spot, String line) {
        statueId = statueId.trim();
        if (!statueId.contains(":")) statueId = ChunkComfort.MODID + ":" + statueId;

        Block block = Block.getBlockFromName(statueId);
        if (!(block instanceof BlockStatue)) {
            ChunkComfort.LOGGER.warn("Unknown statue {} in config entry: {}", statueId, line);
            return null;
        }

        int number;
        try {
            number = Integer.parseInt(spot.trim());
        } catch (NumberFormatException e) {
            ChunkComfort.LOGGER.warn("Candle spot {} is not a number in config entry: {}", spot.trim(), line);
            return null;
        }
        if (number < 1 || number > StatueType.MAX_SLOTS) {
            ChunkComfort.LOGGER.warn("Candle spot {} is not between 1 and {} in config entry: {}", number, StatueType.MAX_SLOTS, line);
            return null;
        }
        return new Requirement(block, number - 1);
    }

    public static void addSlotEffect(Requirement requirement, SlotEffect effect) {
        SLOTS.computeIfAbsent(requirement.statue, k -> new TreeMap<>())
                .computeIfAbsent(requirement.slot, k -> new ArrayList<>())
                .add(effect);
    }

    /** Key for a lit candle (statue + slot), as collected from the statues in a comfort zone. */
    public static int slotKey(Block statue, int slot) {
        return Block.getIdFromBlock(statue) * 32 + slot;
    }

    /** True if the entry needs no statue, or its candle is lit in the comfort zone. */
    public static boolean isLit(@Nullable Requirement requirement, Set<Integer> litSlots) {
        return requirement == null || litSlots.contains(slotKey(requirement.statue, requirement.slot));
    }

    public static void reloadCandles(String[] items) {
        CANDLE_ITEMS.clear();
        if (items == null) return;

        for (String id : items) {
            if (id == null || id.trim().isEmpty()) continue;
            Item item = Item.getByNameOrId(id.trim());
            if (item != null) CANDLE_ITEMS.add(item);
        }
    }

    public static boolean isCandle(ItemStack stack) {
        return !stack.isEmpty() && CANDLE_ITEMS.contains(stack.getItem());
    }

    /** The slots (0-based, in order) that have an effect on this statue. */
    public static Set<Integer> getEffectSlots(Block statue) {
        Map<Integer, List<SlotEffect>> slots = SLOTS.get(statue);
        return slots == null ? Collections.emptySet() : slots.keySet();
    }

    /** True if a candle in this slot does something (and burns with a flame). */
    public static boolean isEffectSlot(Block statue, int slot) {
        return getEffectSlots(statue).contains(slot);
    }

    public static List<SlotEffect> getSlotEffects(Block statue, int slot) {
        Map<Integer, List<SlotEffect>> slots = SLOTS.get(statue);
        if (slots == null) return Collections.emptyList();
        return slots.getOrDefault(slot, Collections.emptyList());
    }
}
