package chunkcomfort.network;

import chunkcomfort.statue.BlockStatue;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketStatueCandleHandler implements IMessageHandler<PacketStatueCandle, IMessage> {

    @Override
    public IMessage onMessage(PacketStatueCandle message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        WorldServer world = player.getServerWorld();

        world.addScheduledTask(() -> {
            // Never trust the client: the block must be loaded, in reach and actually a statue
            if (!world.isBlockLoaded(message.pos)) return;
            if (player.getDistanceSqToCenter(message.pos) > 64) return;

            Block block = world.getBlockState(message.pos).getBlock();
            if (!(block instanceof BlockStatue)) return;

            ((BlockStatue) block).handleCandleClick(world, message.pos, player, message.hand, message.slot, message.insert);
        });
        return null;
    }
}
