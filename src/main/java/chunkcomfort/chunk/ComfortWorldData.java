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
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

        // If never scanned → scan now (once)
        if (!data.initialized) {
            recalcChunkWithFire(world, pos);
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

    static boolean hasFireNearby(World world, BlockPos center, int radius) {
        int minX = center.getX() - radius * 16;
        int maxX = center.getX() + radius * 16;
        int minZ = center.getZ() - radius * 16;
        int maxZ = center.getZ() + radius * 16;

        int minY = 0;
        int maxY = world.getHeight() - 1;

        AtomicBoolean fireFound = new AtomicBoolean(false);

        int chunkMinX = minX >> 4;
        int chunkMaxX = maxX >> 4;
        int chunkMinZ = minZ >> 4;
        int chunkMaxZ = maxZ >> 4;

        outer:
        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                ChunkPos chunkPos = new ChunkPos(cx, cz);
                Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z);

                ChunkScanner.scanChunk(world, chunkPos, minY, maxY, (pos, block) -> {
                    if (fireFound.get()) return; // stop scanning this chunk
                    if (FireBlockRegistry.isFireBlock(block)) {
                        fireFound.set(true);
                        return; // just return, don’t throw
                    }
                });

                if (fireFound.get()) break outer; // stop scanning other chunks
            }
        }

        return fireFound.get();
    }

    /**
     * Recalculate the comfort for a specific chunk.
     * This will scan all blocks in the chunk and update the ChunkComfortData.
     */
    public void recalcChunkWithFire(World world, ChunkPos chunkPos) {
        ChunkComfortData data = new ChunkComfortData();
        int minY = 0;
        int maxY = world.getHeight() - 1;

        try {
            // Scan all blocks once
            ChunkScanner.scanChunk(world, chunkPos, minY, maxY, (pos, block) -> {

                IBlockState state = world.getBlockState(pos);

                // Prevent double-counting multiblocks (beds etc)
                if (!isPrimaryBlock(state)) {
                    return;
                }

                // Comfort blocks
                if (BlockComfortRegistry.isComfortBlock(block)) {
                    String group = BlockComfortRegistry.getGroup(block);
                    int value = BlockComfortRegistry.getValue(block);

                    // Update group totals
                    data.groupTotals.put(group, data.groupTotals.getOrDefault(group, 0) + value);

                    // Update block counts (this is new!)
                    data.blockCounts.put(block, data.blockCounts.getOrDefault(block, 0) + 1);
                }
            });
        } catch (ChunkScanner.StopScanException e) {
            // Expected: stop scanning early if fire found
        }

        // Entities
        for (Entity entity : world.loadedEntityList) {
            BlockPos ePos = entity.getPosition();
            if ((ePos.getX() >> 4) == chunkPos.x && (ePos.getZ() >> 4) == chunkPos.z) {
                EntityComfortRegistry.ComfortEntry entry = EntityComfortRegistry.getEntityEntry(entity);
                if (entry != null) {
                    data.groupTotals.put(entry.group, data.groupTotals.getOrDefault(entry.group, 0) + entry.value);
                }
            }
        }

        // Final calculations
        data.totalComfort = data.groupTotals.values().stream().mapToInt(Integer::intValue).sum();
        data.initialized = true;
        data.lastRecalcTick = world.getTotalWorldTime();

        // Save chunk data
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

    public static boolean isPrimaryBlock(IBlockState state) {
        Block block = state.getBlock();

        // Beds → only count FOOT
        if (block instanceof BlockBed) {
            return state.getValue(BlockBed.PART)
                    == BlockBed.EnumPartType.FOOT;
        }

        return true;
    }
}