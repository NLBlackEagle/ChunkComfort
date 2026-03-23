package chunkcomfort.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ClientParticleHandler {

    public static void spawnComfortParticles(BlockPos pos) {
        World world = Minecraft.getMinecraft().world;

        for (int i = 0; i < 8; i++) {
            double x = pos.getX() + world.rand.nextDouble();
            double y = pos.getY() + 0.8 + world.rand.nextDouble() * 0.5;
            double z = pos.getZ() + world.rand.nextDouble();

            double motionX = (world.rand.nextDouble() - 0.5) * 0.3;
            double motionY = world.rand.nextDouble() * 0.3 + 0.1;
            double motionZ = (world.rand.nextDouble() - 0.5) * 0.3;

            world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, x, y, z, motionX, motionY, motionZ);
        }
    }
}