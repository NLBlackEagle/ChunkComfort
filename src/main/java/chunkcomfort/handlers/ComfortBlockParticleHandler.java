package chunkcomfort.handlers;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.chunk.PlayerChunkComfortCache;
import chunkcomfort.registry.BlockComfortRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ComfortBlockParticleHandler {

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.PlaceEvent event) {
        Block block = event.getPlacedBlock().getBlock();
        World world = event.getWorld();
        BlockPos pos = event.getPos();

        if (!BlockComfortRegistry.isComfortBlock(block)) return; // Only comfort blocks
        if (world.isRemote) return; // Only run client side

        EntityPlayer player = event.getPlayer();
        if (player == null) return;

        // Check the current cached counts for this block and its group
        PlayerChunkComfortCache cache = AreaComfortCalculator.getCache(player);
        int currentBlockCount = cache.blockCounts.getOrDefault(block, 0);
        int blockLimit = 0;
        String groupName = BlockComfortRegistry.getGroup(block);

        BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block);
        if (entry != null) blockLimit = entry.limit;

        int currentGroupPoints = cache.groupTotals.getOrDefault(groupName, 0);
        int groupLimit = BlockComfortRegistry.getGroupLimit(groupName);

        boolean underBlockLimit = currentBlockCount < blockLimit;
        boolean underGroupLimit = currentGroupPoints < groupLimit;

        if (underBlockLimit && underGroupLimit) {
            spawnComfortParticles(world, pos);
        }
    }

    private void spawnComfortParticles(World world, BlockPos pos) {
        for (int i = 0; i < 8; i++) {
            double x = pos.getX() + 0.2 + world.rand.nextDouble() * 0.6;
            double y = pos.getY() + 0.2 + world.rand.nextDouble() * 0.6;
            double z = pos.getZ() + 0.2 + world.rand.nextDouble() * 0.6;

            // Motion vector for sparkle
            double motionX = (world.rand.nextDouble() - 0.5) * 0.1;
            double motionY = world.rand.nextDouble() * 0.1 + 0.05;
            double motionZ = (world.rand.nextDouble() - 0.5) * 0.1;

            // SPELL_MOB particle with green color
            world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.SPELL_MOB,
                    x, y, z,
                    motionX, motionY, motionZ
            );
        }
    }
}