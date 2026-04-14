package chunkcomfort.handlers;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.chunk.PlayerChunkComfortCache;
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

    /**
     * Attempt to spawn comfort particles for a block or entity.
     * This method is safe even if the player's cache hasn't been initialized yet.
     */
    public static void trySpawnComfortParticles(World world, BlockPos pos, EntityPlayer player, Block block, Entity entity) {
        if (world.isRemote) return; // Only server-side

        if (AreaComfortCalculator.isEnvironmentBlocked(world, player, pos)) return;

        boolean isBlock = block != null && BlockComfortRegistry.isComfortBlock(block);
        boolean isEntity = entity != null && EntityComfortRegistry.isComfortEntity(entity);

        if (!isBlock && !isEntity) return;

        if (isBlock) {
            // Get the player's cache safely
            PlayerChunkComfortCache cache = AreaComfortCalculator.getCache(player);
            cache.ensureUpToDate();

            // Skip if cache is empty (not initialized yet)
            if (cache.blockCounts.isEmpty() && cache.groupTotals.isEmpty()) return;

            int currentBlockCount = cache.blockCounts.getOrDefault(block, 0);
            BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block);
            if (entry == null) return;

            int blockLimit = entry.limit;
            String groupName = entry.group;
            int currentGroupPoints = cache.groupTotals.getOrDefault(groupName, 0);
            int groupLimit = BlockComfortRegistry.getGroupLimit(groupName);

            boolean underBlockLimit = currentBlockCount < blockLimit;
            boolean underGroupLimit = currentGroupPoints < groupLimit;

            if (underBlockLimit && underGroupLimit) {
                spawnComfortParticlesServer(world, pos);
            }
        } else {
            // Entities: no block/group limits, just spawn
            spawnComfortParticlesServer(world, pos);
        }
    }

    /**
     * Send particle spawn packet to all players around a position.
     */
    private static void spawnComfortParticlesServer(World world, BlockPos pos) {
        if (world.isRemote) return; // only server

        NetworkHandler.INSTANCE.sendToAllAround(
                new SpawnParticlePacket(pos),
                new NetworkRegistry.TargetPoint(
                        world.provider.getDimension(),
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        16 // radius
                )
        );
    }
}