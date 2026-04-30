package chunkcomfort.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketTierUp implements IMessage {

    public int tierIndex;

    public PacketTierUp() {}

    public PacketTierUp(int tierIndex) {
        this.tierIndex = tierIndex;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        tierIndex = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(tierIndex);
    }
}
