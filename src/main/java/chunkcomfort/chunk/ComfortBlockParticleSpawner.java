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

    // Radius (in blocks) used both for the particle packet's target audience and,
    // for indirect placements with no associated player, for finding a nearby
    // player to evaluate comfort requirements against.
    private static final double PARTICLE_RADIUS = 16.0;

    public static void trySpawnComfortParticles(World world, BlockPos pos, EntityPlayer player, Block block, Entity entity) {
        if (world.isRemote) return; // server side only

        boolean shouldSpawn = false;

        if (block != null && BlockComfortRegistry.isComfortBlock(block)) {
            // REASON: player can be null when called from onNeighborNotify (indirect
            // placements like hammocks have no associated player). In that case we can't
            // run the per-player block/group limit check, but we can still gate on comfort
            // requirements by checking the nearest player within particle range — hammocks
            // are placed rarely enough that this lookup isn't a perf concern. If nobody is
            // nearby, nobody would receive the particle packet anyway, so just spawn.
            if (player == null) {
                EntityPlayer nearest = findNearestPlayer(world, pos, PARTICLE_RADIUS);
                if (nearest != null && !ComfortRequirementCheck.isComfortActive(nearest)) return;
                spawnComfortParticlesServer(world, pos);
                return;
            }

            // REASON: only celebrate the placement with particles if the player's
            // minimum comfort requirements (shelter/light/fire/temperature, per config)
            // are actually satisfied at their position. Without this, particles fired
            // purely off block/group limits, regardless of whether comfort was active.
            if (!ComfortRequirementCheck.isComfortActive(player)) return;

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
                        PARTICLE_RADIUS
                )
        );
    }

    /**
     * Finds the closest player to pos within maxDistance blocks, or null if none.
     * Used for indirect placements (no EntityPlayer available, e.g. hammocks) so we
     * can still gate on comfort requirements without needing the specific placer.
     */
    private static EntityPlayer findNearestPlayer(World world, BlockPos pos, double maxDistance) {
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        EntityPlayer closest = null;
        double closestDistSq = maxDistance * maxDistance;

        for (EntityPlayer candidate : world.playerEntities) {
            double distSq = candidate.getDistanceSq(centerX, centerY, centerZ);
            if (distSq <= closestDistSq) {
                closest = candidate;
                closestDistSq = distSq;
            }
        }

        return closest;
    }
}