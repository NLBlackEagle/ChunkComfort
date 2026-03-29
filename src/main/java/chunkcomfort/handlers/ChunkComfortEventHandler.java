package chunkcomfort.handlers;

import chunkcomfort.chunk.*;
import chunkcomfort.registry.EntityComfortRegistry;
import chunkcomfort.registry.PettingComfortRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.UUID;

public class ChunkComfortEventHandler {


    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.PlaceEvent event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        Block block = event.getPlacedBlock().getBlock();

        ChunkUpdateManager.onBlockPlaced(world, pos, block);
        ComfortBlockParticleSpawner.trySpawnComfortParticles(world, pos, event.getPlayer(), block, null);
    }

    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        Block block = event.getState().getBlock();

        if (world == null || pos == null || block == null) return;

        ChunkUpdateManager.onBlockBroken(world, pos, block);
    }

    @SubscribeEvent
    public void onNonLivingEntitySpawn(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;

        World world = entity.world;
        if (world == null || world.isRemote) return;

        if (entity instanceof EntityLivingBase) {
            if (!(entity instanceof EntityArmorStand)) return; // skip living
        }
        if (!EntityComfortRegistry.isComfortEntity(entity)) return; // only comfort entities

        // Add comfort points immediately
        ChunkUpdateManager.onEntityAdded(world, entity.getPosition(), entity);
        ComfortBlockParticleSpawner.trySpawnComfortParticles(world, entity.getPosition(), null, null, entity);
    }


    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        if (player == null || player.world == null || player.world.isRemote) return;

        if (player.ticksExisted % chunkcomfort.config.ForgeConfigHandler.server.comfortCheckInterval != 0) return;



        chunkcomfort.player.PlayerComfortManager.applyComfortEffects(player);
    }

    @SubscribeEvent
    public void onPlayerMove(TickEvent.PlayerTickEvent event) {

        EntityPlayer player = event.player;
        if (player == null || player.world == null || player.world.isRemote) return;

        ChunkPos pos = new ChunkPos(event.player.getPosition());
        ComfortWorldData.get(event.player.world).getChunkData(pos);
        // this triggers self-healing if the chunk cache is empty
    }

    @SubscribeEvent
    public void onEntityAttacked(AttackEntityEvent event) {

        Entity entity = event.getTarget();
        if (entity == null) return;

        if (!(event.getEntity() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntity();

        ResourceLocation key = EntityList.getKey(entity);
        if (key == null) return;

        String entityId = key.toString();
        PettingComfortData entry = PettingComfortRegistry.getEntry(entityId);
        if (entry == null) return; // Not configured for petting

        // --- Tamed / owner checks ---
        if ((entry.tamed) && !(entity instanceof EntityTameable)) return;
        if ((entry.ownerOnly) && !((EntityTameable) entity).isOwner(player)) return;

        if (player.isSneaking()) event.setCanceled(true);

        // --- Minimal comfort requirements ---
        if (entry.requiresComfortActivation && !ComfortRequirementCheck.isComfortActive(player)) return;


        // --- Cooldown check ---
        if (!PettingComfortManager.canPet(player, entity, entry)) return;

        // --- Max pettable enforcement per entity ---
        PlayerChunkComfortCache cache = PlayerChunkComfortCache.get(player);
        if (cache == null) return;
        long now = System.currentTimeMillis();

        // Count active boosts for this player
        long activeCount = cache.tempComforts.values().stream()
                .filter(boost -> boost.expireTime > now)
                .count();

        if (activeCount >= entry.maxPettable) return; // max active boosts reached

        // --- Apply the petting boost ---
        PettingComfortManager.applyPettingBoost(player, entity, entry);
    }
}