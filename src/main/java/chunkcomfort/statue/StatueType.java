package chunkcomfort.statue;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

/**
 * The statues ChunkComfort adds. What each candle slot does comes from the Statue Candle
 * Slots config, and where the candles sit comes from assets/chunkcomfort/statues/<name>.json
 * (client side, so resource packs can move or add candle spots).
 */
public enum StatueType {

    ARTISAN("artisan_statue"),
    FORTUNE("fortune_statue"),
    WIND("wind_statue"),
    GUARDIAN("guardian_statue"),
    ELEMENTS("elements_statue"),
    ARCANE("arcane_statue"),
    WARRIOR("warrior_statue"),
    HEALER("healer_statue");

    // Hard cap on candle slots per statue (the statue base has 19 candle spots; the lit slots are an int bitmask, so 32 at most)
    public static final int MAX_SLOTS = 19;

    // Candle size in model pixels; the candle model is centred on the slot position.
    // CANDLE_HEIGHT is the wax height of the candle model and the default slot height.
    public static final double CANDLE_HEIGHT = 4;
    private static final double CANDLE_HALF_WIDTH = 1;
    // Extra height above the wick when aiming at a slot, so short candles are easy to hit.
    // No sideways margin: candles can stand right next to each other.
    private static final double AIM_MARGIN = 0.75;

    public final String name;

    StatueType(String name) {
        this.name = name;
    }

    /** Rotates a slot position (model pixels, statue facing north) to block coordinates (0-1). */
    public static Vec3d rotate(Vec3d slot, EnumFacing facing) {
        // Same clockwise rotation the blockstate applies to the model (y = 90 per step)
        double x = slot.x, z = slot.z, rx, rz;
        switch (facing) {
            case EAST:  rx = 16 - z; rz = x;      break;
            case SOUTH: rx = 16 - x; rz = 16 - z; break;
            case WEST:  rx = z;      rz = 16 - x; break;
            default:    rx = x;      rz = z;      break;
        }
        return new Vec3d(rx / 16.0, slot.y / 16.0, rz / 16.0);
    }

    private static final int[] ROMAN_VALUES = {10, 9, 5, 4, 1};
    private static final String[] ROMAN_SYMBOLS = {"X", "IX", "V", "IV", "I"};

    /** Candle spot number as shown to players (1 -> I, 19 -> XIX). */
    public static String roman(int number) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (number >= ROMAN_VALUES[i]) {
                out.append(ROMAN_SYMBOLS[i]);
                number -= ROMAN_VALUES[i];
            }
        }
        return out.toString();
    }

    /** Box around a candle of the given wax height at the rotated slot position, used for aiming and the highlight. */
    public static AxisAlignedBB candleBox(Vec3d rotatedSlot, double height) {
        double w = CANDLE_HALF_WIDTH / 16.0;
        double h = (height + 1 + AIM_MARGIN) / 16.0;
        return new AxisAlignedBB(rotatedSlot.x - w, rotatedSlot.y, rotatedSlot.z - w,
                rotatedSlot.x + w, rotatedSlot.y + h, rotatedSlot.z + w);
    }
}
