package chunkcomfort.network;

import chunkcomfort.client.ComfortTierUpToast;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTierUpHandler implements IMessageHandler<PacketTierUp, IMessage> {

    @Override
    public IMessage onMessage(PacketTierUp message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() ->
                ComfortTierUpToast.show(message.tierIndex)
        );
        return null;
    }
}
