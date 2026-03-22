package chunkcomfort.registry;

public class ComfortRequirements {
    public final boolean shelterOk;
    public final boolean lightOk;
    public final boolean fireOk;

    public ComfortRequirements(boolean shelterOk, boolean lightOk, boolean fireOk) {
        this.shelterOk = shelterOk;
        this.lightOk = lightOk;
        this.fireOk = fireOk;
    }
}
