package chunkcomfort.handlers;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.chunk.PlayerChunkComfortCache;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.LivingComfortRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

public class ChunkComfortClientTooltipHandler {

    /** Cached set of block registry names from config for quick lookup */
    private static final Set<String> CONFIGURED_COMFORT_BLOCKS = new HashSet<>();
    private static final Map<String, Integer> GROUP_LIMITS = new HashMap<>();

    /** Call this if the config is reloaded */
    public static void refreshConfiguredBlocks() {
        CONFIGURED_COMFORT_BLOCKS.clear();
        for (String entry : ForgeConfigHandler.server.blockComfortEntries) {
            if (entry == null || entry.isEmpty()) continue;
            String blockName = entry.split(",")[0]; // extract <block> from <block>,<value>,<group>,<limit>
            CONFIGURED_COMFORT_BLOCKS.add(blockName);
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

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack == null || !(stack.getItem() instanceof ItemBlock)) return;

        Block block = ((ItemBlock) stack.getItem()).getBlock();
        String blockRegistryName = block.getRegistryName().toString();

        // Only for configured comfort blocks
        if (!CONFIGURED_COMFORT_BLOCKS.contains(blockRegistryName)) return;

        List<String> tooltip = event.getToolTip();

        // Always add static info so JEI search sees it
        if (!tooltip.contains("§eComfort Info:")) {
            tooltip.add("§eComfort Info:");
        }

        // Only add dynamic counts if player exists (in-world)
        EntityPlayer player = event.getEntityPlayer();
        if (player != null) {
            PlayerChunkComfortCache cache = AreaComfortCalculator.getCache(player);

            int pointsPerBlock = BlockComfortRegistry.getValue(block);
            String groupName = BlockComfortRegistry.getGroup(block);

            int blockLimit = 0;
            BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block);
            if (entry != null) blockLimit = entry.limit;

            int amountIn3x3 = cache.blockCounts.getOrDefault(block, 0);
            int groupPoints = cache.groupTotals.getOrDefault(groupName, 0);
            int totalGroupLimit = GROUP_LIMITS.getOrDefault(groupName, 0);


            tooltip.add(String.format("§aBlock points: %d  Limit: %d/%d", pointsPerBlock, amountIn3x3, blockLimit));
            tooltip.add(String.format("§bGroup: %s  Points: %d/%d", groupName, groupPoints, totalGroupLimit));
        }
    }
}