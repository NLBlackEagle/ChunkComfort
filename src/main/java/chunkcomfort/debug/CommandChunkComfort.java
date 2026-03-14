package chunkcomfort.debug;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.chunk.ChunkUpdateManager;
import chunkcomfort.chunk.ChunkComfortData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import java.util.HashMap;
import java.util.Map;

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

        sender.sendMessage(new TextComponentString("Chunk Comfort Info (3x3, with group limits applied to total):"));

        // Map to sum all groups across 3x3 area
        Map<String, Integer> summedGroups = new HashMap<String, Integer>();

        // First: print per-chunk raw totals
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                ChunkComfortData data = ChunkUpdateManager.getChunkData(chunkX, chunkZ);

                int chunkRawTotal = 0;
                StringBuilder chunkGroupDisplay = new StringBuilder();

                for (Map.Entry<String, Integer> entry : data.groupTotals.entrySet()) {
                    String group = entry.getKey();
                    int value = entry.getValue();
                    chunkRawTotal += value;

                    chunkGroupDisplay.append(group)
                            .append(": ")
                            .append(value)
                            .append("/")
                            .append(AreaComfortCalculator.getGroupLimit(group))
                            .append("  ");

                    // add to summedGroups for 3x3 total
                    if (summedGroups.containsKey(group)) {
                        summedGroups.put(group, summedGroups.get(group) + value);
                    } else {
                        summedGroups.put(group, value);
                    }
                }

                sender.sendMessage(new TextComponentString(
                        "Chunk [" + chunkX + "," + chunkZ + "] Comfort: " + chunkRawTotal
                                + (chunkGroupDisplay.length() > 0 ? " | " + chunkGroupDisplay.toString() : "")
                ));
            }
        }

        // Apply group limits to the summed total
        int totalComfort = 0;
        StringBuilder totalGroupDisplay = new StringBuilder();

        for (Map.Entry<String, Integer> entry : summedGroups.entrySet()) {
            String group = entry.getKey();
            int value = entry.getValue();

            int limit = AreaComfortCalculator.getGroupLimit(group);
            int applied = Math.min(value, limit);

            totalComfort += applied;

            totalGroupDisplay.append(group)
                    .append(": ")
                    .append(applied)
                    .append("/")
                    .append(limit)
                    .append("  ");
        }

        sender.sendMessage(new TextComponentString("-------------------"));
        sender.sendMessage(new TextComponentString("Total Comfort (3x3, limited): " + totalComfort));
        sender.sendMessage(new TextComponentString("Group breakdown: " + totalGroupDisplay.toString()));
        sender.sendMessage(new TextComponentString("Average Comfort: " + (totalComfort / 9)));
    }
}