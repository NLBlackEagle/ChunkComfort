package chunkcomfort.network;

import chunkcomfort.client.ClientComfortStatus;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketComfortStatusHandler implements IMessageHandler<PacketComfortStatus, IMessage> {

    @Override
    public IMessage onMessage(PacketComfortStatus message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() ->
                ClientComfortStatus.missingFlags = message.missingFlags
        );
        return null;
    }
}
