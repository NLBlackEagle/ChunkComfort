package chunkcomfort.debug;

import chunkcomfort.chunk.ChunkUpdateManager;
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
        return "/chunkcomfort <info>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {

        if (args.length == 0 || !"info".equalsIgnoreCase(args[0])) {
            sender.sendMessage(new TextComponentString("/chunkcomfort info"));
            return;
        }

        BlockPos pos = sender.getPosition();
        int playerChunkX = pos.getX() >> 4;
        int playerChunkZ = pos.getZ() >> 4;

        int totalComfort = 0;
        int countedChunks = 0;

        sender.sendMessage(new TextComponentString("Chunk Comfort Info (3x3):"));

        // Scan 3x3 chunks
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {

                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                ChunkComfortData data = ChunkUpdateManager.getChunkData(chunkX, chunkZ);
                int chunkComfort = 0;

                // Sum all group totals in this chunk
                for (String group : data.groupTotals.keySet()) {
                    chunkComfort += data.getComfort(group);
                }

                totalComfort += chunkComfort;
                countedChunks++;

                sender.sendMessage(new TextComponentString(
                        "Chunk [" + chunkX + "," + chunkZ + "] Comfort: " + chunkComfort
                ));
            }
        }

        sender.sendMessage(new TextComponentString("-------------------"));
        sender.sendMessage(new TextComponentString("Total Comfort (3x3): " + totalComfort));

        if (countedChunks > 0) {
            sender.sendMessage(new TextComponentString(
                    "Average Comfort: " + (totalComfort / countedChunks)
            ));
        }
    }
}