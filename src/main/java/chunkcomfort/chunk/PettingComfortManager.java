package chunkcomfort.chunk;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentString;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PettingComfortManager {

    // Track cooldowns: player UUID -> entity UUID -> next pet time
    private static final Map<UUID, Map<UUID, Long>> cooldowns = new HashMap<>();

    /** Can this player pet this entity right now? */
    public static boolean canPet(EntityPlayer player, Entity entity, PettingComfortData entry) {
        UUID playerId = player.getUniqueID();
        UUID entityId = entity.getUniqueID();
        long now = System.currentTimeMillis();

        Map<UUID, Long> playerCooldowns = cooldowns.getOrDefault(playerId, new HashMap<>());
        Long nextPetTime = playerCooldowns.get(entityId);

        return nextPetTime == null || now >= nextPetTime;
    }

    /** Apply the petting boost */
    public static void applyPettingBoost(EntityPlayer player, Entity entity, PettingComfortData entry) {
        UUID playerId = player.getUniqueID();
        UUID entityId = entity.getUniqueID();
        long now = System.currentTimeMillis();

        PlayerChunkComfortCache cache = PlayerChunkComfortCache.get(player);

        // Enforce maxPettable per entity type
        long activeCountByType = cache.countActiveBoostsByType(entity.getClass());
        if (activeCountByType >= entry.maxPettable) return;

        // Add temporary comfort per entity instance
        cache.addTemporaryComfort(entityId, entry.comfortBoost, entry.boostSeconds, entity.getClass());

        // Spawn hearts above entity
        spawnHeartParticles(player, entry.comfortBoost, entity);

        // Update cooldown per entity instance
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(entityId, now + entry.cooldownSeconds * 1000L);


        String text = I18n.format("tooltip.chunkcomfort.petting.message", entity.getName(), entry.comfortBoost, entry.boostSeconds);
        ITextComponent message = new TextComponentString(text);
        player.sendMessage(message);
    }

    /** Spawn hearts above the entity petted */
    public static void spawnHeartParticles(EntityPlayer player, float comfortAmount, Entity entity) {
        World world = player.world;
        if (!world.isRemote) return;

        int hearts = Math.max(1, Math.round(comfortAmount));
        Vec3d pos = entity.getPositionVector().add(0, 1, 0);

        for (int i = 0; i < hearts; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 0.5;
            double offsetY = world.rand.nextDouble() * 0.5;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 0.5;

            world.spawnParticle(net.minecraft.util.EnumParticleTypes.HEART,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    0, 0.05, 0);
        }
    }
}