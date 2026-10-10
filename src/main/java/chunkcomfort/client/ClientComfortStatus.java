package chunkcomfort.client;

/**
 * Client-side copy of which comfort requirements the player is missing.
 * Updated by PacketComfortStatus on every server comfort check, read by the item tooltip.
 */
public class ClientComfortStatus {

    // ComfortRequirements.MISSING_* / TOO_* flags
    public static int missingFlags = 0;
}
