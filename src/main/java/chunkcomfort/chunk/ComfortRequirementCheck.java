package chunkcomfort.chunk;

import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.integration.simpledifficulty.SimpleDifficultyIntegration;
import chunkcomfort.integration.simpledifficulty.SimpleDifficultyTemperatureBridge;
import chunkcomfort.registry.ComfortRequirements;
import chunkcomfort.registry.FireBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ComfortRequirementCheck {

    public static ComfortRequirements getRequirementsPresent(World world, BlockPos pos, EntityPlayer player, Boolean fireNearbyHint) {

        // --- LIGHT CHECK (cheap) ---
        int light = world.getLight(pos);
        boolean lightOk = ForgeConfigHandler.server.minLightLevel <= 0 || light >= ForgeConfigHandler.server.minLightLevel;

        // --- SHELTER CHECK (cached per player chunk, periodic forced rescan) ---
        // REASON: the column walk (up to 256 block lookups) ran on every comfort check.
        // The result is cached on the player's PlayerChunkComfortCache. It is recomputed:
        // - when the player moves to a new chunk
        // - when world.canSeeSky() disagrees with the cached result (player stepped
        //   outside or came back inside without crossing a chunk boundary)
        // - every SHELTER_RESCAN_INTERVAL checks as a safety net for edge cases
        // world.canSeeSky() is a cheap heightmap lookup — it replaces the expensive
        // column walk as the change detector, only triggering the full scanShelter()
        // when sky exposure actually changes.
        boolean shelterOk = !ForgeConfigHandler.server.requireShelter; // default true if not required

        // REASON: early exit — the player needs every requirement anyway, so skip the
        // shelter check once light has already failed. lightOk is always true when
        // minLightLevel is disabled, so shelter is still checked in that case.
        if (ForgeConfigHandler.server.requireShelter && lightOk) {
            PlayerChunkComfortCache cache = AreaComfortCalculator.getCache(player);
            net.minecraft.util.math.ChunkPos currentChunk = new net.minecraft.util.math.ChunkPos(pos);

            boolean canSeeSky = world.canSeeSky(pos);

            boolean needsScan = cache.lastShelterChunk == null
                    || cache.lastShelterChunk.x != currentChunk.x
                    || cache.lastShelterChunk.z != currentChunk.z
                    || cache.shelterCheckCount >= PlayerChunkComfortCache.SHELTER_RESCAN_INTERVAL
                    || canSeeSky == cache.cachedShelterOk; // sky exposure changed

            if (needsScan) {
                cache.cachedShelterOk = scanShelter(world, pos);
                cache.lastShelterChunk = currentChunk;
                cache.shelterCheckCount = 0;
            } else {
                cache.shelterCheckCount++;
            }

            shelterOk = cache.cachedShelterOk;
        }

        // --- TEMPERATURE CHECK ---
        boolean temperatureOk = true;
        double playerTemp = 0.0;

        // Only calculate temperature if prior required conditions are met
        if (ForgeConfigHandler.server.enableTemperatureComfort && SimpleDifficultyIntegration.LOADED
                && lightOk && shelterOk) {
            playerTemp = SimpleDifficultyTemperatureBridge.getTemperatureLevel(player);
            temperatureOk = playerTemp >= ForgeConfigHandler.server.minComfortTemperature
                    && playerTemp <= ForgeConfigHandler.server.maxComfortTemperature;
        }

        // --- FIRE CHECK ---
        boolean fireOk = !ForgeConfigHandler.server.requireFire; // default true if fire not required

        // Only check fire if prior required conditions are met
        if (ForgeConfigHandler.server.requireFire && lightOk && shelterOk && temperatureOk) {
            // REASON: use the pre-computed fireNearby result when available to avoid
            // running the block scan twice per comfort check.
            if (fireNearbyHint != null) {
                fireOk = fireNearbyHint;
            } else {
                int radius = getRadius();
                int verticalRange = ForgeConfigHandler.server.fireScanVerticalRange;
                int minY = Math.max(0, pos.getY() - verticalRange);
                int maxY = Math.min(world.getHeight() - 1, pos.getY() + verticalRange);

                try {
                    fireOk = ChunkScanner.anyBlockMatches(
                            world,
                            pos,
                            radius,
                            minY,
                            maxY,
                            FireBlockRegistry::isFireBlock
                    );
                } catch (ChunkScanner.StopScanException e) {
                    fireOk = true; // early exit confirmed fire presence
                }
            }
        }

        return new ComfortRequirements(shelterOk, lightOk, fireOk, temperatureOk, playerTemp);
    }

    // Overload without hint — used by paths that don't have a pre-computed fireNearby.
    public static ComfortRequirements getRequirementsPresent(World world, BlockPos pos, EntityPlayer player) {
        return getRequirementsPresent(world, pos, player, null);
    }

    public static boolean isComfortActive(EntityPlayer player) {
        BlockPos pos = player.getPosition();
        ComfortRequirements reqs = getRequirementsPresent(player.world, pos, player);

        return reqs.shelterOk && reqs.lightOk && reqs.fireOk && reqs.temperatureOk;
    }

    private static int getRadius() {
        // Same clamped radius as the comfort calculation so both fire scans cover the same area
        return AreaComfortCalculator.getRadius();
    }

    private static boolean scanShelter(World world, BlockPos pos) {
        for (int y = pos.getY() + 1; y < world.getHeight(); y++) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            IBlockState state = world.getBlockState(checkPos);
            Block block = state.getBlock();
            if (!(block instanceof BlockAir) && !(block instanceof BlockLeaves) && block.getMaterial(state).isSolid()) {
                return true;
            }
        }
        return false;
    }
}