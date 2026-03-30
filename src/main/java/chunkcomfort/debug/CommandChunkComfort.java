package chunkcomfort.debug;

import chunkcomfort.chunk.AreaComfortCalculator;
import chunkcomfort.chunk.ChunkComfortData;
import chunkcomfort.chunk.ComfortRequirementCheck;
import chunkcomfort.chunk.ComfortWorldData;
import chunkcomfort.config.ForgeConfigHandler;
import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.ComfortRequirements;
import chunkcomfort.registry.EntityComfortRegistry;
import chunkcomfort.registry.LivingComfortRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.*;

public class CommandChunkComfort extends CommandBase {

    // ---------------- Command Metadata ----------------
    @Override
    public String getName() { return "chunkcomfort"; }

    @Override
    public String getUsage(ICommandSender sender) { return "/chunkcomfort <info|reload>"; }

    @Override
    public int getRequiredPermissionLevel() { return 2; }

    // ---------------- Command Execution ----------------
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString(I18n.format("debug.chunkcomfort.usage")));
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
                sender.sendMessage(new TextComponentString(I18n.format("debug.chunkcomfort.unknown_subcommand")));
        }
    }

    private void executeInfo(ICommandSender sender) {
        EntityPlayer player = (EntityPlayer) sender.getCommandSenderEntity();
        if (player == null) return;

        int radius = AreaComfortCalculator.getRadius();

        int comfortActive = AreaComfortCalculator.calculateComfortActivation(player.world, player);

        if (!checkRequiredConditions(sender, player, comfortActive)) return;

        Map<String,Integer> groupTotals = new HashMap<>();
        Map<String,Map<String,Integer>> groupContents = new HashMap<>();
        // <--- Shared across all chunks in radius
        Set<UUID> countedPaintings = new HashSet<>();

        Map<ChunkPos, Map<String,Integer>> chunkGroupPoints =
                buildGroupData(player, player.getPosition(), radius, groupTotals, groupContents, countedPaintings);

        displayComfortActivationDetails(sender, player);
        displayPerChunkInfo(sender, player.getPosition(), radius, chunkGroupPoints);
        displayGroupBreakdown(sender, groupTotals, groupContents);
    }

    private void executeReload(MinecraftServer server, ICommandSender sender) {
        sender.sendMessage(new TextComponentString(I18n.format("debug.chunkcomfort.reload_start")));

        // 1. Reload config
        ForgeConfigHandler.initialize();
        AreaComfortCalculator.reloadGroupLimits(ForgeConfigHandler.server.groupLimits);
        sender.sendMessage(new TextComponentString(I18n.format("debug.chunkcomfort.reload_config")));

        // 2. Clear caches
        for (World world : server.worlds) {
            ComfortWorldData.get(world).clearAllChunks();
            sender.sendMessage(new TextComponentString(I18n.format("debug.chunkcomfort.reload_world", world.provider.getDimension())));
        }

        // 3. Recalculate comfort for all players
        for (World world : server.worlds) {
            for (EntityPlayer player : server.getPlayerList().getPlayers()) {
                ChunkPos center = new ChunkPos(player.getPosition());
                int radius = AreaComfortCalculator.getRadius();
                for (int dx = -radius; dx <= radius; dx++)
                    for (int dz = -radius; dz <= radius; dz++)
                        ComfortWorldData.get(world).recalcChunkWithFire(world, new ChunkPos(center.x + dx, center.z + dz));
                AreaComfortCalculator.calculatePlayerComfort(player);
            }
        }

        sender.sendMessage(new TextComponentString(I18n.format("debug.chunkcomfort.reload_complete")));
    }

    // ---------------- Core Helpers (Computation) ----------------

    private boolean checkRequiredConditions(ICommandSender sender, EntityPlayer player, int comfortActive) {
        int requiredConditions = 0;
        if (ForgeConfigHandler.server.requireShelter) requiredConditions++;
        if (ForgeConfigHandler.server.minLightLevel > 0) requiredConditions++;
        if (ForgeConfigHandler.server.requireFire) requiredConditions++;
        if (ForgeConfigHandler.server.enableTemperatureComfort) requiredConditions++;

        StringBuilder status = new StringBuilder();
        BlockPos pos = player.getPosition();
        int playerLight = player.world.getLight(pos);

        if (ForgeConfigHandler.server.requireShelter && player.world.canSeeSky(pos.up()))
            status.append("No shelter. ");
        if (ForgeConfigHandler.server.minLightLevel > 0 && playerLight < ForgeConfigHandler.server.minLightLevel)
            status.append("Too dark. ");
        if (ForgeConfigHandler.server.requireFire && !ComfortRequirementCheck.getRequirementsPresent(player.world, pos, player).fireOk)
            status.append("No fire. ");
        if (ForgeConfigHandler.server.enableTemperatureComfort && !ComfortRequirementCheck.getRequirementsPresent(player.world, pos, player).temperatureOk)
            status.append("Too cold/hot ");

        if (comfortActive < requiredConditions) {
            sender.sendMessage(new TextComponentString(I18n.format("debug.chunkcomfort.inactive", status)));
            return false;
        }
        return true;
    }

    private Map<ChunkPos, Map<String,Integer>> buildGroupData(EntityPlayer player, BlockPos pos, int radius,
                                                              Map<String,Integer> groupTotals,
                                                              Map<String,Map<String,Integer>> groupContents,
                                                              Set<UUID> countedPaintings) {
        Map<ChunkPos, Map<String,Integer>> chunkGroupPoints = new HashMap<>();
        int playerChunkX = pos.getX() >> 4;
        int playerChunkZ = pos.getZ() >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(playerChunkX + dx, playerChunkZ + dz);
                ChunkComfortData data = ComfortWorldData.get(player.world).getChunkData(chunkPos);
                if (!data.initialized) continue;

                Map<String,Integer> perChunkGroup = new HashMap<>();
                processBlocksInChunk(data, perChunkGroup, groupTotals, groupContents);
                // pass shared set
                processEntitiesInChunk(player.world, chunkPos, perChunkGroup, groupTotals, groupContents, countedPaintings);

                chunkGroupPoints.put(chunkPos, perChunkGroup);
            }
        }
        return chunkGroupPoints;
    }

    private void processBlocksInChunk(ChunkComfortData data,
                                      Map<String,Integer> perChunkGroup,
                                      Map<String,Integer> groupTotals,
                                      Map<String,Map<String,Integer>> groupContents) {
        for (Map.Entry<Block, Integer> entry : data.blockCounts.entrySet()) {
            Block block = entry.getKey();
            if (!BlockComfortRegistry.isComfortBlock(block)) continue;

            BlockComfortRegistry.ComfortEntry blockEntry = BlockComfortRegistry.getBlockEntry(block);
            int points = entry.getValue() * blockEntry.value;

            perChunkGroup.merge(blockEntry.group, points, Integer::sum);
            groupTotals.merge(blockEntry.group, points, Integer::sum);

            String canonicalId = BlockComfortRegistry.getCanonicalId(block);
            groupContents.computeIfAbsent(blockEntry.group, k -> new HashMap<>())
                    .merge(canonicalId, entry.getValue(), Integer::sum);
        }
    }

    private void processEntitiesInChunk(World world,
                                        ChunkPos chunkPos,
                                        Map<String,Integer> perChunkGroup,
                                        Map<String,Integer> groupTotals,
                                        Map<String,Map<String,Integer>> groupContents,
                                        Set<UUID> countedPaintings) { // <- use shared

        AxisAlignedBB chunkBox = new AxisAlignedBB(
                chunkPos.getXStart(), 0, chunkPos.getZStart(),
                chunkPos.getXEnd() + 1, 256, chunkPos.getZEnd() + 1
        );

        for (Entity entity : world.getEntitiesWithinAABB(Entity.class, chunkBox)) {
            int points = 0;
            String group = null;
            String id = null;

            // --- Living entities ---
            LivingComfortRegistry.LivingComfortEntry livingEntry = LivingComfortRegistry.getEntry(entity);
            if (livingEntry != null) {
                if (LivingComfortRegistry.getMatchingEntry(entity) != null) {
                    points = livingEntry.value;
                    group = livingEntry.group;
                    id = livingEntry.entityId.toString();
                }
            } else {
                // --- Non-living entities ---
                EntityComfortRegistry.ComfortEntry entityEntry = EntityComfortRegistry.getEntityEntry(entity);
                if (entityEntry != null) {
                    points = entityEntry.value;
                    group = entityEntry.group;
                    ResourceLocation rl = EntityList.getKey(entity);
                    if (rl != null) id = rl.toString();

                    // Only count each painting once globally
                    if (entity instanceof EntityPainting) {
                        UUID uuid = entity.getUniqueID();
                        if (countedPaintings.contains(uuid)) continue; // already counted
                        countedPaintings.add(uuid);
                    }
                }
            }

            if (group != null && points > 0) {
                perChunkGroup.merge(group, points, Integer::sum);
                groupTotals.merge(group, points, Integer::sum);
                groupContents.computeIfAbsent(group, k -> new HashMap<>())
                        .merge(id, 1, Integer::sum);
            }
        }
    }

    // ---------------- Display Helpers ----------------

    private void displayComfortActivationDetails(ICommandSender sender, EntityPlayer player) {
        BlockPos pos = player.getPosition();
        ComfortRequirements reqs =
                ComfortRequirementCheck.getRequirementsPresent(player.world, pos, player);

        StringBuilder sb = new StringBuilder();
        sb.append(I18n.format("debug.chunkcomfort.separator") + "\n");
        sb.append(I18n.format("debug.chunkcomfort.activation_header") + "\n");

        // Shelter
        if (ForgeConfigHandler.server.requireShelter) {
            sb.append(I18n.format("debug.chunkcomfort.shelter_required"));
            sb.append(I18n.format("debug.chunkcomfort.shelter_found", reqs.shelterOk));
        }

        // Light
        if (ForgeConfigHandler.server.minLightLevel > 0) {
            int light = player.world.getLight(pos);
            sb.append(I18n.format("debug.chunkcomfort.light_required", ForgeConfigHandler.server.minLightLevel));
            sb.append(I18n.format("debug.chunkcomfort.light_level", light));
        }

        // Fire
        if (ForgeConfigHandler.server.requireFire) {
            sb.append(I18n.format("debug.chunkcomfort.fire_required"));
            sb.append(I18n.format("debug.chunkcomfort.fire_found", reqs.fireOk));
        }

        // Temperature
        if (ForgeConfigHandler.server.enableTemperatureComfort) {
            sb.append(I18n.format("debug.chunkcomfort.temperature_required"));
            if (reqs.temperatureOk) {
                sb.append(I18n.format("debug.chunkcomfort.temperature_range", ForgeConfigHandler.server.minComfortTemperature, ForgeConfigHandler.server.maxComfortTemperature, reqs.playerTemperature));
            }
        }

        sb.append(I18n.format("debug.chunkcomfort.separator") + "\n");

        sender.sendMessage(new TextComponentString(sb.toString()));
    }

    private void displayPerChunkInfo(ICommandSender sender,
                                     BlockPos pos,
                                     int radius,
                                     Map<ChunkPos, Map<String,Integer>> chunkGroupPoints) {

        int playerChunkX = pos.getX() >> 4;
        int playerChunkZ = pos.getZ() >> 4;

        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(playerChunkX + dx, playerChunkZ + dz);
                Map<String,Integer> perChunkGroup = chunkGroupPoints.getOrDefault(chunkPos, new HashMap<>());

                int chunkTotal = 0;
                StringBuilder chunkGroupDisplay = new StringBuilder();

                for (Map.Entry<String,Integer> entry : perChunkGroup.entrySet()) {
                    String group = entry.getKey();
                    int value = entry.getValue();
                    int limit = getGlobalGroupLimit(group);
                    int displayValue = Math.min(value, limit);
                    chunkTotal += displayValue;
                    String color = (value > limit) ? "§c" : "§a";

                    chunkGroupDisplay.append(color).append(group).append(": ").append(displayValue).append("§r  ");
                }

                sender.sendMessage(new TextComponentString(
                        "[" + chunkPos.x + "," + chunkPos.z + "] " +
                                chunkTotal +
                                (chunkGroupDisplay.length() > 0 ? " | " + chunkGroupDisplay : "")
                ));
            }

    }

    private void displayGroupBreakdown(ICommandSender sender,
                                       Map<String,Integer> groupTotals,
                                       Map<String,Map<String,Integer>> groupContents) {

        int totalComfort = 0;
        for (Map.Entry<String,Integer> entry : groupTotals.entrySet()) {
            int limit = getGlobalGroupLimit(entry.getKey());
            totalComfort += Math.min(entry.getValue(), limit);
        }

        int maxComfort = calculateMaxComfort();

        // ---------------- Precompute maps ----------------
        // Map: ComfortEntry -> canonicalId
        Map<BlockComfortRegistry.ComfortEntry, String> blockEntryToCanonicalId = new HashMap<>();
        // Map: canonicalId -> ComfortEntry
        Map<String, BlockComfortRegistry.ComfortEntry> canonicalIdToBlockEntry = new HashMap<>();
        for (Map.Entry<Block, BlockComfortRegistry.ComfortEntry> be : BlockComfortRegistry.BLOCK_ENTRIES.entrySet()) {
            String canonicalId = BlockComfortRegistry.getCanonicalId(be.getKey());
            blockEntryToCanonicalId.putIfAbsent(be.getValue(), canonicalId);
            canonicalIdToBlockEntry.put(canonicalId, be.getValue());
        }

        sender.sendMessage(new TextComponentString(I18n.format("debug.chunkcomfort.separator")));
        sender.sendMessage(new TextComponentString(I18n.format("debug.chunkcomfort.group_breakdown", totalComfort, maxComfort)));
        sender.sendMessage(new TextComponentString(I18n.format("debug.chunkcomfort.group_breakdown_syntax")));

        for (Map.Entry<String,Map<String,Integer>> groupEntry : groupContents.entrySet()) {
            String group = groupEntry.getKey();
            Map<String,Integer> content = groupEntry.getValue();
            int groupPointsRaw = groupTotals.getOrDefault(group, 0);
            int groupLimit = getGlobalGroupLimit(group);
            int groupPointsDisplay = Math.min(groupPointsRaw, groupLimit);
            String groupColor = (groupPointsRaw > groupLimit) ? "§c" : "§a";

            StringBuilder contentDisplay = new StringBuilder();

            for (Map.Entry<String,Integer> e : content.entrySet()) {
                String name = e.getKey();
                int count = e.getValue();
                int displayCount = 0;
                int itemLimit = 0;
                String color = "§a";

                // --- Block lookup O(1) ---
                BlockComfortRegistry.ComfortEntry blockEntry = canonicalIdToBlockEntry.get(name);
                if (blockEntry != null) {
                    itemLimit = blockEntry.limit;
                    displayCount = Math.min(count, itemLimit);
                    color = (count > itemLimit) ? "§c" : "§a";
                    name = blockEntryToCanonicalId.get(blockEntry); // canonical name
                } else {
                    // --- Living entity ---
                    LivingComfortRegistry.LivingComfortEntry livingEntry = LivingComfortRegistry.ENTITY_MAP.get(new ResourceLocation(name));
                    if (livingEntry != null) {
                        itemLimit = livingEntry.limit;
                        displayCount = Math.min(count, itemLimit);
                        if (count > itemLimit) color = "§c";
                    } else {
                        // --- Other entities ---
                        EntityComfortRegistry.ComfortEntry entityEntry = EntityComfortRegistry.getEntityEntryFromId(new ResourceLocation(name));
                        if (entityEntry != null) {
                            itemLimit = entityEntry.limit;
                            displayCount = Math.min(count, itemLimit);
                            if (count > itemLimit) color = "§c";
                        }
                    }
                }

                contentDisplay.append(color).append("§9").append(name).append("§r ")
                        .append(displayCount).append("/").append(itemLimit).append("§r, ");
            }

            if (contentDisplay.length() > 2) contentDisplay.setLength(contentDisplay.length() - 2);

            sender.sendMessage(new TextComponentString(
                    groupColor + group + ": " + groupPointsDisplay + "/" + groupLimit + "§r | " + contentDisplay
            ));
        }

        sender.sendMessage(new TextComponentString(I18n.format("debug.chunkcomfort.separator")));
    }

    // ---------------- Utility Helpers ----------------
    private int getGlobalGroupLimit(String groupName) {
        if (groupName == null || groupName.isEmpty()) return 0;
        for (String s : ForgeConfigHandler.server.groupLimits) {
            String[] split = s.split(",");
            if (split.length == 2 && split[0].trim().equalsIgnoreCase(groupName.trim())) {
                try { return Integer.parseInt(split[1].trim()); } catch (NumberFormatException ignored) {}
            }
        }
        return Integer.MAX_VALUE;
    }

    private int calculateMaxComfort() {
        int maxComfort = 0;
        for (String s : ForgeConfigHandler.server.groupLimits) {
            String[] split = s.split(",");
            if (split.length == 2) {
                try {
                    maxComfort += Integer.parseInt(split[1].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return maxComfort;
    }
}