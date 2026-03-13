package chunkcomfort.debug;

import chunkcomfort.chunk.ChunkComfortCapability;
import chunkcomfort.chunk.ChunkComfortData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.chunk.Chunk;

public class CommandChunkComfort extends CommandBase {

    @Override
    public String getName() {
        return "chunkcomfort";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/chunkcomfort <info|scan|radius>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(new TextComponentString("/chunkcomfort <info|scan|radius>"));
            return;
        }

        if ("info".equalsIgnoreCase(args[0])) {

            BlockPos pos = sender.getPosition();

            int playerChunkX = pos.getX() >> 4;
            int playerChunkZ = pos.getZ() >> 4;

            int totalComfort = 0;
            int totalFire = 0;
            int countedChunks = 0;

            sender.sendMessage(new TextComponentString("Chunk Comfort Info (3x3):"));

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {

                    Chunk chunk = sender.getEntityWorld().getChunk(playerChunkX + dx, playerChunkZ + dz);

                    ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);

                    if (data == null) continue;

                    totalComfort += data.comfortScore;
                    totalFire += data.fireCount;
                    countedChunks++;

                    sender.sendMessage(new TextComponentString(
                            "Chunk [" + (playerChunkX + dx) + "," + (playerChunkZ + dz) + "] " +
                                    "Comfort: " + data.comfortScore +
                                    ", Fire: " + data.fireCount
                    ));
                }
            }

            sender.sendMessage(new TextComponentString("-------------------"));
            sender.sendMessage(new TextComponentString("Total Comfort (3x3): " + totalComfort));
            sender.sendMessage(new TextComponentString("Total Fire (3x3): " + totalFire));

            if (countedChunks > 0) {
                sender.sendMessage(new TextComponentString("Average Comfort: " + (totalComfort / countedChunks)));
            }
        }
    }
}