package chunkcomfort.chunk;

import chunkcomfort.network.NetworkHandler;
import chunkcomfort.network.SpawnParticlePacket;
import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.EntityComfortRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class ComfortBlockParticleHandler {

    public static void trySpawnComfortParticles(World world, BlockPos pos, EntityPlayer player, Block block, Entity entity) {
        if (world.isRemote) return; // Only run server-side

        // Determine if this is a comfort block or entity
        boolean isBlock = block != null && BlockComfortRegistry.isComfortBlock(block);
        boolean isEntity = entity != null && EntityComfortRegistry.isComfortEntity(entity);

        if (!isBlock && !isEntity) return;

        if (isBlock) {
            // Check cached counts for the block
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
        } else {
            // Entities don't have block limits; just spawn particles
            spawnComfortParticlesServer(world, pos);
        }
    }

    /**
     * Server-side: send a packet to all clients nearby to spawn particles.
     */
    private static void spawnComfortParticlesServer(World world, BlockPos pos) {
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