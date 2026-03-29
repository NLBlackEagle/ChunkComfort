package chunkcomfort.handlers;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.chunk.PettingComfortData;
import chunkcomfort.chunk.PlayerChunkComfortCache;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.EntityComfortRegistry;
import chunkcomfort.registry.LivingComfortRegistry;
import chunkcomfort.registry.PettingComfortRegistry;
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
    private static final Set<String> CONFIGURED_COMFORT_BLOCKS = new HashSet<>();
    private static final Map<String, Integer> GROUP_LIMITS = new HashMap<>();
    private static final Set<String> FIRE_BLOCKS = new HashSet<>();

    /** Call this if the config is reloaded */
    public static void refreshConfiguredBlocks() {
        CONFIGURED_COMFORT_BLOCKS.clear();
        for (String entry : ForgeConfigHandler.server.blockComfortEntries) {
            if (entry == null || entry.isEmpty()) continue;
            String blockName = entry.split(",")[0]; // extract <block> from <block>,<value>,<group>,<limit>
            CONFIGURED_COMFORT_BLOCKS.add(blockName);
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
                    if (entity instanceof EntityLiving) {
                        int entityCount = cache.entityCounts.getOrDefault(entity.getClass(), 0);
                        int groupPoints = cache.entityGroupTotals.getOrDefault(livingEntry.group, 0);
                        int totalGroupLimit = LivingComfortRegistry.getGroupLimit(livingEntry.group);

                        tooltip.add(I18n.format("tooltip.chunkcomfort.living.line1", livingEntry.value, entityCount, livingEntry.limit));
                        tooltip.add(I18n.format("tooltip.chunkcomfort.living.line2", livingEntry.group, groupPoints, totalGroupLimit));

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
        boolean isConfiguredBlock = CONFIGURED_COMFORT_BLOCKS.contains(registryName);
        boolean isEntityItem = NON_BLOCK_ENTITIES.contains(registryName);
        boolean isFireBlock = FIRE_BLOCKS.contains(registryName);
        EntityComfortRegistry.ComfortEntry entityEntry = EntityComfortRegistry.getEntityEntryFromId(new ResourceLocation(registryName));


        // Nothing to show? Exit early
        if (!isConfiguredBlock && entityEntry == null && !isEntityItem && !isFireBlock) return;

        // Add JEI header if it wasn’t added yet
        String header = I18n.format("tooltip.chunkcomfort.header");
        if (!tooltip.contains(header)) tooltip.add(header);

        // -------------------
        // Fire tooltip
        // -------------------
        String fireLine = I18n.format("tooltip.chunkcomfort.fire");
        if (!tooltip.contains(fireLine)) tooltip.add(fireLine);

        if (player == null) return;

        // -------------------
        // Non-living / generic entity tooltip (skip if spawn egg already handled)
        // -------------------
        if (!handledSpawnEgg && (entityEntry != null || isEntityItem)) {
            Class<? extends Entity> entityClass = ENTITY_ITEM_MAP.getOrDefault(registryName, EntityArmorStand.class);

            ResourceLocation entityId = new ResourceLocation(registryName);
            PettingComfortData petEntry = PettingComfortRegistry.getEntry(entityId.toString());

            int entityCount = cache.entityCounts.getOrDefault(entityClass, 0);
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

            tooltip.add(I18n.format("tooltip.chunkcomfort.entity.line1", value, entityCount, entityEntry != null ? entityEntry.limit : 0));
            tooltip.add(I18n.format("tooltip.chunkcomfort.entity.line2", group, groupPoints, totalGroupLimit));

        }

        // -------------------
        // Block tooltip
        // -------------------
        if (isConfiguredBlock) {
            Block block = Block.getBlockFromName(registryName);
            if (block != null) {
                int pointsPerBlock = BlockComfortRegistry.getValue(block);
                String groupName = BlockComfortRegistry.getGroup(block);
                int blockLimit = 0;
                BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block);
                if (entry != null) blockLimit = entry.limit;

                int amountIn3x3 = cache.blockCounts.getOrDefault(block, 0);
                int groupPoints = cache.groupTotals.getOrDefault(groupName, 0);
                int totalGroupLimit = GROUP_LIMITS.getOrDefault(groupName, 0);

                tooltip.add(I18n.format("tooltip.chunkcomfort.block.line1", pointsPerBlock, amountIn3x3, blockLimit));
                tooltip.add(I18n.format("tooltip.chunkcomfort.block.line2", groupName, groupPoints, totalGroupLimit));
            }
        }
    }
}