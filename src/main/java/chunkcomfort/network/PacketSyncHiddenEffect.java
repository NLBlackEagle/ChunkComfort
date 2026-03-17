package chunkcomfort.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketSyncHiddenEffect implements IMessage {

    public int entityId;
    public int potionId;
    public boolean hidden;

    public PacketSyncHiddenEffect() {}

    public PacketSyncHiddenEffect(int entityId, int potionId, boolean hidden) {
        this.entityId = entityId;
        this.potionId = potionId;
        this.hidden = hidden;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        potionId = buf.readInt();
        hidden = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(potionId);
        buf.writeBoolean(hidden);
    }
}