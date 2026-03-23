package chunkcomfort.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel("chunkcomfort");

    public static void register() {

        INSTANCE.registerMessage(
                PacketSyncHiddenEffectHandler.class,
                PacketSyncHiddenEffect.class,
                0,
                Side.CLIENT
        );

        INSTANCE.registerMessage(
                SpawnParticlePacketHandler.class,
                SpawnParticlePacket.class,
                1,
                Side.CLIENT
        );

    }
}