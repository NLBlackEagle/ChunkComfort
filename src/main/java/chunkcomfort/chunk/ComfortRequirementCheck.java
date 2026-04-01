package chunkcomfort.chunk;

import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.integration.simpledifficulty.SimpleDifficultyIntegration;
import chunkcomfort.integration.simpledifficulty.SimpleDifficultyTemperatureBridge;
import chunkcomfort.registry.ComfortRequirements;
import chunkcomfort.registry.FireBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ComfortRequirementCheck {

    public static ComfortRequirements getRequirementsPresent(World world, BlockPos pos, EntityPlayer player) {

        // --- LIGHT CHECK (cheap) ---
        int light = world.getLight(pos);
        boolean lightOk = ForgeConfigHandler.server.minLightLevel <= 0 || light >= ForgeConfigHandler.server.minLightLevel;

        // --- SHELTER CHECK (expensive, only if light is required and passed) ---
        boolean shelterOk = !ForgeConfigHandler.server.requireShelter; // default true if not required

        if (ForgeConfigHandler.server.requireShelter && ForgeConfigHandler.server.minLightLevel > 0 && lightOk) {
            shelterOk = false; // default false
            for (int y = pos.getY() + 1; y < world.getHeight(); y++) {
                BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
                IBlockState state = world.getBlockState(checkPos);
                Block block = state.getBlock();

                // Check if block is a "solid roof block" — not leaves, not plants, not air
                if (block.getMaterial(state).isSolid() && block.isOpaqueCube(state)) {
                    shelterOk = true;
                    break;
                }
            }
        }

        // --- TEMPERATURE CHECK ---
        boolean temperatureOk = true;
        double playerTemp = 0.0;

        if (ForgeConfigHandler.server.enableTemperatureComfort && SimpleDifficultyIntegration.LOADED) {
            // Only calculate temperature if prior required conditions are met
            boolean priorConditions = true;
            if (ForgeConfigHandler.server.requireShelter) priorConditions &= shelterOk;
            if (ForgeConfigHandler.server.minLightLevel > 0) priorConditions &= lightOk;

            if (priorConditions) {
                playerTemp = SimpleDifficultyTemperatureBridge.getTemperatureLevel(player);
                temperatureOk = playerTemp >= ForgeConfigHandler.server.minComfortTemperature
                        && playerTemp <= ForgeConfigHandler.server.maxComfortTemperature;
            }
        }

        // --- FIRE CHECK ---
        boolean fireOk = !ForgeConfigHandler.server.requireFire; // default true if fire not required

        if (ForgeConfigHandler.server.requireFire) {
            boolean priorConditionsMet = true;

            if (ForgeConfigHandler.server.requireShelter) priorConditionsMet &= shelterOk;
            if (ForgeConfigHandler.server.minLightLevel > 0) priorConditionsMet &= lightOk;
            if (ForgeConfigHandler.server.enableTemperatureComfort) priorConditionsMet &= temperatureOk;

            if (priorConditionsMet) {
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

    public static boolean isComfortActive(EntityPlayer player) {
        BlockPos pos = player.getPosition();
        ComfortRequirements reqs = getRequirementsPresent(player.world, pos, player);

        return reqs.shelterOk && reqs.lightOk && reqs.fireOk && reqs.temperatureOk;
    }

    private static int getRadius() {
        return ForgeConfigHandler.server.chunkRadius;
    }
}