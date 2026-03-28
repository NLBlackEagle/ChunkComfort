package chunkcomfort.chunk;

public class PettingComfortData {
    public final String entityId;
    public final int comfortBoost;
    public final int maxPettable;
    public final int boostSeconds;
    public final int cooldownSeconds;
    public final boolean tamed;
    public final boolean ownerOnly;
    public final boolean requiresComfortActivation;

    public PettingComfortData(String configLine) {
        // Format: entity,boost,maxPettable,boostSeconds,cooldown,tamed,ownerOnly,requiresActivation
        String[] parts = configLine.split(",");
        this.entityId = parts[0];
        this.comfortBoost = Integer.parseInt(parts[1]);
        this.maxPettable = Integer.parseInt(parts[2]);
        this.boostSeconds = Integer.parseInt(parts[3]);
        this.cooldownSeconds = Integer.parseInt(parts[4]);
        this.tamed = Boolean.parseBoolean(parts[5]);
        this.ownerOnly = Boolean.parseBoolean(parts[6]);
        this.requiresComfortActivation = Boolean.parseBoolean(parts[7]);
    }
}