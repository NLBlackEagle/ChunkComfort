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
import net.minecraft.util.ResourceLocation;
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

    public static boolean hasFireNearby(World world, BlockPos center, int radius) {
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

        // REASON: ComfortEntry.limit was previously ignored for blocks — all matching
        // blocks were summed unconditionally and only the GroupLimitRegistry cap was
        // applied at the end. This meant e.g. waystones:waystone:2 with limit=1 would
        // still count every waystone in the chunk. Now we track per-BlockKey counts
        // during the scan and stop adding value once the entry limit is reached.
        Map<BlockComfortRegistry.BlockKey, Integer> scanCounts = new java.util.HashMap<>();

        ChunkScanner.scanChunk(world, chunkPos, minY, maxY, (pos, block) -> {
            IBlockState state = world.getBlockState(pos);

            if (!isPrimaryBlock(state)) return;

            BlockComfortRegistry.ComfortEntry entry = BlockComfortRegistry.getBlockEntry(block, state);
            if (entry == null) return;

            // REASON: use the registered BlockKey (which may have meta=-1 for wildcard
            // entries) as the scan counter key, not the actual blockstate metadata.
            // Without this, a wildcard entry like waystones:waystone (no metadata) would
            // give each metadata variant its own counter — both :0 and :2 would pass a
            // limit of 1 independently, counting the structure twice.
            BlockComfortRegistry.BlockKey key = BlockComfortRegistry.getKeyForEntry(block, state);
            if (key == null) return;
            int currentCount = scanCounts.getOrDefault(key, 0);

            if (currentCount >= entry.limit) return; // per-entry limit reached

            scanCounts.put(key, currentCount + 1);
            data.groupTotals.put(entry.group, data.groupTotals.getOrDefault(entry.group, 0) + entry.value);
            data.blockCounts.put(block, data.blockCounts.getOrDefault(block, 0) + 1);
        });

        // REASON: previously iterated world.loadedEntityList (all entities in all loaded
        // chunks) to find decorative entities belonging to this chunk. Replaced with a
        // targeted AABB query scoped to just this chunk, matching the pattern used in
        // addEntityComfort. Significantly cheaper on servers with many loaded entities.
        net.minecraft.util.math.AxisAlignedBB chunkBox = new net.minecraft.util.math.AxisAlignedBB(
                chunkPos.getXStart(), minY, chunkPos.getZStart(),
                chunkPos.getXEnd() + 1, maxY, chunkPos.getZEnd() + 1
        );
        for (Entity entity : world.getEntitiesWithinAABB(Entity.class, chunkBox)) {
            EntityComfortRegistry.ComfortEntry entry = EntityComfortRegistry.getEntityEntry(entity);
            if (entry != null) {
                data.groupTotals.put(entry.group, data.groupTotals.getOrDefault(entry.group, 0) + entry.value);
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

            // REASON: blockCounts was previously not persisted, so after a server restart
            // incremental block placement/break logic (ChunkUpdateManager) started from an
            // empty baseline. Breaking a pre-restart block decremented the count below zero
            // and the group total drifted until the chunk was fully rescanned.
            NBTTagCompound blocksTag = new NBTTagCompound();
            for (Map.Entry<Block, Integer> blockEntry : data.blockCounts.entrySet()) {
                ResourceLocation blockName = Block.REGISTRY.getNameForObject(blockEntry.getKey());
                if (blockName != null) {
                    blocksTag.setInteger(blockName.toString(), blockEntry.getValue());
                }
            }
            chunkTag.setTag("blocks", blocksTag);

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

            // REASON: on first load after this change, "blocks" tag won't exist in saves
            // that predate it. The hasKey check handles that gracefully — blockCounts stays
            // empty and repopulates on the next chunk rescan, same behaviour as before.
            if (chunkTag.hasKey("blocks")) {
                NBTTagCompound blocksTag = chunkTag.getCompoundTag("blocks");
                for (String key : blocksTag.getKeySet()) {
                    Block block = Block.REGISTRY.getObject(new ResourceLocation(key));
                    if (block != null) {
                        data.blockCounts.put(block, blocksTag.getInteger(key));
                    }
                }
            }

            data.totalComfort = data.groupTotals.values().stream().mapToInt(Integer::intValue).sum();
            chunks.put(new ChunkPos(x, z), data);
        }
    }

    public static ComfortWorldData get(World world) {
        assert world.getPerWorldStorage() != null;
        ComfortWorldData data = (ComfortWorldData) world.getPerWorldStorage().getOrLoadData(ComfortWorldData.class, DATA_NAME);
        if (data == null) {
            data = new ComfortWorldData();
            world.getPerWorldStorage().setData(DATA_NAME, data);
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

        try {
            for (net.minecraft.block.properties.IProperty<?> prop : state.getPropertyKeys()) {
                if (prop.getName().equalsIgnoreCase("part")) {
                    String val = state.getValue(prop).toString().toLowerCase();
                    boolean primary = val.equals("foot") || val.equals("bottom");
                    return primary;
                }
            }
        } catch (Exception ignored) {}

        return true;
    }
}