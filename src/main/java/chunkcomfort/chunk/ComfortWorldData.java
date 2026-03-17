package chunkcomfort.chunk;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
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

    public ComfortWorldData(String name) {
        super(name);
    }

    /** Retrieve chunk data or create a new empty one */
    public ChunkComfortData getChunkData(ChunkPos pos) {
        return chunks.computeIfAbsent(pos, k -> new ChunkComfortData());
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
        ComfortWorldData data = (ComfortWorldData) world.getMapStorage().getOrLoadData(ComfortWorldData.class, DATA_NAME);
        if (data == null) {
            data = new ComfortWorldData();
            world.getMapStorage().setData(DATA_NAME, data);
        }
        return data;
    }
}