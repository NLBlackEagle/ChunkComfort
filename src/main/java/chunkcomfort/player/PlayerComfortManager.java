
package chunkcomfort.player;

import chunkcomfort.chunk.AreaComfortCalculator;
import net.minecraft.entity.player.EntityPlayer;

public class PlayerComfortManager {
    public static void applyComfortEffects(EntityPlayer player) {
        int comfort = AreaComfortCalculator.calculatePlayerComfort(player);

        if (comfort >= 20) {
            System.out.println("Player " + player.getName() + " receives Regeneration I + Speed I!");
        } else {
            System.out.println("Player " + player.getName() + " has comfort: " + comfort);
        }
    }
}