package chunkcomfort.chunk;

public class ChunkBossState {

    // server-driven (optional future use)
    public static boolean serverBossActive = false;

    // client-driven (Mixin)
    public static boolean clientBossBarActive = false;

    public static boolean isBossActive() {
        return serverBossActive || clientBossBarActive;
    }

    public static void resetClient() {
        clientBossBarActive = false;
    }
}