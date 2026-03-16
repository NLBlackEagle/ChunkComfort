package chunkcomfort.debug;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.chunk.ChunkUpdateManager;
import chunkcomfort.chunk.ChunkComfortData;
import chunkcomfort.chunk.ComfortWorldData;
import chunkcomfort.config.ForgeConfigHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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
        return "/chunkcomfort info";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {

        if (args.length == 0 || !"info".equalsIgnoreCase(args[0])) {
            sender.sendMessage(new TextComponentString("/chunkcomfort info"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender.getCommandSenderEntity();
        BlockPos pos = player.getPosition();
        int playerChunkX = pos.getX() >> 4;
        int playerChunkZ = pos.getZ() >> 4;

        int comfortActive = AreaComfortCalculator.calculateComfortActivation(
                player.world,
                playerChunkX,
                playerChunkZ,
                player
        );

        sender.sendMessage(new TextComponentString("Comfort Activation Score: " + comfortActive));

        // Count how many conditions are enabled
        int requiredConditions = 0;
        if (ForgeConfigHandler.server.requireShelter) requiredConditions++;
        if (ForgeConfigHandler.server.minLightLevel > 0) requiredConditions++;
        if (ForgeConfigHandler.server.requireFire) requiredConditions++;

        // Build detailed status
        StringBuilder status = new StringBuilder();
        // Shelter check
        if (ForgeConfigHandler.server.requireShelter && player.world.canSeeSky(pos.up())) {
            status.append("No shelter. ");
        }
        // Light check
        int playerLight = player.world.getLight(pos);
        if (ForgeConfigHandler.server.minLightLevel > 0 && playerLight < ForgeConfigHandler.server.minLightLevel) {
            status.append("Too dark. ");
        }
        // Fire check
        if (ForgeConfigHandler.server.requireFire) {
            if (!ChunkUpdateManager.getChunkData(player.world, playerChunkX, playerChunkZ).hasFire()) {
                status.append("No fire. ");
            }
        }

        if (comfortActive < requiredConditions) {
            sender.sendMessage(new TextComponentString("Comfort system inactive: " + status.toString()));
            return;
        }

        sender.sendMessage(new TextComponentString("Chunk Comfort Info (3x3, with group limits applied):"));

        Map<String, Integer> summedGroups = new HashMap<>();

        // Scan 3x3 area
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                ChunkComfortData data = ComfortWorldData.get(player.world).getChunkData(new ChunkPos(chunkX, chunkZ));

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

                    // Add to summed total
                    summedGroups.put(group, summedGroups.getOrDefault(group, 0) + value);
                }

                sender.sendMessage(new TextComponentString(
                        "Chunk [" + chunkX + "," + chunkZ + "] Comfort: " + chunkRawTotal
                                + (chunkGroupDisplay.length() > 0 ? " | " + chunkGroupDisplay.toString() : "")
                ));
            }
        }

        // Apply group limits to summed total
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