package chunkcomfort.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.Collection;

public class PacketStatueStatus implements IMessage {

    // Statue candles lit in the player's comfort zone (StatueRegistry.slotKey values)
    public int[] litSlotKeys;
    // The player's comfort, so locked statue effects can show when they unlock
    public int comfort;

    public PacketStatueStatus() {}

    public PacketStatueStatus(Collection<Integer> litSlots, int comfort) {
        this.litSlotKeys = litSlots.stream().mapToInt(Integer::intValue).toArray();
        this.comfort = comfort;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        comfort = buf.readInt();
        litSlotKeys = new int[buf.readShort()];
        for (int i = 0; i < litSlotKeys.length; i++) litSlotKeys[i] = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(comfort);
        buf.writeShort(litSlotKeys.length);
        for (int key : litSlotKeys) buf.writeInt(key);
    }
}
