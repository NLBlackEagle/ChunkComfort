package chunkcomfort.registry;

public class BlockComfortEntry {

    private final String blockId;
    private final int value;
    private final int limit;
    private final String group;

    public BlockComfortEntry(String blockId, int value, int limit, String group) {
        this.blockId = blockId;
        this.value = value;
        this.limit = limit;
        this.group = group;
    }

    public String getBlockId() {
        return blockId;
    }

    public int getValue() {
        return value;
    }

    public int getLimit() {
        return limit;
    }

    public String getGroup() {
        return group;
    }
}