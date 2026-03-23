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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChunkComfortClientTooltipHandler {

    /** Cached set of block registry names from config for quick lookup */
    private static final Set<String> CONFIGURED_COMFORT_BLOCKS = new HashSet<>();

    /** Call this if the config is reloaded */
    public static void refreshConfiguredBlocks() {
        CONFIGURED_COMFORT_BLOCKS.clear();
        for (String entry : ForgeConfigHandler.server.blockComfortEntries) {
            if (entry == null || entry.isEmpty()) continue;
            String blockName = entry.split(",")[0]; // extract <block> from <block>,<value>,<group>,<limit>
            CONFIGURED_COMFORT_BLOCKS.add(blockName);
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
            int livingLimit = LivingComfortRegistry.getGroupLimit(groupName);
            int totalGroupLimit = blockLimit + livingLimit;

            tooltip.add(String.format("§aBlock points: %d  Limit: %d/%d", pointsPerBlock, amountIn3x3, blockLimit));
            tooltip.add(String.format("§bGroup: %s  Points: %d/%d", groupName, groupPoints, totalGroupLimit));
        }
    }
}