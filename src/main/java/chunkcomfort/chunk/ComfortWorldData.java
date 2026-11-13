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

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ComfortWorldData extends WorldSavedData {

    private final Map<ChunkPos, ChunkComfortData> chunks = new HashMap<>();

    public static final String DATA_NAME = "chunk_comfort";

    /** 2 = raw per-key block counts ("keys"), "groups" holds decorative entity points only. */
    private static final int DATA_FORMAT = 2;

    private static BlockComfortRegistry.BlockKey firstKeyFor(Block block) {
        BlockComfortRegistry.BlockKey wildcard = new BlockComfortRegistry.BlockKey(block, -1);
        if (BlockComfortRegistry.BLOCK_ENTRIES.containsKey(wildcard)) return wildcard;
        for (BlockComfortRegistry.BlockKey key : BlockComfortRegistry.BLOCK_ENTRIES.keySet()) {
            if (key.block == block) return key;
        }
        return null;
    }

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

    // --- Hearth (anchor) index ---
    // REASON: comfort is centred on a hearth now, so we need to find hearths near a player
    // quickly. The old fire check walked every block in the whole area on every comfort
    // check. Instead each chunk is indexed once (a fast pass over its block sections) and
    // then kept current by place/break events. The index is session-only: it is rebuilt
    // lazily after a restart and dropped when a chunk unloads, so it never grows unbounded.
    private final Map<ChunkPos, Set<BlockPos>> anchors = new HashMap<>();
    private int anchorRegistryVersion = -1;

    /**
     * Hearth positions in a chunk, indexing the chunk first if needed. Returns an empty
     * set for chunks that are not loaded (never loads or generates a chunk).
     */
    public Set<BlockPos> getAnchors(World world, ChunkPos chunkPos) {
        if (anchorRegistryVersion != FireBlockRegistry.getVersion()) {
            anchors.clear(); // the Fire Blocks list changed, re-index everything lazily
            anchorRegistryVersion = FireBlockRegistry.getVersion();
        }
        Set<BlockPos> set = anchors.get(chunkPos);
        if (set != null) return set;

        Chunk chunk = world.getChunkProvider().getLoadedChunk(chunkPos.x, chunkPos.z);
        if (chunk == null) return Collections.emptySet();

        set = new HashSet<>();
        int baseX = chunkPos.x << 4;
        int baseZ = chunkPos.z << 4;
        for (ExtendedBlockStorage section : chunk.getBlockStorageArray()) {
            if (section == Chunk.NULL_BLOCK_STORAGE || section.isEmpty()) continue;
            int baseY = section.getYLocation();
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (FireBlockRegistry.isFireBlock(section.get(x, y, z))) {
                            set.add(new BlockPos(baseX + x, baseY + y, baseZ + z));
                        }
                    }
                }
            }
        }
        anchors.put(chunkPos, set);
        return set;
    }

    /** Keeps an already indexed chunk current. Unindexed chunks pick it up when indexed. */
    public void addAnchor(BlockPos pos) {
        Set<BlockPos> set = anchors.get(new ChunkPos(pos));
        if (set != null) set.add(pos.toImmutable());
    }

    public void removeAnchor(BlockPos pos) {
        Set<BlockPos> set = anchors.get(new ChunkPos(pos));
        if (set != null) set.remove(pos);
    }

    /** Forget a chunk's hearths; it is re-indexed the next time it is needed. */
    public void forgetAnchors(ChunkPos chunkPos) {
        anchors.remove(chunkPos);
    }

    public void clearAnchorIndex() {
        anchors.clear();
    }

    public void recalcChunkWithFire(World world, ChunkPos chunkPos) {
        ChunkComfortData data = new ChunkComfortData();
        int minY = 0;
        int maxY = world.getHeight() - 1;

        // REASON: the per-entry limit used to be enforced here, per chunk, so a build
        // spanning several chunks got each limit once per chunk. Now the scan stores raw
        // counts per registered BlockKey (which may have meta=-1 for wildcard entries, so
        // every metadata variant of a wildcard entry shares one counter) and
        // AreaComfortCalculator applies the limit once over the whole comfort area.
        ChunkScanner.scanChunk(world, chunkPos, minY, maxY, (pos, block) -> {
            IBlockState state = world.getBlockState(pos);

            if (!isPrimaryBlock(state)) return;

            BlockComfortRegistry.BlockKey key = BlockComfortRegistry.getKeyForEntry(block, state);
            if (key == null) return;

            data.addBlock(key, block);
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

            NBTTagCompound keysTag = new NBTTagCompound();
            for (Map.Entry<BlockComfortRegistry.BlockKey, Integer> keyEntry : data.keyCounts.entrySet()) {
                ResourceLocation blockName = Block.REGISTRY.getNameForObject(keyEntry.getKey().block);
                if (blockName != null) {
                    keysTag.setInteger(blockName + "@" + keyEntry.getKey().meta, keyEntry.getValue());
                }
            }
            chunkTag.setTag("keys", keysTag);
            chunkTag.setInteger("format", DATA_FORMAT);

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
            boolean currentFormat = chunkTag.getInteger("format") >= DATA_FORMAT;

            // REASON: before format 2, "groups" mixed per-chunk-capped block points with
            // decorative entity points. Block points are now derived from raw key counts, so
            // old group totals would double count blocks and are dropped. Decorative entity
            // points come back on the next rescan of the chunk.
            if (currentFormat) {
                NBTTagCompound groupsTag = chunkTag.getCompoundTag("groups");
                for (String key : groupsTag.getKeySet()) {
                    data.groupTotals.put(key, groupsTag.getInteger(key));
                }
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

            if (currentFormat && chunkTag.hasKey("keys")) {
                NBTTagCompound keysTag = chunkTag.getCompoundTag("keys");
                for (String key : keysTag.getKeySet()) {
                    int at = key.lastIndexOf('@');
                    if (at <= 0) continue;
                    Block block = Block.REGISTRY.getObject(new ResourceLocation(key.substring(0, at)));
                    if (block == null) continue;
                    try {
                        int meta = Integer.parseInt(key.substring(at + 1));
                        data.keyCounts.put(new BlockComfortRegistry.BlockKey(block, meta), keysTag.getInteger(key));
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                // REASON: old saves only have per-Block counts (capped per chunk). Map them to
                // the block's first registered key as a best effort until the chunk is rescanned.
                for (Map.Entry<Block, Integer> blockEntry : data.blockCounts.entrySet()) {
                    BlockComfortRegistry.BlockKey key = firstKeyFor(blockEntry.getKey());
                    if (key != null) data.keyCounts.merge(key, blockEntry.getValue(), Integer::sum);
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
        clearAnchorIndex();
        for (ChunkComfortData data : chunks.values()) {
            data.groupTotals.clear();
            data.keyCounts.clear();
            data.blockCounts.clear();
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