package chunkcomfort.handlers;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.chunk.PettingComfortData;
import chunkcomfort.chunk.PlayerChunkComfortCache;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.registry.*;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

public class ChunkComfortClientTooltipHandler {

    /** Cached set of block registry names from config for quick lookup */
    private static final Set<String> CONFIGURED_ALIAS_BLOCKS = new HashSet<>();
    private static final Set<String> CONFIGURED_COMFORT_BLOCKS = new HashSet<>();
    private static final Map<String, Integer> GROUP_LIMITS = new HashMap<>();
    private static final Set<String> FIRE_BLOCKS = new HashSet<>();
    private static final Set<String> FIRE_SOURCE_ITEMS = new HashSet<>();

    /** Call this if the config is reloaded */
    public static void refreshConfiguredBlocks() {
        CONFIGURED_COMFORT_BLOCKS.clear();
        CONFIGURED_ALIAS_BLOCKS.clear();

        for (String entry : ForgeConfigHandler.server.blockComfortEntries) {
            if (entry == null || entry.isEmpty()) continue;
            String blockName = entry.split(",")[0]; // extract <block> from <block>,<value>,<group>,<limit>
            CONFIGURED_COMFORT_BLOCKS.add(blockName);

            String[] aliases = BlockComfortRegistry.BLOCK_ALIASES.get(blockName);
            if (aliases != null) {
                CONFIGURED_ALIAS_BLOCKS.addAll(Arrays.asList(aliases));
            }
        }
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

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        List<String> tooltip = event.getToolTip();
        EntityPlayer player = event.getEntityPlayer();
        PlayerChunkComfortCache cache = player != null ? AreaComfortCalculator.getCache(player) : null;
        if (player != null) {cache.ensureUpToDate();}

        String registryName = Objects.requireNonNull(stack.getItem().getRegistryName()).toString();
        boolean handledSpawnEgg = false;

        // -------------------
        // SPAWN EGGS (Living entities only)
        // -------------------
        if (stack.getItem() instanceof net.minecraft.item.ItemMonsterPlacer) {
            ResourceLocation entityID = net.minecraft.item.ItemMonsterPlacer.getNamedIdFrom(stack);
            if (entityID != null) {
                LivingComfortRegistry.LivingComfortEntry livingEntry = LivingComfortRegistry.ENTITY_MAP.get(entityID);
                if (livingEntry == null) return; // Only configured entities

                PettingComfortData petEntry = PettingComfortRegistry.getEntry(entityID.toString());

                // Add JEI header if it wasn’t added yet
                String header = I18n.format("tooltip.chunkcomfort.header");
                if (!tooltip.contains(header)) tooltip.add(header);

                if (player != null) {
                    Entity entity = EntityList.createEntityByIDFromName(entityID, player.world);
                    if (cache.isEmpty()) {AreaComfortCalculator.calculatePlayerComfort(player);}

                    if (entity instanceof EntityLiving) {
                        int entityCount = cache.livingEntityCounts.getOrDefault(entity.getClass(), 0);
                        int groupPoints = cache.entityGroupTotals.getOrDefault(livingEntry.group, 0);
                        int totalGroupLimit = LivingComfortRegistry.getGroupLimit(livingEntry.group);

                        tooltip.add(I18n.format("tooltip.chunkcomfort.living.line1", livingEntry.value, entityCount, livingEntry.limit));
                        tooltip.add(I18n.format("tooltip.chunkcomfort.living.line2", livingEntry.group, groupPoints, totalGroupLimit));

                        // Add pet names bonus
                        String nameLine = NamedPetComfortRegistry.formatNamesWithPoints(entityID);
                        if (nameLine != null) {
                            tooltip.add(nameLine);
                        }

                        if (petEntry != null) {tooltip.add(I18n.format("tooltip.chunkcomfort.pet"));}
                    }
                }

                handledSpawnEgg = true; // skip generic entity section
                NON_BLOCK_ENTITIES.add(registryName);
                ENTITY_ITEM_MAP.put(registryName, EntityList.getClass(entityID));
            }
        }

        // -------------------
        // Generic entity / block handling
        // -------------------
        boolean isAliasBlock = CONFIGURED_ALIAS_BLOCKS.contains(registryName);
        boolean isConfiguredBlock = CONFIGURED_COMFORT_BLOCKS.contains(registryName);
        boolean isEntityItem = NON_BLOCK_ENTITIES.contains(registryName);
        boolean isFireBlock = FIRE_BLOCKS.contains(registryName);
        boolean isFireSourceItem = FIRE_SOURCE_ITEMS.contains(registryName);
        EntityComfortRegistry.ComfortEntry entityEntry = EntityComfortRegistry.getEntityEntryFromId(new ResourceLocation(registryName));


        // Nothing to show? Exit early
        if (!isConfiguredBlock && entityEntry == null && !isEntityItem && !isFireBlock && !isFireSourceItem) return;

        // Add JEI header if it wasn’t added yet
        String header = I18n.format("tooltip.chunkcomfort.header");
        if (!tooltip.contains(header)) tooltip.add(header);

        // -------------------
        // Fire Starter tooltip
        // -------------------
        String fireItem = I18n.format("tooltip.chunkcomfort.firestarters");
        if ((isFireSourceItem) && (!tooltip.contains(fireItem))) {
            tooltip.add(fireItem);
        }

        // -------------------
        // Fire Block tooltip
        // -------------------
        String fireBlock = I18n.format("tooltip.chunkcomfort.fireblocks");
        if ((isFireBlock) && (!tooltip.contains(fireBlock))) {
            tooltip.add(fireBlock);
        }


        if (player == null) return;

        if (cache.isEmpty()) {AreaComfortCalculator.calculatePlayerComfort(player);}
        // -------------------
        // Non-living / generic entity tooltip (skip if spawn egg already handled)
        // -------------------

        // todo: add !isAliasBlock here and change the method to be a for-each item in aliases?
        //  could probably just do !no darn banner instances if I am lazy?

        if (!handledSpawnEgg && (entityEntry != null || isEntityItem)) {

            // todo: this is hardcoded mess cuz I was lazy, I aliases may not work? Not sure, only banners are stupid aliases.
            //  probably have to test this with other aliases and test it.
            //  what the actual fuck it still happens!!! argh, tomorrow new day!
            if (stack.getItem() instanceof net.minecraft.item.ItemBlock) {
                Block block = ((net.minecraft.item.ItemBlock) stack.getItem()).getBlock();
                if (block instanceof net.minecraft.block.BlockBanner) {
                    return; // do not show generic entity tooltip for banners
                }
            }
            Class<? extends Entity> entityClass = ENTITY_ITEM_MAP.getOrDefault(registryName, EntityArmorStand.class);

            ResourceLocation entityId = new ResourceLocation(registryName);
            PettingComfortData petEntry = PettingComfortRegistry.getEntry(entityId.toString());

            int entityCount = cache.getDecorativeEntityCount(entityClass); // <-- NEW: use decorativeEntityCounts
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

            tooltip.add(I18n.format("tooltip.chunkcomfort.decorative.line1", value, entityCount, entityEntry != null ? entityEntry.limit : 0));
            tooltip.add(I18n.format("tooltip.chunkcomfort.decorative.line2", group, groupPoints, totalGroupLimit));

            if (petEntry != null) {
                tooltip.add(I18n.format("tooltip.chunkcomfort.pet"));
            }
        }

        // -------------------
        // Block tooltip
        // -------------------
        if (isConfiguredBlock) {
            System.out.println("[ChunkComfort DEBUG]: Configured Block name " + registryName);

            // Get canonical ID (the "main" ID from config, e.g., minecraft:banner)
            String canonicalId = BlockComfortRegistry.getCanonicalIdFromRegistryName(registryName);

            // Get aliases (wall_banner, standing_banner)
            String[] aliases = BlockComfortRegistry.BLOCK_ALIASES.get(canonicalId);

            // Collect all block IDs to check (main + aliases)
            List<String> allIds = new ArrayList<>();
            allIds.add(canonicalId); // include main ID
            if (aliases != null) allIds.addAll(Arrays.asList(aliases));

            int totalAmount = 0;
            Block mainBlock = null; // we'll use the first valid block to fetch points/group/etc

            for (String id : allIds) {
                Block b = Block.getBlockFromName(id);
                if (b != null) {
                    if (mainBlock == null) mainBlock = b; // remember first valid block
                    int count = cache.blockCounts.getOrDefault(b, 0);
                    totalAmount += count;
                    System.out.println("[ChunkComfort DEBUG]: Counting block " + id + " = " + count);
                } else {
                    System.out.println("[ChunkComfort DEBUG]: Block not found for ID " + id);
                }
            }

            if (mainBlock != null) {
                int pointsPerBlock = BlockComfortRegistry.getValue(mainBlock);
                String groupName = BlockComfortRegistry.getGroup(mainBlock);
                int blockLimit = 0;
                BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(mainBlock);
                if (entry != null) blockLimit = entry.limit;

                int groupPoints = cache.groupTotals.getOrDefault(groupName, 0);
                int totalGroupLimit = GROUP_LIMITS.getOrDefault(groupName, 0);

                tooltip.add(I18n.format("tooltip.chunkcomfort.block.line1", pointsPerBlock, totalAmount, blockLimit));
                tooltip.add(I18n.format("tooltip.chunkcomfort.block.line2", groupName, groupPoints, totalGroupLimit));

                System.out.println("[ChunkComfort DEBUG]: Total amount for " + canonicalId + " = " + totalAmount);
            }
        }
    }
}