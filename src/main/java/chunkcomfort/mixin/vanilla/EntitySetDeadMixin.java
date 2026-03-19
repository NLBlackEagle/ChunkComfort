package chunkcomfort.mixin.vanilla;

import chunkcomfort.chunk.ChunkUpdateManager;
import chunkcomfort.registry.EntityComfortRegistry;
import chunkcomfort.registry.EntityComfortRegistry.ComfortEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntitySetDeadMixin {

    @Inject(method = "setDead", at = @At("HEAD"))
    public void onSetDead(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        World world = entity.world;

        if (world.isRemote) return; // only server side

        if (entity instanceof EntityLivingBase) return; // skip mobs/animals
        if (entity instanceof EntityPlayer) return; // skip players

        // Check if this entity is a comfort entity
        if (!EntityComfortRegistry.isComfortEntity(entity)) return;

        ComfortEntry entry = EntityComfortRegistry.getEntityEntry(entity);
        if (entry == null) return; // safety check

        // Remove its comfort contribution from the chunk
        ChunkUpdateManager.onEntityRemoved(world, entity.getPosition(), entity);

        System.out.println("[ChunkComfort] Comfort entity removed on setDead: " + entity);
    }
}