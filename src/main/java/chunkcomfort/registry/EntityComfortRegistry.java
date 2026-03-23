package chunkcomfort.registry;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;

public class EntityComfortRegistry {

    private static final Map<Class<? extends Entity>, ComfortEntry> ENTITY_ENTRIES = new HashMap<>();
    private static final Set<Class<? extends Entity>> COMFORT_ENTITY_CLASSES = new HashSet<>();
    private static final Map<Class<?>, ComfortEntry> ENTITY_RESOLVED_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, ComfortEntry> ENTITY_ID_MAP = new HashMap<>();

    /** Comfort entry for entities */
    public static class ComfortEntry {
        public final int value;
        public final String group;
        public final int limit;

        public ComfortEntry(int value, String group, int limit) {
            this.value = value;
            this.group = group;
            this.limit = limit;
        }
    }

    /**
     * Reload entity comfort entries from config
     */
    public static void reload(String[] entries) {
        ENTITY_ENTRIES.clear();
        COMFORT_ENTITY_CLASSES.clear();
        ENTITY_RESOLVED_CACHE.clear();
        if (entries == null) return;

        for (String line : entries) {
            if (line == null || line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 3) continue;

            String id = parts[0];
            int value;
            String group = parts[2];

            try {
                value = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                value = 0;
            }

            int limit = value;
            if (parts.length >= 4) {
                try {
                    limit = Integer.parseInt(parts[3]);
                } catch (NumberFormatException ignored) {}
            }

            ResourceLocation rl = new ResourceLocation(id);
            if (ForgeRegistries.ENTITIES.containsKey(rl)) {
                Class<? extends Entity> clazz = Objects.requireNonNull(ForgeRegistries.ENTITIES.getValue(rl)).getEntityClass();
                ComfortEntry entry = new ComfortEntry(value, group, limit);

                ENTITY_ENTRIES.put(clazz, entry);
                COMFORT_ENTITY_CLASSES.add(clazz);


                ENTITY_ID_MAP.put(rl, entry);
            }
        }
    }

    /**
     * Get the comfort entry for an entity
     */
    public static ComfortEntry getEntityEntry(Entity entity) {
        Class<?> clazz = entity.getClass();
        if (ENTITY_RESOLVED_CACHE.containsKey(clazz)) return ENTITY_RESOLVED_CACHE.get(clazz);

        for (Class<? extends Entity> comfortClass : COMFORT_ENTITY_CLASSES) {
            if (comfortClass.isAssignableFrom(clazz)) {
                ComfortEntry entry = ENTITY_ENTRIES.get(comfortClass);
                ENTITY_RESOLVED_CACHE.put(clazz, entry);
                return entry;
            }
        }

        ENTITY_RESOLVED_CACHE.put(clazz, null);
        return null;
    }

    /**
     * Get ComfortEntry directly from ResourceLocation ID
     */
    public static ComfortEntry getEntityEntryFromId(ResourceLocation id) {
        return ENTITY_ID_MAP.get(id);
    }

    /**
     * Quick check: is this entity a comfort entity?
     */
    public static boolean isComfortEntity(Entity entity) {
        return getEntityEntry(entity) != null;
    }

    public static Set<Class<? extends Entity>> getComfortEntityClasses() {
        return Collections.unmodifiableSet(COMFORT_ENTITY_CLASSES);
    }
}