package chunkcomfort.network;

import chunkcomfort.client.ClientComfortStatus;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketStatueStatusHandler implements IMessageHandler<PacketStatueStatus, IMessage> {

    @Override
    public IMessage onMessage(PacketStatueStatus message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            ClientComfortStatus.comfort = message.comfort;
            ClientComfortStatus.litStatueSlots.clear();
            for (int key : message.litSlotKeys) ClientComfortStatus.litStatueSlots.add(key);
        });
        return null;
    }
}
