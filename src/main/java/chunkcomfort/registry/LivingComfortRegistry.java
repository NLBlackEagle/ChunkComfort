package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import java.util.*;

public class LivingComfortRegistry {


    public static boolean hasEntries(ResourceLocation id) {
        List<LivingComfortEntry> list = ENTITY_MAP.get(id);
        return list != null && !list.isEmpty();
    }

    public static LivingComfortEntry getDefaultEntry(ResourceLocation id) {

        List<LivingComfortEntry> list = ENTITY_MAP.get(id);
        if (list == null) return null;

        LivingComfortEntry fallback = null;

        for (LivingComfortEntry entry : list) {
            if (entry.alwaysMatch) fallback = entry;
            else return entry; // first specific entry
        }

        return fallback;
    }

    /**
     * Looks up the entry for an entity ID + flat context string (e.g. "SkullType:1").
     *
     * This is the tooltip path. It is a direct CONTEXT_MAP lookup — O(1), no NBT
     * parsing, no coercion. The context string comes from CustomSpawnEggRegistry
     * and was normalised to plain "Key:Value" at parse time, matching the keys
     * stored in CONTEXT_MAP during reload().
     *
     * Falls back to getDefaultEntry() when:
     * - context is null (simple items: dragon skull, Lycanites eggs)
     * - no context map entry exists (complex NBT entries like {OwnerUUID:'*-*'})
     *
     * REASON: all previous approaches (synthetic NBT, getEntryMatchingContext with
     * matches()) failed because they had to reconstruct a matching path designed for
     * real entity NBT. This method sidesteps that entirely — the index is built at
     * reload time so the tooltip just does a plain string lookup.
     */
    public static LivingComfortEntry getEntryForContext(ResourceLocation id, String nbtContext) {
        if (nbtContext == null || nbtContext.isEmpty()) return getDefaultEntry(id);

        // Normalise: strip byte suffix so "SkullType:1b" matches "SkullType:1"
        String normalised = nbtContext.trim();
        if (normalised.contains(":")) {
            String[] kv = normalised.split(":", 2);
            String val = kv[1].trim();
            if (val.endsWith("b") || val.endsWith("B")) {
                try {
                    Integer.parseInt(val.substring(0, val.length() - 1));
                    normalised = kv[0].trim() + ":" + val.substring(0, val.length() - 1);
                } catch (NumberFormatException ignored) {}
            }
        }

        String lookupKey = id.toString() + "|" + normalised;
        LivingComfortEntry direct = CONTEXT_MAP.get(lookupKey);

        if (direct != null) return direct;
        return getDefaultEntry(id);
    }

    // =====================================================
    // Entry
    // =====================================================

    public static class LivingComfortEntry {

        public final ResourceLocation entityId;
        public final int value;
        public final String group;
        public final int limit;

        public final List<Map<String, NBTCondition>> nbtGroups;

        private final boolean alwaysMatch;

        public LivingComfortEntry(ResourceLocation entityId,
                                  int value,
                                  String group,
                                  int limit,
                                  String nbtRaw) {
            this.entityId = entityId;
            this.value = value;
            this.group = group;
            this.limit = limit;
            this.nbtGroups = parseNBT(nbtRaw);
            this.alwaysMatch = isAlwaysMatch(nbtGroups);
        }

        public boolean matches(NBTTagCompound nbt) {
            if (alwaysMatch) return true;

            for (Map<String, NBTCondition> group : nbtGroups) {
                // REASON: removed the early `if (group.isEmpty()) return false` that was here.
                // An empty group inside a multi-group OR expression should simply not match
                // (it contributes nothing), but it should not short-circuit the whole check.
                // The real guard is isAlwaysMatch() at construction time.
                if (!group.isEmpty() && matchesGroup(nbt, group)) {
                    return true;
                }
            }
            return false;
        }
    }

    // =====================================================
    // NBT Condition
    // =====================================================

    private static class NBTCondition {

        enum Type { BYTE, INT, STRING, EXISTS }

        final Type type;
        final Object value;
        boolean negate = false;

        NBTCondition(Type type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    // =====================================================
    // Registry Storage
    // =====================================================

    public static final Map<ResourceLocation, List<LivingComfortEntry>> ENTITY_MAP = new HashMap<>();

    // -------------------------------------------------------
    // CONTEXT_MAP: (entityId + "|" + nbtContext) -> entry
    //
    // REASON: the tooltip has no real entity NBT to check — it only has the item
    // in hand and the flat "Key:Value" string stored in CustomSpawnEggRegistry
    // (e.g. "SkullType:1"). Rather than building synthetic NBT and running it
    // through matches() (which broke on byte/int coercion and was fragile), we
    // pre-build a direct lookup at reload time.
    //
    // For any entry whose NBT spec is a single flat condition (one key, one value,
    // no OR groups, no wildcards, no negation), we extract a canonical "Key:Value"
    // string and store it here. The tooltip calls getEntryForContext(id, context)
    // which is a simple map lookup — O(1), no NBT parsing at runtime, no coercion.
    //
    // Entries with complex NBT ({OwnerUUID:'*-*'}, multi-condition, OR groups) are
    // not added to CONTEXT_MAP since they can't be represented as a flat string.
    // Those entries are only matched via getMatchingEntry(entity) during the entity
    // scan, not via the tooltip path. The tooltip for those items falls back to
    // getDefaultEntry() which is correct — e.g. a dragon with {OwnerUUID} has only
    // one entry for that entity ID so getDefaultEntry returns the right one anyway.
    // -------------------------------------------------------
    private static final Map<String, LivingComfortEntry> CONTEXT_MAP = new HashMap<>();

    public static void reload(String[] entries) {
        ENTITY_MAP.clear();
        CONTEXT_MAP.clear();
        if (entries == null) return;

        for (String line : entries) {

            line = line.split("#", 2)[0].trim();

            if (line == null || line.trim().isEmpty()) continue;

            try {
                String[] parts = line.split(",", 5);
                if (parts.length < 4) throw new IllegalArgumentException("Too few fields");

                ResourceLocation id = new ResourceLocation(parts[0].trim());
                int value = Integer.parseInt(parts[1].trim());
                String group = parts[2].trim();
                int limit = Integer.parseInt(parts[3].trim());

                // REASON: inline comments like "{SkullType:1} #cyclops" are valid in
                // Forge config but the # and everything after it must be stripped before
                // passing the NBT string to the parser. Without this, extractContextKey
                // receives "SkullType:1} #cyclops" after brace stripping, parseInt fails
                // on "1} #cyclops", and the entry is never added to CONTEXT_MAP.
                String rawNbt = parts.length == 5 ? parts[4].trim() : null;
                String nbt = rawNbt != null ? stripInlineComment(rawNbt) : null;

                LivingComfortEntry entry = new LivingComfortEntry(id, value, group, limit, nbt);
                ENTITY_MAP.computeIfAbsent(id, k -> new ArrayList<>()).add(entry);

                // Extract flat context key for CONTEXT_MAP if this entry has a single
                // simple Key:Value NBT condition (e.g. {SkullType:1})
                String contextKey = extractContextKey(nbt);
                if (contextKey != null) {
                    CONTEXT_MAP.put(id + "|" + contextKey, entry);
                }

            } catch (Exception e) {
                ChunkComfort.LOGGER.warn(
                        I18n.translateToLocalFormatted(
                                "chunkcomfort.config.invalid_living_entry",
                                line
                        )
                );
            }
        }
    }

    /**
     * Extracts a canonical "Key:Value" string from a single-condition NBT spec.
     * Returns null if the spec is complex (multi-condition, OR groups, wildcards,
     * negation, EXISTS-only) and cannot be represented as a flat context key.
     *
     * Examples:
     *   "{SkullType:1}"      -> "SkullType:1"
     *   "{SkullType:1b}"     -> "SkullType:1"   (byte suffix stripped, value normalised)
     *   "{OwnerUUID:'*-*'}"  -> null  (wildcard, not a flat value)
     *   "{}"                 -> null  (always-match, no condition to key on)
     *   "{A:1,B:2}"          -> null  (multi-condition)
     */
    /**
     * Reads the entity's actual NBT and returns the CONTEXT_MAP key that matches it,
     * or null if this entity ID has no context entries (single-entry or complex NBT).
     *
     * Used by addLivingEntityComfort() to populate contextEntityCounts in the cache
     * so that NBT variants of the same entity class (e.g. if_mob_skull SkullType:1
     * vs SkullType:3) are counted independently rather than sharing one class-keyed count.
     *
     * REASON: we already have the matched LivingComfortEntry from getMatchingEntry(),
     * but the cache needs a string key that also identifies the variant — not just
     * the entry object — so the tooltip can look it up by the same context string
     * that CustomSpawnEggRegistry provides for the item in hand.
     */
    public static String extractEntityContextKey(ResourceLocation id, net.minecraft.entity.Entity entity) {
        // Only relevant if there are multiple entries for this entity ID
        List<LivingComfortEntry> list = ENTITY_MAP.get(id);
        if (list == null || list.size() <= 1) return null;

        // Check which CONTEXT_MAP keys belong to this entity ID and match the entity's NBT
        net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
        entity.writeToNBTOptional(nbt);

        String prefix = id.toString() + "|";
        for (String key : CONTEXT_MAP.keySet()) {
            if (!key.startsWith(prefix)) continue;
            LivingComfortEntry candidate = CONTEXT_MAP.get(key);
            if (candidate != null && candidate.matches(nbt)) return key;
        }
        return null;
    }
    private static String stripInlineComment(String raw) {
        // Walk characters; once we hit # outside braces/quotes, truncate there
        int depth = 0;
        boolean inQuote = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\'' && !inQuote) inQuote = true;
            else if (c == '\'' && inQuote) inQuote = false;
            else if (!inQuote && c == '{') depth++;
            else if (!inQuote && c == '}') depth--;
            else if (!inQuote && depth == 0 && c == '#') {
                return raw.substring(0, i).trim();
            }
        }
        return raw.trim();
    }

    private static String extractContextKey(String nbt) {
        if (nbt == null || nbt.trim().isEmpty() || nbt.trim().equals("{}")) return null;

        // Must be a single group (no OR split)
        if (nbt.contains("},{")) return null;

        String inner = nbt.trim()
                .replaceAll("^\\{", "")
                .replaceAll("\\}$", "")
                .trim();

        if (inner.isEmpty()) return null;

        // Must be a single condition (no comma = only one key:value pair)
        if (inner.contains(",")) return null;

        // Must not be negated
        if (inner.startsWith("!")) return null;

        String[] kv = inner.split(":", 2);
        if (kv.length != 2) return null;

        String key = kv[0].trim().replaceAll("^\"|\"| '|'$", "");
        String val = kv[1].trim().replaceAll("^\"|\"| '|'$", "");

        // Must be a plain integer value (no wildcards, no string patterns)
        if (val.contains("*") || val.contains("-")) return null;

        // Strip byte suffix if present, normalise to plain integer string
        String normVal = val.endsWith("b") || val.endsWith("B")
                ? val.substring(0, val.length() - 1)
                : val;

        try {
            Integer.parseInt(normVal);
        } catch (NumberFormatException e) {
            // String value — include as-is but only if no special chars
            if (val.contains("'") || val.contains("*")) return null;
            return key + ":" + val;
        }

        return key + ":" + normVal;
    }

    // =====================================================
    // Parsing
    // =====================================================

    private static List<Map<String, NBTCondition>> parseNBT(String raw) {
        List<Map<String, NBTCondition>> groups = new ArrayList<>();

        if (raw == null || raw.isEmpty() || raw.equals("{}")) {
            groups.add(Collections.emptyMap());
            return groups;
        }

        raw = raw.trim();
        String[] splitGroups = raw.split("\\},\\{"); // OR groups

        for (String groupStr : splitGroups) {
            groupStr = groupStr.replaceAll("^\\{", "").replaceAll("\\}$", "").trim();

            if (groupStr.isEmpty()) {
                groups.add(Collections.emptyMap());
                continue;
            }

            Map<String, NBTCondition> group = new HashMap<>();
            String[] parts = groupStr.split("\\s*,\\s*"); // AND conditions

            for (String part : parts) {
                boolean negate = false;

                if (part.startsWith("!")) {
                    negate = true;
                    part = part.substring(1).trim();
                }

                String[] kv = part.split(":", 2);
                String key = kv[0].trim().replaceAll("^\"|\"| '|'$", "");

                NBTCondition cond;

                if (kv.length == 1) {
                    cond = new NBTCondition(NBTCondition.Type.EXISTS, null);
                } else {
                    String val = kv[1].trim().replaceAll("^\"|\"| '|'$", "");
                    cond = parseCondition(val);
                }

                cond.negate = negate;
                group.put(key, cond);
            }

            groups.add(group);
        }

        return groups;
    }

    private static NBTCondition parseCondition(String raw) {
        if ("*".equals(raw)) {
            return new NBTCondition(NBTCondition.Type.EXISTS, null);
        }

        try {
            if (raw.endsWith("b")) {
                byte b = Byte.parseByte(raw.substring(0, raw.length() - 1));
                return new NBTCondition(NBTCondition.Type.BYTE, b);
            }

            boolean isInt = true;
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (i == 0 && c == '-') continue;
                if (c < '0' || c > '9') { isInt = false; break; }
            }

            if (isInt) {
                return new NBTCondition(NBTCondition.Type.INT, Integer.parseInt(raw));
            }
        } catch (Exception ignored) {}

        return new NBTCondition(NBTCondition.Type.STRING, raw);
    }

    private static boolean isAlwaysMatch(List<Map<String, NBTCondition>> groups) {
        return groups.size() == 1 && groups.get(0).isEmpty();
    }

    // =====================================================
    // Matching
    // =====================================================

    private static boolean matchesGroup(NBTTagCompound nbt, Map<String, NBTCondition> group) {
        for (Map.Entry<String, NBTCondition> entry : group.entrySet()) {
            String key = entry.getKey();
            NBTCondition cond = entry.getValue();

            boolean exists = nbt.hasKey(key);
            boolean match;
            switch (cond.type) {
                case EXISTS:
                    match = exists;
                    break;
                default:
                    match = exists && matchesValue(nbt, key, cond);
                    break;
            }

            if (cond.negate) match = !match;
            if (!match) return false; // AND logic
        }
        return true;
    }

    private static boolean matchesValue(NBTTagCompound nbt, String key, NBTCondition cond) {
        switch (cond.type) {
            case BYTE: return nbt.getByte(key) == (byte) cond.value;
            case INT: {
                // REASON: MC 1.12 NBT does not coerce tag types — getInteger() on a
                // TAG_Byte returns 0, not the stored value. I&F stores SkullType as a
                // byte for some skull types and as an int for others. Without this check,
                // skulls whose SkullType tag is TAG_Byte never match, getMatchingEntry()
                // returns null for them, and their points are never added to the total.
                // getTagId() == 1 is the MC 1.12 constant for TAG_Byte.
                if (nbt.getTagId(key) == 1) {
                    return (nbt.getByte(key) & 0xFF) == (int) cond.value;
                }
                return nbt.getInteger(key) == (int) cond.value;
            }
            case STRING: {
                String actual = nbt.getString(key);
                String expected = (String) cond.value;
                if (expected.contains("*")) {
                    return actual.matches(expected.replace("*", ".*"));
                }
                return expected.equals(actual);
            }
            default: return true;
        }
    }

    // =====================================================
    // Lookup
    // =====================================================

    public static LivingComfortEntry getMatchingEntry(Entity entity) {

        ResourceLocation id = EntityList.getKey(entity);
        if (id == null) return null;

        List<LivingComfortEntry> entries = ENTITY_MAP.get(id);
        if (entries == null || entries.isEmpty()) return null;

        NBTTagCompound nbt = new NBTTagCompound();
        entity.writeToNBTOptional(nbt);

        LivingComfortEntry fallback = null;

        for (LivingComfortEntry entry : entries) {

            // {} entry → fallback candidate
            if (entry.alwaysMatch) {
                fallback = entry;
                continue;
            }

            // NBT entries have priority
            if (entry.matches(nbt)) {
                return entry;
            }
        }

        // only used if no NBT matched
        return fallback;
    }

    public static boolean isComfortEntity(Entity entity) {
        ResourceLocation id = EntityList.getKey(entity);
        return id != null && ENTITY_MAP.containsKey(id);
    }

    public static Set<String> getAllGroups() {
        Set<String> groups = new HashSet<>();

        for (List<LivingComfortEntry> list : ENTITY_MAP.values()) {
            for (LivingComfortEntry entry : list) {
                if (entry.group != null) {
                    groups.add(entry.group);
                }
            }
        }

        return groups;
    }
}
