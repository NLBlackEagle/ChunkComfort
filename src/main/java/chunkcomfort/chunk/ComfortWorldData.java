package chunkcomfort.chunk;

import chunkcomfort.registry.BlockComfortRegistry;
import chunkcomfort.registry.EntityComfortRegistry;
import chunkcomfort.registry.FireBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;

public class ComfortWorldData extends WorldSavedData {

    private final Map<ChunkPos, ChunkComfortData> chunks = new HashMap<>();

    public static final String DATA_NAME = "chunk_comfort";

    public ComfortWorldData(String name) {
        super(name);
    }

    public ComfortWorldData() {
        super(DATA_NAME);
    }

    public ChunkComfortData getOrCreateChunkData(World world, ChunkPos pos) {
        ChunkComfortData data = chunks.computeIfAbsent(pos, k -> new ChunkComfortData());

        if (!data.initialized) {
            recalcChunkWithFire(world, pos);
            return chunks.get(pos);
        }

        return data;
    }

    public ChunkComfortData getChunkData(ChunkPos pos) {
        return chunks.computeIfAbsent(pos, k -> new ChunkComfortData());
    }

    static boolean hasFireNearby(World world, BlockPos center, int radius) {
        int verticalRange = chunkcomfort.config.ForgeConfigHandler.server.fireScanVerticalRange;
        int minY = Math.max(0, center.getY() - verticalRange);
        int maxY = Math.min(world.getHeight() - 1, center.getY() + verticalRange);

        try {
            return ChunkScanner.anyBlockMatches(world, center, radius, minY, maxY, FireBlockRegistry::isFireBlock);
        } catch (ChunkScanner.StopScanException e) {
            return true; // early exit = fire was found
        }
    }

    public void recalcChunkWithFire(World world, ChunkPos chunkPos) {
        ChunkComfortData data = new ChunkComfortData();
        int minY = 0;
        int maxY = world.getHeight() - 1;

        // REASON: removed the try/catch for StopScanException inside recalcChunkWithFire.
        // recalcChunkWithFire scans comfort blocks — it never throws StopScanException
        // because no comfort block scan calls throw(). The catch was dead code left over
        // from an earlier version that used fire detection inside this scan.
        ChunkScanner.scanChunk(world, chunkPos, minY, maxY, (pos, block) -> {
            IBlockState state = world.getBlockState(pos);

            if (!isPrimaryBlock(state)) return;

            if (BlockComfortRegistry.isComfortBlock(block)) {
                String group = BlockComfortRegistry.getGroup(block);
                int value = BlockComfortRegistry.getValue(block);
                data.groupTotals.put(group, data.groupTotals.getOrDefault(group, 0) + value);
                data.blockCounts.put(block, data.blockCounts.getOrDefault(block, 0) + 1);
            }
        });

        for (Entity entity : world.loadedEntityList) {
            BlockPos ePos = entity.getPosition();
            if ((ePos.getX() >> 4) == chunkPos.x && (ePos.getZ() >> 4) == chunkPos.z) {
                EntityComfortRegistry.ComfortEntry entry = EntityComfortRegistry.getEntityEntry(entity);
                if (entry != null) {
                    data.groupTotals.put(entry.group, data.groupTotals.getOrDefault(entry.group, 0) + entry.value);
                }
            }
        }

        data.totalComfort = data.groupTotals.values().stream().mapToInt(Integer::intValue).sum();
        data.initialized = true;
        data.lastRecalcTick = world.getTotalWorldTime();

        setChunkData(chunkPos, data);
    }

    public void setChunkData(ChunkPos pos, ChunkComfortData data) {
        chunks.put(pos, data);
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList chunkList = new NBTTagList();

        for (Map.Entry<ChunkPos, ChunkComfortData> entry : chunks.entrySet()) {
            ChunkPos pos = entry.getKey();
            ChunkComfortData data = entry.getValue();

            NBTTagCompound chunkTag = new NBTTagCompound();
            chunkTag.setInteger("x", pos.x);
            chunkTag.setInteger("z", pos.z);

            NBTTagCompound groupsTag = new NBTTagCompound();
            for (Map.Entry<String, Integer> groupEntry : data.groupTotals.entrySet()) {
                groupsTag.setInteger(groupEntry.getKey(), groupEntry.getValue());
            }

            chunkTag.setTag("groups", groupsTag);
            chunkList.appendTag(chunkTag);
        }

        compound.setTag("chunks", chunkList);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        chunks.clear();
        NBTTagList chunkList = compound.getTagList("chunks", 10);

        for (int i = 0; i < chunkList.tagCount(); i++) {
            NBTTagCompound chunkTag = chunkList.getCompoundTagAt(i);
            int x = chunkTag.getInteger("x");
            int z = chunkTag.getInteger("z");

            ChunkComfortData data = new ChunkComfortData();

            NBTTagCompound groupsTag = chunkTag.getCompoundTag("groups");
            for (String key : groupsTag.getKeySet()) {
                data.groupTotals.put(key, groupsTag.getInteger(key));
            }

            data.totalComfort = data.groupTotals.values().stream().mapToInt(Integer::intValue).sum();
            chunks.put(new ChunkPos(x, z), data);
        }
    }

    public static ComfortWorldData get(World world) {
        assert world.getMapStorage() != null;
        ComfortWorldData data = (ComfortWorldData) world.getMapStorage().getOrLoadData(ComfortWorldData.class, DATA_NAME);
        if (data == null) {
            data = new ComfortWorldData();
            world.getMapStorage().setData(DATA_NAME, data);
        }
        return data;
    }

    public void clearAllChunks() {
        for (ChunkComfortData data : chunks.values()) {
            data.groupTotals.clear();
            data.totalComfort = 0;
        }
        markDirty();
    }

    public static boolean isPrimaryBlock(IBlockState state) {
        Block block = state.getBlock();

        if (block instanceof BlockBed) {
            return state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT;
        }

        // REASON: Comforts sleeping bags and hammocks are two-block structures like beds.
        // They use a PART block state property ("head"/"foot") to distinguish the two halves.
        // Without this check both halves get counted, doubling the comfort value.
        // We check by class name to avoid a hard dependency on the Comforts mod.
        String className = block.getClass().getName();
        if (className.startsWith("com.teamabnormals.comforts") || className.startsWith("net.minecraftforge.fml") ) {
            // Intentional no-op — fallthrough to property check below
        }
        try {
            // Check for a "part" or "PART" property with a "head"/"foot" value
            for (net.minecraft.block.properties.IProperty<?> prop : state.getPropertyKeys()) {
                String propName = prop.getName().toLowerCase();
                if (propName.equals("part")) {
                    String val = state.getValue(prop).toString().toLowerCase();
                    // Only count the foot/bottom half to avoid double-counting
                    return val.equals("foot") || val.equals("bottom");
                }
            }
        } catch (Exception ignored) {}

        return true;
    }
}