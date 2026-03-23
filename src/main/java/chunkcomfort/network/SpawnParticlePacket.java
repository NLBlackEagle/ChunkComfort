package chunkcomfort.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class SpawnParticlePacket implements IMessage {
    private BlockPos pos;

    public SpawnParticlePacket() {} // Needed for FML

    public SpawnParticlePacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        pos = new BlockPos(x, y, z);
    }

    public BlockPos getPos() {
        return pos;
    }
}