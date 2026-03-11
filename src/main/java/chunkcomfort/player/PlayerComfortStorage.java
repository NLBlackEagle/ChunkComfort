package chunkcomfort.player;

import net.minecraft.entity.player.EntityPlayer;
import java.util.WeakHashMap;

public class PlayerComfortStorage {

    private static final WeakHashMap<EntityPlayer, Float> comfortMap = new WeakHashMap<>();

    public static void setPlayerComfort(EntityPlayer player, float comfort) {
        comfortMap.put(player, comfort);
    }

    public static float getPlayerComfort(EntityPlayer player) {
        return comfortMap.getOrDefault(player, 0f);
    }
}