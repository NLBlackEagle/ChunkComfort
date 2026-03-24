package chunkcomfort.registry;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.util.*;

public class LivingComfortRegistry {

    public static class LivingComfortEntry {
        public final ResourceLocation entityId;
        public final int value;
        public final String group;
        public final int limit;

        // OR groups → each map is an AND group
        public final List<Map<String, NBTCondition>> nbtGroups;

        private final boolean alwaysMatch;

        public LivingComfortEntry(ResourceLocation entityId, int value, String group, int limit, String nbtRaw) {
            this.entityId = entityId;
            this.value = value;
            this.group = group;
            this.limit = limit;
            this.nbtGroups = parseNBT(nbtRaw);
            this.alwaysMatch = isAlwaysMatch(nbtGroups);
        }

        public boolean matches(Entity entity) {
            if (alwaysMatch) return true;

            NBTTagCompound nbt = new NBTTagCompound();
            entity.writeToNBT(nbt);

            for (Map<String, NBTCondition> group : nbtGroups) {
                if (matchesGroup(nbt, group)) {
                    return true;
                }
            }
            return false;
        }
    }

    // Pre-parsed condition → avoids parsing every tick
    private static class NBTCondition {
        enum Type { BYTE, INT, STRING, WILDCARD }

        final Type type;
        final Object value;

        NBTCondition(Type type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    public static final Map<ResourceLocation, LivingComfortEntry> ENTITY_MAP = new HashMap<>();

    public static void reload(String[] entries) {
        ENTITY_MAP.clear();
        if (entries == null) return;

        for (String line : entries) {
            if (line == null || line.trim().isEmpty()) continue;

            try {
                String[] parts = line.split(",", 5);
                if (parts.length < 4) continue;

                ResourceLocation id = new ResourceLocation(parts[0].trim());
                int value = Integer.parseInt(parts[1].trim());
                String group = parts[2].trim();
                int limit = Integer.parseInt(parts[3].trim());
                String nbt = parts.length == 5 ? parts[4].trim() : null;

                ENTITY_MAP.put(id, new LivingComfortEntry(id, value, group, limit, nbt));

            } catch (Exception ignored) {}
        }
    }

    // ------------------------
    // Parsing
    // ------------------------

    private static List<Map<String, NBTCondition>> parseNBT(String raw) {
        List<Map<String, NBTCondition>> groups = new ArrayList<>();

        if (raw == null || raw.isEmpty() || raw.equals("{}")) {
            groups.add(Collections.emptyMap());
            return groups;
        }

        raw = raw.trim();

        if (raw.startsWith("{") && raw.endsWith("}")) {
            raw = raw.substring(1, raw.length() - 1);
        }

        String[] splitGroups = raw.split("\\},\\{");

        for (String groupStr : splitGroups) {
            groupStr = groupStr.replace("{", "").replace("}", "").trim();

            if (groupStr.isEmpty()) {
                groups.add(Collections.emptyMap());
                continue;
            }

            Map<String, NBTCondition> group = new HashMap<>();

            String[] parts = groupStr.split("\\s*,\\s*");

            for (String part : parts) {
                String[] kv = part.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].trim();
                String value = kv[1].trim();

                group.put(key, parseCondition(value));
            }

            groups.add(group);
        }

        return groups;
    }

    private static NBTCondition parseCondition(String raw) {
        if ("*".equals(raw)) {
            return new NBTCondition(NBTCondition.Type.WILDCARD, null);
        }

        try {
            if (raw.endsWith("b")) {
                byte b = Byte.parseByte(raw.substring(0, raw.length() - 1));
                return new NBTCondition(NBTCondition.Type.BYTE, b);
            }

            // fast int check (no regex)
            boolean isInt = true;
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (i == 0 && c == '-') continue;
                if (c < '0' || c > '9') {
                    isInt = false;
                    break;
                }
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

    // ------------------------
    // Matching
    // ------------------------

    private static boolean matchesGroup(NBTTagCompound nbt, Map<String, NBTCondition> group) {
        for (Map.Entry<String, NBTCondition> entry : group.entrySet()) {
            String key = entry.getKey();
            NBTCondition cond = entry.getValue();

            if (!nbt.hasKey(key)) return false;

            if (cond.type != NBTCondition.Type.WILDCARD) {
                if (!matchesValue(nbt, key, cond)) return false;
            }
        }
        return true;
    }

    private static boolean matchesValue(NBTTagCompound nbt, String key, NBTCondition cond) {
        switch (cond.type) {
            case BYTE:
                return nbt.getByte(key) == (byte) cond.value;
            case INT:
                return nbt.getInteger(key) == (int) cond.value;
            case STRING:
                return cond.value.equals(nbt.getString(key));
            default:
                return true;
        }
    }

    // ------------------------
    // Lookup
    // ------------------------

    public static LivingComfortEntry getEntry(Entity entity) {
        ResourceLocation id = EntityList.getKey(entity);
        return id == null ? null : ENTITY_MAP.get(id);
    }

    public static LivingComfortEntry getMatchingEntry(Entity entity) {
        LivingComfortEntry entry = getEntry(entity);
        return (entry != null && entry.matches(entity)) ? entry : null;
    }

    public static boolean isComfortEntity(Entity entity) {
        return getEntry(entity) != null;
    }

    public static int getGroupLimit(String groupName) {
        int total = 0;
        for (LivingComfortEntry entry : ENTITY_MAP.values()) {
            if (entry.group.equals(groupName)) {
                total += entry.limit;
            }
        }
        return total;
    }
}