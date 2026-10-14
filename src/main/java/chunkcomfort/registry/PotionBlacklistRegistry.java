package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PotionBlacklistRegistry {

    /** One negated potion: from which comfort it is negated, and the statue candle it needs (if any). */
    public static class Entry {
        public final int comfort;
        public final String potionId;
        @Nullable public final StatueRegistry.Requirement statue;

        Entry(int comfort, String potionId, @Nullable StatueRegistry.Requirement statue) {
            this.comfort = comfort;
            this.potionId = potionId;
            this.statue = statue;
        }
    }

    // One group: [<potion>] or [<potion>,<statue>,<candle_spot>]
    private static final Pattern GROUP = Pattern.compile("\\[([^\\[\\]]*)\\]");

    private static final List<Entry> entries = new ArrayList<>();

    public static List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * True if the potion is negated at this comfort: an unlocked entry for it either needs
     * no statue, or its statue candle is lit in the comfort zone.
     */
    public static boolean isNegated(int comfort, String potionId, Set<Integer> litSlots) {
        for (Entry entry : entries) {
            if (comfort >= entry.comfort && entry.potionId.equals(potionId)
                    && StatueRegistry.isLit(entry.statue, litSlots)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Format: <comfort>,[[<potion>],[<potion>,<statue>,<candle_spot>]]
     * The old format <comfort>,[<potion>,<potion>] still works (no statue needed).
     * Call StatueRegistry.clearSlots() first; statue entries are added to it here.
     */
    public static void reload(String[] lines) {

        entries.clear();

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;

            try {
                String[] parts = line.split(",", 2);
                int comfort = Integer.parseInt(parts[0].trim());
                String rawList = parts[1].trim();

                if (!rawList.contains("[[")) {
                    // Old format: one list of potions, none of them statue-gated
                    rawList = rawList.replace("[", "").replace("]", "");
                    for (String potion : rawList.split(",")) {
                        add(comfort, potion.trim(), null, line);
                    }
                    continue;
                }

                Matcher group = GROUP.matcher(rawList);
                while (group.find()) {
                    String[] fields = group.group(1).split(",");
                    StatueRegistry.Requirement statue = null;

                    if (fields.length >= 3) {
                        statue = StatueRegistry.parseRequirement(fields[1], fields[2], line);
                    } else if (fields.length == 2) {
                        ChunkComfort.LOGGER.warn("Blacklist group needs both a statue and a candle spot: [{}] in {}", group.group(1), line);
                    }
                    add(comfort, fields[0].trim(), statue, line);
                }

            } catch (Exception e) {
                ChunkComfort.LOGGER.error("Invalid potion blacklist entry: " + line);
            }
        }
    }

    private static void add(int comfort, String potionId, @Nullable StatueRegistry.Requirement statue, String line) {
        if (potionId.isEmpty()) return;
        entries.add(new Entry(comfort, potionId, statue));

        if (statue != null) {
            Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(potionId));
            if (potion == null) ChunkComfort.LOGGER.warn("Unknown potion {} in Comfort Potion Blacklist entry: {}", potionId, line);
            StatueRegistry.addSlotEffect(statue, new StatueRegistry.SlotEffect(potion, comfort, -1));
        }
    }
}
