package chunkcomfort.handlers;

import chunkcomfort.chunk.*;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.registry.EntityComfortRegistry;
import chunkcomfort.registry.PettingComfortRegistry;
import chunkcomfort.registry.PotionBlacklistRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class ChunkComfortEventHandler {

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.PlaceEvent event) {
        if (event instanceof BlockEvent.MultiPlaceEvent) return;
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        Block block = event.getPlacedBlock().getBlock();
        ChunkUpdateManager.onBlockPlaced(world, pos, block);
        ComfortBlockParticleSpawner.trySpawnComfortParticles(world, pos, event.getPlayer(), block, null);
    }

    @SubscribeEvent
    public void onMultiBlockPlaced(BlockEvent.MultiPlaceEvent event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        Block block = event.getPlacedBlock().getBlock();
        ChunkUpdateManager.invalidateChunk(world, pos);
        ComfortBlockParticleSpawner.trySpawnComfortParticles(world, pos, event.getPlayer(), block, null);
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
        if (entity == null) return;

        World world = entity.world;
        if (world == null || world.isRemote) return;

        if (entity instanceof EntityLivingBase) {
            if (!(entity instanceof EntityArmorStand)) return;
        }
        if (!EntityComfortRegistry.isComfortEntity(entity)) return;

        ComfortBlockParticleSpawner.trySpawnComfortParticles(world, entity.getPosition(), null, null, entity);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        if (player.world.isRemote) return;
        if (player.ticksExisted % ForgeConfigHandler.server.comfortCheckInterval != 0) return;


        int comfort = AreaComfortCalculator.calculatePlayerComfort(player);
        chunkcomfort.player.PlayerComfortManager.applyComfortEffects(player, comfort);
    }

    @SubscribeEvent
    public void onJoinWorld(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote && event.getEntity() instanceof EntityPlayer) {
            ChunkBossState.resetClient();
        }
    }


    @SubscribeEvent
    public void onPlayerLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player != null) {
            AreaComfortCalculator.removePlayerCache(event.player.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!event.shouldSetSpawn()) return;

        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;
        if (AreaComfortCalculator.isEnvironmentBlocked(player.world, player.getPosition())) return;

        int chance = ForgeConfigHandler.server.messagePercentage;
        if (player.world.rand.nextInt(100) >= chance) return;

        int comfort = AreaComfortCalculator.calculatePlayerComfort(player);
        ComfortMessage.send(player, comfort);
    }

    @SubscribeEvent
    public void onEntityAttacked(AttackEntityEvent event) {
        Entity entity = event.getTarget();
        if (entity == null) return;
        if (!(event.getEntity() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (AreaComfortCalculator.isEnvironmentBlocked(player.world, player.getPosition())) return;

        ResourceLocation key = EntityList.getKey(entity);
        if (key == null) return;

        PettingComfortData entry = PettingComfortRegistry.getEntry(key.toString());
        if (entry == null) return;

        // --- Tamed / owner checks ---
        if (entry.tamed && !(entity instanceof EntityTameable)) return;

        if (entry.ownerOnly) {
            if (!(entity instanceof EntityTameable)) return;
            if (!((EntityTameable) entity).isOwner(player)) return;
        }

        // Sneak = cancel attack (pet instead)
        if (player.isSneaking()) {
            event.setCanceled(true);
        } else {
            return;
        }

        // --- Comfort requirement ---
        if (entry.requiresComfortActivation &&
                !ComfortRequirementCheck.isComfortActive(player)) {
            return;
        }

        // --- Cooldown ---
        if (!PettingComfortManager.canPet(player, entity)) return;

        int activeCount = PettingComfortManager.countActivePets(
                player.getUniqueID(), entity.getClass()
        );
        if (activeCount >= entry.maxPettable) return;

        // --- SERVER: register + cooldown ---
        if (!player.world.isRemote) {
            PettingComfortManager.applyPettingBoostServer(player, entity, entry);
        }

        // --- CLIENT: visuals only ---
        if (player.world.isRemote) {
            PettingComfortManager.applyPettingBoostClient(player, entity, entry);
        }
    }

    @SubscribeEvent
    public void onPotionAdded(PotionEvent.PotionApplicableEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        if (AreaComfortCalculator.isEnvironmentBlocked(player.world, player.getPosition())) return;

        // REASON: previously called calculatePlayerComfort() here, triggering a full
        // chunk + entity scan on every potion addition. Since applyComfortEffects()
        // applies the comfort potion itself (via addPotionEffect), this caused a
        // feedback loop: comfort tick -> apply Speed -> onPotionAdded -> full recalc.
        // The cached value written by the regular tick is always fresh enough for
        // blacklist enforcement — comfort does not change between ticks — so reading
        // from the cache is both correct and free.
        int comfort = chunkcomfort.player.PlayerComfortManager.getCachedComfort(player);


        ResourceLocation id = Potion.REGISTRY.getNameForObject(event.getPotionEffect().getPotion());
        if (id == null) return;

        if (PotionBlacklistRegistry.isBlocked(comfort, id.toString())) {
            event.setResult(Event.Result.DENY);
        }
    }
}
