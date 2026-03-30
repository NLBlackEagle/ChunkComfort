package chunkcomfort.chunk;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PettingComfortManager {

    // Track cooldowns: player UUID -> entity UUID -> next allowed pet time
    private static final Map<UUID, Map<UUID, Long>> cooldowns = new HashMap<>();

    // Track active petting: player UUID -> entity UUID -> PetEntry
    private static final Map<UUID, Map<UUID, PetEntry>> activePetting = new HashMap<>();

    /** Represents a single active petting boost */
    public static class PetEntry {
        public final long expireTime;
        public final int comfortAmount;
        public final Class<?> entityClass;

        public PetEntry(long expireTime, int comfortAmount, Class<?> entityClass) {
            this.expireTime = expireTime;
            this.comfortAmount = comfortAmount;
            this.entityClass = entityClass;
        }
    }

    /** Register a petting action with duration, comfort amount, and entity type */
    public static void addPet(UUID playerId, UUID entityId, int durationSeconds, int comfortAmount, Class<?> entityClass) {
        long expireTime = System.currentTimeMillis() + durationSeconds * 1000L;
        activePetting.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(entityId, new PetEntry(expireTime, comfortAmount, entityClass));
    }

    /** Get total active petting points for a player */
    public static int getActivePettingPoints(UUID playerId) {
        Map<UUID, PetEntry> playerMap = activePetting.get(playerId);
        if (playerMap == null) return 0;

        long now = System.currentTimeMillis();
        int sum = 0;

        // Remove expired entries while summing
        playerMap.entrySet().removeIf(e -> e.getValue().expireTime <= now);
        for (PetEntry entry : playerMap.values()) {
            sum += entry.comfortAmount;
        }

        return sum;
    }

    /** Count active pets of a specific entity type (for maxPettable enforcement) */
    public static int countActivePets(UUID playerId, Class<?> entityClass) {
        Map<UUID, PetEntry> playerMap = activePetting.get(playerId);
        if (playerMap == null) return 0;

        long now = System.currentTimeMillis();
        int count = 0;

        playerMap.entrySet().removeIf(e -> e.getValue().expireTime <= now);
        for (PetEntry entry : playerMap.values()) {
            if (entry.entityClass.equals(entityClass)) count++;
        }

        return count;
    }

    /** Optional: remove expired pets for a player */
    public static void removeExpired(UUID playerId) {
        Map<UUID, PetEntry> playerMap = activePetting.get(playerId);
        if (playerMap == null) return;

        long now = System.currentTimeMillis();
        playerMap.entrySet().removeIf(e -> e.getValue().expireTime <= now);
    }

    /** Can this player pet this entity right now (cooldown check) */
    public static boolean canPet(EntityPlayer player, Entity entity) {
        UUID playerId = player.getUniqueID();
        UUID entityId = entity.getUniqueID();
        long now = System.currentTimeMillis();

        Map<UUID, Long> playerCooldowns = cooldowns.getOrDefault(playerId, new HashMap<>());
        Long nextPetTime = playerCooldowns.get(entityId);

        return nextPetTime == null || now >= nextPetTime;
    }

    /** Server-side: apply petting boost (register cooldown and active pet) */
    public static void applyPettingBoostServer(EntityPlayer player, Entity entity, PettingComfortData entry) {
        UUID playerId = player.getUniqueID();
        UUID entityId = entity.getUniqueID();
        long now = System.currentTimeMillis();

        // Update cooldown
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(entityId, now + entry.cooldownSeconds * 1000L);

        // Register active petting points
        addPet(playerId, entityId, entry.boostSeconds, entry.comfortBoost, entity.getClass());

        System.out.println("[Petting] Player " + player.getName() + " petted " + entity.getName() + " for " + entry.comfortBoost);
    }

    /** Client-side: spawn particles & show messages */
    public static void applyPettingBoostClient(EntityPlayer player, Entity entity, PettingComfortData entry) {
        spawnHeartParticles(player, entry.comfortBoost, entity);

        player.sendMessage(new TextComponentString(
                I18n.format("tooltip.chunkcomfort.petting.message.line1", entity.getName())
        ));
        player.sendMessage(new TextComponentString(
                I18n.format("tooltip.chunkcomfort.petting.message.line2", entry.comfortBoost, entry.boostSeconds)
        ));
    }

    /** Spawn hearts above the entity petted */
    public static void spawnHeartParticles(EntityPlayer player, float count, Entity entity) {
        World world = player.world;
        if (!world.isRemote) return;

        int hearts = Math.max(1, Math.round(count));
        Vec3d pos = entity.getPositionVector().add(0, 1, 0);

        for (int i = 0; i < hearts; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 0.5;
            double offsetY = world.rand.nextDouble() * 0.5;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 0.5;

            world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.HEART,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    0, 0.05, 0
            );
        }
    }
}