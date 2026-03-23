package chunkcomfort.debug;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.chunk.ChunkComfortData;
import chunkcomfort.chunk.ComfortRequirementCheck;
import chunkcomfort.chunk.ComfortWorldData;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.handlers.ChunkComfortEventHandler;
import chunkcomfort.registry.BlockComfortRegistry;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class CommandChunkComfort extends CommandBase {

    @Override
    public String getName() {
        return "chunkcomfort";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/chunkcomfort <info|reload>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(new TextComponentString("§cUsage: /chunkcomfort <info|reload>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                executeInfo(sender);
                break;
            case "reload":
                executeReload(server, sender);
                break;
            default:
                sender.sendMessage(new TextComponentString("§cUnknown subcommand."));
        }
    }

    private void executeInfo(ICommandSender sender) {
        EntityPlayer player = (EntityPlayer) sender.getCommandSenderEntity();
        if (player == null) return;

        BlockPos pos = player.getPosition();
        int playerChunkX = pos.getX() >> 4;
        int playerChunkZ = pos.getZ() >> 4;

        int radius = AreaComfortCalculator.getRadius();
        int diameter = radius * 2 + 1;

        int comfortActive = AreaComfortCalculator.calculateComfortActivation(
                player.world,
                player
        );

        sender.sendMessage(new TextComponentString("Comfort Activation Score: " + comfortActive));

        int requiredConditions = 0;
        if (ForgeConfigHandler.server.requireShelter) requiredConditions++;
        if (ForgeConfigHandler.server.minLightLevel > 0) requiredConditions++;
        if (ForgeConfigHandler.server.requireFire) requiredConditions++;

        StringBuilder status = new StringBuilder();
        if (ForgeConfigHandler.server.requireShelter && player.world.canSeeSky(pos.up())) {
            status.append("No shelter. ");
        }

        int playerLight = player.world.getLight(pos);
        if (ForgeConfigHandler.server.minLightLevel > 0 &&
                playerLight < ForgeConfigHandler.server.minLightLevel) {
            status.append("Too dark. ");
        }

        if (ForgeConfigHandler.server.requireFire) {
            if (ComfortRequirementCheck.getRequirementsPresent(player.world, player.getPosition()).fireOk) {
                status.append("No fire. ");
            }
        }

        if (comfortActive < requiredConditions) {
            sender.sendMessage(new TextComponentString("Comfort system inactive: " + status));
            return;
        }

        sender.sendMessage(new TextComponentString(
                "Chunk Comfort Info (" + diameter + "x" + diameter + ", with group limits applied):"
        ));

        Map<String, Integer> summedGroups = new HashMap<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {

                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                ChunkComfortData data = ComfortWorldData
                        .get(player.world)
                        .getChunkData(new ChunkPos(chunkX, chunkZ));

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
                            .append(BlockComfortRegistry.getGroupLimit(group))
                            .append("  ");

                    summedGroups.put(group,
                            summedGroups.getOrDefault(group, 0) + value);
                }

                sender.sendMessage(new TextComponentString(
                        "Chunk [" + chunkX + "," + chunkZ + "] Comfort: " +
                                chunkRawTotal +
                                (chunkGroupDisplay.length() > 0
                                        ? " | " + chunkGroupDisplay
                                        : "")
                ));
            }
        }

        AreaComfortCalculator.addLivingEntityComfort(player.world, player.getPosition(), radius, summedGroups);

        int totalComfort = 0;
        StringBuilder totalGroupDisplay = new StringBuilder();

        for (Map.Entry<String, Integer> entry : summedGroups.entrySet()) {

            String group = entry.getKey();
            int value = entry.getValue();
            int limit = BlockComfortRegistry.getGroupLimit(group);

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
        sender.sendMessage(new TextComponentString("Total Comfort: " + totalComfort));
        sender.sendMessage(new TextComponentString("Group breakdown: " + totalGroupDisplay));
    }

    private void executeReload(MinecraftServer server, ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§a[ChunkComfort] Clearing caches and reloading..."));

        // 0. Clear the tracked TileEntity cache
        sender.sendMessage(new TextComponentString("§a[ChunkComfort] Cleared tracked TileEntity cache."));

        // 1. Reload config and group limits first
        ForgeConfigHandler.initialize();
        AreaComfortCalculator.reloadGroupLimits(ForgeConfigHandler.server.groupLimits);
        sender.sendMessage(new TextComponentString("§a[ChunkComfort] Config reloaded."));

        // 2. Clear caches for all worlds
        for (World world : server.worlds) {
            ComfortWorldData worldData = ComfortWorldData.get(world);
            worldData.clearAllChunks();
            sender.sendMessage(new TextComponentString(
                    "§a[ChunkComfort] Cleared caches for world: " + world.provider.getDimension()
            ));
        }

        // 3. Recalculate chunks and player comfort around all players
        for (World world : server.worlds) {
            for (EntityPlayer player : server.getPlayerList().getPlayers()) {
                ChunkPos center = new ChunkPos(player.getPosition());
                int radius = AreaComfortCalculator.getRadius();

                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);
                        ComfortWorldData.get(world).recalcChunkWithFire(world, pos);
                    }
                }

                // recalc player's comfort fully
                AreaComfortCalculator.calculatePlayerComfort(player);
            }
        }

        sender.sendMessage(new TextComponentString("§a[ChunkComfort] Comfort recalculated for all players."));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // Only ops/admins
    }
}