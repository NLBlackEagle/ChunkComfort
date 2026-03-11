package chunkcomfort.handlers;

import chunkcomfort.chunk.ChunkComfortCapability;
import chunkcomfort.chunk.ChunkComfortData;
import chunkcomfort.player.PlayerComfortStorage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

public class PlayerComfortHandler {

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new PlayerComfortHandler());
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {

        EntityPlayer player = event.player;

        if (player.ticksExisted % ForgeConfigHandler.server.comfortCheckInterval != 0) return;

        Chunk chunk = player.world.getChunk(player.chunkCoordX, player.chunkCoordZ);
        chunkcomfort.chunk.ChunkInitializationManager.initChunkIfNeeded(chunk);

        float comfort = calculatePlayerComfort(player);

        PlayerComfortStorage.setPlayerComfort(player, comfort);
    }

    private float calculatePlayerComfort(EntityPlayer player) {

        World world = player.world;

        int radius = ForgeConfigHandler.server.chunkRadius;

        int totalComfort = 0;
        int count = 0;

        Chunk playerChunk = world.getChunk(player.getPosition());

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {

                Chunk chunk = world.getChunk(playerChunk.x + dx, playerChunk.z + dz);

                ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);

                if (data == null || data.fireCount == 0) continue;

                totalComfort += data.comfortScore;
                count++;
            }
        }

        float comfort = count > 0 ? (float) totalComfort / count : 0;

        if (world.canSeeSky(player.getPosition())) comfort = 0;

        if (world.getLight(player.getPosition()) < ForgeConfigHandler.server.minComfortLightLevel)
            comfort = 0;

        return comfort;
    }
}