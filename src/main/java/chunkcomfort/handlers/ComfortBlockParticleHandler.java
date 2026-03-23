package chunkcomfort.handlers;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.chunk.PlayerChunkComfortCache;
import chunkcomfort.network.NetworkHandler;
import chunkcomfort.network.SpawnParticlePacket;
import chunkcomfort.registry.BlockComfortRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class ComfortBlockParticleHandler {

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.PlaceEvent event) {
        Block block = event.getPlacedBlock().getBlock();
        World world = event.getWorld();
        BlockPos pos = event.getPos();

        if (!BlockComfortRegistry.isComfortBlock(block)) return; // Only comfort blocks

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
            spawnComfortParticlesServer(world, pos);
        }
    }

    /**
     * Server-side: send a packet to all clients nearby to spawn particles.
     */
    private void spawnComfortParticlesServer(World world, BlockPos pos) {
        if (world.isRemote) return; // Only run on server

        // Send packet to all players within 16 blocks
        NetworkHandler.INSTANCE.sendToAllAround(
                new SpawnParticlePacket(pos),
                new NetworkRegistry.TargetPoint(
                        world.provider.getDimension(),
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        16 // radius in blocks
                )
        );
    }
}