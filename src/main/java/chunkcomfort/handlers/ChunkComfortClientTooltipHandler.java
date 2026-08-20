package chunkcomfort.handlers;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.chunk.PettingComfortData;
import chunkcomfort.chunk.PlayerChunkComfortCache;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.registry.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBanner;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

public class ChunkComfortClientTooltipHandler {

    /** Cached set of block registry names from config for quick lookup */
    private static final Set<String> CONFIGURED_ALIAS_BLOCKS = new HashSet<>();
    private static final Set<String> CONFIGURED_ALIAS_KEYS = new HashSet<>();
    private static final Set<String> CONFIGURED_COMFORT_BLOCKS = new HashSet<>();
    private static final Map<String, Integer> GROUP_LIMITS = new HashMap<>();
    private static final Set<String> FIRE_BLOCKS = new HashSet<>();
    private static final Set<String> FIRE_SOURCE_ITEMS = new HashSet<>();
    private static final Set<String> GROUP_TOOLTIP_CACHE = new HashSet<>();

    /** Call this if the config is reloaded */
    public static void refreshConfiguredBlocks() {
        CONFIGURED_COMFORT_BLOCKS.clear();
        CONFIGURED_ALIAS_BLOCKS.clear();
        CONFIGURED_ALIAS_KEYS.clear();

        for (String entry : ForgeConfigHandler.server.blockComfortEntries) {
            if (entry == null || entry.isEmpty()) continue;
            String blockName = entry.split(",")[0]; // extract <block> from <block>,<value>,<group>,<limit>
            CONFIGURED_COMFORT_BLOCKS.add(blockName);

            String[] aliases = BlockComfortRegistry.BLOCK_ALIASES.get(blockName);
            if (aliases != null) {
                CONFIGURED_ALIAS_BLOCKS.addAll(Arrays.asList(aliases));
                // REASON: alias keys (e.g. "comforts:sleeping_bag") are item IDs that map
                // to real block IDs via BLOCK_ALIASES. They are in CONFIGURED_COMFORT_BLOCKS
                // but fail both the Block.getBlockFromName check (returns null) and the
                // CONFIGURED_ALIAS_BLOCKS check (which holds alias VALUES, not keys).
                // Storing keys separately lets isConfiguredBlock pass for these items.
                CONFIGURED_ALIAS_KEYS.add(blockName);
            }
        }
    }

    public static int getGroupLimit(String group) {
        return GROUP_LIMITS.getOrDefault(group, 0);
    }

    public static void refreshFireSourceItems() {
        FIRE_SOURCE_ITEMS.clear();

        if (!ForgeConfigHandler.server.requireFire) return;

        for (String entry : ForgeConfigHandler.server.fireSourceItems) {
            if (entry == null || entry.trim().isEmpty()) continue;
            FIRE_SOURCE_ITEMS.add(entry.trim());
        }
    }

    public static void refreshFireBlocks() {
        FIRE_BLOCKS.clear();

        if (!ForgeConfigHandler.server.requireFire) return;

        for (String entry : ForgeConfigHandler.server.fireBlocks) {
            if (entry == null || entry.trim().isEmpty()) continue;
            FIRE_BLOCKS.add(entry.trim());
        }
    }

    public static void refreshGroupTooltips() {
        GROUP_TOOLTIP_CACHE.clear();

        for (String group : ForgeConfigHandler.getDefinedGroups()) {
            String key = "tooltip.chunkcomfort.hidden." + group;
            String translated = I18n.format(key);
            if (!translated.equals(key)) {
                GROUP_TOOLTIP_CACHE.add(group);
            }
        }
    }

    public static void refreshGroupLimits() {
        GROUP_LIMITS.clear();
        for (String entry : ForgeConfigHandler.server.groupLimits) {
            if (entry == null || entry.isEmpty()) continue;
            String[] split = entry.split(",");
            if (split.length != 2) continue;
            String groupName = split[0].trim();
            int limit;
            try {
                limit = Integer.parseInt(split[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }
            GROUP_LIMITS.put(groupName, limit);
        }
    }

    // -------------------
    // NON-BLOCK / ENTITY ITEM DETECTION
    // -------------------
    private static final Set<String> NON_BLOCK_ENTITIES = new HashSet<>();
    private static final Map<String, Class<? extends Entity>> ENTITY_ITEM_MAP = new HashMap<>();

    public static void refreshNonBlockEntities() {
        NON_BLOCK_ENTITIES.clear();
        ENTITY_ITEM_MAP.clear();

        for (String entry : ForgeConfigHandler.server.blockComfortEntries) {
            if (entry == null || entry.isEmpty()) continue;

            String registryName = entry.split(",")[0].trim();

            // Try to get a block first
            Block block = Block.getBlockFromName(registryName);

            // Only skip it if it is truly a "block" item that should be handled as a block
            // Banners, etc., will remain in CONFIGURED_COMFORT_BLOCKS and handled there
            if (block != null) continue;

            // If we get here, treat as a non-block entity (paintings, item frames, armor stands, etc.)
            NON_BLOCK_ENTITIES.add(registryName);

            // Try to guess the entity class
            try {
                ResourceLocation id = new ResourceLocation(registryName);
                Class<? extends Entity> entityClass = EntityList.getClass(id);
                if (entityClass != null) {
                    ENTITY_ITEM_MAP.put(registryName, entityClass);
                } else {
                    // fallback placeholder
                    ENTITY_ITEM_MAP.put(registryName, EntityArmorStand.class);
                }
            } catch (Exception e) {
                // fallback placeholder
                ENTITY_ITEM_MAP.put(registryName, EntityArmorStand.class);
            }
        }
    }

    public static String getGroupFromItem(ItemStack stack) {

        ResourceLocation id = stack.getItem().getRegistryName();
        if (id == null) return null;

        String name = id.toString();

        // 1. Block-based lookup
        Block block = Block.getBlockFromName(name);
        if (block != null) {
            BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block);
            if (entry != null) return entry.group;
        }

        // 2. Entity-style entries (armor stands, frames, etc)
        EntityComfortRegistry.ComfortEntry entityEntry =
                EntityComfortRegistry.getEntityEntryFromId(id);

        if (entityEntry != null) {
            return entityEntry.group;
        }

        // 3. Living items (spawn eggs etc)
        ResourceLocation entityId = CustomSpawnEggRegistry.resolve(stack);
        if (entityId != null &&
                LivingComfortRegistry.hasEntries(entityId)) {

            String context = CustomSpawnEggRegistry.resolveNBTContext(stack);
            LivingComfortRegistry.LivingComfortEntry living =
                    LivingComfortRegistry.getEntryForContext(entityId, context);

            if (living != null) return living.group;
        }

        return null;
    }

    /**
     * Returns true if the given item registry name corresponds to a living entity
     * that is registered in LivingComfortRegistry under any entity ID.
     *
     * This covers cases like I&F skull items: "iceandfire:amphithere_skull" is not
     * itself a living entity ID, but when placed it becomes "iceandfire:if_mob_skull"
     * which IS in LivingComfortRegistry. We check both the item name directly and
     * whether any CustomSpawnEggRegistry entry maps this item to a living entity.
     *
     * REASON: used to suppress the block tooltip path for skull-type items that
     * are backed by living entities. Without this, any skull item whose name appears
     * in blockComfortEntries (even indirectly) would show a block comfort tooltip
     * when held, regardless of whether it is configured in CustomSpawnEggRegistry.
     */
    private static boolean isRegisteredLivingEntity(String itemRegistryName) {
        // Direct check: item name is itself a living entity ID with entries
        ResourceLocation directId = new ResourceLocation(itemRegistryName);
        if (LivingComfortRegistry.hasEntries(directId)) return true;

        // Indirect check: item maps to a living entity via CustomSpawnEggRegistry
        try {
            net.minecraft.item.Item item = net.minecraft.item.Item.REGISTRY.getObject(directId);
            if (item != null) {
                net.minecraft.item.ItemStack synthetic = new net.minecraft.item.ItemStack(item);
                ResourceLocation resolved = CustomSpawnEggRegistry.resolve(synthetic);
                if (resolved != null && LivingComfortRegistry.hasEntries(resolved)) return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    // -------------------
    // TOOLTIP HELPERS
    // -------------------

    /**
     * Adds the [CC] header line — always the first thing added to the tooltip.
     *
     * We pass messageKey rather than a pre-formatted string so the lang file
     * controls all color formatting. The three possible keys are:
     *   tooltip.chunkcomfort.header              → §8[§6CC§8] §7Hold CTRL for comfort info
     *   tooltip.chunkcomfort.blacklisted.*       → §8[§6CC§8] §7Area is blacklisted / Boss nearby
     *   tooltip.chunkcomfort.inactive            → §8[§6CC§8] §7Comfort inactive
     *
     * The contains() guard prevents duplicates when both the spawn egg path
     * and the block path run for the same item in one tooltip event.
     */
    private void addHeader(List<String> tooltip, String messageKey) {
        String header = I18n.format("tooltip.chunkcomfort.header");
        if (tooltip.contains(header)) return;
        tooltip.add(I18n.format(messageKey));
    }

    /**
     * Adds the CTRL-gated information block when CTRL is held:
     *   1. infoLines   — the actual data (points, counts, group totals)
     *   2. Fire info   — if this item is a fire source or fire block
     *   3. Spacer      — empty line from tooltip.chunkcomfort.spacer
     *   4. Flavor text — if this group has a hidden tooltip defined
     *
     * WHY a Runnable for infoLines?
     * Each tooltip path (block, entity, spawn egg) needs different variables to
     * compute its lines. Rather than passing dozens of parameters, we pass a
     * Runnable — a small piece of code that runs later. The lambda () -> { ... }
     * syntax creates one inline, capturing the variables it needs from the
     * surrounding scope automatically.
     *
     * In Java, variables captured by a lambda must be "effectively final" —
     * their value cannot change after being assigned. You will see "final"
     * declarations before each lambda call below for exactly this reason.
     *
     * WHY does the fire-only path pass () -> {} (empty lambda)?
     * Fire-only items have no block or entity entry so neither the block path
     * nor the entity path calls addCtrlBlock() for them. We still need
     * addCtrlBlock() to run so the fire lines get added — we just have no
     * info lines to contribute, so the Runnable does nothing.
     */
    private void addCtrlBlock(List<String> tooltip, ItemStack stack, boolean ctrlDown,
                              EntityPlayer player, boolean isFireSourceItem, boolean isFireBlock,
                              Runnable infoLines) {
        if (!ctrlDown || player == null) return;

        // Information lines (block points, group totals, entity counts, etc)
        infoLines.run();

        // Fire info, if applicable
        if (isFireSourceItem) tooltip.add(I18n.format("tooltip.chunkcomfort.firestarters"));
        if (isFireBlock)      tooltip.add(I18n.format("tooltip.chunkcomfort.fireblocks"));

        // Spacer + flavor text, if this group has one defined
        String group = getGroupFromItem(stack);
        if (group != null && GROUP_TOOLTIP_CACHE.contains(group)) {
            tooltip.add(I18n.format("tooltip.chunkcomfort.spacer"));
            tooltip.add(I18n.format("tooltip.chunkcomfort.hidden." + group));
        }
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {

        ItemStack stack = event.getItemStack();
        List<String> tooltip = event.getToolTip();
        EntityPlayer player = event.getEntityPlayer();
        boolean ctrlDown = net.minecraft.client.gui.GuiScreen.isCtrlKeyDown();
        PlayerChunkComfortCache cache = player != null ? AreaComfortCalculator.getCache(player) : null;
        if (player != null) { cache.ensureUpToDate(); }

        boolean envBlacklisted = false;
        boolean bossBlacklisted = false;

        if (player != null && player.world != null) {
            BlockPos pos = player.getPosition();
            if (pos != null) {
                envBlacklisted = AreaComfortCalculator.isEnvironmentBlocked(player.world, pos);
            }
        }
        bossBlacklisted = AreaComfortCalculator.isBossBlocked();
        boolean blacklisted = envBlacklisted || bossBlacklisted;

        boolean comfortActive = player != null && AreaComfortCalculator.isComfortActive(player);

        String registryName = Objects.requireNonNull(stack.getItem().getRegistryName()).toString();
        boolean handledSpawnEgg = false;

        // -------------------
        // Resolve entity from item (CUSTOM + VANILLA)
        // -------------------
        ResourceLocation entityID = null;

        // 1. Custom spawn eggs (Lycanites etc)
        entityID = CustomSpawnEggRegistry.resolve(stack);

        // 2. Vanilla spawn eggs (fallback)
        if (entityID == null &&
                stack.getItem() instanceof net.minecraft.item.ItemMonsterPlacer) {
            entityID = net.minecraft.item.ItemMonsterPlacer.getNamedIdFrom(stack);
        }

        // -------------------
        // SPAWN EGGS (Living entities only)
        // -------------------
        if (entityID != null) {

            String nbtContext = CustomSpawnEggRegistry.resolveNBTContext(stack);
            LivingComfortRegistry.LivingComfortEntry livingEntry =
                    LivingComfortRegistry.getEntryForContext(entityID, nbtContext);

            // ONLY continue if this entity is configured for comfort
            if (livingEntry == null) {
                return;
            }

            PettingComfortData petEntry = PettingComfortRegistry.getEntry(entityID.toString());

            // Blacklisted/inactive: show [CC] with the relevant message and stop.
            // We check these before adding the normal header so the header line
            // itself reflects the current state rather than showing "Hold CTRL"
            // when there is nothing to show.
            if (blacklisted) {
                if (envBlacklisted) addHeader(tooltip, "tooltip.chunkcomfort.blacklisted.environment");
                if (bossBlacklisted) addHeader(tooltip, "tooltip.chunkcomfort.blacklisted.boss");
                return;
            }
            if (!comfortActive && player != null) {
                addHeader(tooltip, "tooltip.chunkcomfort.inactive");
                return;
            }

            // Normal state: add the [CC] Hold CTRL header
            addHeader(tooltip, "tooltip.chunkcomfort.header");

            if (player != null) {

                Entity entity = EntityList.createEntityByIDFromName(entityID, player.world);

                // REASON: previously only EntityLiving instances triggered the tooltip,
                // which meant entities registered via the direct CustomSpawnEggRegistry path
                // (dragon skulls, trophies) always silently fell through — their entityID
                // resolves fine but createEntityByIDFromName returns null or a non-living
                // entity because the entity ID is a logical identifier, not a spawnable mob.
                // We now show the tooltip for any entity that has a LivingComfortEntry,
                // regardless of whether it can be spawned as a living entity.
                // entityCount falls back to 0 for non-living — skulls count via the
                // block scanner, not the entity scanner, so 0 is correct here.
                boolean isSpawnableLiving = entity instanceof EntityLiving;

                // REASON: livingEntityCounts is keyed on Java Class, which is shared
                // across all NBT variants of the same entity (all if_mob_skull types
                // use one class). When nbtContext is available we use contextEntityCounts
                // instead — keyed on "entityId|SkullType:1" — so each variant shows its
                // own independent count rather than the combined count of all variants.
                final int entityCount;
                if (nbtContext != null && !nbtContext.isEmpty()) {
                    String contextKey = entityID.toString() + "|" + nbtContext;
                    entityCount = cache.getContextEntityCount(contextKey);
                } else {
                    entityCount = isSpawnableLiving
                            ? cache.livingEntityCounts.getOrDefault(entity.getClass(), 0)
                            : 0;
                }

                // final is required here so the lambda below can capture these values
                final int groupPoints = cache.entityGroupTotals.getOrDefault(livingEntry.group, 0);
                final int totalGroupLimit = getGroupLimit(livingEntry.group);
                final LivingComfortRegistry.LivingComfortEntry capturedEntry = livingEntry;
                final PettingComfortData capturedPet = petEntry;
                final ResourceLocation capturedEntityID = entityID;

                addCtrlBlock(tooltip, stack, ctrlDown, player, false, false, () -> {
                    tooltip.add(I18n.format(
                            "tooltip.chunkcomfort.living.line1",
                            capturedEntry.value,
                            entityCount,
                            capturedEntry.limit));
                    tooltip.add(I18n.format(
                            "tooltip.chunkcomfort.living.line2",
                            capturedEntry.group,
                            groupPoints,
                            totalGroupLimit));

                    String nameLine = NamedPetComfortRegistry.formatNamesWithPoints(capturedEntityID);
                    if (nameLine != null) tooltip.add(nameLine);

                    if (capturedPet != null) tooltip.add(I18n.format("tooltip.chunkcomfort.pet"));
                });
            }

            handledSpawnEgg = true;
            NON_BLOCK_ENTITIES.add(registryName);
            ENTITY_ITEM_MAP.put(registryName, EntityList.getClass(entityID));
        }

        // -------------------
        // Generic entity / block handling
        // -------------------
        boolean isAliasBlock = CONFIGURED_ALIAS_BLOCKS.contains(registryName);
        boolean isAliasKey = CONFIGURED_ALIAS_KEYS.contains(registryName);
        // REASON: three cases for block tooltip items:
        // 1. Normal block items (minecraft:bookshelf) — in CONFIGURED_COMFORT_BLOCKS,
        //    Block.getBlockFromName returns non-null.
        // 2. Alias value items (comforts:sleeping_bag_silver) — in CONFIGURED_ALIAS_BLOCKS.
        // 3. Alias key items (comforts:sleeping_bag) — the item ID is an alias key that
        //    maps to real block IDs. It's in CONFIGURED_COMFORT_BLOCKS and CONFIGURED_ALIAS_KEYS
        //    but Block.getBlockFromName returns null and it's not in CONFIGURED_ALIAS_BLOCKS.
        //    isAliasKey covers this case so the tooltip shows correctly.
        boolean isConfiguredBlock = (CONFIGURED_COMFORT_BLOCKS.contains(registryName) || isAliasBlock)
                && (Block.getBlockFromName(registryName) != null || isAliasBlock || isAliasKey)
                && !isRegisteredLivingEntity(registryName);
        boolean isEntityItem = entityID != null && LivingComfortRegistry.hasEntries(entityID);
        boolean isFireBlock = FIRE_BLOCKS.contains(registryName);
        boolean isFireSourceItem = FIRE_SOURCE_ITEMS.contains(registryName);
        EntityComfortRegistry.ComfortEntry entityEntry = EntityComfortRegistry.getEntityEntryFromId(new ResourceLocation(registryName));

        // Nothing to show? Exit early
        if (!isConfiguredBlock && entityEntry == null && !isEntityItem && !isFireBlock && !isFireSourceItem) return;

        // REASON: whether an item counts as a fire source/fire block is an intrinsic
        // property of the item, not a reflection of the player's current comfort state.
        // Items with no other comfort role (no configured block/entity entry) should
        // always show this hint on CTRL — even outside an established comfort zone or
        // in a blacklisted area — otherwise items like flint & steel never tell you
        // they satisfy the Fire Requirement unless comfort already happens to be active.
        boolean isPureFireItem = !isConfiguredBlock && entityEntry == null && !isEntityItem;
        if (isPureFireItem && (isFireSourceItem || isFireBlock)) {
            addHeader(tooltip, "tooltip.chunkcomfort.header");
            addCtrlBlock(tooltip, stack, ctrlDown, player, isFireSourceItem, isFireBlock, () -> {});
            return;
        }

        // Blacklisted/inactive: show [CC] with the relevant message and stop
        if (blacklisted) {
            if (envBlacklisted) addHeader(tooltip, "tooltip.chunkcomfort.blacklisted.environment");
            if (bossBlacklisted) addHeader(tooltip, "tooltip.chunkcomfort.blacklisted.boss");
            return;
        }
        if (!comfortActive && player != null) {
            addHeader(tooltip, "tooltip.chunkcomfort.inactive");
            return;
        }

        // Normal state: add the [CC] Hold CTRL header
        addHeader(tooltip, "tooltip.chunkcomfort.header");

        if (player == null) return;

        // -------------------
        // Non-living / generic entity tooltip (skip if spawn egg already handled)
        // -------------------

        // todo: add !isAliasBlock here and change the method to be a for-each item in aliases?
        //  could probably just do !no darn banner instances if I am lazy?

        boolean isBanner = false;
        if (stack.getItem() instanceof ItemBlock) {
            Block block = ((ItemBlock) stack.getItem()).getBlock();
            isBanner = block instanceof BlockBanner;
        }

        if (!handledSpawnEgg && (entityEntry != null || isEntityItem) && !isBanner) {

            // todo: this is hardcoded mess cuz I was lazy, aliases may not work? Not sure, only banners are stupid aliases.
            //  probably have to test this with other aliases and test it.

            Class<? extends Entity> entityClass = ENTITY_ITEM_MAP.getOrDefault(registryName, EntityArmorStand.class);
            ResourceLocation entityId = new ResourceLocation(registryName);
            PettingComfortData petEntry = PettingComfortRegistry.getEntry(entityId.toString());

            final int entityCount = cache.getDecorativeEntityCount(entityClass);
            int groupPoints = 0;
            int totalGroupLimit = 0;
            int value = 0;
            String group = "unknown";

            if (entityEntry != null) {
                value = entityEntry.value;
                group = entityEntry.group;
                groupPoints = cache.entityGroupTotals.getOrDefault(group, 0);
                totalGroupLimit = GROUP_LIMITS.getOrDefault(group, 0);
            }

            // final copies required for lambda capture
            final int capturedValue = value;
            final int capturedGroupPoints = groupPoints;
            final int capturedTotalGroupLimit = totalGroupLimit;
            final String capturedGroup = group;
            final EntityComfortRegistry.ComfortEntry capturedEntityEntry = entityEntry;
            final PettingComfortData capturedPet = petEntry;

            addCtrlBlock(tooltip, stack, ctrlDown, player, isFireSourceItem, isFireBlock, () -> {
                tooltip.add(I18n.format("tooltip.chunkcomfort.decorative.line1",
                        capturedValue, entityCount, capturedEntityEntry != null ? capturedEntityEntry.limit : 0));
                tooltip.add(I18n.format("tooltip.chunkcomfort.decorative.line2",
                        capturedGroup, capturedGroupPoints, capturedTotalGroupLimit));
                if (capturedPet != null) tooltip.add(I18n.format("tooltip.chunkcomfort.pet"));
            });
        }

        // -------------------
        // Block tooltip
        // -------------------
        if (isConfiguredBlock) {

            // Get canonical ID (the "main" ID from config, e.g., minecraft:banner)
            String canonicalId = BlockComfortRegistry.getCanonicalIdFromRegistryName(registryName);

            // Get aliases (wall_banner, standing_banner)
            String[] aliases = BlockComfortRegistry.BLOCK_ALIASES.get(canonicalId);

            // Collect all block IDs to check (main + aliases)
            List<String> allIds = new ArrayList<>();
            allIds.add(canonicalId);
            if (aliases != null) allIds.addAll(Arrays.asList(aliases));

            int totalAmount = 0;
            Block mainBlock = null;

            for (String id : allIds) {
                Block b = Block.getBlockFromName(id);
                if (b != null) {
                    if (mainBlock == null) mainBlock = b;
                    int count = cache.blockCounts.getOrDefault(b, 0);
                    totalAmount += count;
                }
            }

            if (mainBlock != null) {
                final int pointsPerBlock = BlockComfortRegistry.getValue(mainBlock);
                final String groupName = BlockComfortRegistry.getGroup(mainBlock);
                int blockLimit = 0;
                BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(mainBlock);
                if (entry != null) blockLimit = entry.limit;
                final int capturedBlockLimit = blockLimit;
                final int groupPoints = cache.groupTotals.getOrDefault(groupName, 0);
                final int totalGroupLimit = GROUP_LIMITS.getOrDefault(groupName, 0);
                final int capturedTotal = totalAmount;

                addCtrlBlock(tooltip, stack, ctrlDown, player, isFireSourceItem, isFireBlock, () -> {
                    tooltip.add(I18n.format("tooltip.chunkcomfort.block.line1",
                            pointsPerBlock, capturedTotal, capturedBlockLimit));
                    tooltip.add(I18n.format("tooltip.chunkcomfort.block.line2",
                            groupName, groupPoints, totalGroupLimit));
                });
            }
        }
    }
}
