package chunkcomfort.client;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side copy of which comfort requirements the player is missing.
 * Updated by PacketComfortStatus on every server comfort check, read by the item tooltip.
 */
public class ClientComfortStatus {

    // ComfortRequirements.MISSING_* / TOO_* flags
    public static int missingFlags = 0;

    // Statue candles lit in the comfort zone (StatueRegistry.slotKey values) and the player's comfort.
    // Updated by PacketStatueStatus, read by the Comfort hover tooltip and the statue hints.
    public static final Set<Integer> litStatueSlots = new HashSet<>();
    public static int comfort = 0;
}
