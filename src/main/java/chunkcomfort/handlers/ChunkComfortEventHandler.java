package chunkcomfort.handlers;

import chunkcomfort.chunk.ChunkUpdateManager;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
}