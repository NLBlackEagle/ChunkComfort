package chunkcomfort.chunk;

import chunkcomfort.ChunkComfort;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PettingComfortManager {

    private static final Map<UUID, Map<UUID, Long>> cooldowns = new HashMap<>();
    private static final Map<UUID, Map<UUID, PetEntry>> activePetting = new HashMap<>();

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

    public static void addPet(UUID playerId, UUID entityId, int durationSeconds, int comfortAmount, Class<?> entityClass) {
        long expireTime = System.currentTimeMillis() + durationSeconds * 1000L;
        activePetting.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(entityId, new PetEntry(expireTime, comfortAmount, entityClass));
    }

    public static int getActivePettingPoints(UUID playerId) {
        Map<UUID, PetEntry> playerMap = activePetting.get(playerId);
        if (playerMap == null) return 0;

        long now = System.currentTimeMillis();
        playerMap.entrySet().removeIf(e -> e.getValue().expireTime <= now);

        int sum = 0;
        for (PetEntry entry : playerMap.values()) {
            sum += entry.comfortAmount;
        }
        return sum;
    }

    public static int countActivePets(UUID playerId, Class<?> entityClass) {
        Map<UUID, PetEntry> playerMap = activePetting.get(playerId);
        if (playerMap == null) return 0;

        long now = System.currentTimeMillis();
        playerMap.entrySet().removeIf(e -> e.getValue().expireTime <= now);

        int count = 0;
        for (PetEntry entry : playerMap.values()) {
            if (entry.entityClass.equals(entityClass)) count++;
        }
        return count;
    }

    public static void removeExpired(UUID playerId) {
        Map<UUID, PetEntry> playerMap = activePetting.get(playerId);
        if (playerMap == null) return;
        long now = System.currentTimeMillis();
        playerMap.entrySet().removeIf(e -> e.getValue().expireTime <= now);
    }

    public static void evictPlayer(UUID playerId) {
        cooldowns.remove(playerId);
        activePetting.remove(playerId);
    }

    public static boolean canPet(EntityPlayer player, Entity entity) {
        UUID playerId = player.getUniqueID();
        UUID entityId = entity.getUniqueID();
        long now = System.currentTimeMillis();

        Map<UUID, Long> playerCooldowns = cooldowns.getOrDefault(playerId, new HashMap<>());
        Long nextPetTime = playerCooldowns.get(entityId);

        if (nextPetTime != null && now < nextPetTime && !player.world.isRemote) {
            long seconds = (nextPetTime - now) / 1000;
            player.sendMessage(new TextComponentTranslation("chunkcomfort.petting.cooldown", seconds));
        }

        return nextPetTime == null || now >= nextPetTime;
    }

    public static void applyPettingBoostServer(EntityPlayer player, Entity entity, PettingComfortData entry) {
        UUID playerId = player.getUniqueID();
        UUID entityId = entity.getUniqueID();
        long now = System.currentTimeMillis();

        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(entityId, now + entry.cooldownSeconds * 1000L);

        addPet(playerId, entityId, entry.boostSeconds, entry.comfortBoost, entity.getClass());

        ChunkComfort.LOGGER.debug("[Petting] Player {} petted {} for {}", player.getName(), entity.getName(), entry.comfortBoost);
    }

    public static void applyPettingBoostClient(EntityPlayer player, Entity entity, PettingComfortData entry) {
        spawnHeartParticles(player, entry.comfortBoost, entity);

        player.sendMessage(new TextComponentString(
                I18n.format("tooltip.chunkcomfort.petting.message.line1", entity.getName())
        ));
        player.sendMessage(new TextComponentString(
                I18n.format("tooltip.chunkcomfort.petting.message.line2", entry.comfortBoost, entry.boostSeconds)
        ));
    }

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
