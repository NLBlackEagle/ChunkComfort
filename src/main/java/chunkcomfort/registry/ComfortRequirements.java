package chunkcomfort.registry;

import chunkcomfort.config.ForgeConfigHandler;

public class ComfortRequirements {

    // Bit flags for requirements that are NOT met, synced to the client so the
    // tooltip can tell the player exactly what their comfort zone is missing.
    public static final int MISSING_SHELTER = 1;
    public static final int MISSING_LIGHT = 1 << 1;
    public static final int MISSING_FIRE = 1 << 2;
    public static final int TOO_HOT = 1 << 3;
    public static final int TOO_COLD = 1 << 4;

    public final boolean shelterOk;
    public final boolean lightOk;
    public final boolean fireOk;
    public final boolean temperatureOk;
    public final double playerTemperature;

    public ComfortRequirements(boolean shelterOk, boolean lightOk, boolean fireOk, boolean temperatureOk, double playerTemperature) {
        this.shelterOk = shelterOk;
        this.lightOk = lightOk;
        this.fireOk = fireOk;
        this.temperatureOk = temperatureOk;
        this.playerTemperature = playerTemperature;
    }

    /**
     * Returns the flag for the first requirement that failed, in the same order
     * ComfortRequirementCheck evaluates them (light → shelter → temperature → fire).
     * Later requirements are skipped once one fails, so their "not ok" value is only
     * a default and must not be reported as missing.
     */
    public int getMissingFlags() {
        if (!lightOk) return MISSING_LIGHT;
        if (!shelterOk) return MISSING_SHELTER;
        if (!temperatureOk) {
            return playerTemperature > ForgeConfigHandler.server.maxComfortTemperature ? TOO_HOT : TOO_COLD;
        }
        if (!fireOk) return MISSING_FIRE;
        return 0;
    }
}
