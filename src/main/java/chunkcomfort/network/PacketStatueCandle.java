package chunkcomfort.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Client -> server: light or take out the candle in a statue slot the player aimed at. */
public class PacketStatueCandle implements IMessage {

    public BlockPos pos;
    public int slot;
    public boolean insert;
    public EnumHand hand;

    public PacketStatueCandle() {}

    public PacketStatueCandle(BlockPos pos, int slot, boolean insert, EnumHand hand) {
        this.pos = pos;
        this.slot = slot;
        this.insert = insert;
        this.hand = hand;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        slot = buf.readByte();
        insert = buf.readBoolean();
        hand = buf.readBoolean() ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeByte(slot);
        buf.writeBoolean(insert);
        buf.writeBoolean(hand == EnumHand.OFF_HAND);
    }
}
