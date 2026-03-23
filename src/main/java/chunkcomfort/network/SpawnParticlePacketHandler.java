package chunkcomfort.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class SpawnParticlePacketHandler implements IMessageHandler<SpawnParticlePacket, IMessage> {

    @Override
    public IMessage onMessage(SpawnParticlePacket message, MessageContext ctx) {
        if (ctx.side == Side.CLIENT) {
            Minecraft.getMinecraft().addScheduledTask(() ->
                    chunkcomfort.client.ClientParticleHandler.spawnComfortParticles(message.getPos())
            );
        }
        return null;
    }
}