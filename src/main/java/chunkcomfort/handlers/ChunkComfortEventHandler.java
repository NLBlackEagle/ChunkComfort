package chunkcomfort.handlers;

import chunkcomfort.api.ICanBeHidden;
import chunkcomfort.chunk.ChunkUpdateManager;
import chunkcomfort.chunk.ComfortWorldData;
import chunkcomfort.registry.BlockComfortRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
    public void onEntityAdded(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        World world = entity.world;

        if (world.isRemote) return;

        // Only track comfort entities
        if (BlockComfortRegistry.getEntityEntry(entity) != null) {
            ChunkUpdateManager.onEntityAdded(world, entity.getPosition(), entity);
        }
    }

    @SubscribeEvent
    public void onEntityDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        Entity entity = event.getEntity();
        World world = entity.world;

        if (world.isRemote) return;

        // Skip all non-comfort entities quickly
        if (!BlockComfortRegistry.isComfortEntity(entity)) return;

        ChunkUpdateManager.onEntityRemoved(world, entity.getPosition(), entity);
    }

    @SubscribeEvent
    public void onCreativeEntityInteract(PlayerInteractEvent.EntityInteract event) {
        EntityPlayer player = event.getEntityPlayer();
        Entity entity = event.getTarget();
        World world = entity.world;

        if (world.isRemote) return;
        if (!player.capabilities.isCreativeMode) return;
        if (!BlockComfortRegistry.isComfortEntity(entity)) return;

        ChunkUpdateManager.onEntityRemoved(world, entity.getPosition(), entity);
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