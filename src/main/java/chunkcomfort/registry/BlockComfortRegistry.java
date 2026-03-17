package chunkcomfort.registry;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlockComfortRegistry {

    // Config-driven blocks
    public static final Map<Block, ComfortEntry> BLOCK_ENTRIES = new HashMap<>();

    // Config-driven entities
    private static final Map<Class<? extends Entity>, ComfortEntry> ENTITY_ENTRIES = new HashMap<>();

    // Quick cache for fast entity checks
    private static final Set<Class<? extends Entity>> COMFORT_ENTITY_CLASSES = new HashSet<>();

    /**
     * Get the comfort entry for an entity
     */
    public static ComfortEntry getEntityEntry(Entity entity) {
        for (Class<? extends Entity> clazz : COMFORT_ENTITY_CLASSES) {
            if (clazz.isInstance(entity)) {
                return ENTITY_ENTRIES.get(clazz);
            }
        }
        return null;
    }

    /**
     * Quick check: is this entity a comfort entity?
     * Optimized to skip non-comfort entities in events like LivingDeathEvent
     */
    public static boolean isComfortEntity(Entity entity) {
        for (Class<? extends Entity> clazz : COMFORT_ENTITY_CLASSES) {
            if (clazz.isInstance(entity)) return true;
        }
        return false;
    }

    /**
     * Reload the registry from config entries
     * Each entry: <block_or_entity_id>,<value>,<group>
     */
    public static void reload(String[] entries) {
        BLOCK_ENTRIES.clear();
        ENTITY_ENTRIES.clear();
        COMFORT_ENTITY_CLASSES.clear();

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

            // Try to resolve as block
            Block block = Block.getBlockFromName(id);
            if (block != null) {
                BLOCK_ENTRIES.put(block, new ComfortEntry(value, group));
                continue;
            }

            // Try to resolve as entity
            ResourceLocation rl = new ResourceLocation(id);
            if (ForgeRegistries.ENTITIES.containsKey(rl)) {
                Class<? extends Entity> entityClass = ForgeRegistries.ENTITIES.getValue(rl).getEntityClass();
                ENTITY_ENTRIES.put(entityClass, new ComfortEntry(value, group));
                COMFORT_ENTITY_CLASSES.add(entityClass);
            }
        }
    }

    /**
     * Registry entry
     */
    public static class ComfortEntry {
        public final int value;
        public final String group;

        public ComfortEntry(int value, String group) {
            this.value = value;
            this.group = group;
        }
    }
}