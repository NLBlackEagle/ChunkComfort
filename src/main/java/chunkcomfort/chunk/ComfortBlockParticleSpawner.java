package chunkcomfort.chunk;

import chunkcomfort.network.NetworkHandler;
import chunkcomfort.network.SpawnParticlePacket;
import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.EntityComfortRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class ComfortBlockParticleSpawner {

    public static void trySpawnComfortParticles(World world, BlockPos pos, EntityPlayer player, Block block, Entity entity) {
        if (world.isRemote) return; // server side only

        boolean shouldSpawn = false;

        if (block != null && BlockComfortRegistry.isComfortBlock(block)) {
            // Block case: same as before
            String groupName = BlockComfortRegistry.getGroup(block);
            PlayerChunkComfortCache cache = AreaComfortCalculator.getCache(player);
            cache.ensureUpToDate();
            int currentCount = cache.blockCounts.getOrDefault(block, 0);
            BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block);
            int itemLimit = entry != null ? entry.limit : 0;
            int groupPoints = cache.groupTotals.getOrDefault(groupName, 0);
            int groupLimit = BlockComfortRegistry.getGroupLimit(groupName);

            if (currentCount < itemLimit && groupPoints < groupLimit) shouldSpawn = true;

        } else if (entity != null) {
            // Non-living entity: armor stand, painting, etc
            ResourceLocation id = EntityList.getKey(entity);
            if (id != null) {
                EntityComfortRegistry.ComfortEntry entry = EntityComfortRegistry.getEntityEntryFromId(id);
                if (entry != null) {
                    String groupName = entry.group;

                    // Count how many exist in the chunk/world
                    int count = 0;
                    AxisAlignedBB box = new AxisAlignedBB(pos).grow(8); // 1 chunk-ish radius
                    for (Entity e : world.getEntitiesWithinAABB(Entity.class, box)) {
                        ResourceLocation eId = EntityList.getKey(e);
                        if (id.equals(eId)) count++;
                    }

                    int itemLimit = entry.limit;
                    int groupPoints = 0;
                    AxisAlignedBB boxGroup = new AxisAlignedBB(pos).grow(8);
                    for (Entity e : world.getEntitiesWithinAABB(Entity.class, boxGroup)) {
                        EntityComfortRegistry.ComfortEntry groupEntry = EntityComfortRegistry.getEntityEntryFromId(EntityList.getKey(e));
                        if (groupEntry != null && groupEntry.group.equals(groupName)) {
                            groupPoints++;
                        }
                    }

                    int groupLimit = BlockComfortRegistry.getGroupLimit(groupName);

                    if (count < itemLimit && groupPoints < groupLimit) shouldSpawn = true;
                }
            }
        }

        if (shouldSpawn) {
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