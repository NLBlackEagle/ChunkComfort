package chunkcomfort.mixin.comforts;

import c4.comforts.common.blocks.BlockRope;
import chunkcomfort.chunk.ChunkUpdateManager;
import chunkcomfort.chunk.ComfortBlockParticleSpawner;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRope.class)
public abstract class BlockRopeMixin {

    @Inject(method = "hangHammock", at = @At("TAIL"), remap = false)
    private void chunkcomfort$onHangHammock(World world, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing facing, CallbackInfo ci) {
        if (world.isRemote) return;

        Block block = world.getBlockState(pos).getBlock();
        ChunkUpdateManager.onBlockPlaced(world, pos, block);
        ComfortBlockParticleSpawner.trySpawnComfortParticles(world, pos, player, block, null);
    }
}