package chunkcomfort.chunk;

import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.integration.simpledifficulty.SimpleDifficultyIntegration;
import chunkcomfort.integration.simpledifficulty.SimpleDifficultyTemperatureBridge;
import chunkcomfort.registry.ComfortRequirements;
import chunkcomfort.registry.FireBlockRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ComfortRequirementCheck {

    public static ComfortRequirements getRequirementsPresent(World world, BlockPos pos, EntityPlayer player) {
        // Shelter check
        boolean shelterOk = !ForgeConfigHandler.server.requireShelter || !world.canSeeSky(pos.up());

        // Light check
        int light = world.getLight(pos);
        boolean lightOk = ForgeConfigHandler.server.minLightLevel <= 0 || light >= ForgeConfigHandler.server.minLightLevel;

        // Fire check (only if required and at least one other requirement is satisfied)
        boolean fireOk = false;
        if (ForgeConfigHandler.server.requireFire && (shelterOk || lightOk)) {
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

        boolean temperatureOk = true; // default true if disabled
        double playerTemp = 0.0;
        if (ForgeConfigHandler.server.enableTemperatureComfort && SimpleDifficultyIntegration.LOADED) {
            playerTemp = SimpleDifficultyTemperatureBridge.getTemperatureLevel(player);
            temperatureOk = playerTemp >= ForgeConfigHandler.server.minComfortTemperature
                    && playerTemp <= ForgeConfigHandler.server.maxComfortTemperature;
        }

        return new ComfortRequirements(shelterOk, lightOk, fireOk, temperatureOk, playerTemp);
    }

    private static int getRadius() {
        return ForgeConfigHandler.server.chunkRadius;
    }
}