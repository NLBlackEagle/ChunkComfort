package chunkcomfort.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import chunkcomfort.chunk.ChunkComfortCapability;
import chunkcomfort.chunk.ChunkComfortData;

@EventBusSubscriber
public class ChunkComfortDebugOverlay {

    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Text event) {

        if (!mc.player.world.isRemote) return;

        ScaledResolution res = new ScaledResolution(mc);

        int chunkX = mc.player.getPosition().getX() >> 4;
        int chunkZ = mc.player.getPosition().getZ() >> 4;

        Chunk chunk = mc.player.world.getChunk(chunkX, chunkZ);
        ChunkPos chunkPos = chunk.getPos();

        ChunkComfortData data =
                chunk.hasCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null)
                        ? chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null)
                        : null;

        if (data == null || !data.initialized) {
            event.getLeft().add("[ChunkComfort] Chunk not initialized");
            return;
        }

        event.getLeft().add("[ChunkComfort]");
        event.getLeft().add("Chunk: " + chunkPos.x + ", " + chunkPos.z);
        event.getLeft().add("Comfort Score: " + data.comfortScore);
        event.getLeft().add("Fire Count: " + data.fireCount);

        for (String group : data.groups.keySet()) {

            ChunkComfortData.GroupData gd = data.groups.get(group);

            event.getLeft().add("Group '" + group + "': " + gd.currentScore + " / Limit: " + gd.limit);

            for (Integer value : gd.counts.keySet()) {
                event.getLeft().add("  Value " + value + " -> Count: " + gd.counts.get(value));
            }
        }
    }
}