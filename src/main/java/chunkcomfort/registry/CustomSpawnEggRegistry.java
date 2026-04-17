package chunkcomfort.registry;

import chunkcomfort.ChunkComfort;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomSpawnEggRegistry {

    // -------------------------------------------------------
    // NBT-based entries (Lycanites style)
    // Format: entity=item,NBTPath
    // Example: lycanitesmobs:roc=lycanitesmobs:avianspawn,CreatureInfoSpawnEgg.creaturename
    //
    // resolve() reads the NBTPath from the item's tag compound and compares its
    // value to the entity's path name to confirm which entity the item represents.
    // This is needed for mod-specific multi-entity spawn eggs where a single item
    // class covers many entity types and stores the type in NBT.
    // -------------------------------------------------------
    public static class Entry {
        public String itemId;
        public String nbtPath;
        public ResourceLocation entityId;
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    // -------------------------------------------------------
    // Direct entries (skull / trophy style)
    // Format: entity=item
    // Example: iceandfire:dragonskull=iceandfire:dragon_skull
    //
    // REASON: Some items directly represent a single entity type with no NBT
    // disambiguation needed — the item IS the identity. Dragon skulls are an
    // example: iceandfire:dragon_skull is always a dragon skull regardless of
    // NBT. The NBT-based path in Entry required a comma-separated NBTPath field;
    // skull items have no such field, so they silently failed to parse and were
    // never loaded. This second map handles that case cleanly without touching
    // the NBT path logic.
    // -------------------------------------------------------
    private static final Map<String, ResourceLocation> DIRECT_ENTRIES = new HashMap<>();

    public static void reload(String[] config) {
        ENTRIES.clear();
        DIRECT_ENTRIES.clear();

        if (config == null) return;

        for (String line : config) {
            if (line == null || line.trim().isEmpty()) continue;

            try {
                String[] split = line.split("=");
                if (split.length != 2) throw new IllegalArgumentException("Expected exactly one '='");

                ResourceLocation entity = new ResourceLocation(split[0].trim());
                String right = split[1].trim();

                if (right.contains(",")) {
                    // --- NBT-based entry (Lycanites style): entity=item,NBTPath ---
                    String[] parts = right.split(",", 2);
                    if (parts.length < 2) throw new IllegalArgumentException("NBT entry needs item,path");

                Entry e = new Entry();
                e.entityId = entity;
                    e.itemId = parts[0].trim();
                    e.nbtPath = parts[1].trim();
                ENTRIES.add(e);

                } else {
                    // --- Direct entry (skull style): entity=item ---
                    // No NBT path needed — the item directly identifies the entity.
                    DIRECT_ENTRIES.put(right, entity);
                    ChunkComfort.LOGGER.debug(
                            "[ChunkComfort] Registered direct item->entity mapping: {} -> {}",
                            right, entity
                    );
                }

            } catch (Exception e) {
                ChunkComfort.LOGGER.warn(
                        I18n.translateToLocalFormatted(
                                "chunkcomfort.config.invalid_spawn_egg_entry",
                                line
                        )
                );
            }
        }
    }

    /**
     * Resolves an ItemStack to the entity ResourceLocation it represents.
     *
     * Tries two paths in order:
     * 1. Direct map — item registry name alone identifies the entity (skulls, trophies)
     * 2. NBT map — item type + NBT value identifies the entity (Lycanites spawn eggs)
     *
     * REASON: direct check comes first because it is O(1) and has no NBT overhead.
     * The NBT path is only needed when one item class covers multiple entity types.
     */
    public static ResourceLocation resolve(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;

        String itemId = stack.getItem().getRegistryName().toString();

        // --- Path 1: direct lookup (no NBT required) ---
        ResourceLocation direct = DIRECT_ENTRIES.get(itemId);
        if (direct != null) return direct;

        // --- Path 2: NBT-based lookup ---
        for (Entry entry : ENTRIES) {
            if (!entry.itemId.equals(itemId)) continue;
            if (!stack.hasTagCompound()) continue;

            String result = readNBTPath(stack.getTagCompound(), entry.nbtPath);
            if (result != null && result.equals(entry.entityId.getPath())) {
                return entry.entityId;
            }
        }

        return null;
    }

    private static String readNBTPath(NBTTagCompound tag, String path) {
        String[] nodes = path.split("\\.");
        NBTTagCompound current = tag;

        for (int i = 0; i < nodes.length - 1; i++) {
            if (!current.hasKey(nodes[i])) return null;
            current = current.getCompoundTag(nodes[i]);
        }

        return current.getString(nodes[nodes.length - 1]);
    }
}
