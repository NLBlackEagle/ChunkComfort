package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
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
        ENTITY_ID_MAP.clear();

        if (entries == null) return;

        for (String line : entries) {

            if (line == null || line.trim().isEmpty()) continue;

            try {
                String[] parts = line.split(",");
                if (parts.length < 3) throw new IllegalArgumentException();

                String id = parts[0].trim();
                int value = Integer.parseInt(parts[1].trim());
                String group = parts[2].trim();

                int limit = value;
                if (parts.length >= 4) {
                    limit = Integer.parseInt(parts[3].trim());
                }

                ResourceLocation rl = new ResourceLocation(id);
                Class<? extends Entity> clazz = null;

                if (ForgeRegistries.ENTITIES.containsKey(rl)) {
                    clazz = Objects.requireNonNull(ForgeRegistries.ENTITIES.getValue(rl)).getEntityClass();
                } else {
                    switch (id) {
                        case "minecraft:armor_stand":
                            clazz = net.minecraft.entity.item.EntityArmorStand.class;
                            break;
                        case "minecraft:painting":
                            clazz = net.minecraft.entity.item.EntityPainting.class;
                            break;
                        case "minecraft:item_frame":
                            clazz = net.minecraft.entity.item.EntityItemFrame.class;
                            break;
                    }
                }

                if (clazz != null) {
                    ComfortEntry entry = new ComfortEntry(value, group, limit);
                    ENTITY_ENTRIES.put(clazz, entry);
                    COMFORT_ENTITY_CLASSES.add(clazz);
                    ENTITY_ID_MAP.put(rl, entry);
                } else {
                    ChunkComfort.LOGGER.warn(
                            I18n.translateToLocalFormatted(
                                    "chunkcomfort.config.invalid_entity_entry",
                                    line
                            )
                    );
                }

            } catch (Exception e) {
                ChunkComfort.LOGGER.warn(
                        I18n.translateToLocalFormatted(
                                "chunkcomfort.config.invalid_entity_entry",
                                line
                        )
                );
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