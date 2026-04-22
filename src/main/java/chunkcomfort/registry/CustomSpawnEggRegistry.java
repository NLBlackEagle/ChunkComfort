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
    // NBT-path entries  (Lycanites style)
    // Config format:  entity=item,Compound.key
    // Example: lycanitesmobs:roc=lycanitesmobs:avianspawn,CreatureInfoSpawnEgg.creaturename
    //
    // resolve() traverses the dot-separated NBT path, reads the string value at
    // the final key, and compares it to the entity's path segment.
    // Used when one item class covers many entity types and stores the type in
    // a nested NBT compound (Lycanites spawn eggs).
    // -------------------------------------------------------
    public static class Entry {
        public String itemId;
        public String nbtPath;
        public ResourceLocation entityId;
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    // -------------------------------------------------------
    // Direct entries  (skull / trophy style)
    //
    // Two sub-formats parsed from the same config section:
    //
    //   A) Simple  —  entity=item
    //      Example: iceandfire:dragonskull=iceandfire:dragon_skull
    //      The item unambiguously identifies one entity. No NBT needed.
    //
    //   B) NBT-discriminated  —  entity=item,Key:Value
    //      Example: iceandfire:if_mob_skull=iceandfire:cyclops_skull,SkullType:1
    //               iceandfire:if_mob_skull=iceandfire:stymphalian_skull,SkullType:3
    //      Multiple items all map to the same entity class. Each item has a flat
    //      NBT Key:Value that distinguishes which variant it represents.
    //
    // REASON for sub-format B: without it, all if_mob_skull items resolve to
    // "iceandfire:if_mob_skull" and getDefaultEntry() always returns the first
    // config entry regardless of which skull item is held, showing wrong tooltip
    // data for every skull type except the first one listed.
    // resolveNBTContext() returns the Key:Value string so the tooltip can call
    // LivingComfortRegistry.getEntryMatchingContext() to select the right entry.
    //
    // Distinction from Lycanites paths: Lycanites paths always contain a dot
    // (compound traversal). Key:Value pairs never do. The parser branches on
    // whether the second part after the comma contains a dot.
    // -------------------------------------------------------
    public static class DirectEntry {
        public final ResourceLocation entityId;
        /** "Key:Value" NBT discriminator string, or null for simple entries. */
        public final String nbtContext;

        public DirectEntry(ResourceLocation entityId, String nbtContext) {
            this.entityId = entityId;
            this.nbtContext = nbtContext;
        }
    }

    /** item registry name -> DirectEntry */
    private static final Map<String, DirectEntry> DIRECT_ENTRIES = new HashMap<>();

    // -------------------------------------------------------
    // Reload
    // -------------------------------------------------------

    public static void reload(String[] config) {
        ENTRIES.clear();
        DIRECT_ENTRIES.clear();

        if (config == null) return;

        for (String line : config) {
            if (line == null || line.trim().isEmpty()) continue;

            try {
                String[] split = line.split("=", 2);
                if (split.length != 2) throw new IllegalArgumentException("Expected exactly one '='");

                ResourceLocation entity = new ResourceLocation(split[0].trim());
                String right = split[1].trim();

                if (right.contains(",")) {
                    String[] parts = right.split(",", 2);
                    String itemId    = parts[0].trim();
                    String secondPart = parts[1].trim();

                    if (secondPart.contains(".")) {
                        // Lycanites style: second part is a dot-path (Compound.key)
                        Entry e = new Entry();
                        e.entityId = entity;
                        e.itemId   = itemId;
                        e.nbtPath  = secondPart;
                        ENTRIES.add(e);
                        ChunkComfort.LOGGER.debug(
                                "[ChunkComfort] Registered NBT-path entry: {} -> {} (path: {})",
                                itemId, entity, secondPart);

                    } else {
                        // NBT-discriminated direct: second part is a flat Key:Value pair
                        // REASON: previously this branch didn't exist — any entry with a
                        // comma was treated as a Lycanites path, so "SkullType:1" became
                        // an nbtPath, readNBTPath looked for a tag literally named
                        // "SkullType:1", found nothing, and resolve() returned null.
                        // Now flat Key:Value entries are stored as DirectEntry with an
                        // nbtContext that the tooltip uses to pick the right living entry.
                        DIRECT_ENTRIES.put(itemId, new DirectEntry(entity, secondPart));
                        ChunkComfort.LOGGER.debug(
                                "[ChunkComfort] Registered NBT-discriminated direct entry: {} -> {} (context: {})",
                                itemId, entity, secondPart);
                    }

                } else {
                    // Simple direct: no comma, item alone identifies the entity
                    DIRECT_ENTRIES.put(right, new DirectEntry(entity, null));
                    ChunkComfort.LOGGER.debug(
                            "[ChunkComfort] Registered direct entry: {} -> {}",
                            right, entity);
                }

            } catch (Exception e) {
                ChunkComfort.LOGGER.warn(
                        I18n.translateToLocalFormatted(
                                "chunkcomfort.config.invalid_spawn_egg_entry", line));
            }
        }
    }

    // -------------------------------------------------------
    // Resolution
    // -------------------------------------------------------

    /**
     * Resolves an ItemStack to the entity ResourceLocation it represents.
     *
     * 1. Direct map (O(1)) — covers both simple and NBT-discriminated skull items.
     * 2. NBT-path scan — covers Lycanites-style multi-entity spawn eggs.
     */
    public static ResourceLocation resolve(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;

        String itemId = stack.getItem().getRegistryName().toString();

        DirectEntry direct = DIRECT_ENTRIES.get(itemId);
        if (direct != null) {
            ChunkComfort.LOGGER.info(
                    "[ChunkComfort][SpawnEgg] resolve: item='{}' -> entityId='{}' (direct)",
                    itemId, direct.entityId);
            return direct.entityId;
        }

        for (Entry entry : ENTRIES) {
            if (!entry.itemId.equals(itemId)) continue;
            if (!stack.hasTagCompound()) continue;
            String result = readNBTPath(stack.getTagCompound(), entry.nbtPath);
            if (result != null && result.equals(entry.entityId.getPath())) {
                ChunkComfort.LOGGER.info(
                        "[ChunkComfort][SpawnEgg] resolve: item='{}' -> entityId='{}' (NBT-path)",
                        itemId, entry.entityId);
                return entry.entityId;
            }
        }

        ChunkComfort.LOGGER.info(
                "[ChunkComfort][SpawnEgg] resolve: item='{}' -> null (not registered)",
                itemId);
        return null;
    }

    /**
     * Returns the flat "Key:Value" NBT discriminator for the item, or null.
     *
     * Used by the tooltip handler so that skull items sharing one entity ID
     * (all if_mob_skull variants) each resolve to the correct LivingComfortEntry
     * via LivingComfortRegistry.getEntryMatchingContext(), rather than always
     * defaulting to whichever entry happens to be first in the config list.
     */
    public static String resolveNBTContext(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        String itemId = stack.getItem().getRegistryName().toString();
        DirectEntry direct = DIRECT_ENTRIES.get(itemId);
        String context = direct != null ? direct.nbtContext : null;
        ChunkComfort.LOGGER.info(
                "[ChunkComfort][SpawnEgg] resolveNBTContext: item='{}' -> context='{}'",
                itemId, context);
        return context;
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    /**
     * Returns true if the given item registry name is registered in either the
     * direct or NBT-path entry maps.
     *
     * REASON: the tooltip handler needs to know whether an item is a skull/spawn-egg
     * type without constructing a full ItemStack. Used to suppress the block comfort
     * tooltip path for items that belong to the living entity tooltip path, whether
     * or not they are actively configured — if the item is a known skull type at all,
     * the block path should never fire for it.
     */
    public static boolean isKnownItem(String itemRegistryName) {
        if (DIRECT_ENTRIES.containsKey(itemRegistryName)) return true;
        for (Entry e : ENTRIES) {
            if (e.itemId.equals(itemRegistryName)) return true;
        }
        return false;
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
