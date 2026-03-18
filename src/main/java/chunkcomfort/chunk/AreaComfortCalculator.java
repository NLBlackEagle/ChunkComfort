package chunkcomfort.chunk;

import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.registry.BiomeComfortRegistry;
import chunkcomfort.registry.FireBlockRegistry;
import chunkcomfort.registry.PotionRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class AreaComfortCalculator {

    private static final Map<String, Integer> GROUP_LIMITS = new HashMap<>();

    public static int getRadius() {
        return Math.min(Math.max(ForgeConfigHandler.server.chunkRadius, 0), 3);
    }

    public static void reloadGroupLimits(String[] limits) {
        GROUP_LIMITS.clear();
        if (limits == null) return;

        for (String line : limits) {
            if (line == null || line.isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 2) continue;

            String group = parts[0];
            int limit;

            try {
                limit = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                limit = Integer.MAX_VALUE;
            }

            GROUP_LIMITS.put(group, limit);
        }
    }

    /**
     * Check activation conditions: shelter + light + fire.
     */
    public static int calculateComfortActivation(World world, int chunkX, int chunkZ, EntityPlayer player) {
        int comfortActive = 0;
        int requiredConditions = 0;

        BlockPos playerPos = player.getPosition();

        // Determine required conditions
        if (ForgeConfigHandler.server.requireShelter) requiredConditions++;
        if (ForgeConfigHandler.server.minLightLevel > 0) requiredConditions++;
        if (ForgeConfigHandler.server.requireFire) requiredConditions++;

        // Shelter requirement
        boolean shelterOk = false;
        if (ForgeConfigHandler.server.requireShelter && !world.canSeeSky(playerPos.up())) {
            comfortActive++;
            shelterOk = true;
        }

        // Early exit: if all conditions already met, no need to scan further
        if (comfortActive >= requiredConditions) return comfortActive;

        // Minimum light requirement
        boolean lightOk = false;
        int light = world.getLight(playerPos);
        if (ForgeConfigHandler.server.minLightLevel > 0 && light >= ForgeConfigHandler.server.minLightLevel) {
            comfortActive++;
            lightOk = true;
        }

        // Early exit: if all conditions already met
        if (comfortActive >= requiredConditions) return comfortActive;

        // Fire requirement — only scan if it can contribute
        if (ForgeConfigHandler.server.requireFire) {
            // Early skip if neither shelter nor light contributes
            if (shelterOk || lightOk) {
                if (isFirePresent(world, playerPos)) {
                    comfortActive++;
                }
            }
        }

        return comfortActive;
    }

    /**
     * Live fire scan around player within chunk radius.
     */
    public static boolean isFirePresent(World world, BlockPos playerPos) {
        // Early exit if fire requirement is disabled
        if (!ForgeConfigHandler.server.requireFire) return false;

        // Early exit: check if shelter and light conditions already fail
        boolean shelterOk = !ForgeConfigHandler.server.requireShelter || !world.canSeeSky(playerPos.up());
        int light = world.getLight(playerPos);
        boolean lightOk = ForgeConfigHandler.server.minLightLevel <= 0 || light >= ForgeConfigHandler.server.minLightLevel;

        // If shelter or light conditions are not met, fire won't help
        if (!shelterOk && !lightOk) return false;

        int radius = getRadius();
        int verticalRange = ForgeConfigHandler.server.fireScanVerticalRange;

        BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;

        outerLoop:
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(playerChunkX + dx, playerChunkZ + dz);
                int startX = chunkPos.x * 16;
                int startZ = chunkPos.z * 16;
                int minY = Math.max(0, playerPos.getY() - verticalRange);
                int maxY = Math.min(world.getHeight() - 1, playerPos.getY() + verticalRange);

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = maxY; y >= minY; y--) {
                            scanPos.setPos(startX + x, y, startZ + z);
                            Block block = world.getBlockState(scanPos).getBlock();
                            if (block.isAir(world.getBlockState(scanPos), world, scanPos)) continue;
                            if (FireBlockRegistry.isFireBlock(block)) {
                                return true; // fire found
                            }
                        }
                    }
                }
            }
        }

        return false; // no fire found
    }

    /**
     * Calculate total comfort for the player using cached group totals.
     */
    public static int calculatePlayerComfort(EntityPlayer player) {
        ChunkPos center = new ChunkPos(player.getPosition());

        int comfortActive = calculateComfortActivation(
                player.world,
                center.x,
                center.z,
                player
        );

        int requiredConditions = 0;
        if (ForgeConfigHandler.server.requireShelter) requiredConditions++;
        if (ForgeConfigHandler.server.minLightLevel > 0) requiredConditions++;
        if (ForgeConfigHandler.server.requireFire) requiredConditions++;

        if (comfortActive < requiredConditions) {
            if (PotionRegistry.COMFORT != null) {
                player.removePotionEffect(PotionRegistry.COMFORT);
            }
            return 0;
        }

        int radius = getRadius();

        Map<String, Integer> summedGroups = new HashMap<>();
        ComfortWorldData worldData = ComfortWorldData.get(player.world);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);
                ChunkComfortData data = worldData.getOrCreateChunkData(player.world, pos);

                for (Map.Entry<String, Integer> entry : data.groupTotals.entrySet()) {
                    summedGroups.put(
                            entry.getKey(),
                            summedGroups.getOrDefault(entry.getKey(), 0) + entry.getValue()
                    );
                }
            }
        }

        int totalComfort = 0;
        for (Map.Entry<String, Integer> entry : summedGroups.entrySet()) {
            String group = entry.getKey();
            int value = entry.getValue();
            int limit = getGroupLimit(group);
            totalComfort += Math.min(value, limit);
        }

        String biomeName = player.world
                .getBiome(player.getPosition())
                .getRegistryName()
                .toString();

        int biomeModifier = BiomeComfortRegistry.getBiomeModifier(biomeName);
        totalComfort += biomeModifier;

        return Math.max(totalComfort, 0);
    }
    public static void recalcChunk(World world, ChunkPos chunkPos) {
        ComfortWorldData worldData = ComfortWorldData.get(world);
        worldData.recalcChunk(world, chunkPos);
    }

    public static int getGroupLimit(String group) {
        return GROUP_LIMITS.getOrDefault(group, Integer.MAX_VALUE);
    }
}