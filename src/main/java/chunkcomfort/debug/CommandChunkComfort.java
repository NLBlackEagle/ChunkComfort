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

            Chunk chunk = sender.getEntityWorld().getChunk(pos);

            ChunkComfortData data = chunk.getCapability(ChunkComfortCapability.CHUNK_COMFORT_CAP, null);

            if (data == null) {
                sender.sendMessage(new TextComponentString("Chunk comfort data not initialized."));
                return;
            }

            sender.sendMessage(new TextComponentString("Chunk Comfort Info:"));
            sender.sendMessage(new TextComponentString("Comfort Score: " + data.comfortScore));
            sender.sendMessage(new TextComponentString("Fire Count: " + data.fireCount));
            sender.sendMessage(new TextComponentString("Initialized: " + data.initialized));
            sender.sendMessage(new TextComponentString("Groups:"));

            data.groups.forEach((name, group) -> {

                sender.sendMessage(new TextComponentString(
                        "  " + name +
                                " -> Limit: " + group.limit +
                                ", CurrentScore: " + group.currentScore +
                                ", Counts: " + group.counts
                ));

            });
        }
    }
}