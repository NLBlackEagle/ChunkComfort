package chunkcomfort.chunk;

import java.util.HashMap;
import java.util.Map;

public class ChunkComfortData {

    public static class GroupData {

        public int limit = 0;
        public Map<Integer, Integer> counts = new HashMap<>();
        public int currentScore = 0;
    }

    public int comfortScore = 0;
    public int fireCount = 0;
    public boolean initialized = false;

    public Map<String, GroupData> groups = new HashMap<>();
}