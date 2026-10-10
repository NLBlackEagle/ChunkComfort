package chunkcomfort.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketComfortStatus implements IMessage {

    // ComfortRequirements.MISSING_* / TOO_* flags for the requirements the player is missing
    public int missingFlags;

    public PacketComfortStatus() {}

    public PacketComfortStatus(int missingFlags) {
        this.missingFlags = missingFlags;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        missingFlags = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(missingFlags);
    }
}
