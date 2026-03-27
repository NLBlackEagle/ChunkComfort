package chunkcomfort.registry;

public class ComfortRequirements {
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
}
