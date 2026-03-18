package chunkcomfort.chunk;

import chunkcomfort.registry.BlockComfortRegistry;
import net.minecraft.block.Block;
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

    public ComfortWorldData() {
        super(DATA_NAME);
    }


    public ChunkComfortData getOrCreateChunkData(World world, ChunkPos pos) {
        ChunkComfortData data = chunks.computeIfAbsent(pos, k -> new ChunkComfortData());

        // If never scanned → scan now (once)
        if (!data.initialized) {
            recalcChunk(world, pos);
            return chunks.get(pos); // get updated data
        }

        return data;
    }

    /**
     * Lazy-load chunk data: if it doesn’t exist, create it and populate from the world.
     */
    public ChunkComfortData getChunkData(ChunkPos pos) {
        return chunks.computeIfAbsent(pos, k -> new ChunkComfortData());
    }

    /**
     * Recalculate the comfort for a specific chunk.
     * This will scan all blocks in the chunk and update the ChunkComfortData.
     */
    public void recalcChunk(World world, ChunkPos chunkPos) {
        ChunkComfortData data = new ChunkComfortData();

        BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
        int startX = chunkPos.x * 16;
        int startZ = chunkPos.z * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < world.getHeight(); y++) {
                    scanPos.setPos(startX + x, y, startZ + z);
                    Block block = world.getBlockState(scanPos).getBlock();

                    if (BlockComfortRegistry.isComfortBlock(block)) {
                        String group = BlockComfortRegistry.getGroup(block);
                        int value = BlockComfortRegistry.getValue(block);

                        data.groupTotals.put(
                                group,
                                data.groupTotals.getOrDefault(group, 0) + value
                        );
                    }
                }
            }
        }

        for (Entity entity : world.loadedEntityList) {
            BlockPos ePos = entity.getPosition();

            if ((ePos.getX() >> 4) == chunkPos.x && (ePos.getZ() >> 4) == chunkPos.z) {

                BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getEntityEntry(entity);

                if (entry != null) {
                    data.groupTotals.put(
                            entry.group,
                            data.groupTotals.getOrDefault(entry.group, 0) + entry.value
                    );
                }
            }
        }

        data.totalComfort = data.groupTotals.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        data.initialized = true; // 👈 IMPORTANT

        setChunkData(chunkPos, data);
    }

    /** Update chunk data and mark dirty for saving */
    public void setChunkData(ChunkPos pos, ChunkComfortData data) {
        chunks.put(pos, data);
        markDirty();
    }

    /** Serialize all ChunkComfortData to NBT */
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

    /** Deserialize all ChunkComfortData from NBT */
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        chunks.clear();
        NBTTagList chunkList = compound.getTagList("chunks", 10); // 10 = TAG_Compound

        for (int i = 0; i < chunkList.tagCount(); i++) {
            NBTTagCompound chunkTag = chunkList.getCompoundTagAt(i);
            int x = chunkTag.getInteger("x");
            int z = chunkTag.getInteger("z");

            ChunkComfortData data = new ChunkComfortData();

            NBTTagCompound groupsTag = chunkTag.getCompoundTag("groups");
            for (String key : groupsTag.getKeySet()) {
                data.groupTotals.put(key, groupsTag.getInteger(key));
            }

            // recalc totalComfort
            data.totalComfort = data.groupTotals.values().stream().mapToInt(Integer::intValue).sum();

            chunks.put(new ChunkPos(x, z), data);
        }
    }

    /** Utility to load or create ComfortWorldData for a world */
    public static ComfortWorldData get(World world) {
        assert world.getMapStorage() != null;
        ComfortWorldData data = (ComfortWorldData) world.getMapStorage().getOrLoadData(ComfortWorldData.class, DATA_NAME);
        if (data == null) {
            data = new ComfortWorldData();
            world.getMapStorage().setData(DATA_NAME, data);
        }
        return data;
    }

    /** Clear all cached comfort totals for all loaded chunks */
    public void clearAllChunks() {
        for (ChunkComfortData data : chunks.values()) {
            data.groupTotals.clear();
            data.totalComfort = 0;
        }
        markDirty();
    }
}