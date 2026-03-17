package chunkcomfort.handlers;

import chunkcomfort.chunk.ChunkUpdateManager;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.player.PlayerComfortManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ChunkComfortEventHandler {

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.PlaceEvent event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        Block block = event.getPlacedBlock().getBlock();

        ChunkUpdateManager.onBlockPlaced(world, pos, block);
    }

    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        Block block = event.getState().getBlock();

        ChunkUpdateManager.onBlockBroken(world, pos, block);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        // Use config interval instead of hardcoded 20
        if (player.ticksExisted % ForgeConfigHandler.server.comfortCheckInterval != 0) return;

        PlayerComfortManager.applyComfortEffects(player);
    }
}