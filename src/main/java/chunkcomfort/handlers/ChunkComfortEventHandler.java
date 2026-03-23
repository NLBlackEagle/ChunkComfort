package chunkcomfort.handlers;

import chunkcomfort.chunk.ChunkUpdateManager;
import chunkcomfort.chunk.ComfortWorldData;
import chunkcomfort.registry.EntityComfortRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

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
    public void onNonLivingEntitySpawn(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        World world = entity.world;

        if (world.isRemote) return; // only server side
        if (entity instanceof EntityLivingBase) {
            if (!(entity instanceof EntityArmorStand)) return; // skip living
        }
        if (!EntityComfortRegistry.isComfortEntity(entity)) return; // only comfort entities

        // Add comfort points immediately
        ChunkUpdateManager.onEntityAdded(world, entity.getPosition(), entity);
    }


    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        if (player.ticksExisted % chunkcomfort.config.ForgeConfigHandler.server.comfortCheckInterval != 0) return;

        chunkcomfort.player.PlayerComfortManager.applyComfortEffects(player);
    }

    @SubscribeEvent
    public void onPlayerMove(TickEvent.PlayerTickEvent event) {
        if (event.player.world.isRemote) return; // only server side

        ChunkPos pos = new ChunkPos(event.player.getPosition());
        ComfortWorldData.get(event.player.world).getChunkData(pos);
        // this triggers self-healing if the chunk cache is empty
    }
}