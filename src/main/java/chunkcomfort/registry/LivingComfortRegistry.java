package chunkcomfort.registry;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class LivingComfortRegistry {

    public static class LivingComfortEntry {
        public final ResourceLocation entityId;
        public final int value;
        public final String group;
        public final int limit;

        public LivingComfortEntry(ResourceLocation entityId, int value, String group, int limit) {
            this.entityId = entityId;
            this.value = value;
            this.group = group;
            this.limit = limit;
        }
    }

    private static final Map<ResourceLocation, LivingComfortEntry> ENTITY_MAP = new HashMap<>();

    public static void reload(String[] entries) {
        ENTITY_MAP.clear();
        if (entries == null) return;

        for (String line : entries) {
            if (line == null || line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 4) continue;

            try {
                ResourceLocation id = new ResourceLocation(parts[0].trim());
                int value = Integer.parseInt(parts[1].trim());
                String group = parts[2].trim();
                int limit = Integer.parseInt(parts[3].trim());

                ENTITY_MAP.put(id, new LivingComfortEntry(id, value, group, limit));
            } catch (Exception e) {
                // ignore malformed line
            }
        }
    }

    public static LivingComfortEntry getEntry(Entity entity) {
        ResourceLocation id = EntityList.getKey(entity);
        if (id == null) return null;
        return ENTITY_MAP.get(id);
    }

    public static boolean isComfortEntity(Entity entity) {
        return getEntry(entity) != null;
    }
}